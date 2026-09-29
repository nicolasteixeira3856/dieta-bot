# Gate Stitch — ST<n> <Assunto>

- Estado: Aguardando o dono no Stitch
- Data: <DD/MM/AAAA>
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Bloqueia: <planos de implementação que dependem deste gate>

Nomes das telas: títulos exatos do Stitch ([tabela](../README.md#nomes-das-telas-regra-do-dono-29092026)). Cada prompt tem um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

## ⛔ Bloqueio do dono — prompt para o Stitch

Este passo é manual e trava tudo abaixo. O agente não executa este prompt.

### Prompt <n>.1 — <o que muda> (`<id de gold>`)

Antes de enviar: <títulos exatos das telas a selecionar, dark e light; ou: duplique "<título>" e renomeie a cópia para **<título final exato>**; anexos>.

**Dark:**

```text
Screen to edit: "<título exato (V2 Expressive)>".

<prompt exato, em inglês, com a copy pt-BR entre aspas; outras telas citadas pelo título exato>
```

**Light:**

```text
Screen to edit: "<título exato (V2 Light)>".

<mesmo prompt, com os títulos e cores do tema claro>
```

Depois de enviar: <o que conferir no Stitch>.

## Verificação (agente)

Começa quando o dono avisa que rodou o prompt.

1. `mcp__stitch__list_screens` no projeto `Nutri`: as telas esperadas existem **com os títulos exatos do gate** (IDs novos anotados abaixo).
2. Atualiza `tools/export-stitch.mjs` (IDs) e a contagem de `tools/check-stitch.mjs`.
3. `node tools/export-stitch.mjs` + `node tools/check-stitch.mjs` verdes.
4. Confere o checklist nos PNGs exportados, dark e light.
5. Qualquer item falho (inclusive título diferente): **para**, lista o que falta para o dono e deixa o estado `Aguardando o dono no Stitch`.

### Checklist

- [ ] <item visual verificável>

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/<id>.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- Lista de golds em `AGENTS.md` e `docs/qa/README.md`; tabela de nomes em `docs/stitch/README.md`

Nada em `apps/` ou `server/`.

## Registro

- <data> — <o que o dono rodou, IDs novos, resultado do checklist>.
