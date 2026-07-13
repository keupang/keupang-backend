# Mini PC Deploy Checklist

Use this checklist before and after deploying `keupang-backend` on the mini PC.

## 1. Local Validation

Run these checks from the repository root before merging to `prod`.

```bash
./gradlew clean build
docker compose --env-file .env.example -f compose.deploy.yml config
```

Check that the rendered Compose output has these properties:

- Only `caddy` publishes host ports `80` and `443`.
- `gateway` exposes `8080` only inside the Docker network.
- MySQL mounts `./docker/mysql/init` to `/docker-entrypoint-initdb.d`.
- `GATEWAY_PUBLIC_URL`, `API_DOMAIN`, and frontend origins match the target domain plan.

## 2. Mini PC Prerequisites

Confirm the mini PC has:

- Docker and Docker Compose plugin installed.
- Jenkins agent permission to run `docker compose`.
- Jenkins secret file credential `keupang-backend-env`.
- DNS `A` record for `API_DOMAIN` pointing to the home network public IP.
- Router port forwarding for `80` and `443` to the mini PC.
- Mini PC firewall allowing inbound `80` and `443`.

## 3. Deploy Commands

Jenkins reads `Jenkinsfile` from its own workspace, then runs checkout/build/deploy from the mini PC deploy directory:

```bash
/home/cj8556/keupang-backend
```

The deploy directory is the source of truth for manual operations. Keep the production env file here:

```bash
/home/cj8556/keupang-backend/.env.deploy
```

Jenkins runs these commands from `Jenkinsfile` inside the deploy directory:

```bash
./gradlew clean build
docker compose --env-file .env.deploy -f compose.deploy.yml build
docker compose --env-file .env.deploy -f compose.deploy.yml up -d --remove-orphans
docker compose --env-file .env.deploy -f compose.deploy.yml ps
```

For manual deploy testing, use a real env file with production values:

```bash
cd /home/cj8556/keupang-backend
docker compose --env-file .env.deploy -f compose.deploy.yml config
docker compose --env-file .env.deploy -f compose.deploy.yml up -d --build
```

If Jenkins cannot write to `/home/cj8556/keupang-backend`, give both the login user and Jenkins access to the same deploy group:

```bash
sudo groupadd -f keupang-deploy
sudo usermod -aG keupang-deploy cj8556
sudo usermod -aG keupang-deploy jenkins
sudo chgrp -R keupang-deploy /home/cj8556/keupang-backend
sudo chmod -R g+rwX /home/cj8556/keupang-backend
sudo find /home/cj8556/keupang-backend -type d -exec chmod g+s {} \;
sudo systemctl restart jenkins
```

## 4. Container Status

Check that every service is running:

```bash
cd /home/cj8556/keupang-backend
docker compose --env-file .env.deploy -f compose.deploy.yml ps
```

Expected externally reachable container:

- `keupang-caddy`

Expected internal-only containers:

- `keupang-gateway`
- `keupang-eureka-server`
- `keupang-config-server`
- `keupang-auth`
- `keupang-user`
- `keupang-product`
- `keupang-stock`
- `keupang-review`
- `keupang-order`
- `keupang-mysql`
- `keupang-redis`

## 5. Eureka Registration

From the mini PC, verify that Eureka is reachable inside the Docker network:

```bash
docker compose -f compose.deploy.yml exec eureka-server sh -c 'hostname'
docker compose -f compose.deploy.yml logs --tail=100 eureka-server
```

Then check the Eureka dashboard from inside the network if needed:

```bash
docker run --rm --network keupang-network curlimages/curl:8.8.0 \
  -u "$security_username:$security_password" \
  http://eureka-server:8761/eureka/apps
```

Registered apps should include:

- `AUTH`
- `USER`
- `PRODUCT`
- `STOCK`
- `REVIEW`
- `ORDER`
- `KEUPANG-GATEWAY`

## 6. Config Server

Check Config Server logs first:

```bash
docker compose -f compose.deploy.yml logs --tail=100 config-server
```

Common failure points:

- `private_key` is missing or not valid for the config repository.
- Config repository SSH access is not allowed from the mini PC.
- Config Server basic auth does not match `security_username` and `security_password`.

## 7. Database

Check MySQL health and service databases:

```bash
docker compose -f compose.deploy.yml ps mysql
docker compose -f compose.deploy.yml exec mysql mysql -uroot -p"$DB_PASSWORD" -e "SHOW DATABASES;"
```

Expected databases:

- `keupang_user`
- `keupang_product`
- `keupang_stock`
- `keupang_review`
- `keupang_order`

If databases are missing, confirm this is the first initialization of the `mysql-data` volume. MySQL only runs `/docker-entrypoint-initdb.d` scripts when the data directory is empty.

For an existing mini PC MySQL volume, create the order database manually before deploying the order service:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml exec mysql \
  mysql -uroot -p"$DB_PASSWORD" \
  -e "CREATE DATABASE IF NOT EXISTS keupang_order DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"
```

After deploying the order service, confirm idempotency columns and unique constraints exist:

```bash
docker compose --env-file .env.deploy -f compose.deploy.yml exec mysql \
  mysql -uroot -p"$DB_PASSWORD" \
  -e "SHOW COLUMNS FROM keupang_order.orders LIKE 'idempotency_key'; SHOW COLUMNS FROM keupang_order.orders LIKE 'request_hash'; SHOW INDEX FROM keupang_order.orders WHERE Key_name = 'uk_orders_user_idempotency_key';"
```

## 8. Gateway API

Check the public Gateway endpoint:

```bash
curl -I "$GATEWAY_PUBLIC_URL/swagger-ui.html"
```

Check the Gateway container logs:

```bash
docker compose -f compose.deploy.yml logs --tail=100 gateway
```

Check Caddy HTTPS and proxy logs:

```bash
docker compose -f compose.deploy.yml logs --tail=100 caddy
```

## 9. Troubleshooting

Use service-specific logs:

```bash
docker compose -f compose.deploy.yml logs --tail=200 auth
docker compose -f compose.deploy.yml logs --tail=200 user
docker compose -f compose.deploy.yml logs --tail=200 product
docker compose -f compose.deploy.yml logs --tail=200 stock
docker compose -f compose.deploy.yml logs --tail=200 review
docker compose -f compose.deploy.yml logs --tail=200 order
```

Use restart only after checking logs:

```bash
docker compose -f compose.deploy.yml restart gateway
docker compose -f compose.deploy.yml restart auth user product stock review order
```

For Caddy certificate issues, check:

- DNS points to the correct public IP.
- Router forwards both `80` and `443`.
- No other process on the mini PC is using `80` or `443`.
- `API_DOMAIN` and `GATEWAY_PUBLIC_URL` use the same API domain.
