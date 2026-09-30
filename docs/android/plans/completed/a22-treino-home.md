# Plano — A22 Treino na Home

- Estado: Concluído
- Aprovação manual: 30/09/2026 (dono: "Quero que você passe todos os planos que estão pendentes de validação manual para completo.")
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/home`, componente de campo/sheet compartilhado com `feature/config`)
- Pré-requisitos: **[ST2](../../../stitch/plans/completed/st2-home-treino.md) em `stitch/plans/completed/`** (golds `home0`, `home1`, `homeX` alterados + `homeW`). [A20](a20-polimento-geral.md) concluído (botões iguais do sheet).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a22-treino-home.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar `docs/stitch/plans/completed/st2-home-treino.md` e `homeW.png` nos golds. Se não, parar e avisar o dono.

## Objetivo

O usuário informa o kcal do treino direto da Home e vê ali quanto ele rendeu na meta.

## Fontes de verdade

- Golds `home0`, `home1`, `homeX`, `homeW`. [home-timeline](../../../produto/specifications/home-timeline.md). Fórmulas do `AGENTS.md` (credit, effectiveCeiling).

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

## Resultados (29/09/2026)

Aprovado pelo dono em 29/09/2026 ("Aprovo o plano `docs/android/plans/a22-treino-home.md`. Implemente o plano aprovado."). Pré-requisitos conferidos antes do código: `st2-home-treino.md` em `stitch/plans/completed/`, golds `docs/qa/stitch/{dark,light}/homeW.png` presentes, A20 no `master`.

### Implementado

- `feature/workout/WorkoutEditor.kt` (novo): `WorkoutEditorState` (campo, política, %), crédito pela fórmula do `AGENTS.md` (`workoutCredit`), linha de crédito ao vivo (`+{n} kcal na meta de hoje (compensação {pct}%)`; 100% → `compensação 100%`; 0% → `Compensação desativada na Config`), `clean` (só dígitos, até 5) e `policyOf`. Uma regra só para Home e Config.
- `feature/workout/WorkoutSheet.kt` (novo): `WorkoutField` (número 28 pt W700, sufixo `kcal` muted, chama gold, borda gold com foco, raio 14, 57 dp, cursor no fim do valor salvo, linha de crédito abaixo) e `WorkoutSheet` (homeW: scrim, Home desfocada atrás, raio 22 no topo, alça, "Treino de hoje", campo com foco automático, `SheetActions` Salvar/Cancelar do A20, back fecha).
- Home: linha "Treino de hoje" abaixo dos macros (card dos macros, 56 dp, chama gold, `Informar` em gold ou `{kcal} kcal · +{crédito} na meta` com o crédito em muted, chevron). O valor encolhe com reticências antes de empurrar o chevron. Toque abre o sheet; o FAB some enquanto o sheet está aberto.
- `HomePanelMapper.map(day, today, workoutDraft)`: `workoutKcal`, `workoutCredit` (do valor salvo) e `workoutEditor` (rascunho do sheet). `HomePanelViewModel`: `openWorkout` (abre com o kcal salvo), `setWorkout`, `closeWorkout`, `saveWorkout` → `DayRepository.setWorkout` (o mesmo `day.workoutKcal` da Config; vazio = `null` = crédito 0). Nada vai ao Room antes do Salvar.
- Config: o sheet "Treino de hoje" usa o mesmo `WorkoutField` (com a linha de crédito); `setWorkout` usa `WorkoutEditorState.clean`; `ConfigMapper.policyOf` delega para `WorkoutEditorState.policyOf`.
- Specs: [home-timeline](../../../produto/specifications/home-timeline.md) regra 11 e aceite; [memoria-push](../../../produto/specifications/memoria-push.md) § Config regra 2.
- `tools/capture-home.sh`: fluxo Home → treino (Informar, abrir, 350, Salvar, Config mostra 350, reabrir com 350, apagar + Salvar → Informar) e captura `homeW`; o seed grava teto base + eat-back 50% + `day.workoutKcal` para bater a pílula "Meta 2000 kcal" dos golds (home1: base 1825 + 175; homeX: 1900 + 100; homeW como home1). `tools/diff-gold.mjs`: `homeW` em `GOLD_CONFLICTS` com região presa ao rodapé por tema (o gold é uma página de 1350 dp com o sheet no fim; a Home desfocada atrás não entra no gate), sem os 40 dp da pílula de navegação.
- Testes: `WorkoutEditorStateTest` (novo: linha por política, vazio = crédito 0, `clean`); `HomePanelMapperTest` (+3: sem treino = Informar/crédito 0; crédito 0 %, 50 %, 100 % e meta; rascunho com linha ao vivo); `ConfigViewModelTest` (+2: salvar pela Home sobe a meta da Home para 2175 e aparece na Config com crédito 175; vazio + Salvar volta a crédito 0 nas duas; Cancelar não grava); `HomeFixtures.home1Workout` / `homeXWorkout`; `StitchGoldTest` `home1`/`homeX` com treino e `homeW_dark` / `homeW_light` com gate na região do sheet; `RoborazziSmokeTest` `homeW` (novo) e `cfgWorkout` regravado (editor novo).

### Validação automatizada

- `testDevDebugUnitTest`: 219 testes, 217 passam. Os 2 que falham são `StitchGoldTest` `o3` dark/light (ink 0,66/0,70) e falham igual na `master` sem este plano (conferido com `git stash`): gold do ST4, do plano A24. `home1`, que falhava desde o ST2, agora passa.
- `verifyRoborazziDevDebug`: `RoborazziSmokeTest` 14/14 (`homeW` novo, `cfgWorkout` regravado de propósito; o resto igual). A tarefa sai vermelha só pelos 2 `o3` acima.
- `assembleDevRelease`: ok.
- `StitchGoldTest` (JVM, borrado): `home1` **1,13 / 1,15 %** (gate ≤ 2 %; antes 6,47 / 7,13 %), região `homeW` **1,28 / 1,57 %** (dark / light). `home0` 5,32 / 6,93 % e `homeX` 9,30 / 13,46 % seguem report only (conflitos de geração já registrados).

### Emulador (Medium_Phone, 780x1688 @ 320 dpi)

- `tools/capture-home.sh dark` e `light`: todos os ✓, inclusive os 5 novos (Informar, abre o sheet, `350 kcal · +0 na meta` depois do Salvar, Config mostra `350 kcal`, sheet reabre com 350, apagar + Salvar → Informar). Da Home ao treino salvo: linha → digitar → Salvar (3 toques; o campo já abre com foco).
- `node tools/diff-gold.mjs` (capturas em `docs/qa/android/current/{dark,light}/`): `home1` **1,24 / 1,19 %** ✓ (antes 4,63 / 4,65 %), região `homeW` **1,88 / 1,38 %** ✓, `home0` 4,85 / 4,96 % e `homeX` 5,48 / 7,65 % report only.

### Diferenças contra o gold

- Layout: linha 56 dp logo abaixo dos macros, 24 dp de respiro como o resto do painel (o gold mede ~25 dp); sheet com o botão Cancelar a 24 dp do fim; campo 57 dp (o Stitch desenha 56 dp no dark e 59 dp no light).
- Tokens: sheet `surf` no dark e `phone` no light; campo `surf`; chama e borda de foco `gold`; valor salvo em `text`, crédito em `muted`; `Informar` em `gold` (só acento).
- Tipo: título 18,5 sp W600, número 28 pt W700, `kcal` 16 sp, linha de crédito 13 sp; linha da Home 14,5 / 13 sp (a fonte do app é mais larga que a do render do Stitch; com os tamanhos do prompt o rótulo quebrava em duas linhas).
- Raio: sheet 22 no topo, campo 14, card da linha igual ao dos macros.
- ButtonGroup / CTA: par `SheetActions` do A20 (Salvar CTA, Cancelar contornado). O Cancelar usa `surf2` como nos outros sheets; o gold pinta com a cor do sheet (delta abaixo da tolerância).
- Timeline e macros semânticos: sem mudança.
- Aceito: pílula de gesto do Android sobre o rodapé do sheet (fora do gate, como o resto dos 40 dp de baixo); cursor piscando no campo.

### Validação manual pendente (dono)

- No APK: informar 350 kcal na Home e ver a meta subir (com eat-back 50 %: "Meta" +175 e a linha `350 kcal · +175 na meta`).

