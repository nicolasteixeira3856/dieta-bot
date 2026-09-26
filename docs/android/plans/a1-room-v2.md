# Plano — A1 Room v2

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: ADR-012 + [room-v2.md](../specifications/room-v2.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a1-room-v2.md`. Implemente o plano aprovado.

## Objetivo

Room v2 no client. Perfil, slots, skip, macros no log, mensagens, digest. Sem tela nova. App v1 continua abrindo.

## Fontes de verdade

- [room-v2.md](../specifications/room-v2.md)
- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)
- [010-room](../../decisions/010-room.md)

## Escopo de implementacao

### 1. Domain puro

- TmbCalculator.kt Mifflin-St Jeor. Teste: male 27y 116kg 180cm = 2072.
- MacroSplit.kt 30/40/30. Teste: 2000 → P 150 C 200 G 67.
- SlotClock.kt minutesFromMidnight America/Sao_Paulo. Slot vigente = ultimo com minutes <= agora; se agora < primeiro, vigente = primeiro.

### 2. Entities Java + kapt

- ProfileEntity + sex, ageYears, heightCm, weightKg, proteinTargetG, carbTargetG, fatTargetG.
- MealSlotEntity, SlotSkipEntity, ChatMessageEntity, DayDigestEntity.
- MealLogEntity + slotId, carbs, fat, source. window permanece.
- DayEntity intacta.
- NutriDatabase version=2, exportSchema=true, MIGRATION_1_2 ALTER/CREATE. Sem destructive fallback.

### 3. DayRepository

- observeToday inclui slots, skips, logs C/G, workout.
- addLog INSERT + DELETE skip date+slot.
- addSkip INSERT skip + DELETE logs date+slot.
- wipeToday: spec regra 9.
- saveSlots replace all.
- insertMessage / observeMessages.
- upsertDigest: seq=3 overwrite seq=1.
- saveProfile estende assinatura; call sites v1 passam defaults sem UI nova.

## Arquivos e areas afetadas

- apps/android/app/src/main/java/com/nutri/android/data/*
- domain/TmbCalculator.kt MacroSplit.kt SlotClock.kt
- test domain + RoomV2Test

## Validacao planejada

1. ./gradlew.bat :app:testDebugUnitTest
2. addLog soma duas linhas no slot
3. addSkip remove logs; addLog remove skip
4. wipeToday preserva chat_message
5. install em cima de v1 sem crash

## Fora de escopo

Telas novas. Push, foto, /v1/chat. Memoria A8. Apagar ChipRule.

## Riscos e controles

- Migration destrutiva: MIGRATION_1_2 explicita. fallbackToDestructiveMigration = false.

## Criterios de aceite

- Database version 2.
- Testes da spec verdes.
- UI v1 ainda abre.

## Encerramento

Ciclo SDD.
