FROM node:22-alpine AS build
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:1.31-alpine
RUN apk add --no-cache openssl \
 && mkdir -p /app/deploy/certs /app/deploy/logs
COPY --from=build /src/dist /app/frontend/dist
COPY deploy/nginx.conf /app/deploy/nginx.conf
COPY deploy/docker/web-entrypoint.sh /app/deploy/web-entrypoint.sh
# 沿用原生部署的設定，只改兩處：監聽所有介面以接收主機轉進來的連線，後端改連 backend 容器
RUN sed -i -e 's/listen 127\.0\.0\.1:/listen /' -e 's#server 127\.0\.0\.1:8080;#server backend:8080;#' /app/deploy/nginx.conf
RUN chown -R nginx:nginx /app/deploy /var/cache/nginx
USER nginx
HEALTHCHECK --interval=5s --timeout=3s CMD curl -kfsS https://127.0.0.1/ > /dev/null
ENTRYPOINT ["sh", "/app/deploy/web-entrypoint.sh"]
CMD ["nginx", "-p", "/app/deploy", "-c", "nginx.conf", "-g", "daemon off;"]
