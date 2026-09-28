---
name: android-architecture
description: Aplica a arquitetura oficial Android da Google no client Dieta Bot. Use quando criar tela, ViewModel, repository, use case, Hilt, navegação ou quando o código pular camada. Fontes developer.android.com/topic/architecture e /recommendations.
---

# android-architecture

Fontes oficiais:
- https://developer.android.com/topic/architecture
- https://developer.android.com/topic/architecture/recommendations
- https://developer.android.com/develop/ui/compose/architecture

## Estrutura de Pacotes (Feature-First)

```
apps/android/app/src/main/java/com/nutri/android/
├── core/
│   ├── designsystem/   # DietaBotTheme, DietaBotTokens, ButtonGroup, ExpressiveShapes
│   ├── database/       # Room v2 em Kotlin puro (KSP), DAOs, Entities
│   └── network/        # OkHttp, Retrofit, Interceptors, DTOs
├── domain/             # Fórmulas e regras (Kotlin puro, sem Android)
└── feature/
    ├── onboarding/     # OnboardingViewModel, OnboardingState, OnboardingScreen (O1..O4)
    ├── home/           # HomeViewModel, HomeUiState, HomeScreen (home0, home1, homeX)
    ├── chat/           # ChatViewModel, ChatUiState, ChatScreen (chat0..chatG)
    └── config/         # ConfigViewModel, ConfigUiState, ConfigScreen (cfg, wipe)
```

## Regras de Camadas

1. **Domain**:
   - Kotlin puro. Sem referências a `android.*`, Context, Room ou Compose.
   - Orçamento calórico (`BudgetCalculator`), TMB (`TmbCalculator`), macros (`MacroSplit`), fuso horário de SP.
   - Teste unitário puro na JVM.

2. **Data (Room v2 + KSP)**:
   - **Zero Java, zero kapt**. Todas as entidades são `@Entity data class` em Kotlin.
   - DAOs retornam `Flow<T>` reativo.
   - Repositórios expõem métodos suspensos (`suspend fun`) ou `Flow<T>`. UI nunca acessa DAO ou Retrofit diretamente.

3. **UI & ViewModel (UDF Estrito)**:
   - **Um ViewModel por tela/fluxo**: `OnboardingViewModel`, `HomeViewModel`, `ChatViewModel`, `ConfigViewModel`.
   - **Proibido God-ViewModel**: nunca agrupar estados de telas diferentes em uma única classe `DayViewModel`/`DayUi`.
   - ViewModel expõe `val uiState: StateFlow<UiState>` imutável.
   - UI coleta estado com `collectAsStateWithLifecycle()`.
   - Composables filhos recebem apenas dados primitivos/imutáveis e lambdas de callback. Nunca passar a instância inteira do ViewModel para sub-composables.
   - Arquivos Composable separados por tela (ex: `HomeScreen.kt`, `ChatScreen.kt`). Proibido empilhar múltiplas telas no mesmo arquivo.

4. **Navegação (Type-Safe Navigation Compose)**:
   - Rotas tipadas com `@Serializable data object RouteHome`, `@Serializable data object RouteChat`, etc.
   - O `NavController` gerencia o backstack naturalmente.
   - **Proibido resetar backstack via LaunchedEffect**: nunca usar `popUpTo(nav.graph.id) { inclusive = true }` reagindo a mudanças de enum de estado.
   - Respeitar o gesto nativo de Voltar e o *Predictive Back* do Android 14+.

5. **Injeção de Dependência**:
   - Hilt em toda a árvore.
   - `@HiltViewModel` com `@Inject constructor(...)`.
   - No Composable de destino: `val vm: HomeViewModel = hiltViewModel()`.

6. **Validação**:
   - `.\gradlew.bat test` antes de marcar done.
   - Screenshot testing com Roborazzi comparando contra `docs/qa/stitch/{dark,light}/<id>.png`.
