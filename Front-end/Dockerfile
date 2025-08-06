# 빌드 스테이지
FROM node:22-alpine AS build

WORKDIR /app

# 의존성 파일 복사
COPY package*.json ./

# 의존성 설치 (빌드에 필요한 devDependencies 포함)
RUN npm ci --prefer-offline --no-audit

# 소스 코드 복사
COPY . .

# 빌드 실행 (.env 파일 포함하여 빌드)
RUN npm run build

# 보안을 위해 .env 파일 제거 (빌드 결과물에만 환경변수가 적용됨)
RUN rm -f .env

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