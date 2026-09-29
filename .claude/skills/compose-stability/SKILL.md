---
name: compose-stability
description: Garante estabilidade de recomposição e alta performance em Jetpack Compose. Use ao criar UI States, Composables complexos, listas (LazyColumn) ou ao investigar lentidão de renderização.
---

# compose-stability

Orientações para garantir que o compilador do Jetpack Compose considere as classes estáveis e ative o *smart recomposition skipping*.

## Regras Fundamentais

1. **Anotar UI States**:
   - Sempre anote classes de estado de UI com `@Immutable` ou `@Stable` (de `androidx.compose.runtime`).
   ```kotlin
   @Immutable
   data class HomeUiState(
       val eatenKcal: Int = 0,
       val proteinG: Int = 0,
       val logs: ImmutableList<MealLogItem> = persistentListOf(),
       val isLoading: Boolean = false,
   )
   ```

2. **Coleções Imutáveis**:
   - A biblioteca padrão do Kotlin `List<T>` é tratada como instável pelo Compose Compiler porque pode ser implementada por uma lista mutável (`ArrayList`).
   - Use `kotlinx.collections.immutable.ImmutableList` ou passe listas embrulhadas em data classes anotadas com `@Immutable`.

3. **Estabilidade de Lambdas**:
   - Evite passar lambdas instáveis ou referências mutáveis dentro de loops ou `items(list)`.
   - Prefira lambdas que invocam métodos diretos do ViewModel no destino de nível superior (`onLogClick = vm::onLogSelected`).

4. **Derivação de Estado com `derivedStateOf`**:
   - Quando um cálculo de estado derivado muda frequentemente (ex: posição de scroll), use `derivedStateOf { ... }` para evitar recomposições a cada pixel.

5. **Auditoria com Compose Compiler Metrics**:
   - Gerar relatórios de estabilidade:
     `./gradlew.bat assembleDevRelease -PcomposeCompilerReports=true`
   - Verificar classes marcadas como `unstable` em `build/compose_compiler/`.
