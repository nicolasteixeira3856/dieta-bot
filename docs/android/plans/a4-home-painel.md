# Plano — A4 Home painel

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A1 A2 A3 + [home-timeline.md](../../produto/specifications/home-timeline.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a4-home-painel.md`. Implemente o plano aprovado.

## Objetivo

Home = doodle. Sem composer. Consumidas + P/C/G + data + timeline + Config + FAB.

## Escopo de implementacao

1. Remove composer, CTA, chips de janela.
2. Circulo eatenKcal 34pt.
3. Linha P/C/G consumido/alvo. Estouro token bad.
4. Data dd/MM/yyyy SP.
5. Timeline por meal_slot. Skip = Pulado. Vazio: tap Pular {nome}.
6. Config topo dir. FAB → Chat.
7. ChatScreen stub scaffold + back. A5 substitui o corpo.
8. T1/T2/T3 rotas ficam. Home nao aponta pra elas.
9. reservedUpcoming = 0.
10. Disclaimer 1 linha.

## Validacao planejada

- test groups by slotId; skip addSkip.
- Captura Home vs doodle, nao vs gold t0.

## Fora de escopo

Chat real A5. Foto. Push. Apagar T1/T2/T3.

## Criterios de aceite

- Zero TextField na Home.
- Circulo = soma kcal hoje.
- FAB visivel.
- Tap slot vazio → skip.

## Encerramento

Ciclo SDD.
