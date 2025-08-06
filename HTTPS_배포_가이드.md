# 🚀 HTTPS 배포 가이드

## 📋 개요

이 가이드는 debate-arena.duckdns.org 도메인으로 HTTPS 접속이 가능한 자동 배포 시스템 설정 방법을 설명합니다.

## 🏗️ 설정된 구조

### 브랜치별 배포 전략
- **develop 브랜치**: 자동 배포 (Jenkins 감지)
- **master 브랜치**: 운영 서버 배포
- **fe/ 브랜치**: 개발 서버 배포

### 도메인 설정
- **도메인**: debate-arena.duckdns.org
- **HTTP**: 자동으로 HTTPS로 리다이렉트
- **HTTPS**: SSL/TLS 암호화 적용

## 🔧 설정 파일

### 1. Nginx 설정
- `Front-end/nginx-dev.conf`: 개발용 HTTPS 설정
- `Front-end/nginx.conf`: 운영용 HTTPS 설정

### 2. Jenkins 설정
- `Jenkinsfile`: develop 브랜치 자동 배포 및 HTTPS URL 반영

### 3. SSL 설정
- `setup-ssl.sh`: SSL 인증서 자동 발급 및 갱신 스크립트

## 🚀 배포 프로세스

### 1단계: 개발 및 커밋
```bash
# 각자 브랜치에서 개발
git checkout -b fe/feature-name
# 개발 작업...
git add .
git commit -m "feat: 새로운 기능 추가"
git push origin fe/feature-name
```

### 2단계: develop 브랜치로 머지 (사용자 담당)
```bash
# Pull Request 생성 후 develop 브랜치로 머지
```

### 3단계: Jenkins 자동 배포 (자동)
- develop 브랜치 머지 감지
- 프론트엔드 빌드 실행
- **SSL 인증서 존재 여부 자동 감지**
  - SSL 있음: HTTPS로 배포
  - SSL 없음: HTTP로 임시 배포
- Docker 컨테이너로 안전한 배포

## 🔐 SSL 인증서 설정

### 초기 SSL 설정 (서버에서 한 번만 실행)
```bash
# 이메일 주소를 실제 이메일로 수정 후 실행
sudo nano setup-ssl.sh  # EMAIL 변수 수정
sudo bash setup-ssl.sh
```

### SSL 인증서 수동 갱신
```bash
sudo certbot renew
sudo systemctl reload nginx
```

### SSL 인증서 상태 확인
```bash
sudo certbot certificates
```

## 🌐 접속 URL

### 개발 환경
- **HTTPS**: https://debate-arena.duckdns.org
- **디버그**: https://debate-arena.duckdns.org/debug

### API 엔드포인트
- **API**: https://debate-arena.duckdns.org/api
- **WebSocket**: wss://debate-arena.duckdns.org/ws

## 🔍 트러블슈팅

### SSL 인증서 문제
```bash
# 인증서 상태 확인
sudo certbot certificates

# 강제 갱신
sudo certbot renew --force-renewal

# Nginx 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl reload nginx
```

### Docker 배포 실패 시
```bash
# Docker 컨테이너 상태 확인
docker ps -a

# 컨테이너 로그 확인
docker logs debate-arena-frontend-dev

# 컨테이너 재시작
docker restart debate-arena-frontend-dev

# 이미지 확인
docker images | grep debate-arena

# 포트 사용 현황 확인
sudo netstat -tlnp | grep :80
sudo netstat -tlnp | grep :443
```

### 배포 실패 시
1. **Jenkins 로그 확인** (가장 중요)
2. Docker 컨테이너 상태 확인: `docker ps -a`
3. 컨테이너 로그 확인: `docker logs debate-arena-frontend-dev`
4. 포트 충돌 확인: `sudo netstat -tlnp | grep :80`
5. SSL 인증서 경로 확인: `/etc/letsencrypt/live/debate-arena.duckdns.org/`

### 도메인 접속 불가 시
1. DNS 전파 확인: https://dnschecker.org/
2. 방화벽 확인 (포트 80, 443 열려있는지)
3. 서버 IP와 도메인 연결 상태 확인
4. Docker 컨테이너 실행 상태 확인

## ⚡ 자동화된 기능

### Jenkins 자동 배포
- develop 브랜치 머지 시 자동 트리거
- 빌드 → Docker 컨테이너 배포 자동 실행
- **SSL 인증서 존재 여부 자동 감지 및 설정 적용**
- 배포 결과 로그로 확인 가능

### SSL 인증서 자동 갱신
- 매일 새벽 2시에 자동 갱신 확인
- 만료 30일 전 자동 갱신
- 갱신 후 Nginx 자동 재시작

### 단계적 HTTPS 전환
1. **1차 배포**: HTTP로 임시 배포 (SSL 인증서 없을 때)
2. **SSL 설정**: setup-ssl.sh 실행으로 인증서 발급
3. **2차 배포**: 자동으로 HTTPS 설정 적용

## ✅ 확인 사항

### 배포 성공 확인
1. https://debate-arena.duckdns.org 접속
2. SSL 인증서 유효성 확인 (브라우저 자물쇠 아이콘)
3. API 엔드포인트 동작 확인
4. 디버그 페이지에서 서버 정보 확인

### 보안 헤더 확인
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block
- Strict-Transport-Security: max-age=31536000

## 📞 문의사항

배포 또는 SSL 설정 관련 문제가 있을 경우:
1. Jenkins 대시보드에서 빌드 로그 확인
2. 서버 로그 확인 (`/var/log/nginx/`)
3. SSL 인증서 상태 확인 (`sudo certbot certificates`)

---

🎉 **축하합니다!** 이제 https://debate-arena.duckdns.org 로 안전하게 접속할 수 있습니다.