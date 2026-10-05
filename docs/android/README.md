# android

## Proposito

Client nativo. Compose, Room, navegacao, foto, push.

## Tipo e ownership

- Tipo: client.
- Codigo principal: `apps/android/`.
- Integracoes: consome [server](../server/README.md). Comportamento visivel: [produto](../produto/README.md).

## Escopo

- Kotlin + Jetpack Compose + the Aero design system ([ADR-030](../design/adrs/ADR-030-own-design-system-aero.md)); no Material 3.
- Camada ui / domain / data. Hilt, Navigation Compose, Retrofit, Room.
- Home, onboarding, sheets, foto. DataStore so como import legado.
- Visual QA: `docs/qa/android/current/{dark,light}/` (captura do emulador: `tools/capture-*.sh` ou `android screen capture`; medidas com `android layout`) vs gold em `docs/qa/figma/{dark,light}/` (Figma `Design`). Gate: `tools/diff-gold.mjs` + `GoldTest` (ver [docs/qa](../qa/README.md)).

## Fora de escopo

- Contrato HTTP e LLM — [server](../server/README.md).
- Job, copy, o que entra no prompt — [produto](../produto/README.md).
- Flutter / RN. Mortos.

## Fronteiras e dependencias

- Teto e orcamento: BudgetCalculator no domain. API nao calcula teto.
- Header X-Invite + BuildConfig.API_PUBLIC_URL.
- Header `X-Client-Instance-Id` (UUID de instalação): [identity/audit](../content-policy/specifications/identity-and-audit.md).
- ADRs: [índice](#adrs).

## Cobertura documental atual

Persistência: [spec do Room](specifications/room-v2.md). Telas e comportamento: specs do [produto](../produto/README.md) + golds do Figma `Design` ([inventário](../qa/README.md)).

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. AGENTS.md (stack, tokens, visual QA)
3. [Spec do Room](specifications/room-v2.md) se o assunto for persistência
4. Codigo em `apps/android/`

## Flavors

[ADR-014](adrs/ADR-014-flavors-firebase-dev.md). Dimensão `env`:

| | dev | prod |
|---|---|---|
| `applicationId` | `app.fibrai.android.dev` ("Dieta Bot Dev", `versionName` `-dev`) | `app.fibrai.android` ("Dieta Bot") |
| `local.properties` | `dev.API_PUBLIC_URL`, `dev.INVITE_CODE` (fallback nas chaves sem prefixo) | `prod.API_PUBLIC_URL`, `prod.INVITE_CODE` (obrigatórias) |
| `BuildConfig.ENV` | `"dev"` | `"prod"` |

- Uso diário: `./gradlew.bat :app:assembleDevRelease` → `app/build/outputs/apk/dev/release/app-dev-release.apk`.
- Testes: `:app:testDevDebugUnitTest`, `verifyRoborazziDevDebug`. Sem `prod.*`, qualquer task de prod (incluindo `test`/`build` agregados) falha de propósito.
- `-PAPI_PUBLIC_URL=...` sobrescreve só o dev (QA com `tools/fake-chat-server.mjs`).
- Pacote Kotlin e `namespace`: `app.fibrai.android` ([ADR-036](adrs/ADR-036-fibrai-technical-identity.md)). Via `adb`, abra com `app.fibrai.android.dev/app.fibrai.android.MainActivity`. Código que precisa do pacote instalado usa `BuildConfig.APPLICATION_ID` ou `context.packageName`, nunca o `namespace`.

## Telemetria

[ADR-014](adrs/ADR-014-flavors-firebase-dev.md). Só no flavor dev.

- Firebase `fibrai-dev` (plano Spark, Analytics ligado), app `1:353075659895:android:05bc4f6ec8410711727272` (`app.fibrai.android.dev`, "Fibrai Dev"), com os SHA-1/SHA-256 da chave de release e da chave de debug. O projeto antigo `nutri-bot-dev` está congelado. `app/src/dev/google-services.json` fica fora do git: sem ele, o build avisa e segue.
- `core/telemetry/Telemetry` em `main`. dev → `FirebaseTelemetry` (Crashlytics + Analytics, sem Advertising ID); prod → `NoopTelemetry`, sem nenhuma classe Firebase no APK.
- Toda chamada à API leva `X-Request-Id`, `X-App-Version` e `X-App-Env` (`RequestIdInterceptor`). O id liga o Crashlytics ao log de conversa do server de dev ([ADR-015](../server/adrs/ADR-015-log-conversa-dev.md), `tools/pull-conversations.ps1 -RequestId <id>`).
- Non-fatals: `ApiFailure` (rede/HTTP) e `ChatFallback` (resposta "nao deu pra estimar" do server), com o `request_id`.
- Eventos: `screen_view` (ids ADR-012), `api_call`, `chat_send`, `chat_result`, `meal_saved`, `meal_skipped`, `onboarding_complete`, `push_action`. Só enums e números; o texto do usuário nunca sai por aqui.
- Crash de teste (só dev, só via adb): `adb shell am broadcast -a app.fibrai.android.dev.TEST_CRASH -n app.fibrai.android.dev/app.fibrai.android.core.telemetry.TestCrashReceiver`.
- Analytics DebugView: `adb shell setprop debug.firebase.analytics.app app.fibrai.android.dev`.

## Marca

Fonte em `design/brand/` (`icon.png`, `icon-mono.png`, `icon-dark-bg.png` de referência).

- `./tools/brand-icons.ps1` gera `mipmap-*/ic_launcher_foreground.png` (símbolo na área segura 66/108 dp), `mipmap-*/ic_launcher_monochrome.png` (ícone temático, Android 13+) e `drawable-nodpi/logo_mark.png` (splash). Rode de novo depois de trocar os arquivos da pasta.
- `mipmap-anydpi-v26/ic_launcher(_round).xml`: fundo `@color/ic_launcher_bg` (`#0B0D10`) + foreground + monochrome. `minSdk` 26, então não há PNG legado.
- Splash do sistema (Android 12+) mostra o foreground do ícone; a splash em Compose mostra `logo_mark` (120 dp) acima do wordmark.

## Release

- `./gradlew.bat :app:assembleDevRelease` (ou `assembleProdRelease`), assinado com a chave própria.
- Chave: `fibrai-release.jks` (o antigo `nutri-release.jks` renomeado: mesma chave e alias) + `key.properties` na raiz do repo, fora do git. Certificado `CN=Nutri, O=Nicolas Teixeira, C=BR`, SHA-256 `C9:97:8F:23:53:3C:F1:AD:91:C3:A7:EA:AE:57:A7:D7:88:E3:92:B9:50:D0:0F:9B:D8:A0:85:3F:7B:2B:8A:1D`.
- Sem `key.properties` o build gera `app-<flavor>-release-unsigned.apk` e avisa. Nunca usa a chave de debug.
- Perder a chave = nenhuma atualização instala por cima. Backup fora do PC é obrigatório.

## Distribuição

Build de teste para o celular do dono, sem cabo:

- `./tools/distribute-dev.ps1 -Notes <arquivo.md>`: notas válidas → árvore git limpa → versão → `testDevDebugUnitTest` → `assembleDevRelease` → confere `CN=Nutri` (`apksigner`) e versão (`aapt2`) → Firebase App Distribution (`fibrai-dev`, grupo `testers`) → `CHANGELOG.md` → commit `chore(release): 0.0.N-dev` + tag `dev-v0.0.N` + push.
- `-Notes`: obrigatório fora do `-DryRun`. Changelog em pt-BR para o tester, escrito pelo agente a partir dos commits e planos desde a tag `dev-v0.0.*` anterior: o que ele nota no app, seções opcionais `### Novidades`, `### Correções`, `### Ajustes`, um item curto por linha. Sem nome de arquivo, id de plano, hash ou termo técnico. O script recusa arquivo ausente ou vazio, hash de commit, prefixo `feat:`/`fix(…):`/`chore:` e título `#`/`##`, sempre antes do build, sem queimar número. Arquivo temporário no scratchpad do agente.
- Notas no App Tester: `Dieta Bot 0.0.N` + linha em branco + o arquivo. O mesmo texto entra no topo de [`apps/android/CHANGELOG.md`](../../apps/android/CHANGELOG.md) como `## 0.0.N — DD/MM/AAAA` (America/Sao_Paulo), no commit `chore(release)`.
- `-DryRun`: faz o build e as conferências; não distribui, não commita, não faz push, e deixa a árvore limpa. Com `-Notes`, valida e mostra as notas.
- Versão: `apps/android/version.properties` (`VERSION_PATCH=N`) → `versionName` `0.0.N` (+ `-dev` do flavor) e `versionCode` `N`. O Gradle falha se o arquivo faltar ou não for inteiro positivo.
- Bump: se a tag `dev-v0.0.N` já existe, o script grava N+1; se não existe (primeira vez), publica N como está. Falha antes do commit desfaz o bump, então o número não é queimado. Nunca editar o arquivo à mão; nunca reutilizar um número.
- Até a `1.0.0`: a virada e a regra depois dela são um plano/ADR próprio.
- Console: `https://console.firebase.google.com/project/fibrai-dev/appdistribution`. O dono instala e atualiza pelo app **Firebase App Tester**.

## Indice

### Especificacoes

- [Room](specifications/room-v2.md) — persistência local (schema, migrations).

### ADRs

Histórico em `docs/decisions/`: [002](../decisions/002-android-client.md), [004](../decisions/004-m3-expressive.md), [005](../decisions/005-android-only.md), [007](../decisions/007-english-identifiers.md), [008](../decisions/008-visual-qa.md), [009](../decisions/009-visual-match.md), [010](../decisions/010-room.md), [011](../decisions/011-t2-t3-actions.md). Status: a linha de status de cada ADR.

- [ADR-014](adrs/ADR-014-flavors-firebase-dev.md) — flavors dev/prod, Firebase só no dev.
- [ADR-018](adrs/ADR-018-foto-2048.md) — foto reduzida a 2048 px no client.
- [ADR-027](adrs/ADR-027-golds-divergentes.md) — QA visual quando os golds divergem.
- [ADR-036](adrs/ADR-036-fibrai-technical-identity.md) — `app.fibrai.android` em todo o app (pacotes, app id, banco, ações), projeto Firebase `fibrai-dev`.

### Planos e validação

- Ativos: arquivos na raiz de [`plans/`](plans/).
- Pendente aprovação manual: [A47 — Chat meal updates](plans/pending_manual_validation/a47-chat-meal-updates.md).
- Pendente aprovação manual: [A48 — Fibrai technical identity: `app.fibrai.android` everywhere and Firebase `fibrai-dev`](plans/pending_manual_validation/a48-fibrai-app-id-firebase.md).
- [A49 — Fibrai and Tali in the app](plans/a49-fibrai-tali-visible-rename.md).
- Fora de escopo: [A35 Registro retroativo](plans/out_of_scope/a35-registro-retroativo.md).
- Histórico: [`plans/completed/`](plans/completed/).
- Validação: [`validation/`](validation/).
