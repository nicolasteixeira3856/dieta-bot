# Gate Stitch — ST1 Chat: bolhas, pergunta separada, anexo com preview

- Estado: Concluído
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-020](../../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md)
- Bloqueia: [A19 Chat visual](../../../android/plans/a19-chat-visual.md)

Nomes das telas: títulos exatos do Stitch ([tabela](../../README.md#nomes-das-telas-regra-do-dono-29092026)). Cada prompt tem um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

## ⛔ Bloqueio do dono — prompts para o Stitch

Este passo é manual e trava o A19. São 3 prompts, cada um enviado duas vezes (dark e light), nesta ordem.

### Prompt 1.1 — bolhas iguais e sem "IA ATIVA"

Antes de enviar, selecione as 7 telas de chat do tema:

| Dark (V2 Expressive) | Light (V2 Light) |
|---|---|
| Chat vazio (V2 Expressive) | Chat vazio (V2 Light) |
| Chat loading - Estimando (V2 Expressive) | Chat loading - Estimando (V2 Light) |
| Estimate com botões de ação (V2 Expressive) | Estimate com botões de ação (V2 Light) |
| Selecionar refeição - Bottom Sheet (V2 Expressive) | Selecionar refeição - Bottom Sheet (V2 Light) |
| Diálogo de confirmação para pular refeição (V2 Expressive) | Diálogo de confirmação para pular refeição (V2 Light) |
| Foto de refeição e estimativa no Chat (V2 Expressive) | Foto de refeição e estimativa no Chat (V2 Light) |
| Confirmação pós-gravação com recibo duplo-check (V2 Expressive) | Confirmação pós-gravação com recibo duplo-check (V2 Light) |

Se o [ST5](../st5-chat-texto-longo.md) já rodou, selecione também "Chat com texto longo demais (V2 Expressive)" / "(V2 Light)" e acrescente o nome dela na lista do prompt.

**Dark:**

```text
Screens to edit: "Chat vazio (V2 Expressive)", "Chat loading - Estimando (V2 Expressive)", "Estimate com botões de ação (V2 Expressive)", "Selecionar refeição - Bottom Sheet (V2 Expressive)", "Diálogo de confirmação para pular refeição (V2 Expressive)", "Foto de refeição e estimativa no Chat (V2 Expressive)", "Confirmação pós-gravação com recibo duplo-check (V2 Expressive)".

Edit only the chat message bubbles and the assistant label on these screens.

1. Give every chat bubble the same shape: user bubbles (right) and assistant bubbles (left) both use a 16px corner radius on all four corners. No tail, no sharp corner, no asymmetric corner.
2. Remove the "IA ATIVA" pill next to the assistant name. Keep the small bolt icon and the name "Dieta Bot AI" exactly as they are.

Keep everything else unchanged: colors, text, spacing, the estimate card, the action bar, the composer, the header and the status bar.
```

**Light:**

```text
Screens to edit: "Chat vazio (V2 Light)", "Chat loading - Estimando (V2 Light)", "Estimate com botões de ação (V2 Light)", "Selecionar refeição - Bottom Sheet (V2 Light)", "Diálogo de confirmação para pular refeição (V2 Light)", "Foto de refeição e estimativa no Chat (V2 Light)", "Confirmação pós-gravação com recibo duplo-check (V2 Light)".

Edit only the chat message bubbles and the assistant label on these screens.

1. Give every chat bubble the same shape: user bubbles (right) and assistant bubbles (left) both use a 16px corner radius on all four corners. No tail, no sharp corner, no asymmetric corner.
2. Remove the "IA ATIVA" pill next to the assistant name. Keep the small bolt icon and the name "Dieta Bot AI" exactly as they are.

Keep everything else unchanged: colors, text, spacing, the estimate card, the action bar, the composer, the header and the status bar.
```

Depois de enviar: confira que nenhuma tela ganhou texto novo e que o card "ENERGIA TOTAL" continua idêntico.

### Prompt 1.2 — pergunta da IA em bolha própria

Antes de enviar: selecione só "Estimate com botões de ação (V2 Expressive)" (dark) ou "Estimate com botões de ação (V2 Light)" (light). Rode depois do 1.1.

**Dark:**

```text
Screen to edit: "Estimate com botões de ação (V2 Expressive)".

The assistant answered with one bubble that holds the text, the "ENERGIA TOTAL" estimate card and the line "Deseja registrar essa refeição no Café da manhã?". Keep that bubble exactly as it is.

Add a second, separate assistant bubble right below it (8px gap, same left alignment, same 16px radius on all corners, same background as the other assistant bubble), holding one clarifying question:
- A 3px vertical accent bar on the left edge of the bubble, in the gold accent color.
- A small "help" (question mark in a circle) icon in gold, 18px, before the text.
- The text "Os pães tinham manteiga ou requeijão?" in the primary text color (not muted), 16px, regular weight.
- Timestamp "20:15" below this second bubble only; remove the timestamp from under the first bubble.

The estimate card stays inside the first bubble only. Do not add buttons to the question bubble. Keep the action bar ("Gravar café", "Trocar", "Pular"), the composer and everything else unchanged.
```

**Light:**

```text
Screen to edit: "Estimate com botões de ação (V2 Light)".

The assistant answered with one bubble that holds the text, the "ENERGIA TOTAL" estimate card and the line "Deseja registrar essa refeição no Café da manhã?". Keep that bubble exactly as it is.

Add a second, separate assistant bubble right below it (8px gap, same left alignment, same 16px radius on all corners, same background as the other assistant bubble), holding one clarifying question:
- A 3px vertical accent bar on the left edge of the bubble, in the gold accent color.
- A small "help" (question mark in a circle) icon in gold, 18px, before the text.
- The text "Os pães tinham manteiga ou requeijão?" in the primary text color (not muted), 16px, regular weight.
- Timestamp "20:15" below this second bubble only; remove the timestamp from under the first bubble.

The estimate card stays inside the first bubble only. Do not add buttons to the question bubble. Keep the action bar ("Gravar café", "Trocar", "Pular"), the composer and everything else unchanged.
```

### Prompt 1.3 — tela nova: foto anexada no composer (`chatA`)

Antes de enviar:

1. Duplique "Chat vazio (V2 Expressive)" (dark) ou "Chat vazio (V2 Light)" (light), pelo menu da tela → Duplicate.
2. Renomeie a cópia para o título exato: **Chat com foto anexada (V2 Expressive)** ou **Chat com foto anexada (V2 Light)**.
3. Selecione só a cópia renomeada e anexe ao prompt uma foto qualquer de prato de comida (vira a miniatura).

**Dark:**

```text
Screen to edit: "Chat com foto anexada (V2 Expressive)". It is a copy of "Chat vazio (V2 Expressive)" and now shows the chat composer with a photo attached, before sending.

1. Right above the composer text field, inside the same composer container, add an attachment row with one thumbnail of the attached food photo: 64x64px, 12px corner radius, 1px border in the line color. On its top-right corner, a 22px circular remove button with an "x" icon (surface background, primary text color).
2. The composer text field shows the typed text "almoço de hoje, comi tudo" (primary text color, not placeholder).
3. The send button is active (gold background, dark arrow icon), as when there is content to send.
4. The camera button on the left of the composer stays as it is.

Keep the header, the date pill, the greeting and everything else of this screen unchanged. Do not send the photo into the conversation: it is only attached in the composer.
```

**Light:**

```text
Screen to edit: "Chat com foto anexada (V2 Light)". It is a copy of "Chat vazio (V2 Light)" and now shows the chat composer with a photo attached, before sending.

1. Right above the composer text field, inside the same composer container, add an attachment row with one thumbnail of the attached food photo: 64x64px, 12px corner radius, 1px border in the line color. On its top-right corner, a 22px circular remove button with an "x" icon (surface background, primary text color).
2. The composer text field shows the typed text "almoço de hoje, comi tudo" (primary text color, not placeholder).
3. The send button is active (gold background, dark arrow icon), as when there is content to send.
4. The camera button on the left of the composer stays as it is.

Keep the header, the date pill, the greeting and everything else of this screen unchanged. Do not send the photo into the conversation: it is only attached in the composer.
```

## Verificação (agente)

Começa quando o dono avisa que rodou os prompts.

1. `mcp__stitch__list_screens` no `Nutri`: as 14 telas antigas continuam com os mesmos IDs e títulos. As telas "Chat com foto anexada (V2 Expressive)" e "Chat com foto anexada (V2 Light)" existem com esses títulos exatos. Anote os IDs em Registro.
2. `tools/export-stitch.mjs`: adicionar `chatA` em `DARK_SCREENS` e `LIGHT_SCREENS`. `tools/check-stitch.mjs`: contagem + 1 por tema (texto final e contagem).
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist nos PNGs exportados, dark e light.
5. Algum item falhou (inclusive uma tela nova com outro título): **para**, lista para o dono o que falta, com o PNG, e o estado volta a `Aguardando o dono no Stitch`.

### Checklist

- [x] Todas as bolhas das 7 telas com os 4 cantos iguais (sem o canto agudo da bolha do usuário).
- [x] Nenhum "IA ATIVA" em nenhuma tela; o ícone e "Dieta Bot AI" continuam.
- [x] "Estimate com botões de ação": card "ENERGIA TOTAL" e "Deseja registrar…" continuam na 1ª bolha.
- [x] "Estimate com botões de ação": 2ª bolha separada com barra gold à esquerda, ícone de ajuda e a pergunta em cor de texto primária.
- [x] "Estimate com botões de ação": barra de ações (Gravar café · Trocar · Pular) sem mudança.
- [x] "Chat com foto anexada": miniatura 64 px com ✕ acima do campo, texto digitado, enviar ativo, nenhuma foto no fio.
- [x] Tokens: fundo, gold e cores P/C/G iguais aos do `AGENTS.md` (sem roxo, sem cor nova).
- [x] Light e dark coerentes entre si.

Depois do checklist verde:

- `AGENTS.md` e `docs/qa/README.md`: lista de golds com `chatA`. `docs/stitch/README.md`: `chatA` sai de "telas novas previstas" e entra na tabela principal.
- Estado `Concluído`, mover para `plans/completed/`, atualizar índices e fazer a entrega git (`docs/st1-stitch-chat`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/{chat0,chatL,chatE,chatT,chatP,chatF,chatG,chatA}.png` (+ `chatX` se o ST5 já rodou)
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, `docs/stitch/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
- 29/09/2026 — Prompts reescritos com os títulos reais do Stitch (pedido do dono).
- 29/09/2026 — Verificação 1: **falhou**. Telas novas achadas: "Chat com foto anexada (V2 Expressive)" `242efa973e9e4bf38f570319af81eed3`, "Chat com foto anexada (V2 Light)" `f2ea449a4ed44d439a35115d0ac1bd1e`. As 14 antigas mantêm IDs e títulos. Passou: bolhas com 4 cantos iguais, "IA ATIVA" removido, 2ª bolha de pergunta no `chatE` (dark e light), miniatura do `chatA`. Falhas para o dono corrigir: (1) "Estimate com botões de ação (V2 Expressive)": os chips perderam os rótulos PROTEÍNA / CARBO / GORDURA; (2) "Chat vazio (V2 Expressive)": o título do header virou "Dieta Bot AI" (era "Chat Dieta Bot"), e a cópia "Chat com foto anexada (V2 Expressive)" herdou o erro; (3) "Foto de refeição e estimativa no Chat (V2 Light)": a foto do prato quebrou (a imagem retorna 403 no próprio Stitch). Não travam: o enviar do `chatA` passa da borda do composer; na "Foto de refeição e estimativa no Chat (V2 Expressive)", "Dieta Bot AI" ocupa o lugar do kcal (defeito anterior ao ST1). Golds não exportados. Estado segue `Aguardando o dono no Stitch`.
- 29/09/2026 — Verificação 2: **passou**. O dono autorizou o agente a corrigir as 3 falhas pelo MCP do Stitch (`edit_screens`); as edições levaram alguns minutos para aparecer na API. IDs mantidos. Checklist inteiro verde, dark e light. `chatA` entrou em `tools/export-stitch.mjs` e `tools/check-stitch.mjs` (38 golds, 19 por tema). Golds de chat re-exportados (`chat0`, `chatL`, `chatE`, `chatF`, `chatG`, `chatA`; `chatT` e `chatP` sem mudança). Fica para o A19 saber: em "Foto de refeição e estimativa no Chat (V2 Expressive)", "Dieta Bot AI" ocupa o lugar do kcal (defeito anterior ao ST1; o app usa o valor, como no light).
