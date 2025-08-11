FROM openjdk:21-jdk-slim

WORKDIR /app

# 로컬에서 빌드한 JAR 파일만 복사 (정확한 파일명 사용)
COPY build/libs/matching-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

# JVM 옵션 추가로 성능 최적화
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"] 