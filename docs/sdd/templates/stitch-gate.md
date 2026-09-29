# Gate Stitch — ST<n> <Assunto>

- Estado: Aguardando o dono no Stitch
- Data: <DD/MM/AAAA>
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Bloqueia: <planos de implementação que dependem deste gate>

## ⛔ Bloqueio do dono — prompt para o Stitch

Este passo é manual e trava tudo abaixo. O agente não executa este prompt.

### Prompt <n>.1 — <telas>

Antes de enviar: <telas a selecionar (nome + id, dark e light), anexos>.

```text
<prompt exato, em inglês, com a copy pt-BR entre aspas>
```

Depois de enviar: <o que conferir no Stitch; ex.: renomear a tela nova para "chatA — dark">.

## Verificação (agente)

Começa quando o dono avisa que rodou o prompt.

1. `mcp__stitch__list_screens` no projeto `Nutri`: as telas esperadas existem (IDs novos anotados abaixo).
2. Atualiza `tools/export-stitch.mjs` (IDs) e a contagem de `tools/check-stitch.mjs`.
3. `node tools/export-stitch.mjs` + `node tools/check-stitch.mjs` verdes.
4. Confere o checklist nos PNGs exportados, dark e light.
5. Qualquer item falho: **para**, lista o que falta para o dono e deixa o estado `Aguardando o dono no Stitch`.

### Checklist

- [ ] <item visual verificável>

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/<id>.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- Lista de golds em `AGENTS.md` e `docs/qa/README.md`

Nada em `apps/` ou `server/`.

## Registro

- <data> — <o que o dono rodou, IDs novos, resultado do checklist>.
