#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-https://keupang-api.duckdns.org}"
USER_EMAIL="${USER_EMAIL:-catalog-seller@keupang.local}"
USER_PASSWORD="${USER_PASSWORD:-keupang1234!}"
STOCK_ID="${STOCK_ID:-1}"
QUANTITY="${QUANTITY:-1}"
REQUESTS="${REQUESTS:-20}"
CONCURRENCY="${CONCURRENCY:-1}"
KEY_PREFIX="${KEY_PREFIX:-load-order-$(date +%Y%m%d%H%M%S)}"
OUT_FILE="${OUT_FILE:-/tmp/keupang-order-load-${KEY_PREFIX}.csv}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required. Install jq first." >&2
  exit 1
fi

if ! [[ "$REQUESTS" =~ ^[0-9]+$ ]] || [[ "$REQUESTS" -lt 1 ]]; then
  echo "REQUESTS must be a positive integer." >&2
  exit 1
fi

if ! [[ "$CONCURRENCY" =~ ^[0-9]+$ ]] || [[ "$CONCURRENCY" -lt 1 ]]; then
  echo "CONCURRENCY must be a positive integer." >&2
  exit 1
fi

print_step() {
  printf "\n\033[36m==> %s\033[0m\n" "$1"
}

login() {
  local payload response body status
  payload=$(jq -cn --arg userEmail "$USER_EMAIL" --arg userPassword "$USER_PASSWORD" \
    '{userEmail: $userEmail, userPassword: $userPassword}')

  response=$(curl -sS -w '\n%{http_code}' -X POST "$API_BASE_URL/api/user/login" \
    -H 'Content-Type: application/json' \
    --data-raw "$payload")

  body=$(printf '%s' "$response" | sed '$d')
  status=$(printf '%s' "$response" | tail -n 1)

  if [[ "$status" != "200" ]]; then
    echo "Login failed. HTTP $status" >&2
    printf '%s\n' "$body" >&2
    exit 1
  fi

  TOKEN=$(printf '%s' "$body" | jq -r '.data.token // empty')
  if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
    echo "Login response did not contain data.token." >&2
    printf '%s\n' "$body" >&2
    exit 1
  fi
}

run_request() {
  local index="$1"
  local idempotency_key payload response body metrics status total_time order_id message

  idempotency_key="${KEY_PREFIX}-${index}"
  payload=$(jq -cn \
    --argjson stockId "$STOCK_ID" \
    --argjson quantity "$QUANTITY" \
    --arg idempotencyKey "$idempotency_key" \
    '{items: [{stockId: $stockId, quantity: $quantity}], idempotencyKey: $idempotencyKey}')

  response=$(curl -sS -w '\n%{http_code},%{time_total}' -X POST "$API_BASE_URL/api/order" \
    -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $TOKEN" \
    --data-raw "$payload" || true)

  body=$(printf '%s' "$response" | sed '$d')
  metrics=$(printf '%s' "$response" | tail -n 1)
  status="${metrics%%,*}"
  total_time="${metrics##*,}"
  order_id=$(printf '%s' "$body" | jq -r '.data.order.id // empty' 2>/dev/null || true)
  message=$(printf '%s' "$body" | jq -r '.message // .error // .detail // empty' 2>/dev/null || true)

  printf '%s,%s,%s,%s,%s,"%s"\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    "$index" \
    "$status" \
    "$total_time" \
    "$order_id" \
    "${message//\"/\"\"}" >> "$OUT_FILE"

  printf "%4s/%s  HTTP %s  %ss  order=%s\n" "$index" "$REQUESTS" "$status" "$total_time" "${order_id:-"-"}"
}

summarize() {
  awk -F, '
    NR > 1 {
      count += 1
      status[$3] += 1
      if ($3 >= 200 && $3 < 300) success += 1
      else failure += 1
      times[count] = $4 + 0
      sum += times[count]
    }
    END {
      if (count == 0) {
        print "No samples."
        exit
      }
      for (i = 1; i <= count; i++) {
        for (j = i + 1; j <= count; j++) {
          if (times[i] > times[j]) {
            tmp = times[i]
            times[i] = times[j]
            times[j] = tmp
          }
        }
      }
      p50_idx = int((count * 0.50) + 0.999)
      p95_idx = int((count * 0.95) + 0.999)
      p99_idx = int((count * 0.99) + 0.999)
      if (p50_idx < 1) p50_idx = 1
      if (p95_idx < 1) p95_idx = 1
      if (p99_idx < 1) p99_idx = 1
      if (p50_idx > count) p50_idx = count
      if (p95_idx > count) p95_idx = count
      if (p99_idx > count) p99_idx = count

      printf "total=%d success=%d failure=%d\n", count, success, failure
      printf "latency_sec min=%.3f avg=%.3f p50=%.3f p95=%.3f p99=%.3f max=%.3f\n", times[1], sum / count, times[p50_idx], times[p95_idx], times[p99_idx], times[count]
      printf "status_counts="
      first = 1
      for (code in status) {
        if (!first) printf ","
        printf "%s:%d", code, status[code]
        first = 0
      }
      printf "\n"
    }
  ' "$OUT_FILE"
}

print_step "Login"
login
echo "login ok: $USER_EMAIL"

print_step "Run sync order load"
echo "api=$API_BASE_URL stockId=$STOCK_ID quantity=$QUANTITY requests=$REQUESTS concurrency=$CONCURRENCY"
echo "output=$OUT_FILE"
printf 'timestamp,request_no,http_status,time_total_sec,order_id,message\n' > "$OUT_FILE"

active_jobs=0
for i in $(seq 1 "$REQUESTS"); do
  run_request "$i" &
  active_jobs=$((active_jobs + 1))

  if [[ "$active_jobs" -ge "$CONCURRENCY" ]]; then
    wait
    active_jobs=0
  fi
done

if [[ "$active_jobs" -gt 0 ]]; then
  wait
fi

print_step "Summary"
summarize
