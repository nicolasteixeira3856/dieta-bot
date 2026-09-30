# Plano — A10 Flavors dev e prod

- Estado: Concluído
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (Gradle, `src/dev/`, `src/prod/`), `tools/capture-*.sh`, `tools/fake-chat-server.mjs`, skills com comandos de build, docs.
- Pré-requisitos: [A9](a9-assinatura-release.md) concluído. [ADR-014](../../adrs/ADR-014-flavors-firebase-dev.md) aceito junto com a aprovação deste plano.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a10-flavors-dev-prod.md`. Implemente o plano aprovado.

A aprovação aceita o ADR-014. Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Dois ambientes no app: **dev** (o que existe hoje: server GCP, e depois Firebase) e **prod** (esqueleto, sem servidor e sem Firebase), instaláveis lado a lado. Nenhuma tela muda.

## Fontes de verdade

- [ADR-014](../../adrs/ADR-014-flavors-firebase-dev.md).
- `apps/android/app/build.gradle.kts`, `AndroidManifest.xml` (`${applicationId}.photos` já é por ID).

## Decisões fixas

| Item | dev | prod |
|---|---|---|
| Dimensão | `env` | `env` |
| `applicationId` | `com.nutri.android.dev` | `com.nutri.android` |
| `versionNameSuffix` | `-dev` | — |
| Nome no launcher | "Nutri Dev" (`src/dev/res/values/strings.xml`) | "Nutri" (main) |
| `BuildConfig.ENV` | `"dev"` | `"prod"` |
| `API_PUBLIC_URL` | `dev.API_PUBLIC_URL`, com fallback em `API_PUBLIC_URL` (compatível com hoje); `-PAPI_PUBLIC_URL` continua sobrescrevendo (QA com fake server) | `prod.API_PUBLIC_URL` |
| `INVITE_CODE` | `dev.INVITE_CODE`, com fallback em `INVITE_CODE` | `prod.INVITE_CODE` |
| Sem URL/convite | usa o fallback | task `preProd*Build` falha com "defina prod.API_PUBLIC_URL e prod.INVITE_CODE em local.properties" |
| Assinatura release | chave do A9 | chave do A9 |

Todas as chaves ficam no `apps/android/local.properties` (não versionado). O pacote Kotlin (`com.nutri.android`) **não** muda; só o `applicationId`.

## Escopo de implementação

### 1. Gradle (AGENTE)

- `flavorDimensions += "env"`, `productFlavors { create("dev") {…}; create("prod") {…} }` com os valores da tabela.
- `buildConfigField` por flavor para `API_PUBLIC_URL`, `INVITE_CODE` e `ENV`.
- Guarda de prod: `tasks.matching { it.name.startsWith("preProd") && it.name.endsWith("Build") }.configureEach { doFirst { … } }`. Só falha quando um variant prod é construído.
- `src/dev/res/values/strings.xml` com `app_name` = "Nutri Dev".

### 2. `local.properties` (AGENTE, arquivo local)

- Renomear as linhas atuais para `dev.API_PUBLIC_URL` / `dev.INVITE_CODE`, sem imprimir o convite. `prod.*` fica ausente de propósito.

### 3. Scripts, skills e docs com nomes de task/APK/pacote (AGENTE)

| Onde | Hoje | Depois |
|---|---|---|
| `tools/capture-chat.sh`, `tools/capture-photo.sh` | `PKG=com.nutri.android`, `/data/user/0/com.nutri.android/` | `com.nutri.android.dev` |
| `tools/fake-chat-server.mjs` (comentário) | `assembleDebug` | `assembleDevDebug` |
| `docs/qa/README.md` | `testDebugUnitTest`, `recordRoborazziDebug`, `verifyRoborazziDebug` | `testDevDebugUnitTest`, `recordRoborazziDevDebug`, `verifyRoborazziDevDebug` |
| skills `dieta-bot-android-ui`, `screenshot-testing` em `.agents/skills`, `.grok/skills` e `.hermes/skills` (sincronizadas) | `*RoborazziDebug` | `*RoborazziDevDebug` |
| `docs/android/README.md`, `docs/server/deploy-gcp.md` (troca de convite), `SETUP.md` | `assembleDebug`, `app-debug.apk`, `INVITE_CODE=` | `assembleDevDebug` / `assembleDevRelease`, `app-dev-*.apk`, `dev.INVITE_CODE=` |

- Baselines Roborazzi em `src/test/snapshots/`: conferir se o nome do arquivo depende do variant. Se depender, regravar com `recordRoborazziDevDebug` e registrar que não houve mudança de pixel (a UI não mudou).

### 4. Instalação no celular (DONO)

1. Instalar o `app-dev-release.apk` (assinado com a chave do A9). O agente instala por `adb` se o celular estiver conectado.
2. Refazer o onboarding no "Nutri Dev".
3. Desinstalar o app antigo ("Nutri", `com.nutri.android` assinado com a chave de debug). Assim o ID `com.nutri.android` fica livre para o prod.

## Arquivos e áreas afetadas

- `apps/android/app/build.gradle.kts`, `apps/android/app/src/dev/res/values/strings.xml` (novo).
- `tools/capture-chat.sh`, `tools/capture-photo.sh`, `tools/fake-chat-server.mjs`.
- Skills (3 pastas), `docs/qa/README.md`, `docs/android/README.md`, `docs/server/deploy-gcp.md`, `SETUP.md`, `AGENTS.md` (linha do stack: "flavors dev/prod, ADR-014").
- Local: `apps/android/local.properties`.

## Validação planejada

1. `./gradlew.bat :app:tasks --all` lista `assembleDevDebug`, `assembleDevRelease`, `assembleProdDebug` e `assembleProdRelease`.
2. `assembleDevRelease` → `BuildConfig` com `ENV="dev"`, URL GCP; `apksigner` mostra a chave do A9; `aapt dump badging` mostra `package: name='com.nutri.android.dev'` e o rótulo "Nutri Dev".
3. `assembleProdRelease` sem `prod.*` → falha com a mensagem definida. Com `-P`/`local.properties` de teste → `package: name='com.nutri.android'`, `ENV="prod"`. Depois o valor de teste sai.
4. `./gradlew.bat :app:testDevDebugUnitTest` verde, incluindo o `StitchGoldTest`, e `verifyRoborazziDevDebug` verde (sem mudança de pixel).
5. Emulador: `tools/capture-chat.sh light` roda com o pacote `.dev` contra o fake server e gera capturas iguais às atuais. É o gate visual do AGENTS: nenhuma tela mudou, e a captura prova isso.
6. Manual (dono): "Nutri Dev" no celular, onboarding feito, uma refeição registrada no 4G.

## Fora de escopo

- Firebase ([A11](a11-firebase-dev.md)), servidor de prod, ícone diferente para dev, `versionCode` automático, buildTypes extras (staging).
- Migrar os dados do app antigo para o dev (não há export; o dono aceitou perder).

## Riscos e controles

- **Dados locais perdidos na troca:** aceito pelo dono; o passo 4.3 só acontece depois do 4.2.
- **Script/skill esquecido com nome antigo:** um `grep` final por `assembleDebug|RoborazziDebug|testDebugUnitTest|PKG=com.nutri.android$` deve dar zero ocorrências fora do histórico de planos concluídos.
- **Prod construído sem URL por engano:** a guarda de `preProd*Build` impede.
- **Skills fora de sincronia:** as três pastas são editadas no mesmo passo e comparadas com `diff -r`.

## Critérios de aceite

- Quatro variants construíveis (prod só com `prod.*` definido).
- Dev e prod com IDs distintos, instaláveis lado a lado.
- Testes, Roborazzi e captura no emulador sem regressão.
- Dono usando o "Nutri Dev" no celular.

## Registro de execução

- 28/09/2026 — ADR-014 aceito com a aprovação. O A9 tinha o código concluído, e só o backup da chave estava pendente com o dono.
- `build.gradle.kts`: dimensão `env`, flavors `dev` (`applicationIdSuffix=".dev"`, `versionNameSuffix="-dev"`) e `prod`; `BuildConfig.ENV`, `API_PUBLIC_URL`, `INVITE_CODE` por flavor; guarda `preProd*Build` com a mensagem do plano.
- `src/dev/res/values/strings.xml`: "Nutri Dev".
- `local.properties`: `API_PUBLIC_URL`/`INVITE_CODE` → `dev.*` (sem imprimir valores).
- Scripts: os **seis** `capture-*.sh` (o plano listava dois; `config`, `home`, `onboarding` e `push` também fixavam o `PKG`) → `PKG=com.nutri.android.dev`. **Achado:** `am start -n $PKG/.MainActivity` resolveria para `com.nutri.android.dev.MainActivity`, que não existe. Os scripts agora usam `ACTIVITY=com.nutri.android.MainActivity`. `fake-chat-server.mjs`: `assembleDevDebug`.
- **Desvio de QA:** num pacote novo, o primeiro Home abre o diálogo `POST_NOTIFICATIONS` do A7 e trava os fluxos. `capture-chat`, `capture-home`, `capture-config` e `capture-photo` fazem `pm grant … POST_NOTIFICATIONS` logo depois do onboarding. O `capture-push` não mudou, porque ele testa o diálogo.
- Skills (`.agents`, `.grok`, `.hermes`, iguais por `diff -r`): `dieta-bot-android-ui`, `screenshot-testing` → `*RoborazziDevDebug`; `compose-stability` → `assembleDevRelease`. O `r8-analyzer` (skill oficial do Google) não foi tocado.
- Docs: `AGENTS.md` (stack), `docs/android/README.md` (seção Flavors), `docs/qa/README.md`, `docs/server/deploy-gcp.md`, `SETUP.md`.
- Push: todos os `PendingIntent` usam `Intent(context, PushReceiver::class.java)` explícito, então dev e prod lado a lado não colidem. O FileProvider usa `${applicationId}`/`packageName`.

### Resultados da validação

1. `:app:tasks --all` → `assembleDevDebug`, `assembleDevRelease`, `assembleProdDebug`, `assembleProdRelease`. ✅
2. `assembleDevRelease` → `package: name='com.nutri.android.dev' versionName='1.0-dev'`, `application-label:'Nutri Dev'`, assinado `CN=Nutri` (SHA-256 `c9978f23…2b8a1d`, A9); `BuildConfig`: `ENV="dev"`, URL GCP, convite de 40 chars (não é o default). ✅
3. `assembleProdRelease` sem `prod.*` → `:app:preProdReleaseBuild FAILED` com "prod flavor: defina prod.API_PUBLIC_URL e prod.INVITE_CODE…". Com valores de teste temporários → `package: name='com.nutri.android'`, rótulo "Nutri", `ENV="prod"`. `local.properties` restaurado e o APK de teste apagado. ✅
4. `testDevDebugUnitTest` → 186 testes, 0 falhas (inclui `StitchGoldTest` e `RoborazziSmokeTest`); `verifyRoborazziDevDebug` verde; `src/test/snapshots` sem mudança. ✅
5. Emulador `Medium_Phone` (780×1688 @ 320) + fake server, `assembleDevDebug -PAPI_PUBLIC_URL=http://10.0.2.2:8765`:
   - `capture-chat.sh light` → **21 ✓, 0 ✗**.
   - Pixel diff contra as capturas anteriores (abaixo da status bar): 0,03–0,42%. Diferenças: data do chip (27 → 28/09), horário da mensagem, cursor do campo e o chip "Tirar foto do prato" habilitado. Este último é **correção de evidência**: a captura `light/chat0` commitada no A6 ainda era do A5 (chip `enabled = false`), e o código atual sempre o habilita.
   - `diff-gold.mjs` light: splash 0,70%, o1 0,82%, o2 1,33%, o3 1,09%, o4 0,91%, todos ✓ (máx. 2%). ✅
6. `grep` final por `assembleDebug|RoborazziDebug|testDebugUnitTest|PKG=com.nutri.android$|$PKG/.MainActivity` fora de planos concluídos: zero ocorrências. ✅
7. Manual (dono): "Nutri Dev" instalado no celular, onboarding e refeição no 4G, "Nutri" antigo desinstalado. Confirmado pelo dono em 28/09/2026: "Nutri dev ok". ✅ APK: `apps/android/app/build/outputs/apk/dev/release/app-dev-release.apk`.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
