# 빌드 스테이지
FROM node:20-alpine AS build

WORKDIR /app

# 의존성 파일 복사
COPY package*.json ./

# 의존성 설치
RUN npm ci --only=production

# 소스 코드 복사
COPY . .

# 빌드 실행
RUN npm run build

# 프로덕션 스테이지 - Nginx
FROM nginx:alpine

# 빌드된 파일들을 Nginx 웹 루트로 복사
COPY --from=build /app/dist /usr/share/nginx/html

# Nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 80 포트 노출
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"] 