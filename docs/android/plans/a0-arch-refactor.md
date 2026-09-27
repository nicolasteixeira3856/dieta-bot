# Plano — A0 Refatoração Arquitetural Feature-First & Kotlin puro

- Estado: Aguardando aprovação
- Data: 26/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`
- Pré-requisitos: Nenhum

## Gate de autorização

> Aprovo o plano `docs/android/plans/a0-arch-refactor.md`. Implemente o plano aprovado.

## Objetivo

Sanear a arquitetura do client Android: eliminar arquivos legados em Java e kapt no Room, decompor o God-ViewModel (`DayViewModel`/`DayUi`) em ViewModels por feature, corrigir o anti-pattern de navegação em `MainActivity.kt` (restaurando o backstack nativo) e organizar a árvore de código em estrutura Feature-First.

## Fontes de verdade

- [android-architecture SKILL](../../../.agents/skills/android-architecture/SKILL.md)
- [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md)
- [010-room](../../decisions/010-room.md)

## Escopo de implementação

### 1. Eliminação de Java e kapt no Room (KSP puro)
- Converter todas as entidades e DAOs Java para Kotlin:
  - `DayDao.java` ➔ `DayDao.kt` (interface com coroutines / Flow)
  - `DayEntity.java` ➔ `DayEntity.kt` (`@Entity data class`)
  - `MealLogDao.java` ➔ `MealLogDao.kt`
  - `MealLogEntity.java` ➔ `MealLogEntity.kt`
  - `ProfileDao.java` ➔ `ProfileDao.kt`
  - `ProfileEntity.java` ➔ `ProfileEntity.kt`
  - `NutriConverters.java` ➔ `NutriConverters.kt`
  - `NutriDatabase.java` ➔ `NutriDatabase.kt`
- Substituir plugin `kapt` por `ksp` no `build.gradle.kts` para o processador de Room (`androidx.room:room-compiler`).

### 2. Estrutura de Pacotes Feature-First
- Reorganizar a estrutura de UI:
  ```
  com.nutri.android/
  ├── core/
  │   ├── database/     # Entidades, DAOs, Database, Converters
  │   ├── designsystem/ # Tokens, Theme, Components
  │   └── network/      # API, Interceptors
  └── feature/
      ├── splash/       # SplashScreen, SplashNavigation
      ├── onboarding/   # OnboardingScreens, OnboardingViewModel, OnboardingState
      ├── home/         # HomeScreen, HomeViewModel, HomeUiState
      └── t2/           # T2Screen, T2ViewModel
  ```

### 3. Decomposição de ViewModel e Estado Imutável
- Desmembrar `DayViewModel.kt` (461 linhas) e `DayUi.kt` (200 linhas) em ViewModels focados por tela/fluxo:
  - `SplashViewModel` (ou gerência simples de splash)
  - `OnboardingViewModel` (controla apenas o fluxo de setup inicial)
  - `HomeViewModel` (controla o painel do dia e saldo)
- Cada ViewModel expõe seu próprio `StateFlow<UiState>` específico, imutável e anotado com `@Immutable`.

### 4. Correção da Navegação e Backstack
- Remover `LaunchedEffect(ui.stage)` e `popUpTo(nav.graph.id) { inclusive = true }` de `MainActivity.kt`.
- Utilizar rotas tipadas `@Serializable` com navegação direta pelo `navController`:
  - `navController.navigate(RouteOnboarding)`
  - `navController.navigate(RouteHome) { popUpTo(RouteSplash) { inclusive = true } }`
- Restaurar o comportamento padrão da tecla Voltar e compatibilidade com *Predictive Back*.

## Validação planejada

1. `./gradlew.bat clean testDebugUnitTest` executando com 100% de sucesso.
2. Build sem dependência de `kapt` gerando artefatos via KSP.
3. Teste de navegação: transição Splash ➔ Onboarding ➔ Home sem limpar o backstack arbitrariamente.
4. Preservação de todos os testes unitários de domínio existentes.

## Fora de escopo

- Telas novas do ADR-012 (O3, O4, Chat, Config).
- Integração de rede com `/v1/chat`.
- Alteração visual de layouts existentes.

## Critérios de aceite

- Zero arquivos `.java` no pacote `com.nutri.android.data`.
- Build limpo rodando com KSP.
- `DayViewModel` e `DayUi` decompostos sem perda de funcionalidade existente.
- Tecla Voltar do sistema funciona normalmente na árvore de telas.

## Encerramento

Ciclo SDD.
