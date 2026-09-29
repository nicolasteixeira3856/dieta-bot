# Gate Stitch — ST4 Refeições por dia da semana

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md)
- Pré-requisito: [ST3](completed/st3-seletor-horario.md) `Concluído` (a tela da O3 precisa estar estável antes).
- Bloqueia: [A24 Refeições por dia](../../android/plans/a24-refeicoes-por-dia.md)

Nomes das telas: títulos exatos do Stitch ([tabela](../README.md#nomes-das-telas-regra-do-dono-29092026)). Cada prompt tem um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

## ⛔ Bloqueio do dono — prompts para o Stitch

Este passo é manual e trava o A24. São 3 prompts, cada um enviado duas vezes (dark e light), nesta ordem.

### Prompt 4.1 — tags de modo na O3 (modo padrão)

Antes de enviar: selecione só "Onboarding 3/4 - Distribuição das refeições (V2 Expressive)" (dark) ou "Onboarding 3/4 - Distribuição das refeições (V2 Light)" (light). Não selecione a "Seletor de horário".

**Dark:**

```text
Screen to edit: "Onboarding 3/4 - Distribuição das refeições (V2 Expressive)".

Add a "mode" selector between the subtitle ("Organize sua rotina…") and "QUANTIDADE DE REFEIÇÕES".

1. Section label "DIAS DA SEMANA" in the same style as "QUANTIDADE DE REFEIÇÕES".
2. Below it, a row of three selectable chips (14px corner radius, 40px tall, 1px line border, 14px text): "Todos os dias", "Seg–Sex · Sáb–Dom", "Cada dia". "Todos os dias" is selected (light fill like the selected "4" in the count selector, dark text). The others are unselected (surface background, primary text). The chips wrap to a second line if they do not fit.

Keep the meal count selector, the four meal cards, the suggestions, the info note and the "Continuar" button exactly as they are.
```

**Light:**

```text
Screen to edit: "Onboarding 3/4 - Distribuição das refeições (V2 Light)".

Add a "mode" selector between the subtitle ("Organize sua rotina…") and "QUANTIDADE DE REFEIÇÕES".

1. Section label "DIAS DA SEMANA" in the same style as "QUANTIDADE DE REFEIÇÕES".
2. Below it, a row of three selectable chips (14px corner radius, 40px tall, 1px line border, 14px text): "Todos os dias", "Seg–Sex · Sáb–Dom", "Cada dia". "Todos os dias" is selected (dark fill like the selected "4" in the count selector, light text). The others are unselected (surface background, primary text). The chips wrap to a second line if they do not fit.

Keep the meal count selector, the four meal cards, the suggestions, the info note and the "Continuar" button exactly as they are.
```

### Prompt 4.2 — tela nova: segunda etapa do modo Seg–Sex · Sáb–Dom (`o3s`)

Antes de enviar:

1. Duplique "Onboarding 3/4 - Distribuição das refeições (V2 Expressive)" (dark) ou "(V2 Light)" (light), já com o 4.1.
2. Renomeie a cópia para o título exato: **Onboarding 3/4 - Refeições Sáb e Dom (V2 Expressive)** ou **Onboarding 3/4 - Refeições Sáb e Dom (V2 Light)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Onboarding 3/4 - Refeições Sáb e Dom (V2 Expressive)". It is a copy of "Onboarding 3/4 - Distribuição das refeições (V2 Expressive)" and shows the second step of the meal setup when the user chose "Seg–Sex · Sáb–Dom".

1. In the "DIAS DA SEMANA" chips, "Seg–Sex · Sáb–Dom" is selected; the other two are unselected.
2. Right below the chips, add a step header row: on the left "Sáb e Dom" (20px, semibold, primary text); on the right "Etapa 2 de 2" (13px, muted). Under it, a thin 2-segment progress bar (both segments gold, 4px tall, 4px gap).
3. Below the step header, one secondary full-width button, 44px tall, outlined, 14px radius, with a "copy" icon and the text "Copiar de Seg a Sex".
4. "QUANTIDADE DE REFEIÇÕES" shows 3 selected instead of 4. Only three meal cards: "Café da manhã" 09:30, "Almoço" 13:30, "Jantar" 20:30, each with its suggestion chips as in the other screen.
5. The bottom CTA text is "Continuar" with the arrow, unchanged.

Keep the header, the onboarding progress (3/4), the title and the subtitle unchanged.
```

**Light:**

```text
Screen to edit: "Onboarding 3/4 - Refeições Sáb e Dom (V2 Light)". It is a copy of "Onboarding 3/4 - Distribuição das refeições (V2 Light)" and shows the second step of the meal setup when the user chose "Seg–Sex · Sáb–Dom".

1. In the "DIAS DA SEMANA" chips, "Seg–Sex · Sáb–Dom" is selected; the other two are unselected.
2. Right below the chips, add a step header row: on the left "Sáb e Dom" (20px, semibold, primary text); on the right "Etapa 2 de 2" (13px, muted). Under it, a thin 2-segment progress bar (both segments gold, 4px tall, 4px gap).
3. Below the step header, one secondary full-width button, 44px tall, outlined, 14px radius, with a "copy" icon and the text "Copiar de Seg a Sex".
4. "QUANTIDADE DE REFEIÇÕES" shows 3 selected instead of 4. Only three meal cards: "Café da manhã" 09:30, "Almoço" 13:30, "Jantar" 20:30, each with its suggestion chips as in the other screen.
5. The bottom CTA text is "Continuar" with the arrow, unchanged.

Keep the header, the onboarding progress (3/4), the title and the subtitle unchanged.
```

### Prompt 4.3 — tela nova: Config com refeições por grupo de dias (`cfgS`)

Antes de enviar:

1. Duplique "Configurações do perfil e dia (V2 Expressive)" (dark) ou "Configurações do perfil e dia (V2 Light)" (light).
2. Renomeie a cópia para o título exato: **Configurações com refeições por dia (V2 Expressive)** ou **Configurações com refeições por dia (V2 Light)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Configurações com refeições por dia (V2 Expressive)". It is a copy of "Configurações do perfil e dia (V2 Expressive)". The user set different meals for weekdays and weekend.

Change only the "HORÁRIOS DAS REFEIÇÕES" section:
1. Replace the four meal rows with two rows in the same card style:
   - "Seg a Sex" with the muted detail line "4 refeições · 07:30 a 20:00" and a chevron ">".
   - "Sáb e Dom" with the muted detail line "3 refeições · 09:30 a 20:30" and a chevron ">".
2. Right of the section label "HORÁRIOS DAS REFEIÇÕES", add the muted text "Seg–Sex · Sáb–Dom".

Title sizes: every row title in this screen ("Meta de calorias", "Compensação de treinos", "Macronutrientes (P · C · G)", "Seg a Sex", "Sáb e Dom", "Gasto calórico do treino") uses the same font size and weight as "Meta de calorias". Keep everything else unchanged.
```

**Light:**

```text
Screen to edit: "Configurações com refeições por dia (V2 Light)". It is a copy of "Configurações do perfil e dia (V2 Light)". The user set different meals for weekdays and weekend.

Change only the "HORÁRIOS DAS REFEIÇÕES" section:
1. Replace the four meal rows with two rows in the same card style:
   - "Seg a Sex" with the muted detail line "4 refeições · 07:30 a 20:00" and a chevron ">".
   - "Sáb e Dom" with the muted detail line "3 refeições · 09:30 a 20:30" and a chevron ">".
2. Right of the section label "HORÁRIOS DAS REFEIÇÕES", add the muted text "Seg–Sex · Sáb–Dom".

Title sizes: every row title in this screen ("Meta de calorias", "Compensação de treinos", "Macronutrientes (P · C · G)", "Seg a Sex", "Sáb e Dom", "Gasto calórico do treino") uses the same font size and weight as "Meta de calorias". Keep everything else unchanged.
```

## Verificação (agente)

1. `mcp__stitch__list_screens`: "Onboarding 3/4 - Distribuição das refeições" e "Configurações do perfil e dia" (dark e light) com os mesmos IDs; as 4 telas novas existem com os títulos exatos acima (IDs no Registro).
2. `tools/export-stitch.mjs` + `o3s`, `cfgS`; `tools/check-stitch.mjs`: contagem + 2 por tema.
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist dark e light. Falhou (inclusive título diferente): **para** e reporta ao dono o que falta.

### Checklist

- [ ] "Distribuição das refeições": seção "DIAS DA SEMANA" com 3 chips, "Todos os dias" selecionado; o resto da tela sem mudança.
- [ ] "Refeições Sáb e Dom": "Seg–Sex · Sáb–Dom" selecionado, cabeçalho "Sáb e Dom · Etapa 2 de 2", barra de 2 segmentos, botão "Copiar de Seg a Sex", 3 refeições.
- [ ] "Configurações com refeições por dia": duas linhas "Seg a Sex" / "Sáb e Dom" com detalhe e o rótulo do modo ao lado do título da seção.
- [ ] "Configurações com refeições por dia": todos os títulos de linha no tamanho de "Meta de calorias".
- [ ] Tokens do `AGENTS.md`; dark e light coerentes.

Checklist verde: lista de golds com `o3s` e `cfgS`, `docs/stitch/README.md` com os títulos na tabela principal, estado `Concluído`, mover para `completed/`, índices e entrega git (`docs/st4-stitch-refeicoes-por-dia`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/{o3,o3s,cfgS}.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, `docs/stitch/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
- 29/09/2026 — Prompts reescritos com os títulos reais do Stitch (pedido do dono).
