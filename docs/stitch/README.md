# stitch

## Propósito

Gate de design. Guarda os prompts que o dono executa no Google Stitch `Nutri` (`6282733070135794645`) e a conferência dos golds que eles geram. Regra: [docs/sdd/README.md § Gate Stitch](../sdd/README.md#gate-stitch).

## Tipo e ownership

- Tipo: gate de design.
- Executor do prompt: **o dono** (passo manual). Verificação: o agente.
- Código: nenhum código de app. Toca só `docs/qa/stitch/`, `tools/export-stitch.mjs`, `tools/check-stitch.mjs` e a lista de golds (`AGENTS.md`, `docs/qa/README.md`).

## Escopo

- Prompt exato para o Stitch no topo de cada plano, com as instruções extras (telas a selecionar, anexos) logo abaixo.
- Checklist visual do que o prompt precisa produzir.
- Registro dos IDs de tela novos.

## Fora de escopo

- Layout ou comportamento do app: [android](../android/README.md).
- Decisão de produto: [produto](../produto/README.md). Um gate só executa uma decisão já tomada.

## Fronteiras

- Um gate bloqueia um ou mais planos do android. Eles o listam em Pré-requisitos.
- Um gate falho (o Stitch não produziu o esperado) para tudo: o agente reporta o que falta e não segue.

## Ciclo de vida

| Situação | Estado | Local |
|---|---|---|
| Esperando o dono rodar o prompt | `Aguardando o dono no Stitch` | `plans/<gate>.md` |
| Dono rodou; agente verificando | `Em verificação` | `plans/<gate>.md` |
| Checklist inteiro passou, golds exportados | `Concluído` | `plans/completed/<gate>.md` |
| Cancelado pelo dono | `Cancelado` | `plans/cancelled/<gate>.md` |

Não há "aprovação" de gate: o prompt já é o que foi decidido nos ADRs. O dono rodar o prompt e avisar o agente é o gatilho da verificação.

## Estado atual

Cinco gates esperando o dono.

## Índice

### Planos

1. [ST1 Chat: bolhas, pergunta separada, anexo (`chatA`)](plans/st1-chat.md) — bloqueia [A19](../android/plans/a19-chat-visual.md).
2. [ST2 Home: atalho de treino (`homeW`)](plans/st2-home-treino.md) — bloqueia [A22](../android/plans/a22-treino-home.md).
3. [ST3 Seletor de horário em rodas (`o3t`)](plans/st3-seletor-horario.md) — bloqueia [A21](../android/plans/a21-seletor-horario.md).
4. [ST4 Refeições por dia da semana](plans/st4-refeicoes-por-dia.md) — bloqueia [A24](../android/plans/a24-refeicoes-por-dia.md). Rodar depois do ST3 (usa o `o3` já com o seletor novo).
5. [ST5 Chat: texto longo demais no composer (`chatX`)](plans/st5-chat-texto-longo.md) — bloqueia [A25](../android/plans/a25-limite-texto-composer.md). Independe do ST1.

ADRs que os gates executam: [ADR-020](../produto/adrs/ADR-020-estados-novos-chat-home-horario.md), [ADR-021](../produto/adrs/ADR-021-refeicoes-por-dia.md), [ADR-022](../produto/adrs/ADR-022-limite-texto-chat.md).
