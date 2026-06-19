# Stage 1: Build the React app using Node 24
FROM node:24-alpine AS builder

WORKDIR /app

# Install dependencies first — this layer is cached unless package*.json changes,
# even when build args (Cognito config, env) change.
COPY package*.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund

# Declare build arguments after npm install so the install layer stays cached
# when only config values change.
ARG APP_ENV
ARG COGNITO_REGION
ARG COGNITO_USER_POOL_ID
ARG COGNITO_USER_POOL_CLIENT_ID
ARG COGNITO_DOMAIN
ARG COGNITO_OAUTH_SCOPES
ARG COGNITO_REDIRECT_SIGN_IN
ARG COGNITO_REDIRECT_SIGN_OUT
ARG VITE_API_BASE_URL

ENV APP_ENV=${APP_ENV} \
    COGNITO_REGION=${COGNITO_REGION} \
    COGNITO_USER_POOL_ID=${COGNITO_USER_POOL_ID} \
    COGNITO_USER_POOL_CLIENT_ID=${COGNITO_USER_POOL_CLIENT_ID} \
    COGNITO_DOMAIN=${COGNITO_DOMAIN} \
    COGNITO_OAUTH_SCOPES=${COGNITO_OAUTH_SCOPES} \
    COGNITO_REDIRECT_SIGN_IN=${COGNITO_REDIRECT_SIGN_IN} \
    COGNITO_REDIRECT_SIGN_OUT=${COGNITO_REDIRECT_SIGN_OUT} \
    VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY . .
RUN npm run build

# Stage 2: Serve static files with Caddy
FROM caddy:2-alpine
COPY --from=builder /app/build /usr/share/caddy
COPY Caddyfile /etc/caddy/Caddyfile

# caddy:2-alpine ships with a built-in 'caddy' user (UID 1000).
# Port 3000 > 1024 so no root privileges are needed to bind.
USER caddy

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost:3000/ || exit 1

EXPOSE 3000

CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]