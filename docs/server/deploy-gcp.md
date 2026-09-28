# Deploy GCP — runbook

Decisão: [ADR-013](adrs/ADR-013-gcp-host.md). Plano de origem: [S5](plans/completed/s5-gcp-deploy.md).

## Recursos

| Item | Valor |
|---|---|
| Billing account | `Dieta-Bot` (`01104C-C14EE5-047BEC`, BRL) |
| Projeto | `dieta-bot-703426` |
| Região / zona | `us-east1` / `us-east1-b` |
| VM | `nutri-api` — e2-micro, Ubuntu 24.04, 30 GB `pd-standard`, tag `nutri-api` |
| IP estático | `nutri-api-ip` — `35.231.53.42` |
| URL da API | `https://35-231-53-42.sslip.io` |
| Firewall | `nutri-allow-web` (80/443 público), `nutri-allow-iap-ssh` (22 só do IAP `35.235.240.0/20`) |
| Orçamento | `nutri-mensal` — R$ 30/mês, alertas 50/90/100% |
| Na VM | `/opt/nutri/{server/, infra/gcp/, .env}` — `.env` root:root 600; `infra/gcp/.env` só com `PUBLIC_HOST` (interpolação do compose) |

## Redeploy

```powershell
./tools/deploy-gcp.ps1          # código (server/ + infra/gcp/)
./tools/deploy-gcp.ps1 -Env     # código + .env da raiz
```

O script empacota, envia por IAP, sobe `docker compose -f infra/gcp/compose.yml up -d --build` e espera `GET /health` = 200 em HTTPS.

## Operação

```powershell
# shell na VM
gcloud compute ssh nutri-api --zone us-east1-b --tunnel-through-iap
# logs
gcloud compute ssh nutri-api --zone us-east1-b --tunnel-through-iap --command "cd /opt/nutri && sudo docker compose -f infra/gcp/compose.yml logs --tail 100"
```

`startup.sh` roda a cada boot (swap 1 GB, Docker). Os containers voltam sozinhos (`restart: unless-stopped`). Logo após um reboot, o SSH pode falhar por alguns segundos até o `sshd` subir.

Na primeira conexão, o `gcloud` gera `~/.ssh/google_compute_engine` e grava a chave no metadata do projeto.

## Trocar o INVITE_CODE

1. Gravar o novo valor na linha `INVITE_CODE=` do `.env` da raiz **e** em `apps/android/local.properties`.
2. `./tools/deploy-gcp.ps1 -Env`.
3. Rebuild e reinstalação do APK (`./gradlew :app:assembleDebug`, `adb install -r`).

## Desligar

- Pausar: `gcloud compute instances stop nutri-api --zone us-east1-b`. O IP estático reservado e parado é cobrado.
- Apagar tudo: `gcloud projects delete dieta-bot-703426`. Só com pedido explícito do dono.

## Limites conhecidos

- A torre + Cloudflare Tunnel continuam como dev.
- `sslip.io` resolve o hostname. Se cair, a URL cai. Domínio próprio exige ADR novo.
- Tráfego de saída acima de 1 GB/mês é cobrado; a foto enviada à OpenAI conta.
