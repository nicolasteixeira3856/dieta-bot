# ADR-013 — API hospedada numa VM e2-micro do Google Cloud

- Estado: Aceito (aprovação do plano S5, 2026-09-28)
- Data: 2026-09-28
- Contexto: `server`
- Substitui: parcialmente `AGENTS.md` (item "VPS" em *Do not*) e [MIGRACAO-VPS.md](../../MIGRACAO-VPS.md) (Hetzner CX23 + domínio próprio). A torre com Cloudflare Tunnel continua válida como ambiente de dev.

## Contexto

O dono quer usar o app no celular físico todo dia, no Wi-Fi e no 4G/5G. A torre em casa só alcança o celular na LAN. Tailscale foi descartado pelo dono. O quick tunnel da Cloudflare muda de URL, e o named tunnel exige domínio próprio. `API_PUBLIC_URL` vai dentro do APK, então precisa ser fixo.

O server é um proxy sem estado: não grava disco, a foto entra e sai no request, e o tempo de resposta é dominado pela chamada à OpenAI (EUA). O dono tem o plano Google AI Pro, que dá créditos de Google Cloud, e criou a billing account `Dieta-Bot`.

## Decisão

- A API roda numa VM **e2-micro** do Compute Engine, em `us-east1`, num projeto próprio ligado à billing account `Dieta-Bot`.
- Tamanho e disco ficam dentro do Free Tier: e2-micro + 30 GB `pd-standard` numa região elegível (`us-west1`, `us-central1`, `us-east1`).
- IPv4 externo **estático** para a URL nunca mudar.
- HTTPS via Caddy (Let's Encrypt/ZeroSSL automático) no hostname `<ip-com-hifens>.sslip.io`. Não precisa de domínio.
- O mesmo `server/Dockerfile` roda sem alteração. Um compose próprio em `infra/gcp/` põe o Caddy na frente e não publica a 8080.
- SSH somente por IAP (`35.235.240.0/20`). Só 80/443 ficam abertas para a internet.
- O plano de controle do GCP é operado pelo agente via MCP oficial `@google-cloud/gcloud-mcp`, com o `gcloud` CLI como fallback. `.env` vai por `scp` e nunca é lido nem impresso.

## Motivação

- A URL fica estável sem domínio, sem roteador e sem PC ligado.
- O custo esperado é ~US$ 0–4/mês (VM e disco no Free Tier; o IPv4 em uso é cobrado), coberto pelos créditos do AI Pro.
- `us-east1` é a região elegível mais perto do Brasil. A latência extra é irrelevante perto da chamada ao LLM.
- A operação fica reproduzível por comando, com o agente no mesmo repo.

## Consequências

### Positivas

- O app funciona em qualquer rede, com HTTPS, e o `usesCleartextTraffic` deixa de ser necessário para o uso real.
- O contrato HTTP não muda e `server/` não é tocado.
- A torre continua funcionando para dev e QA local.

### Negativas

- Há um fornecedor de nuvem e uma billing account para vigiar. Alerta de orçamento avisa, mas não corta gasto.
- A VM tem 1 GB de RAM: exige swap para o `pip install` do build.
- O tráfego de saída acima de 1 GB/mês é cobrado, e a foto enviada à OpenAI conta como saída.
- `sslip.io` é um serviço de terceiro para DNS. Se cair, a URL cai. A troca por domínio próprio é um ADR futuro.

## Alternativas consideradas

### Oracle Cloud Always Free

Grátis, com região em São Paulo. Rejeitada pelo dono em favor do GCP por causa dos créditos do AI Pro e da facilidade da conta.

### Hetzner CX23 (MIGRACAO-VPS.md)

~€5/mês, cartão internacional e domínio próprio. Os créditos do GCP zeram esse custo.

### Tailscale / Cloudflare Tunnel com PC ligado

Tailscale foi descartado pelo dono. O tunnel depende da torre ligada, e o named tunnel ainda exige domínio.

### Cloud Run

Escala a zero, mas tem cold start, cobrança por request e limite de tamanho do corpo. Isso é menos previsível que uma VM fixa no Free Tier para um proxy com fotos e timeout de 60 s.

## Relações

- Especificações afetadas: [api-contract.md](../../api-contract.md) (sem mudança de contrato; só o host).
- ADRs relacionados: [001](../../decisions/001-monorepo.md).
- Plano: [S5 deploy GCP](../plans/completed/s5-gcp-deploy.md).
- Contextos consumidores: [android](../../android/README.md) (`API_PUBLIC_URL` no `local.properties`).

Depois de aceito, este ADR não se edita. Mudança posterior exige ADR novo que declare a substituição.
