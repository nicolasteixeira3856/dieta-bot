---
name: dieta-bot-android-visual
description: Compara a tela do emulador com o wire Nutri. Use depois de qualquer mudança visual ou quando o dono reclamar que ficou rudimentar.
---

# dieta-bot-android-visual

1. `adb devices` não vazio.
2. `adb exec-out screencap -p > docs/qa/android-<tela>.png`
3. Comparar com `wires/nutri-wires.html` e `docs/tokens.md`.
4. Falha se houver roxo/azul Material default, Appbar, FAB, tab Expo, fundo claro, texto coach, saldo pequeno, chip no dia 1.
5. Corrigir e recapturar. Não marcar DONE no escuro.
