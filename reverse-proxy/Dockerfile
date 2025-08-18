## Multi-stage: build Vue app, then serve with Nginx

FROM node:20-alpine AS build-frontend
WORKDIR /app

# 1) deps (레이어 캐시 최적화)
COPY Front-end/Front-end/package*.json ./
RUN npm ci --include=dev

# 2) 소스 복사 및 PATH
ENV PATH="/app/node_modules/.bin:$PATH"
COPY Front-end/Front-end ./

# 3) Vite 빌드 타임 변수
ARG VITE_MAIN_API_URL
ARG VITE_AUTH_API_URL
ARG VITE_API_BASE_URL
ARG VITE_MATCH_API_URL
ARG VITE_MATCH_WS_URL
ARG VITE_STT_API_URL
ARG VITE_STT_WS_URL
ARG VITE_WS_BASE_URL

# 4) 기본값 (빌드 시점에만 사용)
ENV VITE_MAIN_API_URL=${VITE_MAIN_API_URL:-/api/arena}
ENV VITE_AUTH_API_URL=${VITE_AUTH_API_URL:-/api/arena}
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL:-/api}
ENV VITE_MATCH_API_URL=${VITE_MATCH_API_URL:-/api/matching}
ENV VITE_MATCH_WS_URL=${VITE_MATCH_WS_URL:-}
ENV VITE_STT_API_URL=${VITE_STT_API_URL:-/api/debate}
ENV VITE_STT_WS_URL=${VITE_STT_WS_URL:-}
ENV VITE_WS_BASE_URL=${VITE_WS_BASE_URL:-}

# 5) 빌드
RUN npm run build:dev:fast
# 필요 시: RUN npm run build

# ----------------------------------------------------

FROM nginx:1.27-alpine
COPY --from=build-frontend /app/dist /usr/share/nginx/html
COPY reverse-proxy/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
