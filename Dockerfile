# ---- build stage ----
# Используем базовый образ Maven с JDK 17 для сборки
FROM maven:3.9-eclipse-temurin-17 AS build
# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем файл конфигурации Maven
COPY pom.xml .
# Загружаем зависимости проекта заранее
RUN mvn -q -DskipTests dependency:go-offline

# Копируем исходный код приложения
COPY src ./src

# Собираем jar + отдельно зависимости
# Собираем проект, копируем зависимости и выводим список файлов
RUN mvn -q -DskipTests package && \
    mvn -q -DskipTests dependency:copy-dependencies -DoutputDirectory=target/dependency && \
    echo "== target ==" && ls -la target

# ---- runtime stage ----
# Используем легковесный образ JRE для запуска приложения
FROM eclipse-temurin:17-jre
# Устанавливаем рабочую директорию для рантайма
WORKDIR /app

# Копируем скомпилированный jar из стадии сборки
COPY --from=build /app/target/*.jar /app/app.jar
# Копируем библиотеки из стадии сборки
COPY --from=build /app/target/dependency /app/lib

# Запуск веб-приложения: Main на порту 8080
# Открываем порт 8080 для внешнего доступа
EXPOSE 8080
# Команда запуска приложения с указанием classpath
ENTRYPOINT ["sh","-c","java -cp /app/app.jar:/app/lib/* Main"]