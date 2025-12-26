# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src

# Собираем jar + отдельно зависимости
RUN mvn -q -DskipTests package && \
    mvn -q -DskipTests dependency:copy-dependencies -DoutputDirectory=target/dependency && \
    echo "== target ==" && ls -la target

# ---- runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar
COPY --from=build /app/target/dependency /app/lib

# Запуск веб-приложения: Main на порту 8080
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -cp /app/app.jar:/app/lib/* Main"]