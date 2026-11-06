# 建置時不跑測試：整合測試要連資料庫，建置階段還沒有資料庫
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY backend/ .
RUN --mount=type=cache,target=/root/.m2 sh mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system app
COPY --from=build /src/target/financial-product-favorites-0.0.1-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
