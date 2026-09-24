---
name: dieta-bot-android-qa
description: Valida entrega Nutri. Use antes de marcar /goal DONE, depois de UI, ou quando o dono colar log.
---

# dieta-bot-android-qa

Checklist:
- `.\gradlew.bat test` verde
- `.\gradlew.bat :app:compileDebugKotlin` verde
- `adb devices` tem emulador
- screenshots em `docs/qa/` das telas tocadas
- tokens vs `docs/tokens.md` (sem roxo default)
- home dia 1 sem chip
- nenhuma OPENAI_API_KEY no source
- ADR se houve decisão

Sem screenshot de UI nova, não é DONE.
