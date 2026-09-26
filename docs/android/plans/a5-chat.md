# Plano — A5 Chat + chips + prompt

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: S2 + A4 + [chat.md](../../produto/specifications/chat.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a5-chat.md`. Implemente o plano aprovado.

## Objetivo

Chat funcional: POST /v1/chat, bolhas, chips Gravar/Trocar/Pular, Room messages, compact client-side.

## Escopo de implementacao

### 1. ChatScreen

- LazyColumn invertido. Bolha user direita, IA esquerda. Sem avatar.
- Composer TextField + clip (A6 liga; neste plano disabled) + send.
- Send: insert message user, POST /v1/chat, insert assistant. Falha: bolha nao deu, tenta de novo.
- Chips se estimate != null: Gravar {slot}, Trocar lista slots, Pular {slot}.
- Tap Gravar: addLog com estimate. Sem 2o POST.
- suggested_slot fora da lista: esconde Gravar, mostra so Trocar.

### 2. PromptBuilder

- perfil + snapshot dia + <=12 raw + <=2 digest.
- rawCount >= 12 → proximo send compact=true, grava day_digest, digest nao vira bolha.
- Max 2 digest. 3o compact upsert seq=1.
- Se S3 fora: compact no-op, corta raw velhas do PROMPT.
- memory="" ate A8.

### 3. Rede

- postChat. OkHttp 60s. ChatIn/ChatOut. Nao chama estimate/fit.
- UI 60d com separador de data.

## Validacao planejada

- Unit PromptBuilder 12 msgs → compact; 13a raw fora do IN.
- Unit Gravar chama addLog, nao 2o POST.
- Captura Chat dark/light + chips.

## Fora de escopo

Foto A6. Push. Memoria A8. Streaming. Apagar T1/T2/T3.

## Criterios de aceite

- Circulo da Home muda so apos Gravar.
- Trocar slot grava no escolhido.
- ChatScreen nao chama /v1/estimate.

## Encerramento

Ciclo SDD.
