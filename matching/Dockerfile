FROM openjdk:21-jdk-slim

WORKDIR /app

# Gradle wrapper와 build 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사
COPY src src

# Gradle 빌드 실행
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# JAR 파일 실행
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "build/libs/matching-0.0.1-SNAPSHOT.jar"] 