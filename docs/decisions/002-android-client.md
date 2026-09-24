# 002 — Client Android

O client mora em `apps/android/`. Namespace `com.nutri.android`. minSdk 26. targetSdk 36. compileSdk 36. Nenhuma system-image nova.

## Versões

- Android Gradle Plugin 9.4.1. O Android Studio instalado é Quail 4 (2026.1.4, `AI-261.26222.65.2614.16379836`). A tabela oficial cobre AGP 7.1–9.4. 9.4.1 é o estável mais novo nessa faixa; 9.5 segue em alpha.
- Gradle 9.6.1. AGP 9.4 exige Gradle ≥ 9.6.0.
- Kotlin 2.2.20. `android.builtInKotlin=false` e `android.newDsl=false`, senão o AGP 9 recusa o plugin `org.jetbrains.kotlin.android`.
- Compose BOM `2026.01.01`. compileSdk 36. O BOM `2026.09.00` puxa Compose 1.12, que exige compileSdk 37; este build fica no BOM de janeiro.
- Navigation Compose 2.8.9
- Hilt 2.56.2
- Retrofit 2.11.0, OkHttp 4.12.0, Kotlinx Serialization 1.7.3
- DataStore Preferences 1.1.1

## Expressive

`MaterialExpressiveTheme` e `MotionScheme.expressive()` ficam de fora: em Material3 1.4.0 a API expressive é `internal` (`ExperimentalMaterial3ExpressiveApi`). Não foi pinada trilha alpha. O tema é `MaterialTheme` com os tokens do wire (`#0b0d10`, `#e8b86d`, CTA `#f3f5f7` / `#111`). A alça da sheet é `#3a424c`. A sheet usa `ModalBottomSheet` com `ExperimentalMaterial3Api` e raio 22 no topo.

## URL

`apps/android/local.properties` leva só `sdk.dir`. Esse é o caso normal: o arquivo aponta o SDK local e fica fora do git. `API_PUBLIC_URL` e `INVITE_CODE` são chaves opcionais e não precisam estar nesse arquivo. Com elas ausentes, o `app/build.gradle.kts` grava no `BuildConfig` o fallback de `.env.example`: `http://127.0.0.1:8080` e `troca-isto`. O header é `X-Invite`. O `.env` não é lido pelo client. No emulador, `127.0.0.1` é o aparelho, então o host entra com `adb reverse tcp:8080 tcp:8080`. O servidor local foi subido com `INVITE_CODE=troca-isto` já no processo, para o dotenv não trocar o convite e a chave não ser impressa. O card T2 do texto "2 paes, ovo, cafe com leite" mostrou 430 kcal, P, cabe, e uma pergunta porque a confiança não era alta. O toque em Registrar até o número na tela levou 8,2 s.

## Persistência

O dia (perfil, eat-back, teto, treino, logs, chips) fica num JSON em DataStore Preferences. Sem Room e sem SQLite: o experimento não usa banco. Matar o processo e reabrir devolve o mesmo dia.
