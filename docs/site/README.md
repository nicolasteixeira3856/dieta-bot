# site

Este README roteia. Não repete versão, data ou status que outro arquivo possui: linka o dono.

## Propósito

Landing page pública do Fibrai em `fibrai.app`: o que o app faz, telas, Tali, link de política de privacidade e contato.

## Tipo e ownership

- Tipo: `client` (web estático).
- Código principal: `site/` (ainda não existe; nasce no [W1](plans/w1-landing-site.md)).
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

1. [ADR-037](adrs/ADR-037-landing-site.md);
2. confira os planos ativos abaixo;
3. confira o código atual em `site/`.

## Índice

### ADRs

- [ADR-037](adrs/ADR-037-landing-site.md) — site estático Aero em fibrai.app, Cloudflare.

### Planos

- Ativos: [W1 — Landing site on fibrai.app](plans/w1-landing-site.md).
- Fora de escopo: nenhum.
