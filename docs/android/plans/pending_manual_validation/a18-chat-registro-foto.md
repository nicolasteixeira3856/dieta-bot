# Plano — A18 Chat: refeição consolidada, teclado e foto 2048 px

- Estado: Pendente aprovação manual
- Aprovado: 29/09/2026 ("Aprovo o plano `docs/android/plans/a18-chat-registro-foto.md`. Implemente o plano aprovado.")
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat`, `core/photo`, `domain/PhotoGate.kt`, `core/database` repository)
- Pré-requisitos: [S8](../../../server/plans/pending_manual_validation/s8-chat-json-slot-consolidado.md) no ar (slot sugerido e estimativa da refeição inteira). Aceita o [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md) e o [ADR-018](../../adrs/ADR-018-foto-2048.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a18-chat-registro-foto.md`. Implemente o plano aprovado.

## Objetivo

Os ajustes de comportamento do Chat que não mudam gold:

1. Uma refeição tem um registro. Gravar num slot já registrado pede confirmação e substitui.
2. Enter do teclado pula linha.
3. A foto sai reduzida a 2048 px e o "Foto grande demais." some na prática.

Pergunta em bolha separada, formato das bolhas e anexo com preview dependem de gold e ficam no [A19](../a19-chat-visual.md).

## Fontes de verdade

- [chat](../../../produto/specifications/chat.md), [memoria-push](../../../produto/specifications/memoria-push.md), [home-timeline](../../../produto/specifications/home-timeline.md).
- Gold `chatP` (layout do diálogo de confirmação reusado).

## Escopo de implementação

### 1. Registro consolidado (`ChatViewModel`, `DayRepository`)

- `record(estimateId, slotId)`:
  - Slot sem log hoje → grava como hoje.
  - Slot com log(s) hoje → abre a confirmação (novo `Local.replaceConfirm`) em vez de gravar.
- Confirmação: o mesmo diálogo do `chatP` (componente extraído e reusado), com:
  - ícone e horário do slot, como no `chatP`;
  - título `Substituir {slot}?`;
  - texto `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`;
  - primário `Substituir`, secundário `Outra refeição` (fecha e abre o sheet Trocar).
- Substituir: `DayRepository.replaceSlotLog(slotId, …)` numa transação Room: apaga os `meal_log` de hoje daquele slot e insere o novo. Recibo na conversa: `Atualizado em {slot} · {hora} {kcal} kcal`. Linha de memória: `{slot} (atualizado): {descrição} ({kcal} kcal)`.
- O slot sugerido pela IA vem do S8. Nada de inferir slot no client.
- Sem migração de schema.

### 2. Teclado (`Composer`)

- `KeyboardOptions(imeAction = ImeAction.Default, capitalization = Sentences)`, `singleLine = false`, `maxLines = 5`. Enter insere quebra de linha. Enviar só pelo botão.

### 3. Foto 2048 px (`PhotoStore`, `PhotoGate`)

- Câmera e galeria passam pelo mesmo `normalize(file)`:
  1. `inJustDecodeBounds` → `inSampleSize` potência de 2 até o lado maior ficar entre 2048 e 4096.
  2. Decode (API 28+: `ImageDecoder` com `setTargetSize`; senão, `BitmapFactory`) e rotação EXIF aplicada no bitmap.
  3. Resize para o lado maior = 2048 (sem ampliar a menor).
  4. JPEG q85, sem EXIF.
- `PhotoGate`: `MAX_SIDE = 2048`, `JPEG_QUALITY = 85`. `MAX_BYTES` (16 MB) fica como defesa.
- `PhotoResult.TooLarge` passa a significar "não coube na memória para decodificar". A copy continua `Foto grande demais.`.
- Preview da bolha: sem mudança.

### 4. Specs e constituição (na mesma entrega)

- `AGENTS.md`, linha de foto: "Client sends the photo as JPEG q85, longest side ≤ 2048 px, EXIF stripped (ADR-018). ≤16 MB guard."
- `memoria-push.md` (regras de foto) e `chat.md` (regras 4–5, "Estados e falhas").

## Arquivos e áreas afetadas

- `feature/chat/ChatViewModel.kt`, `ChatScreen.kt` (diálogo e composer), `ChatUiState.kt`.
- `core/database/DayRepository.kt` (+ DAO, se precisar de delete por slot/dia).
- `core/photo/PhotoStore.kt`, `domain/PhotoGate.kt`.
- Testes: `ChatViewModelTest`, `PhotoGateTest`, teste instrumentado do `PhotoStore`, se já houver.
- `AGENTS.md`, specs acima.

## Validação planejada

1. `testDevDebugUnitTest`:
   - Gravar em slot vazio → 1 log, sem diálogo.
   - Gravar em slot com log → diálogo; Substituir → 1 log com os números novos; Outra refeição → sheet aberto, Room intacto.
   - `PhotoGate`: 8000×6000 → 2048×1536; 1000×800 → sem ampliar.
2. `verifyRoborazziDevDebug` verde (o diálogo reusa o `chatP`: render novo do estado `replace`).
3. Emulador: foto de 50 MP (fixture `tools/`) → request < 2 MB; `capture-photo.sh` atualizado (o caso "16 MB" vira "foto enorme passa").
4. Emulador + fake server: "também tomei suco" com a Ceia gravada → diálogo → Substituir → Home com 1 linha na Ceia.
5. Captura do diálogo vs `chatP` (layout; a copy muda, e isso é esperado).
6. Manual (dono): repetir o caso das esfihas + suco no APK distribuído.

## Resultados (29/09/2026)

Implementado:

- `DayRepository.replaceSlotLog`: apaga os `meal_log` de hoje do slot, insere o novo e limpa o skip, numa transação. Sem migração.
- `ChatViewModel.record` lê o dia do Room: slot vazio grava como antes; slot com registro abre `ReplaceConfirm` (kcal antigo = soma dos registros de hoje do slot). `confirmReplace` grava, deixa o recibo `replaced` ("Atualizado em {slot} · {hora} {kcal} kcal") e a linha de memória `{slot} (atualizado): …`. `replaceElsewhere` fecha e abre o Trocar sem seleção; back/scrim (`cancelReplace`) só fecham. Confirmar no Trocar num slot com registro também pergunta.
- `ChatScreen`: o diálogo do `chatP` virou `ConfirmDialog`; `SkipDialog` e `ReplaceDialog` o usam (tags `chat-skip-*` preservadas, novas `chat-replace-*`). Composer: `singleLine = false`, `maxLines = 5`, `ImeAction.Default`, `KeyboardCapitalization.Sentences`.
- `PhotoStore.normalize` para câmera e galeria: `ImageDecoder` com `setTargetSampleSize` (API 28+) ou `BitmapFactory` + rotação EXIF (26–27); `PhotoGate.decodeSample` deixa o lado maior entre 2048 e 4096, `PhotoGate.targetSize` reduz a 2048 sem ampliar; JPEG q85 sem EXIF. `OutOfMemoryError` → `TooLarge`. O JPEG da galeria não é mais copiado byte a byte. `PhotoGate.isJpeg` saiu (sem uso).
- `tools/fake-chat-server.mjs`: `/__calls` informa largura/altura do JPEG; `/__mode {"slot", "kcal"}` escolhe o slot sugerido e a kcal. `tools/capture-photo.sh` reescrito para o ADR-018 (cada checagem exige um POST novo). `tools/capture-replace.sh` novo.
- Specs `chat` (regras 3, 5, 7, Estados e falhas), `memoria-push` (memória, foto 2–3), `home-timeline` (regra 5), `AGENTS.md` (linha de foto), ADR-018 aceito.

Validação:

1. `testDevDebugUnitTest`: 183 testes, 0 falhas. Novos: `gravarOnTakenSlot_asksFirst_thenReplacesWithOneLog` (diálogo 880→1220 sem gravar; Substituir → 1 log 1220/40/150/45, recibo `replaced`, memória), `replaceElsewhere_opensTrocarEmpty_roomIntact`, `trocarIntoTakenSlot_asksToo_cancelKeepsRoom`; gravar em slot vazio segue em `gravar_addsLog_withoutSecondPost_andClosesActions`. `PhotoGate`: 8000×6000 → 2048×1536, 1000×800 sem ampliar, `decodeSample` 50/200 MP. `PhotoStore` (Robolectric): 4400×3300 → 2048×1536 (galeria e câmera), rotação EXIF aplicada e EXIF (GPS, Make) não copiado, WebP/PNG → JPEG, arquivo de 16 MB + 1 passa com < 2 MB, não-imagem → `Failed`. OK.
2. `verifyRoborazziDevDebug` verde com os snapshots novos `chatReplace` (dark/light). `StitchGoldTest`: `chatP` segue 0,25 % dark / 0,64 % light depois da extração do diálogo; `chatReplace` contra o gold `chatP` só reportado (9,1 % / 15,9 %: a copy de uma linha deixa o cartão mais baixo). OK.
3. Emulador (AVD `Medium_Phone` 780×1688 @320, `-camera-back virtualscene`) + fake, `tools/capture-photo.sh dark|light`: JPEG 2000 px da galeria → 2000×1116 < 2 MB; câmera → JPEG ≤ 2048; **50 MP (8160×6120) com EXIF orientação 6 → 1536×2048 em pé, < 2 MB, sem "Foto grande demais."**, arquivo guardado sem EXIF; o antigo "> 16 MB" passa; WebP → JPEG 2000×1116. `chatF` região 1,98 % / 1,64 %. OK.
4. Emulador + fake, `tools/capture-replace.sh dark|light`: "4 esfihas" na Ceia vazia grava direto; "tambem tomei 2 copos de suco" → diálogo "Ceia tem 880 kcal. Fica com 1220 kcal." → Outra refeição abre o Trocar → Substituir → recibo "Atualizado em", Home 1220, `meal_log` com 1 linha na Ceia. Enter insere quebra (`Linha1&#10;linha2`) e não envia. OK. Regressão `tools/capture-chat.sh light` verde; `chatE` 1,21 %, `chatT` 1,82 %, `chatP` 0,51 %.
5. `docs/qa/android/current/{dark,light}/chatReplace.png` vs gold `chatP`. Diferenças: título e texto (esperado); corpo em 1 linha, cartão ~50 px mais baixo e centrado; ícone do botão primário (`SwapHoriz` no lugar de `FastForward`). Iguais: raio 24, caixa do ícone, pílula de horário, CTA `#f3f5f7`/`#111`, pílula secundária `surf2`, scrim. OK.
6. **Pendente (manual, dono):** repetir o caso das esfihas + suco no APK distribuído.

Observação para o dono: na substituição, o texto da timeline segue a regra 12 (o que o user escreveu na última mensagem, ex. "também tomei 2 copos de suco"), não a descrição da refeição inteira. Se quiser a descrição da IA ou a lista de itens, é um plano novo.

## Fora de escopo

- Pergunta em bolha separada, bolhas, "IA ATIVA", anexo com preview: [A19](../a19-chat-visual.md).
- Limpar registros duplicados antigos (ADR-017 regra 6).

## Riscos e controles

- **Substituir apaga o registro errado:** confirmação com os dois números; transação única.
- **OOM ao decodificar 200 MP:** `inSampleSize` antes do decode; fallback `TooLarge`.

## Critérios de aceite

- Nunca dois registros novos no mesmo slot pelo Chat.
- Enter pula linha.
- Nenhuma foto de câmera comum dá "Foto grande demais."; request de foto < 2 MB.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
