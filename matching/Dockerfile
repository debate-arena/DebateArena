FROM openjdk:21-jdk-slim

WORKDIR /app

# 로컬에서 빌드된 Boot JAR을 와일드카드로 복사 (버전 변경 대비)
COPY build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8081

# JVM 옵션 추가로 성능 최적화
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]