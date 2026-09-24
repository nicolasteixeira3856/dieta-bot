---
name: dieta-bot-android-feature
description: Implementa feature no client Android Nutri. Use quando o pedido for tela, regra, API client ou persistência do dia. Pasta apps/android.
---

# dieta-bot-android-feature

Pasta: `apps/android/`. Não editar `server/` neste fluxo.

Ordem:
1. Teste da regra em `domain/` (TDD).
2. Repository / DataStore.
3. ViewModel + UiState.
4. Composable.
5. `.\gradlew.bat test` e `compileDebugKotlin`.
6. Screenshot no emulador se mudou UI.

Paridade: foto ≤1280 JPEG 70, header `X-Invite`, timeout 20s, home dia 1 sem chip.

Não restaurar Flutter/RN. Não Room neste corte (DataStore/JSON).
