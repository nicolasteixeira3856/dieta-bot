# 002 — Client Android

O client mora em `apps/android/`. Namespace `com.nutri.android`. minSdk 26. targetSdk 36. compileSdk 37, porque o Compose estável de setembro de 2026 exige a API 37 para compilar. O pacote instalado no SDK foi `platforms;android-37.0`. Nenhuma system-image nova.

## Versões

- Android Gradle Plugin 9.4.1
- Gradle 9.6.1
- Kotlin 2.2.20
- Compose BOM `2026.09.00`
- Material3 `1.4.0` (resolvido pelo BOM)
- Navigation Compose 2.8.9
- Hilt 2.56.2
- Retrofit 2.11.0, OkHttp 4.12.0, Kotlinx Serialization 1.7.3
- DataStore Preferences 1.1.1

## Expressive

`MaterialExpressiveTheme` e `MotionScheme.expressive()` não estão no artefato estável `material3:1.4.0`. A anotação `ExperimentalMaterial3ExpressiveApi` nesse artefato é interna e o app não consegue optar por ela. Não foi pinada a trilha 1.5 alpha. O tema é `MaterialTheme` com os tokens do wire (`#0b0d10`, `#e8b86d`, CTA `#f3f5f7` / `#111`). A sheet usa `ModalBottomSheet` com `ExperimentalMaterial3Api` e raio 22 no topo.

## URL

`API_PUBLIC_URL` e `INVITE_CODE` entram no `BuildConfig` a partir de `apps/android/local.properties`, se existirem. Sem essas chaves, o default é o de `.env.example`: `http://127.0.0.1:8080` e `troca-isto`. O header é `X-Invite`. O `.env` não é lido pelo client. No emulador, `127.0.0.1` é o aparelho, então o host entra com `adb reverse tcp:8080 tcp:8080`. O servidor local foi subido com `INVITE_CODE=troca-isto` já no processo, para o dotenv não trocar o convite e a chave não ser impressa. O card T2 do texto "2 paes, ovo, cafe com leite" mostrou 430 kcal, P, cabe, e uma pergunta porque a confiança não era alta. O toque em Registrar até o número na tela levou 8,2 s.

## Persistência

O dia (perfil, eat-back, teto, treino, logs, chips) fica num JSON em DataStore Preferences. Sem Room e sem SQLite: o experimento não usa banco. Matar o processo e reabrir devolve o mesmo dia.
