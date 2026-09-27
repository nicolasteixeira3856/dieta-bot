# Plano — A5b Ligar compact no Chat (Concluído)

- Estado: Concluído
- Data: 27/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`, `tools/fake-chat-server.mjs`, `tools/capture-chat.sh`
- Pré-requisitos: [A5 (Concluído)](a5-chat.md) + [S3 (Concluído)](../../../server/plans/completed/s3-compact.md) + [chat.md](../../../produto/specifications/chat.md)

## Gate de autorização

> Aprovado pelo usuário via comando:
> ``/goal Aprovo o plano `docs/android/plans/a5b-ligar-compact.md`. Implemente o plano aprovado.``

## Objetivo

Cumprir a regra 9 da spec do Chat, que hoje o app não cumpre. O A5 implementou o compact, mas o deixou desligado (`PromptBuilder.COMPACT_ENABLED = false`) porque o server respondia 400. O S3 ligou o `compact=true` no server. Nenhum plano ficou dono de ligar o lado do app. Hoje, com 12 raw, as mensagens mais velhas só saem do prompt, sem virar digest.

## Fontes de verdade

- [chat.md](../../../produto/specifications/chat.md) regras 8, 9 e 10.
- [v1-chat.md](../../../server/specifications/v1-chat.md) regra 7 e [api-contract.md](../../../api-contract.md) seção `compact=true`.
- [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md) item 6.
- [room-v2.md](../../specifications/room-v2.md) (`day_digest`, máx 2 por dia).

## Escopo de implementação

### 1. Ligar a flag

- `PromptBuilder.COMPACT_ENABLED = true`. Remover o comentário "Flip in S3".
- Fluxo do A5, sem mudança de regra: ao enviar com ≥ 12 raw desde o último digest (e depois do último marcador `wiped`, A3), 1º POST `compact=true` → `upsertDigest(digest)` → prompt reconstruído (o bloco raw some, entram ≤ 2 digests) → 2º POST normal.
- Máx 2 digests por dia. O 3º sobrescreve o mais velho (`upsertDigest`, já existe).

### 2. Falha do compact não derruba o turno

- Hoje a chamada de compact e a do turno estão no mesmo `runCatching` em `ChatViewModel.post`. Com a flag ligada, um erro de rede no compact viraria a bolha "Não deu" da mensagem do user.
- Isolar: se o compact falhar (exceção, timeout, `digest: null`), nada é gravado e o turno normal segue com as 12 raw mais novas (comportamento atual com a flag desligada). A próxima mensagem tenta compactar de novo.
- O digest nunca vira bolha e não entra em `chat_message`.

### 3. Fake server e captura

- `tools/fake-chat-server.mjs`: `compact=true` responde `{reply: "", estimate: null, digest: "<resumo fixo>", model}`; `messages` vazio → 422, como o server real. Contador separado de chamadas de compact em `/__mode`.
- `tools/capture-chat.sh`: após o fluxo atual, enviar mensagens até 12 raw e checar no sqlite: 1 linha em `day_digest`, 2 POSTs nesse envio (compact + turno), nenhuma bolha nova além da troca do user.

## Arquivos e áreas afetadas

- `apps/android/app/src/main/java/com/nutri/android/feature/chat/PromptBuilder.kt`
- `apps/android/app/src/main/java/com/nutri/android/feature/chat/ChatViewModel.kt`
- `apps/android/app/src/test/java/com/nutri/android/feature/chat/ChatViewModelTest.kt`
- `apps/android/app/src/test/java/com/nutri/android/feature/chat/PromptBuilderTest.kt`
- `tools/fake-chat-server.mjs`, `tools/capture-chat.sh`
- Docs: [chat.md](../../../produto/specifications/chat.md) (Estado), README android, matriz.

## Validação planejada

1. `./gradlew.bat :app:testDebugUnitTest`, com testes novos em `ChatViewModelTest`:
   - 12 raw → um envio faz 2 requests: o 1º com `compact=true` e as 12 messages, o 2º normal com `digests=[d1]` e sem as raw compactadas; `day_digest` com 1 linha.
   - 3 compactações no dia → 2 digests; o mais velho foi substituído.
   - compact lança exceção ou volta `digest: null` → turno normal responde, nenhum digest gravado, nenhuma bolha de falha.
   - com < 12 raw, nenhum `compact=true`.
   - digest não aparece em `ChatUiState.items`.
   - depois de um wipe (marcador `wiped`), a contagem de raw recomeça.
   - `PromptBuilderTest`: o default passa a compactar em 12.
2. Emulador contra o fake: `tools/capture-chat.sh dark|light` com as novas checagens. Gate visual do Chat (chatE/T/P) segue ≤ 2%: o digest não muda a tela.
3. Opcional, manual: APK contra o server real (`API_PUBLIC_URL` + `INVITE_CODE` locais), 6 trocas e depois a 7ª; conferir `day_digest` no sqlite.

## Fora de escopo

- `server/` (S3 já entregue).
- Memória (A8), foto (A6), push (A7).
- Mudar o limite de 12 raw ou de 2 digests.
- Mostrar o digest na UI.

## Riscos e controles

- **Custo/latência dobrada no envio que compacta:** 1 envio a cada ~6 trocas faz 2 chamadas; timeout de 60 s por chamada; o indicador de carregando cobre as duas.
- **Compact falha em loop (server fora):** cada envio tenta de novo e cai no turno normal; não bloqueia o user. Sem retry automático extra.
- **Digest perde número:** o snapshot do dia vai em todo turno (regra 10); o digest é só prosa.

## Critérios de aceite

- Com 12 raw, o próximo envio grava 1 `day_digest` e o turno sai com `digests` não vazio.
- Nunca mais de 2 digests por dia.
- Falha do compact não gera bolha de falha nem perde a mensagem do user.
- Digest nunca aparece como bolha.

## Resultado da implementação

### Entregue

- `PromptBuilder.COMPACT_ENABLED = true`. Com ≥ 12 raw desde o último digest (e depois do último `wiped`), o envio faz `compact=true` → `upsertDigest` → turno reconstruído com `digests` e sem as raw compactadas.
- `ChatViewModel.post`: o compact saiu do `runCatching` do turno. Exceção, timeout, `digest: null` ou em branco → nada gravado, o turno segue com as 12 raw mais novas, a próxima mensagem tenta de novo.
- `tools/fake-chat-server.mjs`: `compact=true` responde digest fixo; `messages` vazio → 422; `/__calls` conta `compacts`.
- `tools/capture-chat.sh`: após as capturas, 5 envios (12 raw) sem compact, 6º envio com compact + turno, checagens no sqlite.

### Decisões tomadas na implementação

- Testes de compact numa classe própria (`ChatCompactTest`) com relógio que avança 1 s por leitura: o corte do digest é por `createdAtEpochMs >`, e o relógio fixo do `ChatViewModelTest` empataria todos os timestamps.

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 138/138. Novos: `ChatCompactTest` (5 raw-pares sem compact; 12 raw → 2 requests, compact com 12 messages, turno com `digests=["resumo 1"]` e `messages` vazio, digest fora de `chat_message` e da UI; 19 envios → 3 compacts e 2 digests, "resumo 1" substituído; falha por exceção / `null` / branco → turno responde, sem digest, sem bolha de falha, próximo envio compacta; wipe zera a contagem), `PromptBuilderTest` (default compacta em 12, não em 11).
2. Emulador contra o fake (APK com `-PAPI_PUBLIC_URL=http://10.0.2.2:8765`), `tools/capture-chat.sh light` e `dark`: todas as checagens do A5 + as novas — sem compact abaixo de 12 raw; 6º envio = 1 compact + 1 turno; turno respondido; digest não desenhado; `day_digest` = 1 linha; digest fora de `chat_message`.
3. Gate visual do Chat inalterado: chatE 1,34 / 1,33%, chatT 1,42 / 1,90%, chatP 0,43 / 0,63% (dark / light). Splash e O1–O4 recapturados, 10/10 no gate.
4. Validação manual opcional contra o server real: não executada.

## Encerramento

Ciclo SDD.
