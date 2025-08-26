#!/bin/bash

echo "🚀 AI 시작 "
echo "=================================="

# 인프라 서비스 시작
docker-compose -f docker-compose.ai.yml down
echo ""
echo "📊 인프라 서비스 상태 확인"
docker-compose -f docker-compose.ai.yml ps
