# Gate Stitch — ST6 Chat v2: plano de refeição, memória e sugestão da rotina (`chatR`, `chatM`, `chatS`)

- Estado: Concluído
- Data: 30/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) (decisões 3, 7 e 8)
- Bloqueia: [A29 Chat v2: interface](../../../android/plans/a29-chat-v2-interface.md)

Nomes das telas: títulos exatos do Stitch ([tabela](../../README.md#nomes-das-telas-regra-do-dono-29092026)). Cada prompt tem um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

## ⛔ Bloqueio do dono — prompt para o Stitch

Este passo é manual e trava o A29. São três telas novas, cada uma uma cópia de uma tela existente: três prompts, cada um enviado duas vezes (dark e light). Pode rodar em qualquer ordem e em paralelo com o [S10](../../../server/plans/completed/s10-avaliacao-chat.md), o [S11](../../../server/plans/pending_manual_validation/s11-chat-v2.md), o [A27](../../../android/plans/pending_manual_validation/a27-chat-v2-texto-intencao.md) e o [A28](../../../android/plans/pending_manual_validation/a28-memoria-v2.md).

### Prompt 6.1 — tela nova: resposta de plano com o dia projetado (`chatR`)

Antes de enviar:

1. Duplique "Estimate com botões de ação (V2 Expressive)" (dark) ou "Estimate com botões de ação (V2 Light)" (light), pelo menu da tela → Duplicate.
2. Renomeie a cópia para o título exato: **Chat com plano de refeição (V2 Expressive)** ou **Chat com plano de refeição (V2 Light)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Chat com plano de refeição (V2 Expressive)". It is a copy of "Estimate com botões de ação (V2 Expressive)". The user asked for a recipe with gram amounts; the assistant answers with a plan, not a meal to record.

1. User bubble text: "Vou fazer uma pizza de pão sírio na janta. Quantas gramas de cada item?"
2. Assistant bubble (keep the "Dieta Bot AI" label and the bubble style). Replace its whole content with plain text in the primary text color #f3f5f7, 16px, line by line:
   "Para caber nas 560 kcal que sobram hoje:"
   "• 1 pão sírio (60 g)"
   "• 2 colheres de sopa de molho de tomate (30 g)"
   "• 100 g de frango desfiado"
   "• 30 g de milho"
   "• 30 g de muçarela"
   "Monte e leve ao forno a 200 °C por 8 a 10 min."
   "Total: ~420 kcal · 40P · 38C · 12G" (the values 40P in protein green #4ec994, 38C in carbs orange #e58e42, 12G in fat gold #e8b86d; the rest in the primary text color).
   Remove the "ENERGIA TOTAL" card and the "Deseja registrar essa refeição no Café da manhã?" line.
3. Inside the same assistant bubble, below the text, an inner panel with the same style as the removed "ENERGIA TOTAL" card (darker panel #0e1114, 1px border #2a3139, 14px radius). Two lines:
   - Line 1, 15px: "Dia: 1.640 → 2.060 de 2.200 kcal". The numbers "1.640" and "2.060" in the primary text color #f3f5f7, semibold; "Dia:", "→", "de 2.200 kcal" in muted #8b939c.
   - Line 2, 13px, muted #8b939c: "P 126/167 · C 190/223 · G 58/74", with "126" in protein green #4ec994, "190" in carbs orange #e58e42 and "58" in fat gold #e8b86d.
4. Remove the separate question bubble ("Os pães tinham manteiga ou requeijão?") entirely.
5. Replace the three-button group "Gravar café" | "Trocar" | "Pular" with a single full-width pill button in the same position, same height, same background and border as that group: a check-circle icon and the label "Registrar assim" in the primary text color. No other buttons.

Keep the header, the date pill, the composer and everything else unchanged. Gold stays an accent only: no gold background on the new panel or the button.
```

**Light:**

```text
Screen to edit: "Chat com plano de refeição (V2 Light)". It is a copy of "Estimate com botões de ação (V2 Light)". The user asked for a recipe with gram amounts; the assistant answers with a plan, not a meal to record.

1. User bubble text: "Vou fazer uma pizza de pão sírio na janta. Quantas gramas de cada item?"
2. Assistant bubble (keep the "Dieta Bot AI" label and the bubble style). Replace its whole content with plain text in the primary text color #14161a, 16px, line by line:
   "Para caber nas 560 kcal que sobram hoje:"
   "• 1 pão sírio (60 g)"
   "• 2 colheres de sopa de molho de tomate (30 g)"
   "• 100 g de frango desfiado"
   "• 30 g de milho"
   "• 30 g de muçarela"
   "Monte e leve ao forno a 200 °C por 8 a 10 min."
   "Total: ~420 kcal · 40P · 38C · 12G" (the values 40P in protein green #1b7a4b, 38C in carbs orange #c2651e, 12G in fat gold #b8873d; the rest in the primary text color).
   Remove the "ENERGIA TOTAL" card and the "Deseja registrar essa refeição no Café da manhã?" line.
3. Inside the same assistant bubble, below the text, an inner panel with the same style as the removed "ENERGIA TOTAL" card (panel #eceae6, 1px border #d5d2cc, 14px radius). Two lines:
   - Line 1, 15px: "Dia: 1.640 → 2.060 de 2.200 kcal". The numbers "1.640" and "2.060" in the primary text color #14161a, semibold; "Dia:", "→", "de 2.200 kcal" in muted #5c636b.
   - Line 2, 13px, muted #5c636b: "P 126/167 · C 190/223 · G 58/74", with "126" in protein green #1b7a4b, "190" in carbs orange #c2651e and "58" in fat gold #b8873d.
4. Remove the separate question bubble ("Os pães tinham manteiga ou requeijão?") entirely.
5. Replace the three-button group "Gravar café" | "Trocar" | "Pular" with a single full-width pill button in the same position, same height, same background and border as that group: a check-circle icon and the label "Registrar assim" in the primary text color. No other buttons.

Keep the header, the date pill, the composer and everything else unchanged. Gold stays an accent only: no gold background on the new panel or the button.
```

Depois de enviar: a tela tem só a bolha do usuário, a bolha da IA com o texto e o painel do dia, e o botão único "Registrar assim".

### Prompt 6.2 — tela nova: selo de memória e chips de origem (`chatM`)

Antes de enviar:

1. Duplique "Estimate com botões de ação (V2 Expressive)" (dark) ou "Estimate com botões de ação (V2 Light)" (light).
2. Renomeie a cópia para o título exato: **Chat com memória atualizada (V2 Expressive)** ou **Chat com memória atualizada (V2 Light)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Chat com memória atualizada (V2 Expressive)". It is a copy of "Estimate com botões de ação (V2 Expressive)". The assistant used the user's saved memory to estimate and also saved something new.

1. User bubble text: "Café da manhã igual ao de sempre, mas hoje com pão integral"
2. Assistant bubble: keep its structure (label "Dieta Bot AI", text, "ENERGIA TOTAL" card with PROTEÍNA / CARBO / GORDURA, the question line). Text: "Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar." Energy card values: "~430" kcal, "26g P", "36g C", "20g G". Last line: "Deseja registrar essa refeição no Café da manhã?"
3. Remove the separate question bubble ("Os pães tinham manteiga ou requeijão?") entirely.
4. Right below the assistant bubble, left-aligned with it, 8px gap, a single row of three small chips, 8px apart (wrap to a second line only if they do not fit):
   - "Memória atualizada": a small check icon in gold #e8b86d, label 12px medium in the primary text color #f3f5f7.
   - "Memória permanente": a small pin icon in muted #8b939c, label 12px medium in muted #8b939c.
   - "Memória dinâmica": a small refresh/loop icon in muted #8b939c, label 12px medium in muted #8b939c.
   All three chips: surface #171b20 background, 1px border #2a3139, 14px corner radius, 28px tall, 10px horizontal padding. The message time "20:15" stays below the chips.
5. Keep the "Gravar café" | "Trocar" | "Pular" button group, the header, the date pill and the composer unchanged.

Gold only as the small check icon: no gold background, no gold border.
```

**Light:**

```text
Screen to edit: "Chat com memória atualizada (V2 Light)". It is a copy of "Estimate com botões de ação (V2 Light)". The assistant used the user's saved memory to estimate and also saved something new.

1. User bubble text: "Café da manhã igual ao de sempre, mas hoje com pão integral"
2. Assistant bubble: keep its structure (label "Dieta Bot AI", text, "ENERGIA TOTAL" card with PROTEÍNA / CARBO / GORDURA, the question line). Text: "Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar." Energy card values: "~430" kcal, "26g P", "36g C", "20g G". Last line: "Deseja registrar essa refeição no Café da manhã?"
3. Remove the separate question bubble ("Os pães tinham manteiga ou requeijão?") entirely.
4. Right below the assistant bubble, left-aligned with it, 8px gap, a single row of three small chips, 8px apart (wrap to a second line only if they do not fit):
   - "Memória atualizada": a small check icon in gold #b8873d, label 12px medium in the primary text color #14161a.
   - "Memória permanente": a small pin icon in muted #5c636b, label 12px medium in muted #5c636b.
   - "Memória dinâmica": a small refresh/loop icon in muted #5c636b, label 12px medium in muted #5c636b.
   All three chips: surface #ffffff background, 1px border #d5d2cc, 14px corner radius, 28px tall, 10px horizontal padding. The message time "20:15" stays below the chips.
5. Keep the "Gravar café" | "Trocar" | "Pular" button group, the header, the date pill and the composer unchanged.

Gold only as the small check icon: no gold background, no gold border.
```

Depois de enviar: os três chips aparecem juntos logo abaixo da bolha da IA, acima do horário.

### Prompt 6.3 — tela nova: sugestão da rotina (`chatS`)

Antes de enviar:

1. Duplique "Chat vazio (V2 Expressive)" (dark) ou "Chat vazio (V2 Light)" (light).
2. Renomeie a cópia para o título exato: **Chat com sugestão da rotina (V2 Expressive)** ou **Chat com sugestão da rotina (V2 Light)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Chat com sugestão da rotina (V2 Expressive)". It is a copy of "Chat vazio (V2 Expressive)". It is breakfast time, breakfast is not recorded yet, and the app suggests the user's usual breakfast.

Below the "Meta calórica de hoje" card, 12px gap, add one suggestion card, full width like that card: surface #171b20, 1px border #2a3139, 14px corner radius, 16px padding. Inside, top to bottom:
1. Title, 16px semibold, primary text #f3f5f7: "O de sempre no Café da manhã?"
2. Meal text, 14px, muted #8b939c, up to 2 lines: "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café"
3. Numbers line, 14px: "440 kcal" in primary text semibold, then " · 25P" in protein green #4ec994, " · 38C" in carbs orange #e58e42, " · 22G" in fat gold #e8b86d.
4. One small chip, the same style as a memory chip: small refresh/loop icon and "Memória dinâmica", 12px medium, muted #8b939c, surface #1e242b, 1px border #2a3139, 14px radius, 28px tall.
5. Two buttons side by side, equal width, 8px apart, 48px tall, pill shape:
   - Left, primary: background #f3f5f7, label "Registrar" in #111111, semibold.
   - Right, secondary: background #1e242b, 1px border #2a3139, label "Quase igual" in primary text #f3f5f7.

Keep the header, the date pill, the greeting bubble, the "Meta calórica de hoje" card, the suggestion chips above the composer and the composer unchanged. No gold background anywhere on the new card.
```

**Light:**

```text
Screen to edit: "Chat com sugestão da rotina (V2 Light)". It is a copy of "Chat vazio (V2 Light)". It is breakfast time, breakfast is not recorded yet, and the app suggests the user's usual breakfast.

Below the "Meta calórica de hoje" card, 12px gap, add one suggestion card, full width like that card: surface #ffffff, 1px border #d5d2cc, 14px corner radius, 16px padding. Inside, top to bottom:
1. Title, 16px semibold, primary text #14161a: "O de sempre no Café da manhã?"
2. Meal text, 14px, muted #5c636b, up to 2 lines: "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café"
3. Numbers line, 14px: "440 kcal" in primary text semibold, then " · 25P" in protein green #1b7a4b, " · 38C" in carbs orange #c2651e, " · 22G" in fat gold #b8873d.
4. One small chip, the same style as a memory chip: small refresh/loop icon and "Memória dinâmica", 12px medium, muted #5c636b, surface #e8e6e2, 1px border #d5d2cc, 14px radius, 28px tall.
5. Two buttons side by side, equal width, 8px apart, 48px tall, pill shape:
   - Left, primary: background #111111, label "Registrar" in #f3f5f7, semibold.
   - Right, secondary: background #e8e6e2, 1px border #d5d2cc, label "Quase igual" in primary text #14161a.

Keep the header, the date pill, the greeting bubble, the "Meta calórica de hoje" card, the suggestion chips above the composer and the composer unchanged. No gold background anywhere on the new card.
```

Depois de enviar: o card fica entre o card da meta e os chips de sugestão, com os dois botões lado a lado.

## Verificação (agente)

Começa quando o dono avisa que rodou os prompts. Checagens do gate: [`st6-chat-v2.checks.json`](st6-chat-v2.checks.json) ([tipos](../../README.md#verificação-automática-sv1)).

1. `node tools/verify-stitch.mjs st6 --report --out <scratchpad>/stitch-report.html`: acha as seis telas pelo título exato, confere as checagens a 390 px, a coerência dark × light e as imagens, e gera o relatório. Responde `PASSOU` ou `NÃO PASSOU`.
2. `NÃO PASSOU` (inclusive título diferente): **para**, manda o relatório ao dono numa mensagem só e deixa o estado `Aguardando o dono no Stitch`.
3. `PASSOU`: olha o relatório para os itens de aparência que o script não mede.
4. Adiciona os IDs novos (Registro) a `tools/export-stitch.mjs` (`chatR`, `chatM`, `chatS` em `DARK_SCREENS` e `LIGHT_SCREENS`) e a contagem a `tools/check-stitch.mjs` (27 por tema).
5. `node tools/export-stitch.mjs --only chatR,chatM,chatS` + `node tools/check-stitch.mjs` verdes.

### Checklist

- [x] `chatR`: bolha do usuário com o pedido da pizza — `text has "Vou fazer uma pizza de pão sírio"`
- [x] `chatR`: texto da IA com as gramas e o total — `text has "100 g de frango desfiado"`, `"Total: ~420 kcal"`
- [x] `chatR`: painel do dia com 1.640 → 2.060 de 2.200 e a linha de macros — `text has "1.640"`, `"2.060"`, `"2.200"`, `"126/167"`
- [x] `chatR`: sem "ENERGIA TOTAL", sem pergunta, sem Gravar/Trocar/Pular — `text not`
- [x] `chatR`: botão único "Registrar assim", largura total — `fits "Registrar assim"` / relatório
- [x] `chatM`: três chips abaixo da bolha da IA, na ordem Memória atualizada, permanente, dinâmica — `text has`, `fits` / relatório
- [x] `chatM`: check em gold só no "Memória atualizada"; os outros em muted — relatório
- [x] `chatM`: Gravar café, Trocar e Pular mantidos; sem bolha de pergunta — `text has` / `text not`
- [x] `chatS`: card entre a meta e os chips de sugestão, com título, texto, números, chip "Memória dinâmica" e os dois botões — `text has`, `gap` / relatório
- [x] `chatS`: "Registrar" no estilo CTA do tema (dark `#f3f5f7` sobre `#111`, light `#111111` com texto `#f3f5f7`); "Quase igual" secundário — relatório
- [x] Tokens do `AGENTS.md`, cores semânticas P/C/G, gold só como acento, raio 14 nos cards e chips. Light e dark coerentes — relatório + coerência automática

Depois do checklist verde:

- `AGENTS.md` e `docs/qa/README.md`: lista de golds com `chatR`, `chatM`, `chatS` (27 por tema). `docs/stitch/README.md`: títulos na tabela principal.
- Estado `Concluído`, mover para `plans/completed/` (com o `checks.json`), atualizar índices e fazer a entrega git (`docs/st6-stitch-chat-v2`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/chatR.png`, `chatM.png`, `chatS.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `st6-chat-v2.checks.json`
- Lista de golds em `AGENTS.md` e `docs/qa/README.md`; tabela de nomes em `docs/stitch/README.md`; índices em `docs/`

Nada em `apps/` ou `server/`.

## Registro

- 30/09/2026 — Gate criado. Aguardando o dono.
- 30/09/2026 — Verificação 1: `NÃO PASSOU`. As seis telas foram achadas pelo título exato; textos, gaps e imagens ok. `checks.json` corrigido: `fits "Quase igual"` passa a `maxHeight: 48`, porque o rótulo é texto direto do botão `h-12` e a checagem media o botão (48 px, conforme o prompt), não a linha. Pendências do dono: (1) `chatS` light usa o ícone `autorenew` no chip "Memória dinâmica"; o dark e os dois `chatM` usam `sync` (falha de coerência); (2) `chatR` dark pinta o ícone check-circle de "Registrar assim" em verde proteína `#4ec994`; o prompt pede a cor do texto principal, e o light já usa `#14161a` (visto no relatório). Chips do `chatM` em coluna: aceito, porque os três não cabem na largura da bolha, e os dois temas estão iguais.
- 30/09/2026 — Verificação 2: o dono trocou o ícone do chip no `chatS` light para `sync` e o check-circle do `chatR` dark para `#f3f5f7`. `verify-stitch st6`: `PASSOU`; conferi as seis telas renderizadas. IDs novos em `tools/export-stitch.mjs`, `export-stitch --only chatR,chatM,chatS` e `check-stitch` verdes (27 por tema). Listas de golds e índices atualizados. Concluído.
