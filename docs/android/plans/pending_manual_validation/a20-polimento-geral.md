# Plano — A20 Polimento: feedback de toque, botões dos sheets, respiro de scroll, Config

- Estado: Pendente aprovação manual
- Aprovado: 29/09/2026 ("Aprovo o plano `docs/android/plans/a20-polimento-geral.md`. Implemente o plano aprovado.")
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`core/designsystem`, `feature/config`, `feature/home`, `feature/onboarding`, `feature/chat` só no toque)
- Pré-requisitos: Nenhum. Nenhum gold muda: os ajustes aproximam o app dos golds atuais (`cfg`, `home1`, `chatP`).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a20-polimento-geral.md`. Implemente o plano aprovado.

## Objetivo

Os ajustes de layout do feedback que não precisam de Stitch.

## Escopo de implementação

### 1. Feedback de toque e vibração (`core/designsystem`)

- Todo controle tocável ganha indicação visual (ripple M3 / estado pressionado). Hoje há 34 `clickable(...)` e alguns com `indication = null` em controle real. `indication = null` fica só em scrim e fundo de sheet (que bloqueiam toque).
- Um `Modifier.dietaClick(haptic = …)` no design system centraliza a regra. Vibração (`LocalHapticFeedback`), sempre respeitando a configuração de vibração ao toque do sistema:
  - `Confirm` em Gravar, Substituir, Salvar, Continuar e Pular refeição.
  - `ContextClick` (leve) em chips, linhas da Config, abas, Trocar, Cancelar e FAB.
  - Nada em rolagem nem em digitação.
- Botões M3 que já têm ripple ganham só a vibração.

### 2. Botões dos sheets (Config e onde houver o par Salvar/Cancelar)

- `Cancelar` passa a ser um botão de verdade, do mesmo tamanho do `Salvar` (largura total, 52 dp, pill, 1 dp de borda, texto 15 sp W700 na cor primária). É o par `Pular refeição`/`Cancelar` do gold `chatP`. Hoje é um `Text` `labelMd` muted com área de toque pequena.
- Um componente só (`SheetActions(primary, secondary)`), usado nos sheets da Config e no diálogo de pular da Home, que hoje usa `TextButton`.

### 3. Respiro no fim das telas com scroll

- Home: o disclaimer "Estimativa nutricional…" fica acima do FAB. Padding inferior do conteúdo = altura do FAB + margem + inset de navegação, medido no gold `home1`.
- Config, O1–O4 e sheets com scroll: o último item termina acima do CTA/barra, com ≥ 24 dp de respiro + inset de navegação (`WindowInsets.navigationBars`/`safeDrawing`).
- Chat fica como está (dono: "está bom").

### 4. Config: tamanho dos títulos

- No gold `cfg`, "Compensação de treinos", "Macronutrientes (P · C · G)" e "Gasto calórico do treino" têm o mesmo tamanho de "Meta de calorias". O código usa o mesmo estilo (16 sp W500), então a diferença nasce no layout (quebra de linha, peso da coluna do valor, detalhe). Diagnóstico na captura vs gold, e ajuste até bater.

### 5. Copy dos sheets da Config (`ConfigScreen.kt`)

| Sheet | Hoje | Depois |
|---|---|---|
| Treino de hoje | "Kcal do treino. Vazio: crédito 0. Zera à meia-noite." | (sem texto) |
| Horários das refeições | "Nome e horário. Os registros de hoje ficam." | "Mudar nome ou horário não apaga o que você já registrou hoje." |

- `produto/specifications/memoria-push.md` § Config: registrar a copy.

## Arquivos e áreas afetadas

- `core/designsystem/*` (modificador + `SheetActions`).
- `feature/config/ConfigScreen.kt`, `feature/home/HomePanelScreen.kt`, `feature/onboarding/*`, `feature/chat/ChatScreen.kt` (só `clickable` → modificador).
- Roborazzi e scripts de captura, se algum testTag mudar.

## Validação planejada

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug` (renders novos dos sheets).
2. Emulador: `cfg`, `home1` (disclaimer inteiro acima do FAB), O1–O4 com scroll até o fim; captura vs gold com diff escrito.
3. Área de toque: `Cancelar` ≥ 48 dp de altura (Accessibility Scanner ou `UiAutomator` bounds).
4. Manual (dono): sentir a vibração em Gravar/Salvar; desligar a vibração ao toque no sistema e confirmar que o app para de vibrar.

## Resultados (29/09/2026)

### Implementado

- `core/designsystem/DietaBotClick.kt`: `Modifier.dietaClick(haptic, enabled, role)` = `clickable` com `ripple()` M3 + `LocalHapticFeedback` (`Haptic.Confirm` → `HapticFeedbackType.Confirm`, `Haptic.Light` → `ContextClick`). `rememberHaptic()` para os botões M3 que já têm ripple (`TextButton` dos diálogos de horário e ajuda do O4, `ToggleButton` do `ExpressiveButtonGroup`). A chamada da plataforma (`View.performHapticFeedback`) respeita a vibração ao toque do sistema.
- Todos os `clickable` de controle real de Config, Home, O1–O4 e Chat viraram `dietaClick`. `indication = null` ficou só em scrim e fundo de sheet/diálogo (6 lugares). Confirm: Salvar, Continuar/Concluir (O1–O4), Gravar, Confirmar refeição, Substituir, Pular refeição, Pular (Home), Confirmar e reiniciar dia. O resto é leve.
- `SheetActions(primary, secondary)` em `DietaBotComponents.kt`: dois pills de largura total, 52 dp, 15 sp W700; secundário em `surf2` com 1 dp `line`, como o par do chatP. Usado nos 5 sheets da Config (tags `cfg-save`/`cfg-cancel` mantidas) e no diálogo de pular da Home (`BasicAlertDialog`, tags `home-skip-confirm`/`home-skip-cancel`), que deixou de usar `TextButton`.
- Respiro: Home = 28 + 64 (FAB) + 48 dp + inset de navegação; O1–O4 = 108 dp (rodapé do CTA) + 24 dp + inset; sheets da Config = 24 dp entre a lista e o par de botões; Config já tinha 32 dp + inset.
- Copy: sheet "Treino de hoje" sem subtítulo; "Horários das refeições": "Mudar nome ou horário não apaga o que você já registrou hoje." Spec [memoria-push](../../../produto/specifications/memoria-push.md) § Config itens 8–9 e [home-timeline](../../../produto/specifications/home-timeline.md) § Estado.
- Títulos da Config (item 4): diagnóstico sem mudança de código. Na captura de 390 dp as linhas de título medem 24 px ("Meta de calorias", "Compensação de" e "Gasto calórico do") e 30/29 px nas linhas com descendentes, com 44 px de entrelinha, igual ao gold `cfg` pixel a pixel. As três quebram em duas linhas também no gold. O tamanho já bate.

### Validação automatizada

- `testDevDebugUnitTest`: 187 testes, 181 passam. Os 6 que falham são `StitchGoldTest` (`home1`, `chatE`, `o3`, dark e light) e falham igual na `master` sem este plano (mesmos scores: home1 6,47/7,13, chatE 4,48/3,93, o3 ink 0,66/0,70), porque os golds mudaram nos gates ST2/ST3/ST1 e só os planos A22/A21/A19 os implementam.
- `verifyRoborazziDevDebug --tests RoborazziSmokeTest`: 10/10. Novos baselines `cfgWorkout` e `cfgSlots` (dark/light).

### Emulador (Medium_Phone, 780x1688 @ 320 dpi)

- `tools/capture-config.sh dark|light` e `tools/capture-home.sh dark|light`: todos os ✓ (os scripts usam `cfg-save`/`cfg-cancel` e o novo diálogo de pular).
- `node tools/diff-gold.mjs`: `cfg` 0,40/0,37 %, `wipe` 0,76/1,26 %, `o1` 1,16/0,82 %, `o2` 0,85/1,13 %, `light/o4` 1,02 % passam. `home1` 4,63/4,65 % (antes 4,72/4,60 %), `o3`, `dark/o4` (ink 0,76) e os report-only `home0`/`homeX` estão iguais às capturas anteriores: diferenças dos golds novos (linha Treino, seletor de horário), fora deste plano.
- Diff escrito: layout, tokens, tipo, raio 22 do sheet, CTA e macros sem mudança nas capturas estáticas (o ripple só aparece durante o toque). Home no fim da rolagem: disclaimer termina em y=1360, FAB começa em 1456 → 48 dp, igual ao gold `home1`. O1–O4 no fim: último item ≥ 48 dp acima do CTA (y 1472). Sheet de horários no fim: último card termina 24 dp acima do Salvar. Config no fim: nota do wipe inteira acima da barra de gestos.
- Área de toque (UiAutomator): `cfg-save` e `cfg-cancel` 700×104 px = 52 dp de altura cada.

### Pendente (dono)

- Sentir a vibração em Gravar/Salvar; desligar a vibração ao toque no sistema e confirmar que o app para de vibrar.

## Fora de escopo

- Qualquer mudança de gold ou de tela nova.
- Seletor de horário: [A21](../a21-seletor-horario.md).

## Riscos e controles

- **Ripple em lugar que o gold não mostra:** ripple só aparece durante o toque; a captura estática não muda.
- **Vibração demais:** só as duas intensidades acima.

## Critérios de aceite

- Todo botão responde ao toque visualmente; Gravar/Salvar vibram.
- Cancelar e Salvar do mesmo tamanho.
- Nada cortado sob FAB/CTA no fim de nenhuma tela com scroll.
- Títulos da Config iguais ao gold.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
