# Plano — A0 Refatoração Arquitetural Feature-First & Kotlin puro

- Estado: Concluído
- Data: 26/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`
- Pré-requisitos: Nenhum

## Gate de autorização

> Aprovo o plano `docs/android/plans/a0-arch-refactor.md`. Implemente o plano aprovado.

## Objetivo

Sanear a arquitetura do client Android: eliminar arquivos legados em Java e kapt no Room, decompor o God-ViewModel (`DayViewModel`/`DayUi`) em ViewModels por feature, corrigir o anti-pattern de navegação em `MainActivity.kt` (restaurando o backstack nativo) e organizar a árvore de código em estrutura Feature-First.

## Fontes de verdade

- [android-architecture SKILL](../../../../.agents/skills/android-architecture/SKILL.md)
- [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md)
- [010-room](../../../decisions/010-room.md)

## Entregas Realizadas

### 1. Eliminação de Java e kapt no Room (KSP puro)
- Convertidos todos os arquivos Java do Room para Kotlin sob `com.nutri.android.core.database`:
  - `DayDao.kt` (interface com Flow e coroutines)
  - `DayEntity.kt` (`@Entity data class`)
  - `MealLogDao.kt`
  - `MealLogEntity.kt`
  - `ProfileDao.kt`
  - `ProfileEntity.kt`
  - `NutriConverters.kt`
  - `NutriDatabase.kt`
- Removidos todos os 8 arquivos `.java` de `com.nutri.android.data`.
- Substituído `kapt(libs.room.compiler)` por `ksp(libs.room.compiler)`.

### 2. Estrutura de Pacotes Feature-First
Árvore reestruturada:
```
com.nutri.android/
├── core/
│   ├── database/     # Entidades, DAOs, Database, Converters, Repository, Snapshots
│   ├── designsystem/ # Tokens, Theme, Components
│   └── network/      # API, Interceptors, Models, Gate, Compressor
└── feature/
    ├── splash/       # SplashScreen, SplashViewModel, SplashUiState
    ├── onboarding/   # OnboardingScreens, OnboardingViewModel, OnboardingUiState
    ├── home/         # HomeScreen, HomeSheets, HomeViewModel, HomeUiState
    └── t2/           # T2Screen, T2ViewModel, T2UiState
```

### 3. Decomposição de ViewModel e Estado Imutável
- Criados ViewModels e UiStates dedicados e anotados com `@Immutable`:
  - `SplashViewModel` / `SplashUiState`
  - `OnboardingViewModel` / `OnboardingUiState`
  - `HomeViewModel` / `HomeUiState`
  - `T2ViewModel` / `T2UiState`
- Cobertura de testes unitários dedicada adicionada: `OnboardingViewModelTest.kt`, `HomeViewModelTest.kt`.

### 4. Correção da Navegação e Backstack
- Removido `LaunchedEffect(ui.stage)` e `popUpTo(nav.graph.id) { inclusive = true }` de `MainActivity.kt`.
- Implementadas rotas tipadas `@Serializable` (`RouteSplash`, `RouteCeiling`, `RouteEat`, `RouteHome`, `RouteT2`).
- Backstack nativo do Android restaurado com suporte a gestos de voltar e Predictive Back.

## Evidência de Validação

- Comando: `.\gradlew.bat testDebugUnitTest`
- Resultado: `BUILD SUCCESSFUL in 13s` (44/44 testes passando com 100% de sucesso).
- Verificação KSP: processamento de código Room gerado via KSP com sucesso.
- Verificação de arquivos `.java`: 0 arquivos Java remanescentes no código-fonte.
