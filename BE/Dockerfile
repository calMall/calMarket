# 1. Java 21 JDK 이미지 사용 (빌드 단계)
# Java 21의 JDK 이미지를 사용하여 빌드 환경을 구성.
# "AS build"는 이 단계를 build라는 이름으로 지정하여, 후속 단계에서 참조할 수 있도록 합니다.
FROM azul/zulu-openjdk:17-latest

# 빌드 단계에서 작업할 디렉터리를 /app으로 설정합니다.
WORKDIR /app

# 2. 프로젝트 코드 복사 및 빌드
# 현재 디렉터리의 모든 파일을 컨테이너의 /app 디렉터리로 복사합니다.
COPY . .

RUN chmod +x ./gradlew
# Gradle 일 경우 빌드 방법
RUN ./gradlew build -x test

# 3. 실행 단계 (JDK 대신 JRE만 사용하여 경량화)
# Java 21 JRE 이미지를 사용해 실행 환경을 구성합니다.
FROM eclipse-temurin:17-jre

# 실행 단계에서도 작업할 디렉터리를 /app으로 설정합니다.
WORKDIR /app

# 4. 빌드된 JAR 파일 복사

# Gradle 일 경우 경로
COPY --from=build /app/build/libs/*.jar app.jar

# 5. 컨테이너 시작 시 실행할 명령어
# 컨테이너가 시작되면 "java -jar app.jar" 명령어로 애플리케이션을 실행합니다.
CMD ["java", "-jar", "app.jar"]