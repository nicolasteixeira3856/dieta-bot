# Plano — S5 Deploy da API no Google Cloud (e2-micro)

- Estado: Concluído
- Data: 28/09/2026
- Contexto proprietário: `server`
- Código afetado: `infra/gcp/` (novo), `tools/deploy-gcp.ps1` (novo), `.mcp.json` (novo), docs. `server/` **não** muda. `apps/android/` **não** muda (só o `local.properties`, que não é versionado).
- Pré-requisitos: [ADR-013](../../adrs/ADR-013-gcp-host.md) aceito junto com a aprovação deste plano.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s5-gcp-deploy.md`. Implemente o plano aprovado.

A aprovação aceita o ADR-013. Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

A API do Nutri no ar 24/7 em `https://<ip-com-hifens>.sslip.io`, numa VM e2-micro do GCP cobrada na billing account `Dieta-Bot`, e um APK no celular do dono apontando para essa URL, funcionando no Wi-Fi e no 4G/5G com o PC desligado.

## Fontes de verdade

- [ADR-013](../../adrs/ADR-013-gcp-host.md) — decisão de host.
- [api-contract.md](../../../api-contract.md) — contrato (inalterado).
- `server/Dockerfile`, `server/config.py`, `server/docker-compose.yml` — como a API sobe hoje.
- `AGENTS.md` — nenhuma chave no APK, `.env` nunca impresso nem commitado.

## Decisões fixas deste plano

| Item | Valor |
|---|---|
| Billing account | `Dieta-Bot` (ID resolvido por `gcloud billing accounts list`) |
| Projeto | nome `Dieta-Bot`, ID `dieta-bot-<6 dígitos>` (IDs são globais; o final é escolhido na hora e registrado aqui) |
| Região / zona | `us-east1` / `us-east1-b` |
| VM | `nutri-api`, `e2-micro`, Ubuntu 24.04 LTS amd64, disco 30 GB `pd-standard`, tag `nutri-api` |
| IP | estático regional `nutri-api-ip` |
| URL | `https://<a-b-c-d>.sslip.io` (IP com hífens) |
| Firewall | `nutri-allow-web` tcp:80,443 de `0.0.0.0/0` → tag `nutri-api`; `nutri-allow-iap-ssh` tcp:22 de `35.235.240.0/20`; apagar `default-allow-ssh` e `default-allow-rdp` |
| Orçamento | `nutri-mensal`, R$ 30 (billing account criada no Brasil, moeda BRL; conferir `currencyCode`), alertas em 50/90/100% por e-mail |
| Convite | `INVITE_CODE` novo e forte (≥ 32 chars aleatórios). O `.env` hoje está no default `troca-isto`, que não pode ir para a internet |
| Layout na VM | `/opt/nutri/{server/, infra/gcp/, .env}`; `.env` com `chmod 600` |
| MCP | `gcloud` → `cmd /c npx -y @google-cloud/gcloud-mcp`, escopo de projeto (`.mcp.json`) |

### Custo esperado (conferir no painel; os preços são do conhecimento do agente)

| Recurso | Free Tier | Esperado |
|---|---|---|
| e2-micro em `us-east1` (720 h/mês) | sim | US$ 0 |
| 30 GB `pd-standard` | sim | US$ 0 |
| IPv4 externo em uso | não | ~US$ 3,65/mês |
| Saída de rede (1 GB/mês grátis; inclui a foto enviada à OpenAI) | parcial | ~US$ 0 no uso de 1 pessoa |
| **Total** | | **~US$ 4/mês, coberto pelos créditos do AI Pro** |

## Divisão de trabalho

Legenda: **[DONO]** = você faz; **[AGENTE]** = eu faço. `!` = comando que você digita no prompt do Claude Code para rodar nesta sessão.

### Fase 0 — Preparação (antes da aprovação ou logo depois dela)

1. **[DONO]** No console (Billing → `Dieta-Bot` → Créditos), confirmar que os créditos do AI Pro / Google Developer Program aparecem **nesta** billing account. Se estiverem em outra, resgate ou transfira para a `Dieta-Bot`.
2. **[DONO]** Login do `gcloud` com a conta Google dona da billing account (abre o navegador):
   ```
   ! gcloud auth login
   ```
   Não rode `gcloud auth application-default login`. Não é necessário, e o `gcloud` instalado veio do Stitch MCP; não mexemos no ADC dele.
3. **[DONO]** Enviar a frase de aprovação deste plano.

### Fase 1 — MCP do GCP

4. **[AGENTE]** Criar `.mcp.json` na raiz:
   ```json
   {
     "mcpServers": {
       "gcloud": {
         "command": "cmd",
         "args": ["/c", "npx", "-y", "@google-cloud/gcloud-mcp"]
       }
     }
   }
   ```
   O servidor MCP usa as credenciais do `gcloud auth login` da Fase 0. Não há chave em arquivo.
5. **[DONO]** Reiniciar o Claude Code nesta pasta, **aprovar** o servidor `gcloud` do projeto quando perguntado e conferir com `/mcp` que ele está `connected`.
6. **[AGENTE]** Sanidade pelo MCP: `gcloud auth list`, `gcloud billing accounts list`. Parar se `Dieta-Bot` não aparecer como `open: True`.

**Divisão MCP × CLI:** plano de controle (projeto, billing, APIs, IP, firewall, VM, orçamento) pelo MCP. SSH/SCP (`gcloud compute ssh|scp --tunnel-through-iap`) direto no PowerShell, porque usa PuTTY no Windows e transfere arquivo local. Se o MCP recusar ou não expuser algum comando, uso o mesmo comando `gcloud` no PowerShell e registro aqui.

### Fase 2 — Projeto, billing e guarda-corpos (AGENTE, via MCP)

7. `gcloud projects create dieta-bot-<nnnnnn> --name="Dieta-Bot"`.
8. `gcloud billing projects link dieta-bot-<nnnnnn> --billing-account=<ID da Dieta-Bot>`.
9. `gcloud config set project dieta-bot-<nnnnnn>`; `compute/region us-east1`; `compute/zone us-east1-b`.
10. Habilitar APIs: `compute.googleapis.com`, `iap.googleapis.com`, `billingbudgets.googleapis.com`.
11. Orçamento `nutri-mensal` (R$ 30, filtro no projeto, alertas 50/90/100%).
    - **[DONO]** Se o comando falhar por permissão ou moeda, criar pelo console (Billing → `Dieta-Bot` → Orçamentos e alertas) com os mesmos valores. Leva 1 minuto.

### Fase 3 — Rede e VM (AGENTE, via MCP)

12. `gcloud compute addresses create nutri-api-ip --region=us-east1` e ler o IP.
13. Firewall: criar `nutri-allow-web` e `nutri-allow-iap-ssh`; apagar `default-allow-ssh` e `default-allow-rdp` (a porta 3389 não serve para nada aqui; a 22 aberta ao mundo é substituída pela regra de IAP).
14. Criar a VM:
    ```
    gcloud compute instances create nutri-api \
      --machine-type=e2-micro --zone=us-east1-b \
      --image-family=ubuntu-2404-lts-amd64 --image-project=ubuntu-os-cloud \
      --boot-disk-size=30GB --boot-disk-type=pd-standard \
      --address=nutri-api-ip --tags=nutri-api \
      --metadata-from-file=startup-script=infra/gcp/startup.sh
    ```
15. `startup.sh` (idempotente, roda a cada boot): cria swap de 1 GB se não existir, instala `docker.io` + `docker-compose-v2` se faltarem, habilita o `docker` no boot, cria `/opt/nutri`.

### Fase 4 — Arquivos novos no repo (AGENTE)

16. `infra/gcp/startup.sh`, descrito no passo 15.
17. `infra/gcp/Caddyfile`:
    ```
    {$PUBLIC_HOST} {
        encode gzip
        reverse_proxy api:8080
    }
    ```
18. `infra/gcp/compose.yml`:
    - `api`: `build: ../../server`, `env_file: ../../.env`, **sem** `ports`, `restart: unless-stopped`, `command` com `uvicorn main:app --host 0.0.0.0 --port 8080 --timeout-keep-alive 75 --proxy-headers --forwarded-allow-ips=*`. O keep-alive ≥ 60 s é exigência do README do server, e os proxy headers fazem o rate limit enxergar o IP real. O `server/Dockerfile` não muda.
    - `caddy`: `caddy:2`, portas `80:80` e `443:443`, `PUBLIC_HOST` vindo do ambiente, volumes `caddy_data` e `caddy_config` (guardam o certificado entre restarts), `restart: unless-stopped`.
19. `tools/deploy-gcp.ps1`, o redeploy de um comando só:
    1. Empacota `server/` (sem `tests/`, `.venv`, `__pycache__`) + `infra/gcp/` com o `tar` do Windows.
    2. `gcloud compute scp --tunnel-through-iap` do pacote para a VM.
    3. `-Env`: envia também o `.env` da raiz para `/opt/nutri/.env` e aplica `chmod 600`. O script nunca lê nem imprime o conteúdo.
    4. `gcloud compute ssh --tunnel-through-iap --command` que extrai o pacote e roda `PUBLIC_HOST=<host> docker compose -f infra/gcp/compose.yml up -d --build`.
    5. Smoke: `GET https://<host>/health` precisa responder 200.
20. Nada de `.env` novo: o `.env` da raiz já tem `OPENAI_API_KEY`, e é ele que vai para a VM. Antes do primeiro deploy, gerar um `INVITE_CODE` aleatório de 32+ chars e gravar o **mesmo** valor na linha `INVITE_CODE=` do `.env` (substituição só dessa linha, sem ler nem imprimir as demais) e em `apps/android/local.properties`. Hoje o `.env` está com `troca-isto` e o `local.properties` não tem a linha, então o APK cai no default do Gradle.

### Fase 5 — Primeiro deploy (AGENTE)

21. Esperar o `startup.sh` terminar (`gcloud compute ssh ... --command "docker --version && swapon --show"`).
    - **[DONO]** Na primeira conexão, o `gcloud` pode abrir uma janela do PuTTY pedindo para aceitar a chave do host. É só clicar **Aceitar**. Ele também gera `~/.ssh/google_compute_engine` sem passphrase; o que já existe em `~/.ssh` não é tocado.
22. `tools/deploy-gcp.ps1 -Env`.
23. Validar: certificado emitido (`/health` em HTTPS, 200), `401` sem `X-Invite` e um `POST /v1/estimate` real com `X-Invite`. O convite é lido do `.env` dentro do script, sem ser ecoado.

### Fase 6 — APK no celular

24. **[AGENTE]** Mostrar a linha `API_PUBLIC_URL=https://<a-b-c-d>.sslip.io` para o `apps/android/local.properties` e conferir que existe a linha `INVITE_CODE=` (sem mostrar o valor). Posso editar o arquivo se você autorizar na hora, porque ele é seu e não é versionado.
25. **[AGENTE]** `./gradlew :app:assembleDebug` em `apps/android/` e informar o caminho do APK.
26. **[DONO]** Instalar no celular, de uma destas formas:
    - Cabo USB com Depuração USB ligada: eu rodo `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
    - Ou copiar o `.apk` para o celular e abrir, permitindo "instalar apps desconhecidos".
27. **[DONO]** Teste real: no **4G com o Wi-Fi desligado**, mandar uma refeição por texto e uma por foto no Chat e conferir que a Home atualiza. Depois repetir no Wi-Fi.
28. **[DONO]** Permitir "Alarmes e lembretes" e tirar o app da otimização de bateria. Em MIUI/HyperOS, liberar também o Início automático.

### Fase 7 — Docs e encerramento (AGENTE)

29. `AGENTS.md`: em *Do not*, trocar `VPS` por `VPS outside ADR-013`; em *Live stack*, adicionar a linha `Host: GCP e2-micro us-east1 + Caddy (ADR-013)`.
30. `docs/MIGRACAO-VPS.md`: marcar como substituído pelo ADR-013 e apontar para o runbook.
31. `docs/server/deploy-gcp.md` (runbook): URL, IDs, como redeployar (`tools/deploy-gcp.ps1`), logs (`docker compose logs`), como trocar o `INVITE_CODE`, como desligar tudo.
32. `SETUP.md` (seção Rede), `docs/server/README.md` (escopo, fora de escopo, índice), `docs/README.md` (matriz + planos): refletir o host.
33. ADR-013 → `Aceito`. Plano → `pending_manual_validation/` até o dono confirmar o passo 27, depois `completed/`.

## Arquivos e áreas afetadas

- Novos: `infra/gcp/startup.sh`, `infra/gcp/Caddyfile`, `infra/gcp/compose.yml`, `tools/deploy-gcp.ps1`, `.mcp.json`, `docs/server/deploy-gcp.md`.
- Editados: `AGENTS.md`, `SETUP.md`, `docs/MIGRACAO-VPS.md`, `docs/server/README.md`, `docs/README.md`, `docs/server/adrs/ADR-013-gcp-host.md` (estado).
- Local, não versionado: `apps/android/local.properties`.
- Fora do repo: projeto GCP `dieta-bot-<nnnnnn>` e seus recursos.

## Validação planejada

1. `gcloud compute instances describe nutri-api` mostra `e2-micro`, `us-east1-b`, `pd-standard` 30 GB, status `RUNNING`.
2. `gcloud compute firewall-rules list` mostra só `nutri-allow-web`, `nutri-allow-iap-ssh`, `default-allow-internal` e `default-allow-icmp`.
3. `curl https://<host>/health` → 200 com certificado válido. `curl http://<ip>:8080/health` → timeout (porta fechada).
4. `POST /v1/estimate` sem `X-Invite` → 401. Com `X-Invite` → 200 no shape do contrato.
5. `sudo reboot` na VM → a API volta sozinha em < 2 min.
6. `server/tests` continuam verdes (`pytest`), como sanidade de que `server/` não mudou.
7. Manual (dono): passo 27, no 4G e no Wi-Fi, texto e foto.

## Fora de escopo

- Qualquer mudança em `server/` ou no contrato HTTP.
- Qualquer mudança de código em `apps/android/`, incluindo assinar o release com a chave de debug. Isso fica para um plano A* separado, se o dono quiser.
- Domínio próprio, Cloud Run, Secret Manager, CI/CD, monitoramento/uptime check, backups (a API não tem estado).
- Desligar a torre ou o Cloudflare Tunnel. Continuam como dev.
- Apagar o projeto GCP. Só com pedido explícito do dono.

## Riscos e controles

- **Cobrança inesperada:** Free Tier só vale para e2-micro + `pd-standard` em região elegível; orçamento com alertas. O alerta não corta gasto, e o dono recebe e-mail.
- **Chave da OpenAI vazar:** `.env` só por `scp` e `chmod 600`, nunca lido nem impresso pelo agente; 8080 não publicada; SSH só via IAP.
- **Convite trafegando em claro:** HTTPS obrigatório via Caddy.
- **Let's Encrypt limitar o `sslip.io`:** o Caddy cai para o ZeroSSL sozinho. Se ambos falharem, parar e voltar ao Planning (domínio próprio).
- **RAM de 1 GB no build:** swap de 1 GB no `startup.sh`.
- **MCP sem algum comando ou pedindo aprovação a cada chamada:** fallback para o `gcloud` no PowerShell, com a mesma credencial.
- **`gcloud` do Stitch MCP:** só `gcloud auth login` (credencial de usuário); o ADC não é tocado.
- **Rate limit atrás do proxy:** `--proxy-headers` repassa o IP real; sem isso, o limite de 30/min seria compartilhado (aceitável para 1 usuário).
- **Keep-alive < timeout do Caddy (502 esporádico):** `--timeout-keep-alive 75`.

## Critérios de aceite

- A API responde em `https://<host>` com certificado válido, 24/7, sem o PC ligado.
- A VM está no Free Tier e o projeto na billing account `Dieta-Bot`, com orçamento e alertas.
- Só as portas 80/443 estão públicas; o SSH é só via IAP.
- O APK debug no celular registra refeição por texto e por foto no 4G e no Wi-Fi.
- `tools/deploy-gcp.ps1` refaz o deploy num comando.
- Docs e `AGENTS.md` refletem o ADR-013.

## Registro de execução

- 28/09/2026 — `.mcp.json` criado; pacote `@google-cloud/gcloud-mcp` 0.5.3 existe no npm. O MCP só carrega após reiniciar a sessão, e o `/goal` rodou na mesma sessão, então o plano de controle foi feito pelo fallback (`gcloud` no PowerShell, mesma credencial).
- Billing account `Dieta-Bot` = `01104C-C14EE5-047BEC`, aberta, BRL.
- Projeto `dieta-bot-703426` criado, ligado à `Dieta-Bot`; `compute`, `iap` e `billingbudgets` habilitadas; defaults `us-east1` / `us-east1-b`.
- `infra/gcp/{startup.sh,Caddyfile,compose.yml}` e `tools/deploy-gcp.ps1` escritos.
- Docs da Fase 7 atualizadas (AGENTS, SETUP, MIGRACAO-VPS, server README, matriz, runbook `docs/server/deploy-gcp.md`).
- A execução parou por falha transitória do verificador de permissões do Claude Code. Foi retomada após o reinício da sessão, já com o MCP `gcloud` carregado.
- Via MCP: orçamento `nutri-mensal` R$ 30 (id `17b24901-39b4-4c2c-aa9f-dc46ca0baa32`, 50/90/100%); IP `nutri-api-ip` = `35.231.53.42`; `nutri-allow-web` e `nutri-allow-iap-ssh` criadas; `default-allow-ssh` e `default-allow-rdp` apagadas; VM `nutri-api` criada.
- `INVITE_CODE` trocado: 40 chars aleatórios (CSPRNG), mesmo valor no `.env` e no `apps/android/local.properties`, nunca impresso. `API_PUBLIC_URL=https://35-231-53-42.sslip.io` no `local.properties`.
- Ajustes no `tools/deploy-gcp.ps1` durante o deploy: `mkdir -p /opt/nutri` antes de instalar o `.env` (o startup ainda não tinha criado a pasta); `ErrorActionPreference=Continue`, porque o PS 5.1 tratava o progresso do docker no stderr como erro (a checagem continua por `$LASTEXITCODE`); grava `infra/gcp/.env` com `PUBLIC_HOST` para `docker compose ps|logs` funcionarem sem variável.
- Certificado TLS emitido na primeira subida (Caddy, automático).

### Resultados da validação

Validação manual (passos 26–28) confirmada pelo dono em 28/09/2026: "Deu certo".

1. VM: `e2-micro`, `us-east1-b`, `RUNNING`, disco `pd-standard` 30 GB, IP `35.231.53.42`, tag `nutri-api`. ✅
2. Firewall: só `default-allow-icmp`, `default-allow-internal`, `nutri-allow-iap-ssh` (22 de `35.235.240.0/20`) e `nutri-allow-web` (80/443). ✅
3. `https://35-231-53-42.sslip.io/health` → 200 `{"ok": true, "model": "gpt-6-luna"}` com TLS válido; `http://35.231.53.42:8080` → timeout; `http://…/health` → 308 para HTTPS. ✅
4. `POST /v1/estimate`: sem `X-Invite` → 401; `troca-isto` → 401; convite novo → 200 (`{"kcal":340,"p":19,"c":29,"g":16,"confidence":"medium",…}`). ✅
5. `sudo reboot`: a VM voltou (boot 14:26:58 UTC), `gcp-api-1` e `gcp-caddy-1` subiram sozinhos, `/health` 200. ✅
6. `server/.venv` `pytest -q` → 43 passed, 19 subtests; `git status server` limpo. ✅
7. APK debug gerado (`apps/android/app/build/outputs/apk/debug/app-debug.apk`); `BuildConfig.API_PUBLIC_URL = "https://35-231-53-42.sslip.io"` e `INVITE_CODE` diferente de `troca-isto`. Instalado pelo dono no celular. ✅

## Encerramento

Depois da implementação, registre resultados reais (ID do projeto, IP, URL, custos vistos) e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
