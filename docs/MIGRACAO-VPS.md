# Migração futura: torre → VPS

Não executar na semana 0.

Quando for gastar ~€6:
1. Criar CX23 em https://console.hetzner.cloud/ (Ubuntu 24.04, IPv4, SSH key)
2. Copiar o repo + `.env` por scp (não commitar o .env)
3. Mesmo `server/docker-compose.yml`, SEM o serviço cloudflared
4. Caddy + DNS do domínio → :8080
5. API_PUBLIC_URL=https://api.seudominio.com
6. Rebuild do Flutter com a URL nova
7. Desligar o compose na torre

Nada muda no contrato /v1/estimate. Só o host.
