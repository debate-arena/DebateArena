FROM node:20-bullseye

# 필수 패키지 설치
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    make \
    g++ \
    && ln -s /usr/bin/python3 /usr/bin/python \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

# UDP 포트는 docker-compose에서 지정
EXPOSE 40000-40100/udp


CMD ["npm", "start"]
