# Gate Stitch — ST2 Home: atalho de treino

- Estado: Concluído
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-020](../../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md)
- Bloqueia: [A22 Treino na Home](../../../android/plans/completed/a22-treino-home.md)

Nomes das telas: títulos exatos do Stitch ([tabela](../../README.md#nomes-das-telas-regra-do-dono-29092026)). Cada prompt tem um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

## ⛔ Bloqueio do dono — prompts para o Stitch

Este passo é manual e trava o A22. São 2 prompts, cada um enviado duas vezes (dark e light), nesta ordem.

### Prompt 2.1 — linha de treino abaixo dos macros

Antes de enviar, selecione as 3 telas de Home do tema:

| Dark | Light |
|---|---|
| Home vazia - Day 1 (V2 Expressive Timeline) | Home vazia - Day 1 (V2 Light Timeline) |
| Home no dia - 1300 kcal (V2 Expressive Timeline) | Home no dia - 1300 kcal (V2 Light Timeline) |
| Home meta excedida - 2280 / 2000 kcal (V2 Expressive Timeline) | Home meta excedida - 2280 / 2000 kcal (V2 Light Timeline) |

**Dark:**

```text
Screens to edit: "Home vazia - Day 1 (V2 Expressive Timeline)", "Home no dia - 1300 kcal (V2 Expressive Timeline)", "Home meta excedida - 2280 / 2000 kcal (V2 Expressive Timeline)".

Add one tappable row right below the macros card (P Proteína / C Carboidratos / G Gorduras), 12px gap, before the "LINHA DO TEMPO NUTRICIONAL" section. It uses the same card surface, 1px line border and 14px corner radius as the macros card, full width, 56px tall, 20px horizontal padding.

Row content, left to right:
- A flame icon (outlined, 20px) in the gold accent color.
- The label "Treino de hoje" in the primary text color, 16px, medium weight.
- On the right, the value and a chevron ">" in the muted color.

Value per screen:
- "Home vazia - Day 1 (V2 Expressive Timeline)": value "Informar" in the gold accent color.
- "Home no dia - 1300 kcal (V2 Expressive Timeline)": value "350 kcal · +175 na meta" (numbers in primary text color, "· +175 na meta" in muted).
- "Home meta excedida - 2280 / 2000 kcal (V2 Expressive Timeline)": value "200 kcal · +100 na meta".

Do not change the ring, the "Meta … kcal" pill, the macros card, the timeline, the disclaimer or the "Chat" button. Keep the bottom spacing so the disclaimer text stays fully visible above the "Chat" button.
```

**Light:**

```text
Screens to edit: "Home vazia - Day 1 (V2 Light Timeline)", "Home no dia - 1300 kcal (V2 Light Timeline)", "Home meta excedida - 2280 / 2000 kcal (V2 Light Timeline)".

Add one tappable row right below the macros card (P Proteína / C Carboidratos / G Gorduras), 12px gap, before the "LINHA DO TEMPO NUTRICIONAL" section. It uses the same card surface, 1px line border and 14px corner radius as the macros card, full width, 56px tall, 20px horizontal padding.

Row content, left to right:
- A flame icon (outlined, 20px) in the gold accent color.
- The label "Treino de hoje" in the primary text color, 16px, medium weight.
- On the right, the value and a chevron ">" in the muted color.

Value per screen:
- "Home vazia - Day 1 (V2 Light Timeline)": value "Informar" in the gold accent color.
- "Home no dia - 1300 kcal (V2 Light Timeline)": value "350 kcal · +175 na meta" (numbers in primary text color, "· +175 na meta" in muted).
- "Home meta excedida - 2280 / 2000 kcal (V2 Light Timeline)": value "200 kcal · +100 na meta".

Do not change the ring, the "Meta … kcal" pill, the macros card, the timeline, the disclaimer or the "Chat" button. Keep the bottom spacing so the disclaimer text stays fully visible above the "Chat" button.
```

### Prompt 2.2 — tela nova: sheet do treino aberto na Home (`homeW`)

Antes de enviar:

1. Duplique "Home no dia - 1300 kcal (V2 Expressive Timeline)" (dark) ou "Home no dia - 1300 kcal (V2 Light Timeline)" (light), já com a linha do 2.1.
2. Renomeie a cópia para o título exato: **Home com treino de hoje - Bottom Sheet (V2 Expressive Timeline)** ou **Home com treino de hoje - Bottom Sheet (V2 Light Timeline)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Home com treino de hoje - Bottom Sheet (V2 Expressive Timeline)". It is a copy of "Home no dia - 1300 kcal (V2 Expressive Timeline)".

Show a modal bottom sheet open over this Home screen (dim the Home behind it with the usual scrim).

Sheet: top corners radius 22px, surface background, 24px padding, drag handle on top.
1. Title "Treino de hoje", 20px, semibold, primary text color.
2. One large numeric field (28pt value, like the other number fields of the app) with the value "350" and the suffix "kcal" in muted color. 14px corner radius, 1px line border, focused state with gold border.
3. One line below the field, 14px, muted: "+175 kcal na meta de hoje (compensação 50%)".
4. Two buttons stacked, both full width, 52px tall, same pill shape and same text size (16px, semibold):
   - "Salvar": primary CTA (light fill #f3f5f7 with dark text).
   - "Cancelar": outlined, surface background, 1px line border, primary text color.
   Exactly like the "Pular refeição" / "Cancelar" pair of the screen "Diálogo de confirmação para pular refeição (V2 Expressive)".

No other text in the sheet. Keep the Home behind it unchanged.
```

**Light:**

```text
Screen to edit: "Home com treino de hoje - Bottom Sheet (V2 Light Timeline)". It is a copy of "Home no dia - 1300 kcal (V2 Light Timeline)".

Show a modal bottom sheet open over this Home screen (dim the Home behind it with the usual scrim).

Sheet: top corners radius 22px, surface background, 24px padding, drag handle on top.
1. Title "Treino de hoje", 20px, semibold, primary text color.
2. One large numeric field (28pt value, like the other number fields of the app) with the value "350" and the suffix "kcal" in muted color. 14px corner radius, 1px line border, focused state with gold border.
3. One line below the field, 14px, muted: "+175 kcal na meta de hoje (compensação 50%)".
4. Two buttons stacked, both full width, 52px tall, same pill shape and same text size (16px, semibold):
   - "Salvar": primary CTA (#111111 fill with light text).
   - "Cancelar": outlined, surface background, 1px line border, primary text color.
   Exactly like the "Pular refeição" / "Cancelar" pair of the screen "Diálogo de confirmação para pular refeição (V2 Light)".

No other text in the sheet. Keep the Home behind it unchanged.
```

## Verificação (agente)

1. `mcp__stitch__list_screens`: as 3 Homes com os mesmos IDs e títulos; "Home com treino de hoje - Bottom Sheet (V2 Expressive Timeline)" e "(V2 Light Timeline)" existem com esses títulos exatos (IDs no Registro).
2. `tools/export-stitch.mjs` + `homeW`; `tools/check-stitch.mjs`: contagem + 1 por tema.
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist dark e light. Falhou (inclusive título diferente): **para** e reporta ao dono o que falta.

### Checklist

- [x] As 3 Homes: linha "Treino de hoje" com chama gold logo abaixo dos macros, antes da timeline.
- [x] "Home vazia - Day 1": valor "Informar" em gold; "Home no dia" e "Home meta excedida": "{n} kcal · +{n} na meta".
- [x] Anel, pílula da meta, macros e timeline sem mudança.
- [x] Disclaimer inteiro visível acima do botão "Chat".
- [x] "Home com treino de hoje - Bottom Sheet": sheet com raio 22 no topo, campo 28pt, uma linha de crédito, Salvar e Cancelar do mesmo tamanho e mesma fonte.
- [x] Nenhuma outra copy no sheet.
- [x] Tokens do `AGENTS.md`; dark e light coerentes.

Checklist verde: lista de golds (`AGENTS.md`, `docs/qa/README.md`) com `homeW`, `docs/stitch/README.md` com o título na tabela principal, estado `Concluído`, mover para `completed/`, índices e entrega git (`docs/st2-stitch-home-treino`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/{home0,home1,homeX,homeW}.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, `docs/stitch/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
- 29/09/2026 — Prompts reescritos com os títulos reais do Stitch (pedido do dono).
- 29/09/2026 — Verificação: **passou** (após 2 rodadas de ajuste do dono). IDs novos: "Home com treino de hoje - Bottom Sheet (V2 Expressive Timeline)" `df7066fb62474c2aa9aea3804c2ba548`, "(V2 Light Timeline)" `cad05772505e480c997b722296b64473`. As 3 Homes mantêm IDs e títulos. Linha "Treino de hoje" medida no HTML a 390 px: 56 px, sem transbordar, nos 8 frames. Exportador: a viewport agora cresce até a altura do frame (o FAB fixo cobria o disclaimer só no render), e o `homeW` light é renderizado do HTML porque o screenshot do Stitch estava velho (`RENDER_FROM_HTML`). `homeW` entrou nos scripts (40 golds, 20 por tema). Sobram no projeto duas cópias sem uso com o título "Home no dia - 1300 kcal": `ad36520e364b41619c1c7d0e40551df1` (dark) e `43d0cbff74434241b8312d770ac262a1` (light), sem a linha de treino. Ignorar; o dono pode apagá-las.
