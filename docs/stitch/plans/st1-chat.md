# Gate Stitch — ST1 Chat: bolhas, pergunta separada, anexo com preview

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-020](../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md)
- Bloqueia: [A19 Chat visual](../../android/plans/a19-chat-visual.md)

## ⛔ Bloqueio do dono — prompts para o Stitch

Este passo é manual e trava o A19. São 3 prompts, e cada um roda duas vezes: uma com as telas dark selecionadas, outra com as light.

### Prompt 1.1 — bolhas iguais e sem "IA ATIVA" (7 telas por tema)

Antes de enviar, selecione as 7 telas de chat do tema:

| Tela | Dark | Light |
|---|---|---|
| chat0 | `be092fe5db2f40b4bca8da18ae9ce580` | `84f166d46b6f4a6d847a3c64977e07d3` |
| chatL | `e021044511b44e25b8a8de6433b8dbad` | `611e2752cdd04d2fb103c251b6d21c7b` |
| chatE | `a91c63f2a624456f9d5e30ef14422578` | `2811a76b200a447aad6d5233cbb5cce4` |
| chatT | `8537f8a0e82a4529b3fb0efb9ea89c38` | `9feb6e7d276a41d6956df9fc001f445a` |
| chatP | `e64a7a52e1a54259b2e488e7dfdf7413` | `66c301014a5f4fdc8b002756faa87396` |
| chatF | `ac502aadad004c18bc271e0c74355960` | `35a756967193481fa84c48e05e33e0be` |
| chatG | `5e95d8451bf447989bd62a2e9dd38909` | `c226508d6c9b4830952a42005f1cf2f4` |

Se o [ST5](st5-chat-texto-longo.md) já rodou, selecione também a `chatX` do tema (IDs no Registro do ST5).

```text
Edit only the chat message bubbles and the assistant label on these screens.

1. Give every chat bubble the same shape: user bubbles (right) and assistant bubbles (left) both use a 16px corner radius on all four corners. No tail, no sharp corner, no asymmetric corner.
2. Remove the "IA ATIVA" pill next to the assistant name. Keep the small bolt icon and the name "Dieta Bot AI" exactly as they are.

Keep everything else unchanged: colors, text, spacing, the estimate card, the action bar, the composer, the header and the status bar.
```

Depois de enviar: confira que nenhuma tela ganhou texto novo e que o card "ENERGIA TOTAL" continua idêntico.

### Prompt 1.2 — pergunta da IA em bolha própria (só `chatE`)

Antes de enviar: selecione só a `chatE` do tema (dark `a91c63f2a624456f9d5e30ef14422578`, light `2811a76b200a447aad6d5233cbb5cce4`). Rode depois do 1.1.

```text
In this chat screen, the assistant answered with one bubble that holds the text, the "ENERGIA TOTAL" estimate card and the line "Deseja registrar essa refeição no Café da manhã?". Keep that bubble exactly as it is.

Add a second, separate assistant bubble right below it (8px gap, same left alignment, same 16px radius on all corners, same background as the other assistant bubble), holding one clarifying question:
- A 3px vertical accent bar on the left edge of the bubble, in the gold accent color.
- A small "help" (question mark in a circle) icon in gold, 18px, before the text.
- The text "Os pães tinham manteiga ou requeijão?" in the primary text color (not muted), 16px, regular weight.
- Timestamp "20:15" below this second bubble only; remove the timestamp from under the first bubble.

The estimate card stays inside the first bubble only. Do not add buttons to the question bubble. Keep the action bar ("Gravar café", "Trocar", "Pular"), the composer and everything else unchanged.
```

### Prompt 1.3 — nova tela `chatA`: foto anexada no composer

Antes de enviar: **duplique** a `chat0` do tema (menu da tela → Duplicate) e selecione só a cópia. Anexe ao prompt uma foto de prato de comida qualquer (para a miniatura).

```text
This screen shows the chat composer with a photo attached, before sending.

1. Right above the composer text field, inside the same composer container, add an attachment row with one thumbnail of the attached food photo: 64x64px, 12px corner radius, 1px border in the line color. On its top-right corner, a 22px circular remove button with an "x" icon (surface background, primary text color).
2. The composer text field shows the typed text "almoço de hoje, comi tudo" (primary text color, not placeholder).
3. The send button is active (gold background, dark arrow icon), as when there is content to send.
4. The camera button on the left of the composer stays as it is.

Keep the header, the date pill, the greeting and everything else of this screen unchanged. Do not send the photo into the conversation: it is only attached in the composer.
```

Depois de enviar: renomeie a tela para `chatA — dark` ou `chatA — light`.

## Verificação (agente)

Começa quando o dono avisa que rodou os prompts.

1. `mcp__stitch__list_screens` no `Nutri`: as 14 telas antigas continuam com os mesmos IDs e existem duas telas `chatA` (dark e light). Anote os IDs em Registro.
2. `tools/export-stitch.mjs`: adicionar `chatA` em `DARK_SCREENS` e `LIGHT_SCREENS`. `tools/check-stitch.mjs`: 18 → 19 por tema (texto final e contagem).
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist nos PNGs exportados, dark e light.
5. Algum item falhou: **para**, lista para o dono o que falta, com o PNG, e o estado volta a `Aguardando o dono no Stitch`.

### Checklist

- [ ] Todas as bolhas das 7 telas com os 4 cantos iguais (sem o canto agudo da bolha do usuário).
- [ ] Nenhum "IA ATIVA" em nenhuma tela; o ícone e "Dieta Bot AI" continuam.
- [ ] `chatE`: card "ENERGIA TOTAL" e "Deseja registrar…" continuam na 1ª bolha.
- [ ] `chatE`: 2ª bolha separada com barra gold à esquerda, ícone de ajuda e a pergunta em cor de texto primária.
- [ ] `chatE`: a barra de ações (Gravar café · Trocar · Pular) está igual.
- [ ] `chatA`: miniatura 64 px com ✕ acima do campo, texto digitado, enviar ativo, nenhuma foto no fio.
- [ ] Tokens: fundo, gold e cores P/C/G iguais aos do `AGENTS.md` (sem roxo, sem cor nova).
- [ ] Light e dark coerentes entre si.

Depois do checklist verde:

- `AGENTS.md` e `docs/qa/README.md`: lista de golds com `chatA` (19 por tema).
- Estado `Concluído`, mover para `plans/completed/`, atualizar índices e fazer a entrega git (`docs/st1-stitch-chat`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/{chat0,chatL,chatE,chatT,chatP,chatF,chatG,chatA}.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
