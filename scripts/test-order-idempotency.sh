#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-https://keupang-api.duckdns.org}"
USER_EMAIL="${USER_EMAIL:-catalog-seller@keupang.local}"
USER_PASSWORD="${USER_PASSWORD:-keupang1234!}"
STOCK_ID="${STOCK_ID:-1}"
QUANTITY="${QUANTITY:-1}"
CONFLICT_QUANTITY="${CONFLICT_QUANTITY:-2}"
IDEMPOTENCY_KEY="${IDEMPOTENCY_KEY:-test-order-$(date +%Y%m%d%H%M%S)}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required. Install jq first." >&2
  exit 1
fi

pass() {
  printf "\033[32mPASS\033[0m %s\n" "$1"
}

fail() {
  printf "\033[31mFAIL\033[0m %s\n" "$1" >&2
  exit 1
}

step() {
  printf "\n\033[36m==> %s\033[0m\n" "$1"
}

request_json() {
  local method="$1"
  local url="$2"
  local data="${3:-}"
  local auth_header="${4:-}"

  local response
  if [[ -n "$data" && -n "$auth_header" ]]; then
    response=$(curl -sS -w '\n%{http_code}' -X "$method" "$url" \
      -H 'Content-Type: application/json' \
      -H "$auth_header" \
      --data-raw "$data")
  elif [[ -n "$data" ]]; then
    response=$(curl -sS -w '\n%{http_code}' -X "$method" "$url" \
      -H 'Content-Type: application/json' \
      --data-raw "$data")
  elif [[ -n "$auth_header" ]]; then
    response=$(curl -sS -w '\n%{http_code}' -X "$method" "$url" \
      -H "$auth_header")
  else
    response=$(curl -sS -w '\n%{http_code}' -X "$method" "$url")
  fi

  HTTP_BODY="$(printf '%s' "$response" | sed '$d')"
  HTTP_STATUS="$(printf '%s' "$response" | tail -n 1)"
}

step "Login"
login_payload=$(jq -cn --arg userEmail "$USER_EMAIL" --arg userPassword "$USER_PASSWORD" \
  '{userEmail: $userEmail, userPassword: $userPassword}')
request_json POST "$API_BASE_URL/api/user/login" "$login_payload"
[[ "$HTTP_STATUS" == "200" ]] || fail "login returned HTTP $HTTP_STATUS: $HTTP_BODY"
TOKEN="$(printf '%s' "$HTTP_BODY" | jq -r '.data.token // empty')"
[[ -n "$TOKEN" && "$TOKEN" != "null" ]] || fail "login response did not contain data.token"
pass "login succeeded"

step "JWT Payload"
payload_json=$(TOKEN="$TOKEN" python3 - <<'PY'
import base64, json, os
token = os.environ["TOKEN"]
payload = token.split(".")[1]
payload += "=" * (-len(payload) % 4)
print(json.dumps(json.loads(base64.urlsafe_b64decode(payload)), ensure_ascii=False))
PY
)
printf '%s\n' "$payload_json" | jq .

step "Validate Token"
request_json POST "$API_BASE_URL/api/auth/validate" "" "Authorization: Bearer $TOKEN"
[[ "$HTTP_STATUS" == "200" ]] || fail "validate returned HTTP $HTTP_STATUS: $HTTP_BODY"
printf '%s\n' "$HTTP_BODY" | jq .
pass "token is valid"

order_payload=$(jq -cn \
  --argjson stockId "$STOCK_ID" \
  --argjson quantity "$QUANTITY" \
  --arg idempotencyKey "$IDEMPOTENCY_KEY" \
  '{items: [{stockId: $stockId, quantity: $quantity}], idempotencyKey: $idempotencyKey}')

step "Create Order"
request_json POST "$API_BASE_URL/api/order" "$order_payload" "Authorization: Bearer $TOKEN"
[[ "$HTTP_STATUS" == "201" ]] || fail "create order returned HTTP $HTTP_STATUS: $HTTP_BODY"
FIRST_ORDER_ID="$(printf '%s' "$HTTP_BODY" | jq -r '.data.order.id')"
FIRST_ORDER_NUMBER="$(printf '%s' "$HTTP_BODY" | jq -r '.data.order.orderNumber')"
printf '%s\n' "$HTTP_BODY" | jq '.data.order | {id, orderNumber, userEmail, totalPrice, status, items}'
pass "created order id=$FIRST_ORDER_ID orderNumber=$FIRST_ORDER_NUMBER"

step "Replay Same Idempotency Key"
request_json POST "$API_BASE_URL/api/order" "$order_payload" "Authorization: Bearer $TOKEN"
[[ "$HTTP_STATUS" == "201" ]] || fail "replay returned HTTP $HTTP_STATUS: $HTTP_BODY"
REPLAY_ORDER_ID="$(printf '%s' "$HTTP_BODY" | jq -r '.data.order.id')"
REPLAY_ORDER_NUMBER="$(printf '%s' "$HTTP_BODY" | jq -r '.data.order.orderNumber')"
printf '%s\n' "$HTTP_BODY" | jq '.data.order | {id, orderNumber, userEmail, totalPrice, status, items}'
[[ "$REPLAY_ORDER_ID" == "$FIRST_ORDER_ID" ]] || fail "replay created different order id=$REPLAY_ORDER_ID"
[[ "$REPLAY_ORDER_NUMBER" == "$FIRST_ORDER_NUMBER" ]] || fail "replay returned different orderNumber=$REPLAY_ORDER_NUMBER"
pass "same request returned same order"

conflict_payload=$(jq -cn \
  --argjson stockId "$STOCK_ID" \
  --argjson quantity "$CONFLICT_QUANTITY" \
  --arg idempotencyKey "$IDEMPOTENCY_KEY" \
  '{items: [{stockId: $stockId, quantity: $quantity}], idempotencyKey: $idempotencyKey}')

step "Reuse Same Key With Different Request"
request_json POST "$API_BASE_URL/api/order" "$conflict_payload" "Authorization: Bearer $TOKEN"
printf '%s\n' "$HTTP_BODY" | jq .
[[ "$HTTP_STATUS" == "409" ]] || fail "conflict check expected HTTP 409 but got $HTTP_STATUS"
pass "different request with same key was rejected"

step "Summary"
jq -n \
  --arg apiBaseUrl "$API_BASE_URL" \
  --arg idempotencyKey "$IDEMPOTENCY_KEY" \
  --arg orderId "$FIRST_ORDER_ID" \
  --arg orderNumber "$FIRST_ORDER_NUMBER" \
  '{apiBaseUrl: $apiBaseUrl, idempotencyKey: $idempotencyKey, orderId: ($orderId | tonumber), orderNumber: $orderNumber, result: "PASS"}'
