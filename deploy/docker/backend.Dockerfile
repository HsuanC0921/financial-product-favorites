# 建置時不跑測試：整合測試要連資料庫，建置階段還沒有資料庫
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY backend/ .
RUN --mount=type=cache,target=/root/.m2 sh mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
# 套用基底映像尚未包含的系統安全更新
RUN apt-get update && apt-get upgrade -y \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system app
COPY --from=build /src/target/financial-product-favorites-1.0.0.jar /app/app.jar
USER app
EXPOSE 8080
# 未登入時應回 401，代表 API 已可接受請求
HEALTHCHECK --interval=5s --timeout=3s --start-period=10s CMD curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/me | grep -qx 401
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
