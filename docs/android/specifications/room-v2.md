# Especificação — Room v2

## Estado

Room v1: `profile`, `day`, `meal_log`.

## Contexto e objetivo

Persistir perfil completo e o dia estruturado. Sem UI neste spec.

## Escopo

`NutriDatabase` version = 2. Migration 1→2. `exportSchema = true`.

## Fora de escopo

Telas, push, foto, `/v1/chat`, arquivo de memória.

## Regras funcionais

1. `profile` ganha: `sex` TEXT DEFAULT '', `ageYears` INT DEFAULT 0, `heightCm` INT DEFAULT 0, `weightKg` REAL DEFAULT 0, `proteinTargetG` INT DEFAULT 150, `carbTargetG` INT DEFAULT 200, `fatTargetG` INT DEFAULT 67. Teto e eat-back v1 ficam.
2. `meal_slot`: `id` PK auto, `name` TEXT, `minutesFromMidnight` INT 0..1439, `sortOrder` INT. Sem unique de nome.
3. `meal_log` ganha: `slotId` INT NULL FK ON DELETE SET NULL, `carbs` INT DEFAULT 0, `fat` INT DEFAULT 0, `source` TEXT DEFAULT 'user'. `window` permanece. Segundo Gravar = INSERT (soma).
4. `slot_skip` PK (`date`,`slotId`). Nunca auto-insert. `addLog` apaga skip. `addSkip` apaga logs do slot+date.
5. `chat_message`: `date`, `role` user|assistant, `text`, `createdAtEpochMs`, estimate* nullable, `photoPath`. UI 60 d.
6. `day_digest` PK (`date`,`seq`). seq 1 ou 2. 3º bloco substitui seq=1.
7. `day.workoutKcal` intacto.
8. TMB Mifflin-St Jeor. Macros 30/40/30.
9. `wipeToday`: DELETE meal_log, slot_skip, day_digest WHERE date. Preserva chat_message, profile, slots, workoutKcal.
10. Snapshot do prompt: date, tetoEfetivo, eaten*, remaining, slots[{name,minutes,status,lines}], workoutKcal.

## Estados e falhas

Migration 1→2 sem destructive fallback. slotId órfão → “Outros”.

## Decisões relacionadas

- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)
- [010](../../decisions/010-room.md)

## Planos relacionados

- `docs/android/plans/a1-room-v2.md`

## Critérios de aceite funcionais

Testes: migration 1→2, TmbCalculator, MacroSplit, addLog soma, addSkip apaga logs, wipeToday preserva chat_message. version=2. Sem UI nova.
