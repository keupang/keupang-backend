# keupang-backend

쿠팡을 모티브로 한 MSA 구조의 E-Commerce 백엔드 프로젝트입니다. Spring Cloud 기반의 Gateway, Eureka, Config Server와 도메인별 마이크로서비스를 Docker Compose로 구성하고, 미니 PC 환경에서 Jenkins CI/CD와 Caddy HTTPS reverse proxy를 통해 배포합니다.

## Architecture

> TODO: 이 영역에 Canva/PPT 등으로 제작한 아키텍처 이미지를 추가할 예정입니다.

```text
Vercel Frontend
  -> HTTPS API Domain
  -> Caddy Reverse Proxy
  -> Spring Cloud Gateway
  -> Eureka / Config Server
  -> Auth / User / Product / Stock / Review / Order
  -> MySQL / Redis / AWS S3
```

## Services

| Service | Role |
| --- | --- |
| `keupang-gateway` | 외부 API 진입점, CORS, Swagger aggregation, service routing |
| `keupang-eureka-server` | 마이크로서비스 service discovery |
| `keupang-config-server` | private config repository 기반 중앙 설정 관리 |
| `keupang-auth` | 인증, JWT 발급/검증, Redis 기반 인증 상태 관리 |
| `keupang-user` | 회원 도메인, 메일 인증 |
| `keupang-product` | 상품 기본 정보, 상품 이미지 S3 업로드 |
| `keupang-stock` | 재고/판매 상품 도메인, 상품/리뷰 연동 조회 |
| `keupang-review` | 리뷰 도메인 |
| `keupang-order` | 주문 도메인, 주문 생성 및 Outbox 이벤트 저장 |
| `mysql` | 서비스별 DB schema 저장소 |
| `redis` | 인증/캐시성 데이터 저장소 |
| `caddy` | HTTPS 인증서 자동 발급 및 Gateway reverse proxy |

## Tech Stack

- Java 17
- Spring Boot
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Cloud Config
- Spring Security / JWT
- Spring Data JPA
- MySQL 8
- Redis
- AWS S3
- Docker / Docker Compose
- Caddy
- Jenkins

## Deployment Overview

운영 배포는 `compose.deploy.yml` 기준으로 수행합니다.

- 외부로 직접 열리는 컨테이너는 `caddy`뿐입니다.
- Caddy가 `80`, `443` 포트를 열고 HTTPS 인증서를 자동 발급합니다.
- Gateway와 내부 서비스들은 Docker network 안에서만 통신합니다.
- MySQL과 Redis는 Docker volume에 데이터를 보관합니다.
- Jenkins는 `prod` 브랜치를 checkout한 뒤 Gradle build와 Docker Compose 배포를 수행합니다.

배포 흐름:

```text
prod branch push/merge
  -> GitHub Webhook
  -> Jenkins Pipeline
  -> ./gradlew clean build
  -> docker compose build
  -> docker compose up -d
```

## Data Pipeline

상품 데이터는 `data-pipeline`에서 Keupang 서비스 형식으로 가공할 수 있습니다.

파이프라인 역할:

- 원천 CSV 데이터 검증
- 상품 대표/상세 이미지 다운로드
- 프론트 표시 위치에 맞춘 이미지 variant 생성
- S3 prefix 규칙에 맞춘 업로드
- `keupang_product`, `keupang_stock` DB 적재용 `seed.sql` 생성

자세한 사용법은 `data-pipeline/README.md`를 참고합니다.

## MSA Consistency Roadmap

주문 도메인은 MSA 분산 데이터 일관성 확장을 위한 첫 단계로 Outbox Pattern을 적용합니다.

현재 단계:

```text
Order API
  -> userEmail + idempotencyKey로 중복 요청 확인
  -> orders / order_item 저장
  -> outbox_event에 OrderCreated 저장
```

주문 생성은 `idempotencyKey`를 필수로 받습니다. 같은 사용자가 같은 `idempotencyKey`와 같은 주문 내용을 다시 보내면 기존 주문을 반환하고, 같은 key로 다른 주문 내용을 보내면 `409 CONFLICT`로 거절합니다. 이 처리는 사용자의 더블 클릭뿐 아니라 "서버는 저장에 성공했지만 클라이언트가 응답을 받지 못해 재시도하는 상황"을 안전하게 다루기 위한 장치입니다.

주문 데이터와 이벤트 데이터는 같은 DB 트랜잭션 안에서 저장됩니다. 다음 단계에서는 `outbox_event.status = PENDING`인 이벤트를 Kafka로 발행하고, Stock/Payment 서비스가 이벤트를 구독해 재고 예약과 결제 흐름을 처리하도록 확장할 예정입니다.

주문 생성과 멱등성 동작은 아래 스크립트로 한 번에 확인할 수 있습니다.

```bash
./scripts/test-order-idempotency.sh
```

기본 대상은 `https://keupang-api.duckdns.org`이며, 필요하면 환경변수로 바꿀 수 있습니다.

```bash
API_BASE_URL=http://localhost:8080 \
USER_EMAIL=catalog-seller@keupang.local \
USER_PASSWORD='keupang1234!' \
STOCK_ID=1 \
./scripts/test-order-idempotency.sh
```

Kafka 도입 전 현재 동기 주문 흐름의 latency와 실패율을 측정하려면 아래 스크립트를 사용합니다. 이 스크립트는 실제 주문을 생성하므로 운영 데이터에서 실행할 때는 테스트 주문이 남는 점을 감안합니다.

```bash
REQUESTS=20 ./scripts/load-order-sync.sh
```

동시 요청으로 동기 호출 체인의 흔들림을 보고 싶으면 `CONCURRENCY`를 함께 지정합니다.

```bash
REQUESTS=100 CONCURRENCY=10 ./scripts/load-order-sync.sh
```

Kafka 도입 근거를 만드는 관측 절차는 `docs/kafka-readiness-observability.md`에 정리되어 있습니다.

Config Server는 주문 feature flag와 운영 정책값 관리에 사용합니다. 자세한 적용 방법은 `docs/config-server-feature-flags.md`를 참고합니다.

## Required Environment

실제 배포 환경에서는 repository root에 `.env.deploy`를 준비하거나 Jenkins의 Secret file credential로 등록합니다. 민감 정보가 포함되므로 `.env.deploy`는 Git에 commit하지 않습니다.

기본 예시는 `.env.example`을 참고합니다.

### Spring / Service Auth

| Key | Description |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Docker 배포용 profile. 기본값은 `docker` |
| `security_username` | Eureka / Config Server basic auth username |
| `security_password` | Eureka / Config Server basic auth password |
| `private_key` | Config Server가 private config repo에 접근하기 위한 SSH private key. 보통 base64 single-line 값 사용 |

### Database / Redis

| Key | Description |
| --- | --- |
| `DB_HOST` | Docker 배포에서는 `service-mysql` 권장 |
| `DB_PORT` | MySQL port. 기본 `3306` |
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password. 최초 volume 생성 시 root password로 초기화됨 |
| `USER_DB_NAME` | User service database name |
| `PRODUCT_DB_NAME` | Product service database name |
| `STOCK_DB_NAME` | Stock service database name |
| `REVIEW_DB_NAME` | Review service database name |
| `ORDER_DB_NAME` | Order service database name |
| `REDIS_HOST` | Docker 배포에서는 `service-redis` 권장 |
| `REDIS_PORT` | Redis port. 기본 `6379` |

MySQL 초기 schema는 `docker/mysql/init/01-create-service-databases.sql`에서 생성합니다. 단, MySQL init script는 Docker volume이 비어 있는 최초 실행 시점에만 실행됩니다.

### Mail / JWT / S3

| Key | Description |
| --- | --- |
| `google_username` | 메일 발송용 Gmail 주소 |
| `google_password` | Gmail App Password |
| `jwt_private_key` | JWT 서명용 private key |
| `jwt_public_key` | JWT 검증용 public key |
| `AWS_S3_BUCKET` | S3 bucket name |
| `AWS_S3_ACCESS_KEY` | S3 접근용 IAM access key |
| `AWS_S3_SECRET_KEY` | S3 접근용 IAM secret key |
| `AWS_S3_REGION` | S3 region. 예: `ap-northeast-2` |

### Public URL / CORS

| Key | Description |
| --- | --- |
| `GATEWAY_PUBLIC_URL` | 외부에서 접근하는 Gateway HTTPS URL. 예: `https://keupang-api.duckdns.org` |
| `API_DOMAIN` | Caddy가 인증서를 발급받을 API domain |
| `CADDY_EMAIL` | Let's Encrypt 인증서 발급용 이메일 |
| `FRONTEND_LOCAL_ORIGIN` | 로컬 프론트 개발 origin |
| `FRONTEND_PUBLIC_ORIGIN` | 배포된 프론트 origin |
| `FRONTEND_ROOT_ORIGIN` | root domain을 따로 쓰는 경우의 프론트 origin |

## Local Validation

배포 전 전체 build를 확인합니다.

```bash
./gradlew clean build
```

Docker Compose 설정이 올바르게 렌더링되는지 확인합니다.

```bash
docker compose --env-file .env.example -f compose.deploy.yml config
```

## Manual Deployment

미니 PC 또는 배포 서버에서 실제 환경 값을 담은 `.env.deploy`를 준비합니다.

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml up -d --build
```

컨테이너 상태 확인:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml ps
```

로그 확인:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml logs --tail=200 gateway
docker compose --env-file .env.deploy -f compose.deploy.yml logs --tail=200 caddy
```

외부 API 확인:

```bash
curl -I https://keupang-api.duckdns.org/swagger-ui.html
```

## Jenkins CI/CD

Jenkins Pipeline은 `Jenkinsfile`을 사용합니다.

필요한 Jenkins credential:

| ID | Kind | Usage |
| --- | --- | --- |
| `github-key` | Username with password | GitHub repository checkout. Password에는 GitHub PAT 사용 |

Jenkins job 설정:

```text
Definition: Pipeline script from SCM
SCM: Git
Repository URL: https://github.com/keupang/keupang-backend.git
Credentials: github-key
Branches to build: */prod
Script Path: Jenkinsfile
Build Trigger: GitHub hook trigger for GITScm polling
```

Jenkins는 자체 workspace에서 Docker Compose를 실행하지 않고, 미니 PC의 운영 repo인 `/home/cj8556/keupang-backend`를 기준으로 배포합니다.

```text
Jenkins workspace
  -> Jenkinsfile 읽기
  -> /home/cj8556/keupang-backend checkout/pull
  -> /home/cj8556/keupang-backend 에서 build/deploy
```

운영 env 파일은 미니 PC에 한 번만 배치합니다.

```bash
/home/cj8556/keupang-backend/.env.deploy
```

Jenkins 서버는 Docker 명령을 실행해야 하므로 `jenkins` 사용자가 `docker` 그룹에 포함되어 있어야 합니다.

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

또한 Jenkins가 운영 repo를 갱신하고 빌드 산출물을 만들 수 있어야 하므로 `/home/cj8556/keupang-backend`에 대한 쓰기 권한이 필요합니다. 권한 문제로 checkout/build가 실패하면 미니 PC에서 다음처럼 배포용 그룹을 만들어 `cj8556`과 `jenkins`가 함께 접근하게 합니다.

```bash
sudo groupadd -f keupang-deploy
sudo usermod -aG keupang-deploy cj8556
sudo usermod -aG keupang-deploy jenkins
sudo chgrp -R keupang-deploy /home/cj8556/keupang-backend
sudo chmod -R g+rwX /home/cj8556/keupang-backend
sudo find /home/cj8556/keupang-backend -type d -exec chmod g+s {} \;
sudo systemctl restart jenkins
```

## Network Checklist

미니 PC 배포 기준:

- DuckDNS 또는 DNS record가 현재 공인 IP를 가리켜야 합니다.
- 공유기 port forwarding이 필요합니다.
  - `80 -> mini PC:80`
  - `443 -> mini PC:443`
  - Jenkins webhook을 직접 받을 경우 `8080 -> mini PC:8080`
- 서버 방화벽에서 필요한 포트를 허용해야 합니다.
- Jenkins `8080`을 외부에 직접 노출하는 경우 관리자 계정, webhook secret, 접근 제어를 별도로 관리해야 합니다.

## API Documentation

Swagger UI:

```text
https://keupang-api.duckdns.org/swagger-ui.html
```

Gateway Swagger UI는 각 서비스의 OpenAPI 문서를 모아서 보여줍니다. 일부 내부 API 또는 보안상 감추고 싶은 API는 `@Hidden`으로 Swagger에서 제외할 수 있습니다.

## Useful Commands

서비스 상태:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml ps
```

전체 로그:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml logs --tail=200
```

특정 서비스 로그:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml logs --tail=200 product
```

재배포:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml up -d --build
```

중지:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml down
```

## Related Docs

- `docs/deploy-checklist.md`
- `docs/reverse-proxy-deploy.md`
- `.env.example`
- `compose.deploy.yml`
- `Jenkinsfile`
