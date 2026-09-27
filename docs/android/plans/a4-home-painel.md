# Plano — A4 Home painel

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A1 A2 A3 + [home-timeline.md](../../produto/specifications/home-timeline.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a4-home-painel.md`. Implemente o plano aprovado.

## Objetivo

Home painel oficial Stitch Gold. Sem composer. Consumidas + P/C/G com cores semânticas + data + timeline contínua com refeição consolidada + Config + FAB.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{home0,home1,homeX}.png` e `docs/qa/stitch/light/{home0,home1,homeX}.png`
- spec [home-timeline.md](../../produto/specifications/home-timeline.md)
- ADR-012

## Escopo de implementacao

1. Remove composer, CTA, chips de janela.
2. Circulo central de saldo/consumo com tipografia de destaque (34pt w590).
3. Linha P/C/G consumido/alvo com cores semânticas de macronutrientes (P: menta, C: âmbar, G: ouro, estouro: bad).
4. Data dd/MM/yyyy SP.
5. Timeline contínua com linha guia vertical e marcadores de nó. Refeições consolidadas em linha única (ex: "520 kcal · 28P · 52C · 22G"). Skip = Pulado. Vazio: "Nenhum registro · Toque para pular".
6. Config topo dir. FAB → Chat.
7. ChatScreen stub scaffold + back. A5 substitui o corpo.
8. T1/T2/T3 rotas ficam. Home nao aponta pra elas.
9. reservedUpcoming = 0.
10. Disclaimer 1 linha: "Estimativa, não consulta."

## Validacao planejada

- test groups by slotId; skip addSkip.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/{home0,home1,homeX}.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/{home0,home1,homeX}.png` com diff list aprovada.

## Fora de escopo

Chat real A5. Foto. Push. Apagar T1/T2/T3.

## Criterios de aceite

- Zero TextField na Home.
- Circulo = soma kcal hoje.
- FAB visivel.
- Tap slot vazio → skip.

## Encerramento

Ciclo SDD.
