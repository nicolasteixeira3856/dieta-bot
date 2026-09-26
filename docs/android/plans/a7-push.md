# Plano — A7 push no horario do slot

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A3 + A5 + [memoria-push.md](../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a7-push.md`. Implemente o plano aprovado.

## Objetivo

Exact alarm por slot. So se empty. Registrar abre Chat. Pular grava skip.

## Escopo de implementacao

- POST_NOTIFICATIONS + SCHEDULE_EXACT_ALARM. Android 14 exact: se negar, fallback inexact documentado.
- AlarmScheduler.resync(slots) hoje + 00:05.
- Receiver: se eaten ou skipped, return.
- Copy: {name}. Ainda nao registrou.
- Registrar → Chat. Pular → addSkip + cancela notify.
- Resync no boot, midnight SP e saveSlots.
- Sem Firebase. Sem 16:30 hardcode.

## Validacao planejada

- Unit resync 4 slots → 4 intents; skip existente nao agenda.

## Fora de escopo

Fds especial so almoco+janta.

## Criterios de aceite

- Slots novos em Config re-agendam.
- Slot gravado nao notifica.

## Encerramento

Ciclo SDD.
