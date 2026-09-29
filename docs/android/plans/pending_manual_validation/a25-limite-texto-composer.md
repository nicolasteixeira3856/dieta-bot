# Plano — A25 Composer: limite de 2000 caracteres com estado de erro

- Estado: Pendente aprovação manual
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat`, `domain`)
- Pré-requisitos:
  - [S9](../../../server/plans/completed/s9-limite-texto-2000.md) no ar no dev (`/v1/chat` aceita 2000). **Concluído em 29/09/2026.**
  - **[ST5](../../../stitch/plans/completed/st5-chat-texto-longo.md) em `stitch/plans/completed/`** (gold `chatX`). Sem ele a implementação não começa.
  - [A18](a18-chat-registro-foto.md) no `master` (mesmo `ChatScreen.kt`).
  - Aceita o [ADR-022](../../../produto/adrs/ADR-022-limite-texto-chat.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a25-limite-texto-composer.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O composer deixa de cortar o texto em 1000 caracteres em silêncio. Até 2000 envia normal; acima, mostra o estado de erro do gold `chatX` e não envia.

## Fontes de verdade

- [ADR-022](../../../produto/adrs/ADR-022-limite-texto-chat.md), [chat](../../../produto/specifications/chat.md).
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

## Resultados (29/09/2026)

Aprovado pelo dono em 29/09/2026 ("Aprovo o plano `docs/android/plans/a25-limite-texto-composer.md`. Implemente o plano aprovado."). Pré-requisitos conferidos antes do código: `st5-chat-texto-longo.md` em `stitch/plans/completed/`, golds `docs/qa/stitch/{dark,light}/chatX.png` presentes, A18 e A19 no `master`, S9 no ar.

### Implementado

- `domain/ChatText.kt`: `MAX_CHARS = 2000`; `length` = code points de `text.trim()`; `tooLong`; `clip` (primeiros 2000 code points, sem partir emoji).
- `ChatViewModel`: `setComposer` guarda o texto inteiro e calcula `composerTooLong` uma vez por mudança (não a cada render). `send()` não faz nada com `composerTooLong` (com ou sem foto anexada); `openPhotoSheet()` também não. Enviar zera o estado.
- `ChatUiState`: `composerTooLong`; `canSend` exige `!composerTooLong`; `canAttach = !sending && !composerTooLong` (botão da câmera).
- `PromptBuilder`: `take(1000)` → `ChatText.clip` no texto do turno e no histórico. Desvio consciente do "`take(MAX_CHARS)`" do plano: `take` conta UTF-16 e cortaria 2000 emojis pela metade, o que o server aceita (mesma intenção de defesa, sem cortar o que é válido).
- `ChatScreen.Composer`: com mais de uma linha (`onTextLayout`), a pílula vira caixa de raio 24 dp, padding 12/14/12/13 dp, botões de 40 dp alinhados embaixo, texto a 12 dp da câmera e 4 dp do enviar (a quebra de linha do gold dark bate). `composerTooLong`: borda 1,5 dp `bad`, `Texto muito longo` 12 sp W500 `bad` 4 dp abaixo, no início do campo de texto (padding + câmera), tag `chat-too-long`; enviar em `surf2` com seta `dim`; câmera com ícone `dim`, os dois desabilitados. Uma linha continua pílula (`chat0`); com anexo continua 28 dp (`chatA`). Sem contador, sem snackbar.
- Chip "Tirar foto do prato" continua ativo (o gold o desenha normal); a foto que ele anexa não sai enquanto o texto passa de 2000 (`canSend` falso, testado).
- Spec [chat](../../../produto/specifications/chat.md): regra 3 e "Estados e falhas".
- `tools/capture-chat.sh`: bloco `chatX` (dia vazio, 2100 caracteres, captura, enviar e câmera sem POST, apagar 100, envia, o fake recebe 2000). `adb input text` às vezes perde 1–2 caracteres: o script lê o composer pelo `uiautomator` e completa até 2100. `tools/fake-chat-server.mjs`: `/__calls` devolve `textLen` (code points) e responde 422 acima de 2000 como o server. `tools/diff-gold.mjs`: `chatX` em `GOLD_CONFLICTS` com gate por região (light só reportado, como o `chatA`).
- Testes: `ChatTextTest` (novo: 2000 ok, 2001 longo, espaços nas pontas, 2000 emojis, `clip`); `ChatViewModelTest` (+2: 1500 inteiros e enviados inteiros; 2001 → `composerTooLong`, `canSend`/`canAttach` falsos, `send()` e foto sem POST, apagar até 2000 envia 2000); `PromptBuilderTest` (limite 2000 em code points); `ChatFixtures.chatX`; `StitchGoldTest` `chatX_dark` / `chatX_light` com a região `COMPOSER_TOO_LONG`.

### Validação automatizada

- `testDevDebugUnitTest`: 207 testes, 203 passam. Os 4 que falham são `StitchGoldTest` `home1` e `o3` (dark e light) e falham igual na `master` sem este plano (conferido com `git stash`): golds do ST2/ST4, dos planos A22/A24.
- `verifyRoborazziDevDebug --tests RoborazziSmokeTest`: 12/12 (sem regravação: `chat0`, `chatA`, `chatQuestion`, `chatReplace` iguais).
- `StitchGoldTest` (JVM, borrado): região `chatX` dark **0,82 %** (gate ≤ 2 %), light 6,69 % report only (o gold light pinta o composer na cor da página e quebra as linhas em outro ponto, como o `chatA` light). `chatE` 1,88 / 1,92 %, `chatT` 1,27 / 1,74 %, `chatP` 0,25 / 0,64 %, região `chatA` dark 1,74 %: sem regressão.

### Emulador (Medium_Phone, 780x1688 @ 320 dpi)

- Fake: `tools/capture-chat.sh dark` e `light`, todos os ✓: 2100 caracteres → "Texto muito longo", câmera não abre o sheet, enviar e câmera = 0 POST; apagar 100 → o erro some, envia, o fake recebe 2000 code points (também no retry isolado do bloco). Resto do fluxo de chat sem regressão.
- Server de dev (S9 no ar, APK devDebug sem override): 2100 bloqueado; apagar até 2000 → resposta normal com estimativa, pergunta e "Gravar almoço", sem 422 e sem bolha de falha.
- `node tools/diff-gold.mjs`:

| Tela | Dark | Light |
|---|---|---|
| `chatX` região do composer | ✗ 4,77 % (conteúdo, ver abaixo) | ~ 7,24 % (report only) |
| `chatX` tela inteira | ~ 6,65 % | ~ 6,95 % |
| `chat0` | ~ 5,02 % | ~ 4,72 % |
| `chatE` | ✓ 1,76 % | ✓ 1,70 % |
| `chatT` | ✓ 1,39 % | ✓ 1,82 % |
| `chatP` | ✓ 0,36 % | ✓ 0,51 % |

A região `chatX` do emulador mede o texto, não o layout: o campo rola até o cursor e mostra o **fim** da mensagem colada, sem acentos (adb), com o cursor; o gold mostra o começo. Borda, raio, botões e rótulo caem no mesmo lugar (lado a lado conferido). O gate de layout é o `StitchGoldTest` com o texto do gold (0,82 %).

### Lista de diffs (captura vs gold `chatX`)

- Layout: caixa de 5 linhas com raio 24, câmera e enviar de 40 dp embaixo, "Texto muito longo" abaixo — igual. O rótulo começa no início do campo (68 dp); o gold o põe em 64 dp. O header segue o canônico do `chatE` (o `chatX` é cópia do `chat0`, outra geração).
- Tokens: borda e rótulo `bad`, enviar `surf2` + seta `dim`, câmera `dim` — igual. Light: o gold pinta o composer na cor da página; o app usa `card`, como o `chatE` light canônico.
- Tipo: texto 14 sp/20; rótulo 12 sp W500 — igual. No emulador, o texto mostrado é o fim da mensagem (rolagem até o cursor), sem acentos.
- Raio: 24 dp com mais de uma linha; pílula com uma; 28 dp com anexo; sheet 22 sem mudança.
- ButtonGroup / barra de ações, CTA, timeline, macros semânticos: sem mudança.

### Pendente (dono)

- Colar um texto longo (mais de 2000 caracteres) no Chat do APK distribuído: borda vermelha, "Texto muito longo", enviar e câmera sem efeito; apagar até sumir o aviso e enviar.

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
