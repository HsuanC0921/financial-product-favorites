#!/bin/sh
set -e

# 各容器首次啟動時產生憑證，重啟沿用，不把私鑰放進映像檔
if [ ! -f /app/deploy/certs/cert.pem ]; then
  openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
    -keyout /app/deploy/certs/key.pem -out /app/deploy/certs/cert.pem \
    -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1'
fi
exec "$@"
