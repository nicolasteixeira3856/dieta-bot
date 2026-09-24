# 004 — Material 3 Expressive no client Android

Substitui o trecho Expressive do 002. O client em `apps/android` usa a API pública. Sem dynamic color.

## Versões

- BOM Compose `2026.01.01` → `androidx.compose:compose-bom:2026.09.00`. Não é `compose-bom-alpha`.
- O BOM `2026.09.00` ainda alinha `material3` em `1.4.0`. O override direto ganha.
- `releaseCompileClasspath`:
  - `androidx.compose.material3:material3:1.4.0 -> 1.5.0-alpha29`
  - `androidx.compose.material3:material3-android:1.4.0 -> 1.5.0-alpha29`
  - dependência direta `material3:1.5.0-alpha29` e `material3-android:1.5.0-alpha29`
- AGP permanece `9.4.1`. Kotlin permanece `2.2.20`.
- `compileSdk` 36 → 37. `targetSdk` continua 36. O `:app:checkDebugAarMetadata` falhou em 36: os AARs do alpha (entre eles `material3-android:1.5.0-alpha29` e `material3-ripple-android:1.5.0-alpha29`) exigem compileSdk de pelo menos 37. O compile em 36 não pedia bump de AGP nem de Kotlin.

## Theme

`NutriTheme` chama `MaterialExpressiveTheme` com o scheme escuro dos tokens, `motionScheme = MotionScheme.expressive()`, `Shapes` (`largeIncreased` 14, extra large 22) e a tipografia do saldo 34sp peso 590. Sem `dynamicDarkColorScheme`, `dynamicLightColorScheme`, wallpaper ou `MaterialTheme { }` na raiz. `surfaceTint` é transparente. CTA `#f3f5f7` / `#111`. Gold `#e8b86d` é o acento do item selecionado, não o botão principal.

## OptIn

`ExperimentalMaterial3ExpressiveApi` continua público e `@RequiresOptIn`. O compilador não avisou opt-in sobrando.

- Obrigatório em `LoadingIndicator` (`IndicadorEspera`).
- `MaterialExpressiveTheme`, `MotionScheme.expressive()` e `ButtonGroup` compilam sem essa anotação no alpha29. O `@OptIn` no arquivo do theme ficou: o compile não pediu para tirar.

## O que entrou

Presentes no `1.5.0-alpha29`:

- `MaterialExpressiveTheme`
- `MotionScheme.expressive()`
- `ButtonGroup` (`customItem`, `toggleableItem`)
- `LoadingIndicator`
- `Shapes.largeIncreased`

Nenhuma dessas APIs faltou. `toggleableItem` desenha o rótulo numa linha só (`TextOverflow.Visible`) e o `ToggleButton` padrão pintava o item marcado num pill claro com o texto ilegível. O1, O2 e T3 usam o mesmo `ButtonGroup`, com `customItem` + `ToggleButton`: texto em até 3 linhas, marcado em gold com texto `#111`, solto em `#1e242b` com borda `#2a3139`. Mesmos três rótulos. Sem quarto modo, sem `RadioButton`, sem chip Material.

Sheet T1/T3 continua `ModalBottomSheet`, raio 22 no topo, não vira dialog. O motion vem do `MotionScheme.expressive()` do theme. Loading de estimate/fit troca o rótulo `…` por `LoadingIndicator` gold.

## Cor dinâmica

Zero. O scheme passado é o escuro fixo dos tokens.
