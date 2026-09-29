# Gate Stitch — ST5 Chat: texto longo demais no composer (`chatX`)

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md)
- Bloqueia: [A25 limite de texto no composer](../../android/plans/a25-limite-texto-composer.md)

## ⛔ Bloqueio do dono — prompt para o Stitch

Este passo é manual e trava o A25. Um prompt, enviado duas vezes (dark e light). Independe do [ST1](completed/st1-chat.md): se o ST1 rodar depois, "Chat com texto longo demais" entra na seleção do prompt 1.1 dele.

### Prompt 5.1 — tela nova: composer no estado de erro (`chatX`)

Nomes das telas: títulos exatos do Stitch ([tabela](../README.md#nomes-das-telas-regra-do-dono-29092026)). Um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

Antes de enviar:

1. Duplique "Chat vazio (V2 Expressive)" (dark) ou "Chat vazio (V2 Light)" (light), pelo menu da tela → Duplicate.
2. Renomeie a cópia para o título exato: **Chat com texto longo demais (V2 Expressive)** ou **Chat com texto longo demais (V2 Light)**.
3. Selecione só a cópia renomeada.

**Dark:**

```text
Screen to edit: "Chat com texto longo demais (V2 Expressive)". It is a copy of "Chat vazio (V2 Expressive)" and shows the chat composer in an error state: the typed message is too long to send.

1. The composer text field holds a long typed message in the primary text color (not placeholder): "Hoje no almoço comi arroz branco, feijão carioca, duas coxas de frango assadas sem pele, salada de alface com tomate e cebola, uma colher de farofa, meio bife acebolado e de sobremesa um pedaço de pudim de leite. No lanche da tarde tomei um café com leite e comi um pão de queijo grande e uma banana..." The field grows to show 5 lines of this text and the text continues beyond them (it scrolls inside the field). The composer container grows with it: keep its background, and use a 24px corner radius while it is taller than one line.
2. The composer container border becomes 1.5px in the error red #e07a6a instead of the neutral line color.
3. Right below the composer container, left-aligned with the start of the text field, a single line of error text: "Texto muito longo", 12px, medium weight, in the same error red. No icon.
4. The send button on the right is disabled: neutral surface background #1e242b and a dim arrow icon #5c6570 instead of the gold background.
5. The camera button on the left of the composer is disabled too: same dim icon color.

Keep the header, the date pill, the greeting, the suggestion chips and everything else of this screen unchanged. No counter, no dialog, no toast: only the red border, the error line and the disabled buttons.
```

**Light:**

```text
Screen to edit: "Chat com texto longo demais (V2 Light)". It is a copy of "Chat vazio (V2 Light)" and shows the chat composer in an error state: the typed message is too long to send.

1. The composer text field holds a long typed message in the primary text color (not placeholder): "Hoje no almoço comi arroz branco, feijão carioca, duas coxas de frango assadas sem pele, salada de alface com tomate e cebola, uma colher de farofa, meio bife acebolado e de sobremesa um pedaço de pudim de leite. No lanche da tarde tomei um café com leite e comi um pão de queijo grande e uma banana..." The field grows to show 5 lines of this text and the text continues beyond them (it scrolls inside the field). The composer container grows with it: keep its background, and use a 24px corner radius while it is taller than one line.
2. The composer container border becomes 1.5px in the error red #c14d40 instead of the neutral line color.
3. Right below the composer container, left-aligned with the start of the text field, a single line of error text: "Texto muito longo", 12px, medium weight, in the same error red. No icon.
4. The send button on the right is disabled: neutral surface background #e8e6e2 and a dim arrow icon #8b939c instead of the gold background.
5. The camera button on the left of the composer is disabled too: same dim icon color.

Keep the header, the date pill, the greeting, the suggestion chips and everything else of this screen unchanged. No counter, no dialog, no toast: only the red border, the error line and the disabled buttons.
```

## Verificação (agente)

Começa quando o dono avisa que rodou o prompt.

1. `mcp__stitch__list_screens` no `Nutri`: as telas antigas continuam com os mesmos IDs e títulos; "Chat com texto longo demais (V2 Expressive)" e "(V2 Light)" existem com esses títulos exatos. Anote os IDs em Registro.
2. `tools/export-stitch.mjs`: adicionar `chatX` em `DARK_SCREENS` e `LIGHT_SCREENS`. `tools/check-stitch.mjs`: contagem + 1 por tema (texto final e contagem).
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist nos PNGs exportados, dark e light.
5. Algum item falhou (inclusive título diferente): **para**, lista para o dono o que falta, com o PNG, e o estado volta a `Aguardando o dono no Stitch`.

### Checklist

- [ ] Composer com o texto longo em cor primária, 5 linhas visíveis, e a caixa mais alta que uma linha com cantos de 24 px.
- [ ] Borda da caixa do composer em vermelho `bad` do tema (dark `#e07a6a`, light `#c14d40`), e só ela.
- [ ] "Texto muito longo" logo abaixo da caixa, alinhado ao início do texto, em `bad`, sem ícone.
- [ ] Enviar sem gold: fundo `surf2`, seta `dim`.
- [ ] Botão de câmera com ícone `dim`.
- [ ] Sem contador, diálogo ou toast.
- [ ] Header, pílula de data, saudação e chips iguais à `chat0`.
- [ ] Tokens do `AGENTS.md` (sem roxo, sem cor nova além de `bad`). Light e dark coerentes.

Depois do checklist verde:

- `AGENTS.md` e `docs/qa/README.md`: lista de golds com `chatX`. `docs/stitch/README.md`: título na tabela principal.
- Estado `Concluído`, mover para `plans/completed/`, atualizar índices e fazer a entrega git (`docs/st5-stitch-chat-texto-longo`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/chatX.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
- 29/09/2026 — Prompt reescrito com os títulos reais do Stitch (pedido do dono).
