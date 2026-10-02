# android

## Planned content-policy work

[CP4](../content-policy/plans/completed/cp4-android-installation-identity.md) is delivered in dev 0.0.6 (2026-10-01, `Concluído`): a private random installation UUID in `noBackupFilesDir`, sent as `X-Client-Instance-Id` only to the configured API origin's `v1/` routes (network interceptor, no cross-origin redirect), under [identity/audit policy](../content-policy/specifications/identity-and-audit.md). No account, Firebase identifier, new screen or Room migration. Server and GCP work have separate plans in the [content-policy order](../content-policy/plans/README.md).

## Proposito

Client nativo. Compose, Room, navegacao, foto, push.

## Tipo e ownership

- Tipo: client.
- Codigo principal: `apps/android/`.
- Integracoes: consome [server](../server/README.md). Comportamento visivel: [produto](../produto/README.md).

## Escopo

- Kotlin + Jetpack Compose + Material 3 Expressive.
- Camada ui / domain / data. Hilt, Navigation Compose, Retrofit, Room.
- Home, onboarding, sheets, foto. DataStore so como import legado.
- Visual QA: `docs/qa/android/current/{dark,light}/` (captura do emulador: `tools/capture-*.sh` ou `android screen capture`; medidas com `android layout`) vs gold em `docs/qa/stitch/{dark,light}/` (Stitch `Nutri`). Gate: `tools/diff-gold.mjs` + `StitchGoldTest` (ver [docs/qa](../qa/README.md)).

## Fora de escopo

- Contrato HTTP e LLM — [server](../server/README.md).
- Job, copy, o que entra no prompt — [produto](../produto/README.md).
- Flutter / RN. Mortos.

## Fronteiras e dependencias

- Teto e orcamento: BudgetCalculator no domain. API nao calcula teto.
- Header X-Invite + BuildConfig.API_PUBLIC_URL.
- ADRs vigentes em docs/decisions/: 002, 004, 005, 007, 008, 009, 010, 011.

## Cobertura documental atual

Sem specifications/ neste contexto. Telas descritas por ADR-012 + Stitch (`docs/qa/stitch/`).

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. AGENTS.md (stack, tokens, visual QA)
3. ADR 010 se o assunto for Room (o 011, T2/T3, é histórico: fluxo removido no A12)
4. Codigo em `apps/android/`

## Estado atual

Client vivo. Room v7 (refeições por dia, A24; Chat v2, A27; memória por mensagem, A28; índice da página do Chat, A32): profile, day, meal_log, meal_slot, slot_skip, chat_message, day_digest. Home painel (A4). Chat (A5) com compact (A5b), memória cifrada em fatos (A8b, A28) e foto (A6); Config com wipe e treino do dia (A3). Push por slot (A7).

## Flavors

[ADR-014](adrs/ADR-014-flavors-firebase-dev.md), [A10](plans/completed/a10-flavors-dev-prod.md). Dimensão `env`:

| | dev | prod |
|---|---|---|
| `applicationId` | `com.nutri.android.dev` ("Dieta Bot Dev", `versionName` `-dev`) | `com.nutri.android` ("Dieta Bot") |
| `local.properties` | `dev.API_PUBLIC_URL`, `dev.INVITE_CODE` (fallback nas chaves sem prefixo) | `prod.API_PUBLIC_URL`, `prod.INVITE_CODE` (obrigatórias) |
| `BuildConfig.ENV` | `"dev"` | `"prod"` |

- Uso diário: `./gradlew.bat :app:assembleDevRelease` → `app/build/outputs/apk/dev/release/app-dev-release.apk`.
- Testes: `:app:testDevDebugUnitTest`, `verifyRoborazziDevDebug`. Sem `prod.*`, qualquer task de prod (incluindo `test`/`build` agregados) falha de propósito.
- `-PAPI_PUBLIC_URL=...` sobrescreve só o dev (QA com `tools/fake-chat-server.mjs`).
- O pacote Kotlin continua `com.nutri.android`. Via `adb`, abra com `com.nutri.android.dev/com.nutri.android.MainActivity`.

## Telemetria

[ADR-014](adrs/ADR-014-flavors-firebase-dev.md), [A11](plans/completed/a11-firebase-dev.md). Só no flavor dev.

- Firebase `nutri-bot-dev` ("Nutri Dev", plano Spark), app `1:823717355877:android:d01b29a0b20b0674bd818c` (`com.nutri.android.dev`). `app/src/dev/google-services.json` fica fora do git: sem ele, o build avisa e segue.
- `core/telemetry/Telemetry` em `main`. dev → `FirebaseTelemetry` (Crashlytics + Analytics, sem Advertising ID); prod → `NoopTelemetry`, sem nenhuma classe Firebase no APK.
- Toda chamada à API leva `X-Request-Id`, `X-App-Version` e `X-App-Env` (`RequestIdInterceptor`). O id liga o Crashlytics ao log de conversa do server de dev ([ADR-015](../server/adrs/ADR-015-log-conversa-dev.md), `tools/pull-conversations.ps1 -RequestId <id>`).
- Non-fatals: `ApiFailure` (rede/HTTP) e `ChatFallback` (resposta "nao deu pra estimar" do server), com o `request_id`.
- Eventos: `screen_view` (ids ADR-012), `api_call`, `chat_send`, `chat_result`, `meal_saved`, `meal_skipped`, `onboarding_complete`, `push_action`. Só enums e números; o texto do usuário nunca sai por aqui.
- Crash de teste (só dev, só via adb): `adb shell am broadcast -a com.nutri.android.dev.TEST_CRASH -n com.nutri.android.dev/com.nutri.android.core.telemetry.TestCrashReceiver`.
- Analytics DebugView: `adb shell setprop debug.firebase.analytics.app com.nutri.android.dev`.

## Marca

[A14](plans/completed/a14-marca-icone-splash.md). Fonte em `design/brand/` (`icon.png`, `icon-mono.png`, `icon-dark-bg.png` de referência).

- `./tools/brand-icons.ps1` gera `mipmap-*/ic_launcher_foreground.png` (símbolo na área segura 66/108 dp), `mipmap-*/ic_launcher_monochrome.png` (ícone temático, Android 13+) e `drawable-nodpi/logo_mark.png` (splash). Rode de novo depois de trocar os arquivos da pasta.
- `mipmap-anydpi-v26/ic_launcher(_round).xml`: fundo `@color/ic_launcher_bg` (`#0B0D10`) + foreground + monochrome. `minSdk` 26, então não há PNG legado.
- Splash do sistema (Android 12+) mostra o foreground do ícone; a splash em Compose mostra `logo_mark` (120 dp) acima do wordmark.

## Release

- `./gradlew.bat :app:assembleDevRelease` (ou `assembleProdRelease`), assinado com a chave própria ([A9](plans/completed/a9-assinatura-release.md)).
- Chave: `nutri-release.jks` + `key.properties` na raiz do repo, fora do git. Certificado `CN=Nutri, O=Nicolas Teixeira, C=BR`, SHA-256 `C9:97:8F:23:53:3C:F1:AD:91:C3:A7:EA:AE:57:A7:D7:88:E3:92:B9:50:D0:0F:9B:D8:A0:85:3F:7B:2B:8A:1D`.
- Sem `key.properties` o build gera `app-<flavor>-release-unsigned.apk` e avisa. Nunca usa a chave de debug.
- Perder a chave = nenhuma atualização instala por cima. Backup fora do PC é obrigatório.

## Distribuição

[A16](plans/completed/a16-app-distribution.md). Build de teste para o celular do dono, sem cabo:

- `./tools/distribute-dev.ps1 -Notes <arquivo.md>`: notas válidas → árvore git limpa → versão → `testDevDebugUnitTest` → `assembleDevRelease` → confere `CN=Nutri` (`apksigner`) e versão (`aapt2`) → Firebase App Distribution (`nutri-bot-dev`, grupo `testers`) → `CHANGELOG.md` → commit `chore(release): 0.0.N-dev` + tag `dev-v0.0.N` + push.
- `-Notes` ([A17](plans/completed/a17-changelog-deploy.md)): obrigatório fora do `-DryRun`. Changelog em pt-BR para o tester, escrito pelo agente a partir dos commits e planos desde a tag `dev-v0.0.*` anterior: o que ele nota no app, seções opcionais `### Novidades`, `### Correções`, `### Ajustes`, um item curto por linha. Sem nome de arquivo, id de plano, hash ou termo técnico. O script recusa arquivo ausente ou vazio, hash de commit, prefixo `feat:`/`fix(…):`/`chore:` e título `#`/`##`, sempre antes do build, sem queimar número. Arquivo temporário no scratchpad do agente.
- Notas no App Tester: `Dieta Bot 0.0.N` + linha em branco + o arquivo. O mesmo texto entra no topo de [`apps/android/CHANGELOG.md`](../../apps/android/CHANGELOG.md) como `## 0.0.N — DD/MM/AAAA` (America/Sao_Paulo), no commit `chore(release)`.
- `-DryRun`: faz o build e as conferências; não distribui, não commita, não faz push, e deixa a árvore limpa. Com `-Notes`, valida e mostra as notas.
- Versão: `apps/android/version.properties` (`VERSION_PATCH=N`) → `versionName` `0.0.N` (+ `-dev` do flavor) e `versionCode` `N`. O Gradle falha se o arquivo faltar ou não for inteiro positivo.
- Bump: se a tag `dev-v0.0.N` já existe, o script grava N+1; se não existe (primeira vez), publica N como está. Falha antes do commit desfaz o bump, então o número não é queimado. Nunca editar o arquivo à mão; nunca reutilizar um número.
- Até a `1.0.0`: a virada e a regra depois dela são um plano/ADR próprio.
- Console: `https://console.firebase.google.com/project/nutri-bot-dev/appdistribution`. O dono instala e atualiza pelo app **Firebase App Tester**.

## Indice

### Especificacoes

[Room v8](specifications/room-v2.md).

### ADRs

Historico: [002](../decisions/002-android-client.md), [004](../decisions/004-m3-expressive.md), [005](../decisions/005-android-only.md), [007](../decisions/007-english-identifiers.md), [008](../decisions/008-visual-qa.md), [009](../decisions/009-visual-match.md), [010](../decisions/010-room.md), [011](../decisions/011-t2-t3-actions.md).

- [ADR-014](adrs/ADR-014-flavors-firebase-dev.md) — flavors dev/prod, Firebase só no dev (aceito).
- [ADR-018](adrs/ADR-018-foto-2048.md) — foto reduzida a 2048 px no client (aceito com o A18).
- [ADR-027](adrs/ADR-027-golds-divergentes.md) — QA visual quando os golds divergem (aceito em 01/10/2026, após o A31).

### Planos e validacao

Registro autônomo ([ADR-028](../produto/adrs/ADR-028-registro-autonomo.md), aceito em 02/10/2026):

- [A34 Registro autônomo, recibos com ações](plans/pending_manual_validation/a34-registro-autonomo.md) — pendente aprovação manual (código, testes, golds e captura entregues em 02/10/2026; falta a rodada manual do dono no servidor dev e o DebugView); [S14](../server/plans/completed/s14-registro-autonomo.md) e gate [ST9](../stitch/plans/completed/st9-registro-autonomo.md); Room v8
- [A35 Registro retroativo](plans/out_of_scope/a35-registro-retroativo.md) — fora de escopo (decisão do dono, 01/10/2026)

Ajustes de O1 e Chat (decisões do dono em 01/10/2026):

- [A31 O1: perfil obrigatório e navegação pelo teclado](plans/completed/a31-o1-perfil-obrigatorio-teclado.md) — gate [ST8](../stitch/plans/completed/st8-teto-sem-perfil.md), gold `o1e`; concluído em 01/10/2026
- [A32 Chat: abre no fim, paginação reversa, teclado fecha na foto](plans/completed/a32-chat-rolagem-paginacao.md) — Room v7; concluído em 01/10/2026

Perguntas antes da estimativa ([ADR-026](../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), aceito): [A30 Perguntas antes da estimativa, Forçar estimativa](plans/completed/a30-perguntas-antes-da-estimativa.md) — golds `chatE` e `chatQ` do [ST7](../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md); server [S13](../server/plans/completed/s13-perguntas-antes-da-estimativa.md); concluído em 30/09/2026.

Chat v2 + Memória v2 ([ADR-023](../produto/adrs/ADR-023-chat-v2-memoria-v2.md), 30/09/2026). A27, A28 e A29 concluídos (aprovado pelo dono em 30/09/2026):

- [A27 Chat v2: texto da refeição, intenção e histórico de 7 dias](plans/completed/a27-chat-v2-texto-intencao.md) — Room v5; concluído
- [A28 Memória v2: fatos permanentes e dinâmicos](plans/completed/a28-memoria-v2.md) — Room v6; concluído
- [A33 capture-chat: checagens de memória no formato A28](plans/completed/a33-capture-chat-memoria-v2.md) — só `tools/capture-chat.sh`; concluído em 01/10/2026
- [A29 Chat v2: plano de refeição, avisos de memória e sugestão da rotina](plans/completed/a29-chat-v2-interface.md) — golds do [ST6](../stitch/plans/completed/st6-chat-v2.md) batidos; concluído

Lote do feedback (29/09/2026), concluído (aprovado pelo dono em 30/09/2026):

- [A21 seletor de horário em rodas (Concluído)](plans/completed/a21-seletor-horario.md)
- [A23 editor de memória e perfil (dev) (Concluído)](plans/completed/a23-editor-memoria-dev.md)
- [A24 refeições por dia da semana](plans/completed/a24-refeicoes-por-dia.md) — concluído

- [A22 treino na Home (Concluído)](plans/completed/a22-treino-home.md)
- [A25 composer: limite de 2000 caracteres com estado de erro (Concluído)](plans/completed/a25-limite-texto-composer.md)
- [A19 chat visual: pergunta em bolha, bolhas iguais, anexo com preview (Concluído)](plans/completed/a19-chat-visual.md)
- [A20 polimento geral (Concluído)](plans/completed/a20-polimento-geral.md)
- [A26 Android CLI no loop de QA visual (Concluído)](plans/completed/a26-android-cli-qa.md)
- [A12 remover legado T1/T2/T3 (Concluído)](plans/completed/a12-remover-legado-t123.md)
- [A13 rename visível "Dieta Bot" (Concluído)](plans/completed/a13-rename-dieta-bot.md)
- [A14 marca: ícone e splash (Concluído)](plans/completed/a14-marca-icone-splash.md)
- [A15 ajuste aos golds novos (Concluído)](plans/completed/a15-ajuste-visual-golds-novos.md)
- [A16 Firebase App Distribution, versão 0.0.N (Concluído)](plans/completed/a16-app-distribution.md)
- [A9 chave de assinatura do release (Concluído)](plans/completed/a9-assinatura-release.md)
- [A10 flavors dev/prod (Concluído)](plans/completed/a10-flavors-dev-prod.md)
- [A18 chat: refeição consolidada, Enter, foto 2048 px (Concluído)](plans/completed/a18-chat-registro-foto.md)
- [A17 changelog humano no deploy (Concluído)](plans/completed/a17-changelog-deploy.md)
- [A11 Firebase Crashlytics + Analytics no dev (Concluído)](plans/completed/a11-firebase-dev.md)

- [A0 refatoracao arquitetural (Concluído)](plans/completed/a0-arch-refactor.md) (Feature-first, eliminacao de Java/kapt, navegacao limpa)
- [A0 tokens Expressive (Concluído)](plans/completed/a0-tokens-expressive.md) (Tokens semanticos P/C/G, tipografia 34pt, ButtonGroup)
- [A0 Roborazzi setup (Concluído)](plans/completed/a0-roborazzi-setup.md) (Testes de screenshot headless JVM)
- [A1 Room v2 (Concluído)](plans/completed/a1-room-v2.md) (Room v2, migration 1→2, domain TMB/macros/slots)
- [A2 Onboarding perfil (Concluído)](plans/completed/a2-onboarding-perfil.md) (splash + O1..O4 Stitch, gate visual)
- [A4 Home painel (Concluído)](plans/completed/a4-home-painel.md) (home1 canônica; home0/homeX com conflito de gold)
- [A5 Chat (Concluído)](plans/completed/a5-chat.md) (chat0..chatG Stitch)
- [A3 Config + wipe + treino (Concluído)](plans/completed/a3-config-wipe-treino.md) (cfg, wipe Stitch)
- [A5b ligar compact (Concluído)](plans/completed/a5b-ligar-compact.md) (compact=true ligado no Chat)
- [A8 memoria (Concluído)](plans/completed/a8-memoria.md) (memory no POST)
- [A8b memória com gravação atômica (Concluído)](plans/completed/a8b-memoria-gravacao-atomica.md) (Keystore AES-GCM + move atômico, migração do A8)
- [A6 foto (Concluído)](plans/completed/a6-foto.md) (câmera + galeria, chatF)
- [A7 push (Concluído)](plans/completed/a7-push.md) (alarme por slot, Registrar/Pular)
