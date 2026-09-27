# Plano — A0 Tokens semânticos P/C/G & Material 3 Expressive

- Estado: Aguardando aprovação
- Data: 26/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`
- Pré-requisitos: `a0-arch-refactor.md` + [tokens.md](../../tokens.md)

## Gate de autorização

> Aprovo o plano `docs/android/plans/a0-tokens-expressive.md`. Implemente o plano aprovado.

## Objetivo

Codificar o Design System oficial do Stitch no Android client: tokens de cores semânticas de macronutrientes (P/C/G e Alerta), tipografia oficial (34pt w590, 28pt), shapes (raio 22 top, card/chip 14, gold bar 6px), tema `MaterialExpressiveTheme` com `MotionScheme.expressive()` e componente base reutilizável `ExpressiveButtonGroup`.

## Fontes de verdade

- Visual Gold (Stitch): [.stitch/DESIGN.md](../../../.stitch/DESIGN.md)
- [tokens.md](../../tokens.md)
- [material3-expressive SKILL](../../../.agents/skills/material3-expressive/SKILL.md)

## Escopo de implementação

### 1. Tokens de Macronutrientes Semânticos (`NutriTokens.kt`)
- Adicionar ao `NutriTokens`:
  - `protein`: `#4ec994` Dark / `#1b7a4b` Light
  - `carbs`: `#e58e42` Dark / `#c2651e` Light
  - `fat`: `#e8b86d` Dark / `#b8873d` Light
  - `bad` (estouro/alerta): `#e07a6a` Dark / `#c14d40` Light
  - `gold` (acento da marca): `#e8b86d` Dark / `#b8873d` Light
  - `good` (meta batida): `#7dda9a` Dark / `#1f8a4c` Light

### 2. Tipografia e Formatação Numérica
- `TextStyle` de destaque para saldo calórico: 34sp com weight 590 (`FontWeight.SemiBold`).
- `TextStyle` para campos de formulário e entrada numérica: 28sp.
- Formatação de milhares com ponto (`2.000` em vez de `2000`).

### 3. Shapes e Geometria Material 3 Expressive
- `SheetRadius`: `RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)`.
- `CardRadius`: `RoundedCornerShape(14.dp)`.
- `ContinuousBar`: altura fixa de `6.dp` com cor ouro da marca.

### 4. Componente Base `ExpressiveButtonGroup`
- Implementar componente genérico baseado em Material 3 Expressive ButtonGroup / SingleChoiceSegmentedButtonRow para uso compartilhado em:
  - O1 (modo teto: igual / semana+fim / 7 dias)
  - O2 (política de eat-back: 0% / % / 100%)
  - O3 (stepper de refeições)
  - Config (edição de modos)

## Validação planejada

1. `./gradlew.bat testDebugUnitTest` validando paleta Dark e Light em `TokensTest.kt`.
2. Teste de renderização e preview do `ExpressiveButtonGroup`.
3. Verificação de contraste de cores WCAG AA para os tokens P/C/G em ambos os temas.

## Fora de escopo

- Implementação das telas O1 a O4 (escopo do plano A2).
- Alteração da tela Home (escopo do plano A4).

## Critérios de aceite

- Tokens de Proteína, Carboidrato e Gordura acessíveis globalmente via `LocalNutriColors` ou `NutriTheme`.
- Zero regressão nos testes visuais de tokens existentes.
- ExpressiveButtonGroup funcional para seleções segmentadas.

## Encerramento

Ciclo SDD.
