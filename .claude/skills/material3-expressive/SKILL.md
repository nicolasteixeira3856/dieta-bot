---
name: material3-expressive
description: Liga e mantém Material 3 Expressive no client Dieta Bot. Use quando mexer em theme, ButtonGroup, LoadingIndicator, motion, shapes ou quando a UI voltar roxa/Material default.
---

# material3-expressive

## Pin

- BOM Compose estável (`2026.09.00` ou mais novo estável).
- Override avulso: `androidx.compose.material3:material3:1.5.0-alpha29` (ou alpha 1.5 mais nova).
- Sem `compose-bom-alpha` no projeto inteiro.
- Sem dynamic color / wallpaper / `dynamicDarkColorScheme`.

Conferir:

```
.\gradlew.bat :app:dependencies --configuration releaseCompileClasspath
```

Tem que listar `material3:1.5.0-alpha*`. Se listar `1.4.0`, o override falhou.

## Theme

Root do app:

```
MaterialExpressiveTheme(
    colorScheme = nutriDarkScheme,
    motionScheme = MotionScheme.expressive(),
    shapes = nutriShapes,
    typography = nutriTypography,
) { ... }
```

`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` onde o compilador exigir.

## O que usar

- O1 / O2 / T3 modos: `ButtonGroup` single-select.
- Loading estimate/fit: `LoadingIndicator`.
- Sheet T1/T3: radius 22 top + spring do motion scheme. Não Dialog central.

## O que não usar

Appbar default, FAB, BottomNav, NavigationRail, `Button mode=contained` primary gold no CTA.

CTA = fundo `#f3f5f7` texto `#111`. Gold `#e8b86d` é acento.

## Tokens

`docs/tokens.md`. Fundo `#0b0d10`. Se o screenshot tiver roxo/azul default, não é DONE.
