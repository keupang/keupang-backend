# Reverse Proxy Deployment

## Mini PC network checklist

- Point the API DNS record to the public IP that reaches the mini PC network.
- Forward router ports `80` and `443` to the mini PC.
- Allow inbound `80` and `443` on the mini PC firewall.
- Keep Jenkins private. Do not route public traffic to Jenkins.

## Runtime flow

```text
Vercel frontend
  -> https://api.example.com
  -> Caddy :443
  -> keupang-gateway:8080
  -> internal services
```

Only Caddy publishes host ports. Gateway and internal services stay on the Docker network.

## Required env values

Set these values in the Jenkins secret env file before deployment:

```text
API_DOMAIN=api.example.com
CADDY_EMAIL=admin@example.com
GATEWAY_PUBLIC_URL=https://api.example.com
FRONTEND_PUBLIC_ORIGIN=https://www.example.com
FRONTEND_ROOT_ORIGIN=https://example.com
```
