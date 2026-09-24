# SETUP — torre em casa + grok-cli

Semana 0: R$ 0. Sem VPS. Sem porta aberta no roteador.
O agente edita ESTE repo. Você sobe Docker na torre.

## 10 minutos de mão (uma vez)

Precisa: Git, Flutter, Docker, Python 3.12, grok-cli + `grok login`.

```bash
mkdir nutri && cd nutri
git init
flutter create --org com.nutri --project-name nutri --platforms=android,ios .
```

Cola na RAIZ os arquivos deste kit (AGENTS.md, SETUP.md, GOALS.md, .env.example, .gitignore, docs/, infra/, server/, wires/).

```bash
cp .env.example .env
# edita .env:
# OPENAI_API_KEY=sk-proj-...
# INVITE_CODE=uma-frase-sua
```

Chave: https://platform.openai.com/api-keys

```bash
git add AGENTS.md SETUP.md GOALS.md .env.example .gitignore docs infra server wires
git commit -m "kit: constituição + goals"
# NÃO git add .env
```

## Liga o agente

```bash
cd /caminho/nutri
grok --always-approve --trust \
  --deny 'Read(.env)' \
  --deny 'Read(**/.env)' \
  --deny 'Bash(rm -rf *)' \
  --deny 'Bash(git push --force*)'
```

No TUI: `/goal` + bloco GOAL 1 de GOALS.md. Espere DONE.
Repita GOAL 2, depois GOAL 3. Não cole os três no mesmo /goal.

`/goal status` · `/goal pause` · `/goal resume`
`grok inspect` tem que listar o AGENTS.md deste repo.

--always-approve = --yolo = --dangerously-skip-permissions.
Deny de .env continua valendo.

## Rede da torre

A. Emulador: API_PUBLIC_URL=http://127.0.0.1:8080
B. Celular na mesma Wi-Fi: http://192.168.X.X:8080 — 8080 só na LAN, sem port-forward
C. Amigo fora de casa: Cloudflare Tunnel grátis
   Docs: https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/get-started/

Quick tunnel (URL muda ao reiniciar):
```
cloudflared tunnel --url http://127.0.0.1:8080
```

Named tunnel (estável, o que vale pra APK):
1. dash.cloudflare.com → Zero Trust → Networks → Tunnels → Create → Cloudflared
2. Token no .env: TUNNEL_TOKEN=eyJ...
3. docker compose -f server/docker-compose.yml --profile named-tunnel up -d
4. Public Hostname num domínio que já está na sua conta Cloudflare. O serviço, neste compose, é http://api:8080 (não 127.0.0.1: o túnel roda noutro container).
5. API_PUBLIC_URL=https://seu-hostname e rebuild do app

Luz caiu = API caiu. É o preço de R$ 0.

## Migração futura

docs/MIGRACAO-VPS.md. Não fazer hoje.
