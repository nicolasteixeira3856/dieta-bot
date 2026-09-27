# Plano — A0 Tokens semânticos P/C/G & Material 3 Expressive (Concluído)

- Estado: Concluído
- Data de conclusão: 26/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`
- Pré-requisitos: [a0-arch-refactor.md](a0-arch-refactor.md) + [tokens.md](../../../tokens.md)

## Gate de autorização

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a0-tokens-expressive.md. Analise e implemente o plano aprovado.`

## Objetivo

Codificar o Design System oficial do Stitch no Android client: tokens de cores semânticas de macronutrientes (P/C/G e Alerta), tipografia oficial (34pt w590, 28pt), shapes (raio 22 top, card/chip 14, gold bar 6px), tema `MaterialExpressiveTheme` com `MotionScheme.expressive()` e componente base reutilizável `ExpressiveButtonGroup`.

## Fontes de verdade

- Visual Gold (Stitch): [.stitch/DESIGN.md](../../../../.stitch/DESIGN.md)
- [tokens.md](../../../tokens.md)
- [material3-expressive SKILL](../../../../.agents/skills/material3-expressive/SKILL.md)

## Escopo implementado

### 1. Tokens de Macronutrientes Semânticos (`NutriTokens.kt` e `NutriTheme.kt`)
- Adicionados ao `NutriHex` e expostos em `NutriColors` / `LocalNutriColors` / `NutriTheme.colors`:
  - `protein`: `#4ec994` Dark / `#1b7a4b` Light
  - `carbs`: `#e58e42` Dark / `#c2651e` Light
  - `fat`: `#e8b86d` Dark / `#b8873d` Light
  - `bad` (estouro/alerta): `#e07a6a` Dark / `#c14d40` Light
  - `gold` (acento da marca): `#e8b86d` Dark / `#b8873d` Light
  - `good` (meta batida): `#7dda9a` Dark / `#1f8a4c` Light

### 2. Tipografia e Formatação Numérica (`NutriTypography.kt` e `NutriTokens.kt`)
- `NutriTypography.remainingHighlight`: 34sp com `FontWeight.SemiBold` (w590).
- `NutriTypography.fieldNumeric`: 28sp.
- `formatRemaining(Int)`: formatação com ponto de milhar (`2.000` em vez de `2000`).

### 3. Shapes e Geometria Material 3 Expressive (`NutriShapes.kt`)
- `NutriShapes.sheetTopRadius`: `RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)`.
- `NutriShapes.cardRadius`: `RoundedCornerShape(14.dp)`.
- `NutriShapes.continuousBarHeight`: `6.dp`.

### 4. Componente Base `ExpressiveButtonGroup` (`ExpressiveButtonGroup.kt`)
- Implementado em `com.nutri.android.core.designsystem.ExpressiveButtonGroup`.
- Suporta layout horizontal (segmentado fluido) e empilhado vertical (`stacked = true`).
- Feedback tátil com `LocalHapticFeedback`.
- Acessibilidade completa com semântica de rádio (`Role.RadioButton`, `selected`).
- Previews Jetpack Compose para temas Claro e Escuro.

## Validação e Testes

- Executado `./gradlew.bat testDebugUnitTest` com sucesso absoluto:
  - `TokensTest`: 8 testes aprovados (ctaIsNotGold, darkAndLightHexes, splashIsBootNotFreeze, semanticMacros, formatRemainingUsesThousandsDot, wcagContrastVerification, remainingAndFieldMatchWire, shapesAndGeometry).
  - `ExpressiveButtonGroupTest`: 2 testes aprovados no Robolectric (horizontal e stacked com cliques e verificação de nós).
  - Total geral da suíte: 49 testes executados, 49 aprovados, 0 falhas.
