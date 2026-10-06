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
| Na VM | `/opt/nutri/{server/, infra/gcp/, logs/, .env}` — `.env` root:root 600; `infra/gcp/.env` só com `PUBLIC_HOST` (interpolação do compose) |

## Redeploy

```powershell
./tools/deploy-gcp.ps1          # código (server/ + infra/gcp/)
./tools/deploy-gcp.ps1 -Env     # código + .env.deploy da raiz (vira /opt/nutri/.env na VM)
```

Duas chaves de modelo, dois arquivos na raiz do repositório, ambos ignorados pelo git:

| Arquivo | `OPENAI_API_KEY` | Quem lê |
| --- | --- | --- |
| `.env` | chave de avaliação | testes, `evals.run`, servidor local |
| `.env.deploy` | chave do app (só a VM dev) | `./tools/deploy-gcp.ps1 -Env` |

As outras linhas (`INVITE_CODE`, `SAFETY_ID_SECRET`, `SERVER_ENV` etc.) são iguais nos dois; ao mudar uma, mude nas duas. O script recusa um `.env.deploy` sem chave. Motivo: a moderação do provedor tem teto diário de requisições por chave; a avaliação não pode esgotar a cota do app.

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

## Log de conversa (dev)

[ADR-015](adrs/ADR-015-log-conversa-dev.md). Ligado pelo `infra/gcp/compose.yml` (`CONVERSATION_LOG_PATH=/data/conversations.jsonl`, volume `/opt/nutri/logs`, `chmod 700`). Uma linha JSON por chamada ao modelo. Rotação: 20 MB × 5 pelo servidor e 30 dias por idade pelo `logrotate` do host (`/etc/logrotate.d/nutri`, escrito pelo `startup.sh`; CP5).

```powershell
./tools/pull-conversations.ps1                  # últimas 20 linhas
./tools/pull-conversations.ps1 -RequestId <id>  # uma chamada (id do header X-Request-Id)
./tools/pull-conversations.ps1 -Download        # copia para logs/ (gitignored)
```

Campos: `ts`, `request_id`, `route`, `app_version`, `app_env`, `prompt`, `input_text`, `has_photo`, `photo_b64_chars`, `raw_output`, `error`, `response`, `fallback`, `latency_ms`. Nunca a foto, o convite ou a chave.

## Ingress e logs (CP5)

[CP5](../content-policy/plans/completed/cp5-gcp-dev-ingress.md). Só o Caddy publica portas (80/443); `api:8080` fica na rede `edge` do compose.

- Rede `edge` com sub-rede fixa `172.30.53.0/28`. O uvicorn confia em `X-Forwarded-For` só dessa faixa (`--forwarded-allow-ips`). Nunca `*`, nem RFC1918 inteiro, nem em rollback.
- O Caddy fica sem `trusted_proxies`: substitui o `X-Forwarded-For` do cliente pelo IP do par. `Forwarded` e `X-Real-IP` passam, mas o servidor não os lê.
- Logs Docker: `json-file` 10 MB × 3 por container. Conferir: `sudo docker inspect --format '{{.Name}} {{.HostConfig.LogConfig.Config}}' $(sudo docker ps -q)`.
- `conversations.jsonl`: `logrotate` diário, 30 arquivos, `maxage 30`, `copytruncate`. O `lastaction` apaga backups de tamanho do servidor (`.1`..`.5`) com mais de 30 dias. Dry run: `sudo logrotate -d /etc/logrotate.d/nutri`.
- `startup.sh` mora no metadata da VM (cópia). Mudou o arquivo no repo:

```powershell
gcloud compute instances add-metadata nutri-api --zone us-east1-b --metadata-from-file startup-script=infra/gcp/startup.sh
gcloud compute ssh nutri-api --zone us-east1-b --tunnel-through-iap --command "sudo google_metadata_script_runner startup"
```

## Segredo do safety identifier

`SAFETY_ID_SECRET` (32 bytes aleatórios, hex) e `SERVER_ENV=dev` ficam no `.env` e no `.env.deploy` da raiz e chegam à VM por `./tools/deploy-gcp.ps1 -Env` (root:root 600). Nunca imprimir o valor, commitar, nem rodar `docker compose config` sem `-q` na VM (o resolvido expõe o `.env`). `/health` mostra `safety_id: on`.

Trocar o segredo muda todos os pseudônimos (`safety_identifier`). Só com motivo (vazamento) e registro.

## Trocar o INVITE_CODE

Só se houver abuso observado ou vazamento do convite.

1. Gravar o novo valor na linha `INVITE_CODE=` do `.env` e do `.env.deploy` da raiz **e** na linha `dev.INVITE_CODE=` de `apps/android/local.properties`. Não imprimir nem commitar.
2. `./tools/deploy-gcp.ps1 -Env`.
3. Novo build de teste pelo A16: `./tools/distribute-dev.ps1 -Notes <notas.md>` (versão sobe sozinha). Os testers atualizam pelo Firebase App Tester.
4. Conferir: o convite antigo recebe 401 e o novo passa.

```powershell
# 401 esperado com o convite antigo (sem chamada ao modelo)
Invoke-WebRequest -Method Post -Uri https://35-231-53-42.sslip.io/v1/estimate -Headers @{ "X-Invite" = "<antigo>" } -ContentType application/json -Body '{"text":"x"}' -UseBasicParsing
```

## Desligar

- Pausar: `gcloud compute instances stop nutri-api --zone us-east1-b`. O IP estático reservado e parado é cobrado.
- Apagar tudo: `gcloud projects delete dieta-bot-703426`. Só com pedido explícito do dono.

## Limites conhecidos

- A torre + Cloudflare Tunnel continuam como dev.
- `sslip.io` resolve o hostname. Se cair, a URL cai. Domínio próprio exige ADR novo.
- Tráfego de saída acima de 1 GB/mês é cobrado; a foto enviada à OpenAI conta.
