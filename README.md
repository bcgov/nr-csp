# NRS CSP App

A starting point for NR application teams. Spring Boot 4 backend, React 19 frontend, Oracle DB.

## Prerequisites

- Docker Compose
- VPN (Cisco Secure Client) — required to reach the DB and Jasper server
- `hosts` file entries for the DB and Jasper server — ask your team for the values

## Getting started

**1. Configure your environment**

```bash
cp .env.example .env
```

Open `.env` and fill in the required values:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — DB credentials
- `JASPER_SERVER_USERNAME`, `JASPER_SERVER_PASSWORD` — Jasper server credentials
- `JASPER_IP` — IP address for the Jasper server from your hosts file

> Use single quotes if a value contains special characters: `PASSWORD='p@ss!'`

**2. Connect VPN, then start**

```bash
docker compose up --build
```

| Service  | URL                                   |
|----------|---------------------------------------|
| Frontend | http://localhost:3000                 |
| Backend  | http://localhost:8080                 |
| Swagger  | http://localhost:8080/swagger-ui.html |


## Authentication

By default, authentication is mocked — no Cognito login is required. This is controlled by `VITE_MOCK_USER=true` in your `.env`.

To run with real Cognito auth, set the following in `.env`:

```env
VITE_MOCK_USER=false
AUTH_MOCK_ENABLED=false
JWT_JWKS_URI=https://cognito-idp.ca-central-1.amazonaws.com/{userPoolId}/.well-known/jwks.json
COGNITO_USER_POOL_ID=
COGNITO_USER_POOL_CLIENT_ID=
COGNITO_DOMAIN=
```