# Gate Stitch — ST2 Home: atalho de treino

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-020](../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md)
- Bloqueia: [A22 Treino na Home](../../android/plans/a22-treino-home.md)

## ⛔ Bloqueio do dono — prompts para o Stitch

Este passo é manual e trava o A22. São 2 prompts, cada um rodado uma vez por tema.

### Prompt 2.1 — linha de treino abaixo dos macros (home0, home1, homeX)

Antes de enviar, selecione as 3 telas de Home do tema:

| Tela | Dark | Light |
|---|---|---|
| home0 | `9e798987e4794692a03d42e2fb7cb249` | `9f2849ad1ae9413f8ef23ae7777051a0` |
| home1 | `fad15337390b4638b5d51fdac191a010` | `83adedc45ff1441ea398414b7f41dede` |
| homeX | `1df72fbb44824b6a861d334672f31c6c` | `74299a8d5cff46f790fab34d5626b5a7` |

```text
Add one tappable row right below the macros card (P Proteína / C Carboidratos / G Gorduras), 12px gap, before the "LINHA DO TEMPO NUTRICIONAL" section. It uses the same card surface, 1px line border and 14px corner radius as the macros card, full width, 56px tall, 20px horizontal padding.

Row content, left to right:
- A flame icon (outlined, 20px) in the gold accent color.
- The label "Treino de hoje" in the primary text color, 16px, medium weight.
- On the right, the value and a chevron ">" in the muted color.

Value per screen:
- home0 (day zero, nothing logged): value "Informar" in the gold accent color.
- home1: value "350 kcal · +175 na meta" (numbers in primary text color, "· +175 na meta" in muted).
- homeX: value "200 kcal · +100 na meta".

Do not change the ring, the "Meta … kcal" pill, the macros card, the timeline, the disclaimer or the "Chat" button. Keep the bottom spacing so the disclaimer text stays fully visible above the "Chat" button.
```

### Prompt 2.2 — nova tela `homeW`: sheet do treino aberto na Home

Antes de enviar: **duplique** a `home1` do tema (já com a linha do 2.1) e selecione só a cópia.

```text
Show a modal bottom sheet open over this Home screen (dim the Home behind it with the usual scrim).

Sheet: top corners radius 22px, surface background, 24px padding, drag handle on top.
1. Title "Treino de hoje", 20px, semibold, primary text color.
2. One large numeric field (28pt value, like the other number fields of the app) with the value "350" and the suffix "kcal" in muted color. 14px corner radius, 1px line border, focused state with gold border.
3. One line below the field, 14px, muted: "+175 kcal na meta de hoje (compensação 50%)".
4. Two buttons stacked, both full width, 52px tall, same pill shape and same text size (16px, semibold):
   - "Salvar": primary CTA (light fill #f3f5f7 with dark text in dark theme; #111111 fill with light text in light theme).
   - "Cancelar": outlined, surface background, 1px line border, primary text color.
   Exactly like the "Pular refeição" / "Cancelar" pair of the skip-meal dialog.

No other text in the sheet. Keep the Home behind it unchanged.
```

Depois de enviar: renomeie a tela para `homeW — dark` ou `homeW — light`.

## Verificação (agente)

1. `mcp__stitch__list_screens`: IDs das 3 Homes iguais; `homeW` dark e light existem (IDs no Registro).
2. `tools/export-stitch.mjs` + `homeW`; `tools/check-stitch.mjs`: contagem +1 por tema.
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist dark e light. Falhou: **para** e reporta ao dono o que falta.

### Checklist

- [ ] home0/home1/homeX: linha "Treino de hoje" com chama gold logo abaixo dos macros, antes da timeline.
- [ ] home0: valor "Informar" em gold; home1/homeX: "{n} kcal · +{n} na meta".
- [ ] Anel, pílula da meta, macros e timeline sem mudança.
- [ ] Disclaimer inteiro visível acima do botão "Chat".
- [ ] `homeW`: sheet com raio 22 no topo, campo 28pt, uma linha de crédito, Salvar e Cancelar do mesmo tamanho e mesma fonte.
- [ ] Nenhuma outra copy no sheet.
- [ ] Tokens do `AGENTS.md`; dark e light coerentes.

Checklist verde: lista de golds (`AGENTS.md`, `docs/qa/README.md`) com `homeW`, estado `Concluído`, mover para `completed/`, índices e entrega git (`docs/st2-stitch-home-treino`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/{home0,home1,homeX,homeW}.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
