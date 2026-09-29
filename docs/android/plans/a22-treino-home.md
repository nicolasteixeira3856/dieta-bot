# Plano — A22 Treino na Home

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/home`, componente de campo/sheet compartilhado com `feature/config`)
- Pré-requisitos: **[ST2](../../stitch/plans/st2-home-treino.md) em `stitch/plans/completed/`** (golds `home0`, `home1`, `homeX` alterados + `homeW`). [A20](a20-polimento-geral.md) concluído (botões iguais do sheet).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a22-treino-home.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar `docs/stitch/plans/completed/st2-home-treino.md` e `homeW.png` nos golds. Se não, parar e avisar o dono.

## Objetivo

O usuário informa o kcal do treino direto da Home e vê ali quanto ele rendeu na meta.

## Fontes de verdade

- Golds `home0`, `home1`, `homeX`, `homeW`. [home-timeline](../../produto/specifications/home-timeline.md). Fórmulas do `AGENTS.md` (credit, effectiveCeiling).

## Escopo de implementação

### 1. Linha "Treino de hoje" (Home)

- Abaixo do card de macros, antes da timeline (gold).
- Sem treino hoje: valor `Informar` em gold.
- Com treino: `{kcal} kcal · +{crédito} na meta`. Crédito pela fórmula do `AGENTS.md` (0% → `+0 na meta`; a linha continua).
- Toque → sheet `homeW`.

### 2. Sheet "Treino de hoje" (`homeW`)

- Campo numérico 28pt, sufixo `kcal`, uma linha de crédito calculada ao vivo (`+{n} kcal na meta de hoje (compensação {pct}%)`; 0%: `Compensação desativada na Config`).
- Salvar / Cancelar iguais (componente do A20). Salvar grava o mesmo campo `day.workoutKcal` que a Config usa hoje. Campo vazio + Salvar = sem treino (crédito 0).
- Um componente só para o sheet da Home e o editor de treino da Config (mesmo estado, mesma validação).

### 3. Specs

- `home-timeline.md`: regra nova da linha de treino.
- `memoria-push.md` § Config: o treino também pode ser informado pela Home.

## Arquivos e áreas afetadas

- `feature/home/HomePanel.kt`, `HomePanelScreen.kt`, `HomePanelViewModel.kt`.
- `feature/config/ConfigScreen.kt` (reuso do componente).
- `tools/capture-home.sh` (estado `homeW`), Roborazzi.

## Validação planejada

1. `testDevDebugUnitTest`: crédito na linha para 0%, 50% e 100%; salvar pela Home muda a meta da Home e o valor na Config; vazio = crédito 0.
2. `verifyRoborazziDevDebug`.
3. Capturas `home0`, `home1`, `homeX`, `homeW` vs gold, dark e light, com diff.
4. Manual (dono): informar 350 kcal na Home e ver a meta subir.

## Fora de escopo

- Histórico de treinos; integração com relógio ou Health (proibido).

## Critérios de aceite

- Da Home ao treino salvo em 3 toques; número visível na Home; 4 capturas batendo com o gold.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
