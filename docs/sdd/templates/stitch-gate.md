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

Começa quando o dono avisa que rodou o prompt. Checagens do gate: [`st<n>-<assunto>.checks.json`](st<n>-<assunto>.checks.json), escrito junto com este plano ([tipos](../README.md#verificação-automática-sv1)).

1. `node tools/verify-stitch.mjs st<n> --report --out <scratchpad>/stitch-report.html`: acha as telas pelo título exato (tela nova) ou pelo ID (tela antiga), confere as checagens no HTML renderizado a 390 px, a coerência dark × light e as imagens, e gera o relatório com antes/depois e o prompt de correção por tema. Responde `PASSOU` ou `NÃO PASSOU`.
2. `NÃO PASSOU` (inclusive título diferente): **para**, manda o relatório ao dono numa mensagem só e deixa o estado `Aguardando o dono no Stitch`.
3. `PASSOU`: olha o relatório (antes/depois) para os itens de aparência que o script não mede.
4. Adiciona os IDs novos (Registro) a `tools/export-stitch.mjs` e a contagem a `tools/check-stitch.mjs`.
5. `node tools/export-stitch.mjs --only <golds do gate>` + `node tools/check-stitch.mjs` verdes. O filtro de ruído devolve ao git os PNGs sem mudança visual.

### Checklist

Cada item diz a checagem que o cobre, ou "relatório" quando só o olho confere.

- [ ] <item visual verificável> — `text has "<texto>"` / `fits "<texto>"` / relatório

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/<id>.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `st<n>-<assunto>.checks.json` (checagens do gate)
- Inventário de golds em `docs/qa/README.md` (dono único da lista); tabela de nomes em `docs/stitch/README.md`

Nada em `apps/` ou `server/`.

## Registro

- <data> — <o que o dono rodou, IDs novos, resultado do checklist>.
