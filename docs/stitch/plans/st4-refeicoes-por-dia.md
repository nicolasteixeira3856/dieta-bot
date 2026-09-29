# Gate Stitch — ST4 Refeições por dia da semana

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md)
- Pré-requisito: [ST3](st3-seletor-horario.md) `Concluído` (o `o3` precisa estar estável antes).
- Bloqueia: [A24 Refeições por dia](../../android/plans/a24-refeicoes-por-dia.md)

## ⛔ Bloqueio do dono — prompts para o Stitch

Este passo é manual e trava o A24. São 3 prompts, cada um rodado uma vez por tema.

### Prompt 4.1 — `o3` ganha as tags de modo (modo padrão)

Antes de enviar: selecione só a `o3` do tema (dark `14440390e2d94582af1efa4e8aa4573b`, light `87f1e6f1b9334faeae8f40f6cebf56e5`).

```text
Add a "mode" selector to this "Distribuição das refeições" screen, between the subtitle ("Organize sua rotina…") and "QUANTIDADE DE REFEIÇÕES".

1. Section label "DIAS DA SEMANA" in the same style as "QUANTIDADE DE REFEIÇÕES".
2. Below it, a row of three selectable chips (14px corner radius, 40px tall, 1px line border, 14px text): "Todos os dias", "Seg–Sex · Sáb–Dom", "Cada dia". "Todos os dias" is selected (light fill like the selected "4" in the count selector, dark text). The others are unselected (surface background, primary text). The chips wrap to a second line if they do not fit.

Keep the meal count selector, the four meal cards, the suggestions, the info note and the "Continuar" button exactly as they are.
```

### Prompt 4.2 — nova tela `o3s`: segunda etapa do modo Seg–Sex · Sáb–Dom

Antes de enviar: **duplique** a `o3` do tema (já com o 4.1) e selecione só a cópia.

```text
This is the second step of the meal setup when the user chose "Seg–Sex · Sáb–Dom".

1. In the "DIAS DA SEMANA" chips, "Seg–Sex · Sáb–Dom" is selected; the other two are unselected.
2. Right below the chips, add a step header row: on the left "Sáb e Dom" (20px, semibold, primary text); on the right "Etapa 2 de 2" (13px, muted). Under it, a thin 2-segment progress bar (both segments gold, 4px tall, 4px gap).
3. Below the step header, one secondary full-width button, 44px tall, outlined, 14px radius, with a "copy" icon and the text "Copiar de Seg a Sex".
4. "QUANTIDADE DE REFEIÇÕES" shows 3 selected instead of 4. Only three meal cards: "Café da manhã" 09:30, "Almoço" 13:30, "Jantar" 20:30, each with its suggestion chips as in the other screen.
5. The bottom CTA text is "Continuar" with the arrow, unchanged.

Keep the header, the onboarding progress (3/4), the title and the subtitle unchanged.
```

Depois de enviar: renomeie para `o3s — dark` ou `o3s — light`.

### Prompt 4.3 — nova tela `cfgS`: Config com refeições por grupo de dias

Antes de enviar: **duplique** a `cfg` do tema (dark `ffb8e640dff34e8b9015e4936f36ebe5`, light `d582887ce63242a9ab48b882647ddbb9`) e selecione só a cópia.

```text
Change only the "HORÁRIOS DAS REFEIÇÕES" section of this settings screen. The user set different meals for weekdays and weekend.

1. Replace the four meal rows with two rows in the same card style:
   - "Seg a Sex" with the muted detail line "4 refeições · 07:30 a 20:00" and a chevron ">".
   - "Sáb e Dom" with the muted detail line "3 refeições · 09:30 a 20:30" and a chevron ">".
2. Right of the section label "HORÁRIOS DAS REFEIÇÕES", add the muted text "Seg–Sex · Sáb–Dom".

Title sizes: every row title in this screen ("Meta de calorias", "Compensação de treinos", "Macronutrientes (P · C · G)", "Seg a Sex", "Sáb e Dom", "Gasto calórico do treino") uses the same font size and weight as "Meta de calorias". Keep everything else unchanged.
```

Depois de enviar: renomeie para `cfgS — dark` ou `cfgS — light`.

## Verificação (agente)

1. `mcp__stitch__list_screens`: `o3` e `cfg` com os mesmos IDs; `o3s` e `cfgS` dark e light existem (IDs no Registro).
2. `tools/export-stitch.mjs` + `o3s`, `cfgS`; `tools/check-stitch.mjs`: contagem +2 por tema.
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist dark e light. Falhou: **para** e reporta ao dono o que falta.

### Checklist

- [ ] `o3`: seção "DIAS DA SEMANA" com 3 chips, "Todos os dias" selecionado; resto da tela igual.
- [ ] `o3s`: "Seg–Sex · Sáb–Dom" selecionado, cabeçalho "Sáb e Dom · Etapa 2 de 2", barra de 2 segmentos, botão "Copiar de Seg a Sex", 3 refeições.
- [ ] `cfgS`: duas linhas "Seg a Sex" / "Sáb e Dom" com detalhe; rótulo do modo ao lado do título da seção.
- [ ] `cfgS`: todos os títulos de linha no tamanho de "Meta de calorias".
- [ ] Tokens do `AGENTS.md`; dark e light coerentes.

Checklist verde: lista de golds com `o3s` e `cfgS`, estado `Concluído`, mover para `completed/`, índices e entrega git (`docs/st4-stitch-refeicoes-por-dia`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/{o3,o3s,cfgS}.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
