# Plano — A3 Config + wipe + treino do dia

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A1 + A2 + [memoria-push.md](../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a3-config-wipe-treino.md`. Implemente o plano aprovado.

## Objetivo

Tela Config. Edita teto, slots, alvos, eat-back, treino hoje. Wipe de hoje se mudar teto.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{cfg,wipe}.png` e `docs/qa/stitch/light/{cfg,wipe}.png`
- spec perfil-onboarding / memoria-push
- ADR-012

## Escopo de implementacao

1. Rota Config. Icone topo dir na Home.
2. Secoes: teto 3 modos, eat-back, alvos P/C/G, slots 2-6, treino hoje NumberField vazio=null.
3. Mudou teto: dialogo Apaga os logs de hoje? O chat fica. Confirmar wipeToday. Cancelar nao salva teto.
4. Mudou nome/hora: saveSlots. Sem wipe.
5. Treino hoje → day.workoutKcal. Rollover SP zera.
6. Back → Home.

## Validacao planejada

- Teste wipeToday. Mudar hora nao chama wipe.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/{cfg,wipe}.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/{cfg,wipe}.png` com diff list aprovada.

## Fora de escopo

Push A7. Chat. Memoria A8.

## Criterios de aceite

- Treino vazio ⇒ credito 0.
- Wipe nao apaga chat_message nem profile.

## Encerramento

Ciclo SDD.
