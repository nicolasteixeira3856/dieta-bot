# Plano — A0 Roborazzi setup & smoke test de regressão visual (Concluído)

- Estado: Concluído
- Data de conclusão: 26/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`
- Pré-requisitos: [a0-tokens-expressive.md](a0-tokens-expressive.md)

## Gate de autorização

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a0-roborazzi-setup.md. Analise e implemente o plano aprovado`

## Revisão de escopo (26/09/2026)

Decisão do dono após revisão da entrega: o A0 fica restrito a **regressão visual** (a tela contra o próprio baseline gravado). A comparação contra o Stitch Gold sai deste plano e passa aos planos de feature, começando pelo [A2](a2-onboarding-perfil.md).

Motivos registrados na revisão:

- O smoke test compara contra o baseline em `docs/qa/android/current/`, não lê `docs/qa/stitch/`.
- Diff medido da splash atual contra o gold: 2,8% (dark) e 4,8% (light), acima de 1%. A splash atual diverge do gold (ícone, posição, barra, copy).
- Gold exportado em 226×512 contra captura 780×1688. `tools/export-stitch.mjs` precisa exportar em resolução real antes de qualquer tolerância de 1%.

Decisão de copy da splash: vale o Stitch gold. Regra antiga removida de `AGENTS.md`. Ajuste de código fica no A2.

## Objetivo

Configurar o **Roborazzi** no projeto Android para renderizar e verificar telas Jetpack Compose de forma *headless* na JVM, sem emulador, detectando regressão visual contra baseline gravado.

## Fontes de verdade

- [screenshot-testing SKILL](../../../../.agents/skills/screenshot-testing/SKILL.md)
- [docs/qa/README.md](../../../qa/README.md)

## Escopo implementado

### 1. Configuração do Gradle
- Plugin em `apps/android/build.gradle.kts`: `alias(libs.plugins.roborazzi) apply false`
- Plugin em `apps/android/app/build.gradle.kts`: `alias(libs.plugins.roborazzi)`
- Dependências via version catalog (`1.44.0`): `libs.roborazzi`, `libs.roborazzi.compose`, `libs.roborazzi.rule`
- `isIncludeAndroidResources = true` já estava ativo.
- Bloco `roborazzi { outputDir.set(file("../../../docs/qa/android/current")) }`.

### 2. Baseline
- Baseline em `docs/qa/android/current/{dark,light}/splash.png` (render JVM, 390×844 dp @ xhdpi).
- Tolerância: `changeThreshold = 0.01f`.
- `NutriTheme(darkTheme = isSystemInDarkTheme())`: parâmetro só para teste; o app segue o sistema.

### 3. Smoke test (`RoborazziSmokeTest.kt`)
- `splash_dark` e `splash_light` renderizam `SplashScreen` e verificam contra o baseline.
- Robolectric `@GraphicsMode(NATIVE)`, `@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")`.

### 4. Comandos
- `./gradlew.bat recordRoborazziDebug`: grava/atualiza baseline.
- `./gradlew.bat verifyRoborazziDebug`: falha em divergência.
- `./gradlew.bat compareRoborazziDebug`: relatório em `app/build/reports/roborazzi/index.html`.

## Validação

1. `testDebugUnitTest --tests "*RoborazziSmokeTest*" -Proborazzi.test.verify=true --rerun`: 2/2 PASSED, ~9 s, sem emulador (revisão 26/09).
2. Quebra forçada (tema trocado em `splash_dark`) falhou com `AssertionError` e gerou `splash_compare.png` (execução do agente implementador).
3. `testDebugUnitTest` completo: passou.

## Fora de escopo

- Comparação contra Stitch Gold (A2 em diante).
- Correção de `tools/export-stitch.mjs` para resolução real (pré-requisito do A2).
- Cobertura das 18 telas (cada plano de feature adiciona as suas).

## Critérios de aceite

- `verifyRoborazziDebug` roda sem dispositivo/emulador. ✅
- Smoke test da splash passa contra o baseline com tolerância 1% e detecta quebra forçada. ✅

## Encerramento

Ciclo SDD.
