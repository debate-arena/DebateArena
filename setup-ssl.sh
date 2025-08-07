#!/bin/bash

# 🔐 SSL 인증서 설정 스크립트 (Let's Encrypt)
# 도메인: debate-arena.duckdns.org

echo "🔐 SSL 인증서 설정을 시작합니다..."

# 도메인 설정
DOMAIN="debate-arena.duckdns.org"
EMAIL="kjt4566@gmail.com"  # 실제 이메일로 변경하세요

# 시스템 업데이트
echo "📦 시스템 패키지 업데이트 중..."
sudo apt update

# Certbot 설치
echo "📦 Certbot 설치 중..."
sudo apt install -y certbot python3-certbot-nginx

# Nginx가 실행 중인지 확인
if ! systemctl is-active --quiet nginx; then
    echo "⚠️ Nginx가 실행되지 않고 있습니다. Nginx를 시작합니다..."
    sudo systemctl start nginx
    sudo systemctl enable nginx
fi

# 기존 SSL 설정이 있는지 확인
if [ -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    echo "⚠️ 이미 $DOMAIN에 대한 SSL 인증서가 존재합니다."
    echo "🔄 인증서를 갱신하시겠습니까? (y/n)"
    read -r RENEW_CERT
    
    if [ "$RENEW_CERT" = "y" ] || [ "$RENEW_CERT" = "Y" ]; then
        echo "🔄 SSL 인증서 갱신 중..."
        sudo certbot renew --nginx --quiet
        echo "✅ SSL 인증서가 갱신되었습니다."
    else
        echo "ℹ️ SSL 인증서 갱신을 건너뜁니다."
    fi
else
    # 새로운 SSL 인증서 발급
    echo "🆕 새로운 SSL 인증서를 발급받습니다..."
    echo "📝 이메일 주소: $EMAIL"
    echo "🌐 도메인: $DOMAIN"
    
    # Let's Encrypt 이용약관 동의 및 인증서 발급
    sudo certbot --nginx \
        --agree-tos \
        --no-eff-email \
        --email "$EMAIL" \
        -d "$DOMAIN" \
        --non-interactive
    
    if [ $? -eq 0 ]; then
        echo "✅ SSL 인증서가 성공적으로 발급되었습니다!"
    else
        echo "❌ SSL 인증서 발급에 실패했습니다."
        echo "🔍 다음을 확인하세요:"
        echo "   1. 도메인이 올바르게 이 서버 IP를 가리키는지 확인"
        echo "   2. 포트 80, 443이 열려있는지 확인"
        echo "   3. Nginx가 정상 실행 중인지 확인"
        exit 1
    fi
fi

# 자동 갱신 설정
echo "🔄 SSL 인증서 자동 갱신 설정 중..."

# crontab에 자동 갱신 스케줄 추가 (매일 새벽 2시에 확인)
CRON_JOB="0 2 * * * /usr/bin/certbot renew --quiet --post-hook 'systemctl reload nginx'"

# 기존 certbot cron job이 있는지 확인
if ! crontab -l 2>/dev/null | grep -q "certbot renew"; then
    (crontab -l 2>/dev/null; echo "$CRON_JOB") | crontab -
    echo "✅ SSL 인증서 자동 갱신이 설정되었습니다. (매일 새벽 2시)"
else
    echo "ℹ️ SSL 인증서 자동 갱신이 이미 설정되어 있습니다."
fi

# Nginx 설정 테스트 및 재시작
echo "🔧 Nginx 설정 테스트 중..."
sudo nginx -t
if [ $? -eq 0 ]; then
    echo "✅ Nginx 설정이 올바릅니다."
    echo "🔄 Nginx 재시작 중..."
    sudo systemctl reload nginx
    echo "✅ Nginx가 성공적으로 재시작되었습니다."
else
    echo "❌ Nginx 설정에 오류가 있습니다."
    exit 1
fi

# SSL 인증서 정보 출력
echo ""
echo "🎉 SSL 설정이 완료되었습니다!"
echo "📋 인증서 정보:"
sudo certbot certificates | grep -A 10 "$DOMAIN"

echo ""
echo "🌐 접속 URL:"
echo "   HTTPS: https://$DOMAIN"
echo "   HTTP: http://$DOMAIN (자동으로 HTTPS로 리다이렉트됨)"

echo ""
echo "🔧 추가 설정:"
echo "   - 인증서 위치: /etc/letsencrypt/live/$DOMAIN/"
echo "   - 자동 갱신: 매일 새벽 2시에 확인"
echo "   - 만료일 확인: sudo certbot certificates"
echo "   - 수동 갱신: sudo certbot renew"

echo ""
echo "✅ 설정 완료! 이제 https://$DOMAIN 으로 접속할 수 있습니다."