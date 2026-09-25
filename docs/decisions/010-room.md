# 010 — Room for profile, day, meal_log

Date: 2026-09-25

## Decision

Local day state lives in one Room 2.6.x database `NutriDatabase` version 1. The ViewModel talks to `DayRepository` only. Domain formulas stay pure.

Chosen: **B**.

## Why B

- **A** — keep one DataStore JSON blob (`SavedDay`). Profile, workout, chips, and logs share a single document. A new calendar date either copies yesterday’s meals or requires wiping the blob and re-saving the profile by hand.
- **B** — Room tables `profile` (id = 1), `day` (PK date `yyyy-MM-dd` in America/Sao_Paulo), `meal_log` (rows keyed by that date). Profile survives midnight. Today’s logs are a query, not a copied list. One source of truth after import.
- **C** — two DataStore files (profile + day). Logs stay a blob. Two files can diverge. No date query.

B matches rollover: `observeToday` keys off `SaoPaulo.date(now)`. A new date yields empty logs without copying yesterday.

## Schema (version 1)

`profile` — single row `id = 1`: `ceilingMode` TEXT, `kcalSame` / `kcalWeekday` / `kcalWeekend` INT, `kcalDays` TEXT JSON int[7], `eat` TEXT, `pct` INT, `onboardingDone` INT, `firstDay` TEXT (`yyyy-MM-dd` or empty).

`day` — PK `date` TEXT `yyyy-MM-dd`: `workoutKcal` INT NULL, `removedWindows` TEXT JSON string list, `askedWindows` TEXT JSON string list.

`meal_log` — `id` INTEGER PK auto, `date` TEXT NOT NULL, `window` TEXT, `text` TEXT, `kcal` INT, `p` INT, `stable` INT.

Room compiler stays on kapt (same pipeline as Hilt). No KSP.

`@Database` / `@Entity` / `@Dao` / `@TypeConverter` are Java: Room 2.6.1’s processor cannot read Kotlin 2.2 metadata. The repository and ViewModel stay Kotlin.

## Rollover

Today is `SaoPaulo.date(now)` only. `addLog` writes `meal_log` under that date. When the clock is the next SaoPaulo calendar date, `observeToday` returns empty logs and does not copy yesterday’s meals. Profile fields remain.

## Import

If the DataStore file `nutri_day` still has preference `day` JSON (`SavedDay`):

1. Read `SavedDay`.
2. Write `profile`.
3. Write `day` + `meal_log` under `firstDay` if set, else today.
4. Clear the DataStore key.

After that, day state is not also in DataStore. `DayStore.kt` is gone. DataStore remains only so this one-shot import can read the old file.

ADR 001 / 002 “no Room, DataStore JSON for the day” is superseded for the Android client.

## Refused

Firebase. A second live store beside Room. KSP. Copying yesterday’s meals. Cap on eat-back. New screens.
