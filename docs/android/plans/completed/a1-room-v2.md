# Plano — A1 Room v2 (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 26/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: ADR-012 + [room-v2.md](../../specifications/room-v2.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a1-room-v2.md. Analise e implemente o plano aprovado.`

## Objetivo

Room v2 no client. Perfil, slots, skip, macros no log, mensagens, digest. Sem tela nova. App v1 continua abrindo.

## Fontes de verdade

- [room-v2.md](../../specifications/room-v2.md)
- [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md)
- [010-room](../../../decisions/010-room.md)

## Escopo de implementacao

### 1. Domain puro

- TmbCalculator.kt Mifflin-St Jeor. Teste: male 27y 116kg 180cm = 2155 (prefill 2160). *Corrigido na implementação: o valor 2072 original não corresponde à fórmula da spec [perfil-onboarding](../../../produto/specifications/perfil-onboarding.md) regra 1 (10w+6.25h−5a+5); spec prevalece.*
- MacroSplit.kt 30/40/30. Teste: 2000 → P 150 C 200 G 67.
- SlotClock.kt minutesFromMidnight America/Sao_Paulo. Slot vigente = ultimo com minutes <= agora; se agora < primeiro, vigente = primeiro.

### 2. Entities Kotlin Data Classes + KSP (Eliminacao de Java e kapt)

- Migracao de todos os arquivos Java legados do Room (`DayDao.java`, `DayEntity.java`, `MealLogDao.java`, `MealLogEntity.java`, `ProfileDao.java`, `ProfileEntity.java`, `NutriConverters.java`, `NutriDatabase.java`) para Kotlin puro (`data class`, `interface`, `@Database`).
- Troca de `kapt` por `ksp` para o compilador do Room no Gradle.
- Novas tabelas em Kotlin: `MealSlotEntity`, `SlotSkipEntity`, `ChatMessageEntity`, `DayDigestEntity`.
- `ProfileEntity` com sex, ageYears, heightCm, weightKg, proteinTargetG, carbTargetG, fatTargetG.
- `MealLogEntity` com slotId, carbs, fat, source. window permanece.
- `DayEntity` em Kotlin intacta.
- `NutriDatabase` version=2, exportSchema=true, MIGRATION_1_2 ALTER/CREATE. Sem destructive fallback.

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

## Resultado da implementação

### Entregue

- `domain/`: `TmbCalculator` (Mifflin-St Jeor + prefill arredondado a 10), `MacroSplit` (30/40/30), `SlotClock` (minutos em America/Sao_Paulo, slot vigente).
- `core/database/`: `MealSlotEntity`, `SlotSkipEntity`, `ChatMessageEntity`, `DayDigestEntity` + DAOs. `ProfileEntity` e `MealLogEntity` estendidos (`@ColumnInfo(defaultValue)` igual à migration). FK `meal_log.slotId → meal_slot.id ON DELETE SET NULL` + índice.
- `NutriDatabase` version 2, `exportSchema = true`, schemas em `apps/android/app/schemas/` (1.json e 2.json). `MIGRATION_1_2` em `Migrations.kt`: ALTER em `profile`, rebuild de `meal_log` (SQLite não adiciona FK via ALTER), CREATE das 4 tabelas. Sem destructive fallback.
- `DayRepository`: `observeToday` com slots, skips, C/G, source, perfil v2; `addLog` (INSERT + apaga skip, transação); `addSkip`; `wipeToday` (regra 9); `saveSlots` (replace all via `@Upsert`, preserva id dos slots mantidos; removidos deixam logs órfãos → “Outros”); `insertMessage`/`observeMessages` (60 dias); `upsertDigest` (máx. 2; o 3º sobrescreve o mais antigo, seq 1 primeiro); `digestsToday`.
- `saveProfile`: campos v2 opcionais; `null` preserva o valor gravado. Call sites v1 inalterados.

### Já existente

- Entities Kotlin + Room via KSP vieram do [A0 arch-refactor](a0-arch-refactor.md). Hilt segue em kapt (fora de escopo).

### Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 67/67 passando (3 execuções, 2 com `--rerun`).
2. `RoomV2Test`: addLog soma duas linhas no slot; addSkip remove logs e addLog remove skip; wipeToday preserva chat_message, perfil, slots e treino; saveSlots preserva ids; saveProfile v1 preserva corpo/macros; digest 3º → seq 1; fio 60 dias; version 2.
3. `MigrationV1V2Test`: banco v1 construído a partir de `1.json` (DDL + identity hash), migrado por Room v2: dados, ids e defaults preservados; autoincrement continua; FK SET NULL ativa.
4. `ProfileMathTest`: TMB 2155/1989/null, prefill 2160, split 150/200/67, SlotClock.
5. Install em cima de v1 (emulador `Medium_Phone`, API do AVD local): APK v1 (`87fd073`) instalado e aberto; identity hash real `5ff2b440…` = `1.json`; DB populado (perfil + 2 logs + treino 320); APK v2 instalado com `install -r`. App abriu sem crash; Home v1 mostra 1.368 restantes e os 2 logs. DB pós-migração: `user_version 2`, hash `4c5acf10…` = `2.json`, 7 tabelas, defaults v2 aplicados, FK presente.

### Ajuste de testes existentes

- `DayActionsTest` e `OnboardingViewModelTest` deixam de usar executores diretos (`it.run()`). Com `withTransaction` nas escritas, executor direto + `UnconfinedTestDispatcher` faz a Flow do Room re-consultar na thread da transação suspensa (`assertNotSuspendingTransaction`). Em produção os executores são threads reais.

## Encerramento

Ciclo SDD.
