# Config Server Feature Flags

Config Server는 비밀번호나 JWT 키 같은 민감정보보다, 운영 중 바꿀 수 있는 정책값과 feature flag를 관리하는 용도로 사용합니다.

## Order Policy

`keupang-order`는 아래 설정을 읽습니다.

```yaml
keupang:
  order:
    create-enabled: true
    max-items-per-order: 10
    stock-reservation-mode: SYNC
```

| Key | Meaning |
| --- | --- |
| `create-enabled` | 주문 생성 API를 받을지 여부 |
| `max-items-per-order` | 한 주문에 담을 수 있는 최대 상품 종류 수 |
| `stock-reservation-mode` | 현재는 `SYNC`만 사용. Kafka 도입 후 `ASYNC` 전환 지점 |

## Where To Put It

Config Server가 바라보는 private config repository에 넣습니다.

예시 파일명:

```text
order.yml
```

또는 profile별로 나누는 경우:

```text
order-dev.yml
order-prod.yml
```

현재 서비스 로그에 `profiles=[dev]`가 보이면 `order-dev.yml` 설정이 적용됩니다. 공통값은 `order.yml`에 두고, 환경별 차이만 `order-dev.yml`, `order-prod.yml`에 둡니다.

## Deployment Flow

1. Config repository에 order policy YAML을 commit/push합니다.
2. Backend repository의 `keupang-order` 변경사항을 commit/push합니다.
3. 기존 Jenkins 흐름으로 prod 배포합니다.
4. order 서비스 로그에서 정상 기동을 확인합니다.

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml logs --tail=80 order
```

## Validation

정상 주문:

```bash
./scripts/test-order-idempotency.sh
```

주문 생성을 잠시 막아보고 싶으면 Config repository에서:

```yaml
keupang:
  order:
    create-enabled: false
    max-items-per-order: 10
    stock-reservation-mode: SYNC
```

그 다음 order 서비스를 재시작합니다.

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml restart order
```

주문 생성 API가 `503 ORDER_CREATE_DISABLED`를 반환하면 Config Server 정책값이 실제로 적용된 것입니다.

다시 정상화할 때는 `create-enabled: true`로 되돌리고 order 서비스를 재시작합니다.

## Kafka Transition Story

Kafka 도입 전:

```yaml
stock-reservation-mode: SYNC
```

Kafka 도입 후:

```yaml
stock-reservation-mode: ASYNC
```

현재 코드는 `ASYNC`를 아직 처리하지 않고 `503 ASYNC_STOCK_RESERVATION_NOT_READY`로 막습니다. Kafka publisher/consumer가 구현되면 이 flag를 기준으로 동기 Stock 호출과 비동기 이벤트 발행을 나누면 됩니다.
