# site

Este README roteia. Não repete versão, data ou status que outro arquivo possui: linka o dono.

## Propósito

Landing page pública do Fibrai em `fibrai.app`: o que o app faz, telas, Tali, link de política de privacidade e contato.

## Tipo e ownership

- Tipo: `client` (web estático).
- Código principal: o projeto npm `web/` na raiz do repositório ([ADR-038](adrs/ADR-038-web-project-folder.md)). O contexto de documentação continua `site`.
- Consumidores: visitantes públicos. Nenhum dado do usuário entra no site.

## Escopo

- HTML, CSS e JS do site, tokens gerados de `docs/design/tokens.json`, hospedagem Cloudflare e domínios `fibrai.app` e `fibrai.com.br`.

## Fora de escopo

- Layout: Figma `Design`, página "Landing page" ([design](../design/README.md)).
- Texto da política de privacidade e páginas legais: [content-policy](../content-policy/README.md).
- App e server.

## Fronteiras e dependências

- Visual: golds `land`, `landM` e `priv` no [inventário](../qa/README.md#golds), tokens em [docs/tokens.md](../tokens.md).
- Produção: o site não oferece instalação, convite nem coleta de dados enquanto houver bloqueio aberto no [production gate](../content-policy/production-gate.md).

## Como usar esta documentação

Este contexto segue [docs/sdd/README.md](../sdd/README.md).

1. [ADR-037](adrs/ADR-037-landing-site.md) e [ADR-038](adrs/ADR-038-web-project-folder.md);
2. confira os planos ativos abaixo;
3. confira o código atual em `web/`.

## Código e verificação

- `web/public/` é o site servido, sem etapa de build: `index.html` (golds `land` e `landM`, breakpoint 768 px) e `privacidade/index.html` (gold `priv`, `noindex`). Tema claro por padrão, troca escuro/claro guardada no `localStorage`.
- Gerados, nunca editados à mão:
  - `css/tokens.css`, a partir de `docs/design/tokens.json`;
  - `img/screens/`, recorte das telas dos golds `ob3`, `home1` e `chatE`; refaça depois de reexportar esses golds;
  - `img/favicon.svg` e `img/favicon-{32,180,512}.png`, o logo (semente de aveia) de `design/brand/icon.svg`; refaça depois de trocar o logo ([android README § Marca](../android/README.md#marca));
  - `fonts/`, copiadas do pacote Nunito Sans (OFL).
- Comandos (rode `npm --prefix web ci` uma vez antes):
  - `npm --prefix web run preview` serve o site em `http://127.0.0.1:4173/`;
  - `npm --prefix web run check` confere os arquivos gerados, valida o HTML e procura conteúdo proibido pelo ADR-037 § 6;
  - `npm --prefix web run capture` grava as capturas em `docs/qa/site/current/{light,dark}/`, compara com os golds pelo gate de [docs/qa](../qa/README.md#gate) e testa responsivo e tema;
  - `npm --prefix web run lighthouse` mede a acessibilidade (mínimo 95) nas duas páginas e nos dois temas.

## Índice

### ADRs

- [ADR-037](adrs/ADR-037-landing-site.md) — site estático Aero em fibrai.app, Cloudflare.
- [ADR-038](adrs/ADR-038-web-project-folder.md) — o código da landing fica no projeto `web/` na raiz.

### Planos

- Ativos: [W2 — Landing hosting on fibrai.app](plans/w2-landing-hosting.md).
- Concluídos: [`plans/completed/`](plans/completed/).
- Fora de escopo: nenhum.
