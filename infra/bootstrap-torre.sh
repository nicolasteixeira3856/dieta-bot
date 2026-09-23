#!/usr/bin/env bash
# Roda NA TORRE, uma vez. Não abre porta no roteador.
set -euo pipefail
cd "$(dirname "$0")/.."
test -f .env || { echo "falta .env — cp .env.example .env e cole OPENAI_API_KEY"; exit 1; }
grep -q "OPENAI_API_KEY=sk" .env || { echo "OPENAI_API_KEY vazia no .env"; exit 1; }
docker compose -f server/docker-compose.yml up -d --build api
echo "API local: http://127.0.0.1:8080/health"
echo "Dev público (URL muda): cloudflared tunnel --url http://localhost:8080"
echo "Named tunnel: preencha TUNNEL_TOKEN e: docker compose -f server/docker-compose.yml --profile named-tunnel up -d"
