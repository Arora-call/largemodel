# ============================================
# CodeForge 前端 — Nginx 静态资源服务
# ============================================
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 创建自定义 entrypoint，彻底绕过官方 /docker-entrypoint.sh
RUN printf '#!/bin/sh\nexec nginx -g "daemon off;"\n' > /custom-entrypoint.sh \
    && chmod +x /custom-entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/custom-entrypoint.sh"]
