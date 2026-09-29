# Plano — A25 Composer: limite de 2000 caracteres com estado de erro

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat`, `domain`)
- Pré-requisitos:
  - [S9](../../server/plans/completed/s9-limite-texto-2000.md) no ar no dev (`/v1/chat` aceita 2000). **Concluído em 29/09/2026.**
  - **[ST5](../../stitch/plans/st5-chat-texto-longo.md) em `stitch/plans/completed/`** (gold `chatX`). Sem ele a implementação não começa.
  - [A18](pending_manual_validation/a18-chat-registro-foto.md) no `master` (mesmo `ChatScreen.kt`).
  - Aceita o [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a25-limite-texto-composer.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O composer deixa de cortar o texto em 1000 caracteres em silêncio. Até 2000 envia normal; acima, mostra o estado de erro do gold `chatX` e não envia.

## Fontes de verdade

- [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md), [chat](../../produto/specifications/chat.md).
- Gold `chatX` (dark e light), exportado pelo ST5.

## Escopo de implementação

### 1. Regra (`domain/ChatText.kt`, novo)

- `MAX_CHARS = 2000`.
- `length(text)`: code points de `text.trim()` (igual ao Pydantic do server; emoji conta 1).
- `tooLong(text) = length(text) > MAX_CHARS`.

### 2. Estado (`ChatViewModel`, `ChatUiState`)

- `setComposer`: sai o `take(1000)`. O texto fica como o usuário digitou ou colou.
- `ChatUiState.composerTooLong` (derivado do composer).
- `canSend = composer.isNotBlank() && !sending && !composerTooLong`.
- `send()` e o envio de foto (legenda = composer) não fazem nada com o texto longo demais. O botão de foto fica desabilitado no mesmo estado.
- `PromptBuilder`: `take(1000)` → `take(MAX_CHARS)` no histórico e no texto do turno (defesa; o client não envia acima disso).

### 3. UI (`ChatScreen.Composer`)

- Layout, tokens e medidas pelo gold `chatX`:
  - borda do composer em `bad` quando `composerTooLong`;
  - `Texto muito longo` abaixo da caixa, em `bad`, alinhado ao início do texto (`testTag("chat-too-long")`);
  - enviar sem gold, `surf2` + seta `dim`; câmera com ícone `dim`, ambos desabilitados;
  - caixa com cantos de 24 dp quando passa de uma linha (hoje ela vira cápsula); uma linha continua pílula como no `chat0`.
- Sem contador. Sem snackbar.

### 4. Specs

- `chat.md`: regra 3 (limite, estado de erro) e "Estados e falhas".

## Arquivos e áreas afetadas

- `domain/ChatText.kt` (novo), `feature/chat/ChatViewModel.kt`, `ChatUiState.kt`, `ChatScreen.kt`, `PromptBuilder.kt`.
- Testes: `ChatTextTest` (novo), `ChatViewModelTest`, `ChatFixtures` (`chatX`), `StitchGoldTest` (`chatX_dark`, `chatX_light`).
- `tools/capture-chat.sh` (captura `chatX`), `docs/produto/specifications/chat.md`.

## Validação planejada

1. `testDevDebugUnitTest`:
   - `ChatText`: 2000 → ok; 2001 → longo; espaços nas pontas não contam; 2000 emojis → ok (code points).
   - VM: 1500 caracteres ficam inteiros no composer (sem corte); 2001 → `composerTooLong`, `canSend` falso, `send()` sem POST, foto sem POST; apagar até 2000 → volta a enviar.
2. `verifyRoborazziDevDebug` e `StitchGoldTest` `chatX` dark/light ≤ 2 % contra o gold; `chat0` e `chatE` sem regressão.
3. Emulador + fake: colar 2100 caracteres → borda vermelha, "Texto muito longo", enviar e câmera sem efeito (0 POST); apagar até 2000 → envia, o fake recebe 2000 caracteres. Captura `docs/qa/android/current/{dark,light}/chatX.png` + `node tools/diff-gold.mjs dark/chatX light/chatX` + lista de diferenças.
4. Contra o server de dev (S9 no ar): mensagem de 2000 caracteres → resposta normal, sem 422.

## Fora de escopo

- Contador visível.
- Limite do campo de memória, digests ou `/v1/estimate` e `/v1/fit`.
- Bolhas, anexo com preview: [A19](a19-chat-visual.md).

## Riscos e controles

- **Colar um texto enorme (dezenas de milhares de caracteres):** o campo aceita; a contagem é linear e só roda na mudança do texto. Se a digitação travar no emulador com 50 k caracteres, parar e voltar ao Planning (corte de segurança seria decisão nova).
- **Client e server contarem diferente:** os dois contam code points; teste com emoji.

## Critérios de aceite

- Nada é cortado em silêncio.
- Acima de 2000: borda vermelha, "Texto muito longo", enviar e foto desabilitados, igual ao gold `chatX`.
- Até 2000: envia e o server aceita.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, com a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
