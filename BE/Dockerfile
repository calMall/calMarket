# 1. 빌드 단계 - JDK 사용
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# 2. 실행 단계 - JRE 사용
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]