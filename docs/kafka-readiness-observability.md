# Kafka Readiness Observability

Kafka를 도입하기 전에 먼저 확인할 것은 "현재 동기 MSA 구조에서 어떤 문제가 실제로 보이는가"입니다. 이 문서는 주문 흐름을 기준으로 병목, 장애 전파, 재시도 필요성을 관측해서 Kafka 도입 근거를 만드는 절차입니다.

## Current Sync Flow

```text
Client
  -> Gateway
  -> Order API
  -> Auth validate
  -> Stock lookup
  -> Order DB write
  -> Outbox PENDING write
```

현재 주문 API는 요청을 받는 동안 Auth와 Stock을 동기 HTTP 호출로 기다립니다. 그래서 주문 API의 응답 시간과 실패율은 Order 서비스만의 문제가 아니라 Auth, Stock, DB 상태까지 함께 반영합니다.

Kafka를 도입하면 이 흐름을 아래처럼 분리하는 것이 목표입니다.

```text
Order API
  -> Order DB write
  -> Outbox PENDING write
  -> fast response

Outbox Publisher
  -> Kafka OrderCreated
  -> Stock consumer reserves stock
  -> Payment consumer handles payment later
```

## What To Prove Before Kafka

아래 지표 중 하나라도 명확히 보이면 Kafka 도입 근거로 설명하기 좋습니다.

| Signal | Meaning | Kafka로 개선되는 점 |
| --- | --- | --- |
| 주문 API p95/p99 latency 증가 | 동기 호출 체인이 길어질수록 사용자가 오래 기다림 | 주문 접수와 후속 처리를 분리 |
| Stock/Auth 장애 시 주문 API 5xx 증가 | downstream 장애가 사용자 요청 실패로 전파됨 | 이벤트 재시도/보류로 장애 흡수 |
| 요청 spike 때 Stock/Order가 같이 느려짐 | 서비스 간 결합도가 높음 | consumer 처리량을 독립적으로 조절 |
| 같은 요청 재시도/중복 처리 위험 | 네트워크 실패 후 클라이언트 재시도 발생 | idempotency + event key로 중복 방지 |
| `outbox_event` PENDING 누적 | 이미 비동기 발행 경계가 존재함 | Kafka publisher를 붙일 준비 완료 |

## Baseline Test

현재 운영 API 기준으로 주문 생성 latency와 성공률을 먼저 측정합니다.

```bash
REQUESTS=20 ./scripts/load-order-sync.sh
```

조금 더 압박을 주고 싶으면 요청 수를 늘립니다.

```bash
REQUESTS=100 STOCK_ID=1 ./scripts/load-order-sync.sh
```

동시 요청으로 Order/Auth/Stock 동기 호출 체인이 함께 흔들리는지 보려면 `CONCURRENCY`를 지정합니다.

```bash
REQUESTS=100 CONCURRENCY=10 STOCK_ID=1 ./scripts/load-order-sync.sh
```

이 스크립트는 실제 주문을 생성합니다. 운영 DB에서 실행하면 테스트 주문이 남으므로, 면접용 자료를 만들 때는 실행 시간과 idempotency key prefix를 함께 기록해두는 것이 좋습니다.

## Failure Propagation Test

장애 전파를 보여주고 싶으면 운영보다 staging/local 환경에서 짧게 실행합니다.

1. 정상 상태에서 baseline을 기록합니다.
2. Stock 서비스를 잠시 중지하거나 재시작합니다.
3. 같은 부하 테스트를 다시 실행합니다.
4. 5xx 비율, p95/p99 latency, order service log를 비교합니다.

예시:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml restart stock
REQUESTS=20 ./scripts/load-order-sync.sh
```

Stock 재시작 순간에는 Order API가 Stock 응답을 기다리거나 실패할 수 있습니다. 이 결과가 "동기 호출 기반 주문 생성은 downstream 상태에 강하게 묶여 있다"는 근거가 됩니다.

## Metrics To Capture

최소 캡처:

- `scripts/load-order-sync.sh` 결과의 total/success/failure/p95/p99
- `docker compose ps`
- `docker compose logs --tail=100 order`
- `docker compose logs --tail=100 stock`
- `docker stats --no-stream`

다음 단계 캡처:

- Spring Boot Actuator `/actuator/prometheus`
- Prometheus + Grafana dashboard
- Gateway request latency
- Order service request latency
- Feign downstream latency
- JVM memory/GC
- Hikari connection pool
- MySQL slow query

## Monitoring Stack

`compose.deploy.yml`에는 Prometheus와 Grafana가 포함되어 있습니다.

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml up -d --build
```

배포 후 접속:

```text
Prometheus: http://my-mini-pc:9090
Grafana:    http://my-mini-pc:3000
```

Grafana 초기 계정은 `.env.deploy`의 `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` 값을 사용합니다. datasource는 `Prometheus`로 자동 등록됩니다.

Prometheus에서 먼저 확인할 것:

```promql
up
```

서비스별 요청 수:

```promql
sum by (application, status) (rate(http_server_requests_seconds_count[5m]))
```

서비스별 평균 latency:

```promql
sum by (application) (rate(http_server_requests_seconds_sum[5m]))
/
sum by (application) (rate(http_server_requests_seconds_count[5m]))
```

서비스별 p95 latency:

```promql
histogram_quantile(
  0.95,
  sum by (le, application) (rate(http_server_requests_seconds_bucket[5m]))
)
```

## Kafka Adoption Story

면접이나 README에서는 이렇게 설명할 수 있습니다.

```text
초기 주문 생성은 Order 서비스가 Auth/Stock을 동기 호출하는 구조였다.
부하 테스트와 장애 재현 테스트를 통해 주문 API latency와 실패율이 downstream 서비스 상태에 영향을 크게 받는 것을 확인했다.
그래서 주문 접수와 후속 재고/결제 처리를 분리하기 위해 Outbox Pattern을 먼저 적용했고,
다음 단계로 outbox_event를 Kafka에 발행해 Stock/Payment consumer가 독립적으로 처리하도록 설계했다.
```

## AX Extension

Kafka 도입 이후에는 자동화/AX 흐름도 붙이기 쉽습니다.

- Prometheus alert rule로 p95 latency, 5xx rate, outbox PENDING count 감지
- Alertmanager 또는 n8n으로 Slack/Telegram 알림 전송
- 로그/메트릭 요약을 AI에 전달해 "원인 후보, 영향 범위, 확인 명령어" 리포트 생성
- 자동 수정은 바로 적용하지 않고, 처음에는 PR 생성 또는 운영자 승인 방식으로 제한
