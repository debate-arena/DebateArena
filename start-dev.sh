#!/usr/bin/env bash
set -euo pipefail

echo "🚀 Starting DEV stack (Nginx reverse proxy)"

# Build all Java services first to ensure JARs exist for Docker build
echo "🛠️  Building backend services (bootJar) before docker compose..."
sudo bash ./build-all.sh || { echo "❌ build-all.sh failed"; exit 1; }

echo "🔎 Verifying build artifacts..."
sudo bash ./check-build.sh || true

# Ensure external network exists
if ! docker network ls --format '{{.Name}}' | grep -q '^debate-network$'; then
  echo "🔧 Creating docker network: debate-network"
  docker network create debate-network
fi

echo "🔧 docker compose up (dev)"
docker compose -f docker-compose.dev.yml up -d --build

echo ""
echo "✅ DEV services are starting. Check containers:"
docker compose -f docker-compose.dev.yml ps | cat

cat <<INFO

🔗 Endpoints (via Nginx http://localhost):
  - Arena API:    http://localhost/api/arena
  - Matching API: http://localhost/api/matching
  - Debate API:   http://localhost/api/debate
  - Signaling WS: ws://localhost/signaling

📚 Swagger (proxied):
  - Arena:    http://localhost/api/arena/swagger-ui.html
  - Matching: http://localhost/api/matching/swagger-ui.html
  - Debate:   http://localhost/api/debate/swagger-ui.html

💡 Spring profiles applied in containers: docker,dev
INFO


