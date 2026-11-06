FROM node:22-alpine AS build
WORKDIR /src
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:1.31-alpine
# 憑證在建置映像檔時自簽，只存在本機的映像檔裡
RUN apk add --no-cache openssl \
 && mkdir -p /app/deploy/certs /app/deploy/logs \
 && openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
      -keyout /app/deploy/certs/key.pem -out /app/deploy/certs/cert.pem \
      -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1'
COPY --from=build /src/dist /app/frontend/dist
COPY deploy/nginx.conf /app/deploy/nginx.conf
# 沿用原生部署的設定，只改兩處：監聽所有介面以接收主機轉進來的連線，後端改連 backend 容器
RUN sed -i -e 's/listen 127\.0\.0\.1:/listen /' -e 's#http://127\.0\.0\.1:8080#http://backend:8080#' /app/deploy/nginx.conf
CMD ["nginx", "-p", "/app/deploy", "-c", "nginx.conf", "-g", "daemon off;"]
