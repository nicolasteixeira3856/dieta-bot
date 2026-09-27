---
name: screenshot-testing
description: Executa e valida testes de regressão visual automatizados na JVM via Roborazzi sem necessidade de emulador Android ativo. Use após qualquer alteração visual ou de tokens.
---

# screenshot-testing

Regras para teste de screenshot automatizado na JVM com **Roborazzi** e Robolectric.

## Fluxo de Execução

1. **Gravar / Gerar Novas Capturas**:
   ```bash
   ./gradlew.bat recordRoborazziDebug
   ```
2. **Verificar / Comparar com as Imagens de Ouro**:
   ```bash
   ./gradlew.bat verifyRoborazziDebug
   ```
3. **Gerar Relatório HTML com Diffs**:
   ```bash
   ./gradlew.bat compareRoborazziDebug
   ```
   O relatório é salvo em: `build/reports/roborazzi/index.html`.

## Padrão Ouro de Diretórios

- As imagens de referência oficiais residem em:
  `docs/qa/stitch/dark/<id>.png` e `docs/qa/stitch/light/<id>.png`
- O teste Roborazzi configura o `RoborazziOptions` para comparar diretamente com a pasta `docs/qa/stitch/` com tolerância máxima de 0.5% (`dumpThreshold = 0.005`).

## Estrutura do Teste de Screenshot (Robolectric)

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun home_dark_matches_stitch_gold() {
        composeTestRule.setContent {
            NutriTheme(darkTheme = true) {
                HomeScreen(uiState = sampleHomeState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "docs/qa/stitch/dark/home1.png",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
            )
        )
    }
}
```

## Benefícios para Agentes

- Rápido: roda em ~3 segundos na JVM sem ligar emulador.
- Determinístico: não depende da rasterização da GPU do host ou variações de tela.
- Autônomo: o agente pode rodar o comando Gradle e receber imediatamente o diff numérico ou falha de teste.
