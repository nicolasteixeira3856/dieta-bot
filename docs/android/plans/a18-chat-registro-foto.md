# Plano — A18 Chat: refeição consolidada, teclado e foto 2048 px

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat`, `core/photo`, `domain/PhotoGate.kt`, `core/database` repository)
- Pré-requisitos: [S8](../../server/plans/pending_manual_validation/s8-chat-json-slot-consolidado.md) no ar (slot sugerido e estimativa da refeição inteira). Aceita o [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md) e o [ADR-018](../adrs/ADR-018-foto-2048.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a18-chat-registro-foto.md`. Implemente o plano aprovado.

## Objetivo

Os ajustes de comportamento do Chat que não mudam gold:

1. Uma refeição tem um registro. Gravar num slot já registrado pede confirmação e substitui.
2. Enter do teclado pula linha.
3. A foto sai reduzida a 2048 px e o "Foto grande demais." some na prática.

Pergunta em bolha separada, formato das bolhas e anexo com preview dependem de gold e ficam no [A19](a19-chat-visual.md).

## Fontes de verdade

- [chat](../../produto/specifications/chat.md), [memoria-push](../../produto/specifications/memoria-push.md), [home-timeline](../../produto/specifications/home-timeline.md).
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

## Fora de escopo

- Pergunta em bolha separada, bolhas, "IA ATIVA", anexo com preview: [A19](a19-chat-visual.md).
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
