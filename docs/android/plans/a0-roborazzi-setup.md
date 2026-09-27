# Plano — A0 Roborazzi setup & smoke test de regressão visual

- Estado: Aguardando aprovação
- Data: 26/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`
- Pré-requisitos: [a0-tokens-expressive.md](completed/a0-tokens-expressive.md)

## Gate de autorização

> Aprovo o plano `docs/android/plans/a0-roborazzi-setup.md`. Implemente o plano aprovado.

## Objetivo

Configurar o framework de testes visuais **Roborazzi** no projeto Android para permitir a renderização e verificação de telas Jetpack Compose de forma autônoma e *headless* na JVM, comparando diretamente contra os PNGs Stitch Gold (`docs/qa/stitch/{dark,light}/`) sem depender de emuladores ligados.

## Fontes de verdade

- [screenshot-testing SKILL](../../../.agents/skills/screenshot-testing/SKILL.md)
- [Stitch Gold PNGs](../../qa/stitch/)
- [docs/qa/README.md](../../qa/README.md)

## Escopo de implementação

### 1. Configuração do Gradle
- Adicionar o plugin do Roborazzi em `apps/android/build.gradle.kts`:
  - `alias(libs.plugins.roborazzi)`
- Adicionar dependências de teste em `app/build.gradle.kts`:
  - `io.github.takahirom.roborazzi:roborazzi`
  - `io.github.takahirom.roborazzi:roborazzi-compose`
  - `org.robolectric:robolectric`
- Habilitar `isIncludeAndroidResources = true` em `unitTests`.

### 2. Mapeamento de Diretórios de Imagens
- Configurar o diretório de saída do Roborazzi para apontar para `docs/qa/android/current/` e comparar diretamente com `docs/qa/stitch/{dark,light}/`.
- Definir tolerância padrão de diferença visual (`changeThreshold = 0.01f`, equivalente a 1%).

### 3. Smoke Test de Validação Visual (`RoborazziSmokeTest.kt`)
- Criar teste de fumaça inicial renderizando a `SplashScreen`:
  - Teste 1: `splash_dark` renderiza e compara contra `docs/qa/stitch/dark/splash.png`.
  - Teste 2: `splash_light` renderiza e compara contra `docs/qa/stitch/light/splash.png`.
- Validar geração do relatório HTML de diffs em `build/reports/roborazzi/`.

### 4. Integração no Checklist de QA
- Criar/atualizar script ou instrução para execução rápida:
  - `./gradlew.bat verifyRoborazziDebug`

## Validação planejada

1. `./gradlew.bat verifyRoborazziDebug` executa na JVM em menos de 10 segundos e valida o smoke test da Splash Screen.
2. Forçar intencionalmente uma alteração de cor no teste para confirmar que o Roborazzi detecta a quebra e gera a imagem de diff.

## Fora de escopo

- Cobertura de todas as 18 telas (cada tela será adicionada ao Roborazzi no seu respectivo plano de feature: A2 para O1..O4, A4 para Home, A5 para Chat, A3 para Config).

## Critérios de aceite

- `./gradlew.bat verifyRoborazziDebug` roda com sucesso sem necessidade de dispositivo/emulador conectado.
- O smoke test da Splash Screen passa com tolerância < 1% contra o Stitch Gold.

## Encerramento

Ciclo SDD.
