# Stage 1: Build the React app using Node 24
FROM node:24-alpine AS builder

WORKDIR /app

# Install dependencies first — this layer is cached unless package*.json changes,
# even when build args (Cognito config, env) change.
COPY package*.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund

# Cognito config is injected at runtime via OpenShift ConfigMap (not baked in),
# so only Vite-compiled env vars need to be declared here as build args.
ARG VITE_API_BASE_URL
ARG VITE_MOCK_USER

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_MOCK_USER=${VITE_MOCK_USER}

COPY . .
RUN npm run build

# Stage 1b: Vite dev server with hot-module reloading. Used only by
# docker-compose.override.yml for local development. At runtime the host source
# tree is bind-mounted over /app and an anonymous volume preserves this image's
# node_modules; file watching uses polling (VITE_USE_POLLING=true) so edits made
# on the host are detected inside the container across the Docker boundary.
FROM node:24-alpine AS dev
WORKDIR /app
COPY package*.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund
EXPOSE 3000
CMD ["npm", "run", "start"]

# Stage 2: Serve static files with Caddy
FROM caddy:2-alpine

# caddy:2-alpine sets CAP_NET_BIND_SERVICE as a file capability on /usr/bin/caddy
# so it can bind to port 80/443 without root. OpenShift's restricted SCC with
# allowPrivilegeEscalation=false refuses to exec any binary that has file
# capabilities. Strip it — we listen on port 3000 so the capability is unneeded.
RUN apk add --no-cache libcap && \
    setcap -r /usr/bin/caddy && \
    apk del libcap && \
    addgroup -S caddy && adduser -S -G caddy caddy && \
    chown caddy:caddy /data /config

COPY --from=builder --chown=caddy:caddy /app/build /usr/share/caddy
COPY --chown=caddy:caddy Caddyfile /etc/caddy/Caddyfile

USER caddy

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost:3000/ || exit 1

EXPOSE 3000

CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]
