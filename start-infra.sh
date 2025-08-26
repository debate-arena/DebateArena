#!/bin/bash

echo "🚀 인프라 서비스 시작 (MySQL, Redis)"
echo "=================================="

# 인프라 서비스 시작
docker-compose -f docker-compose.infra.yml up -d

echo ""
echo "⏳ 데이터베이스 서비스가 준비될 때까지 대기 중..."
sleep 15

echo ""
echo "📊 인프라 서비스 상태 확인"
docker-compose -f docker-compose.infra.yml ps

echo ""
echo "✅ 인프라 서비스 시작 완료!"
echo ""
echo "🔗 접속 정보:"
echo "   MySQL: localhost:3306"
echo "   - Database: debate_arena"
echo "   - User: root"
echo "   - Password: ssafy"
echo ""
echo "   Redis: localhost:6379"
echo "   - Password: redispass"
echo ""
echo "💡 다음 단계:"
echo "1. 각 서비스를 로컬에서 빌드: ./build-all.sh"
echo "2. 애플리케이션 서비스 시작: ./start-apps.sh"