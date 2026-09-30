# Plano — A11 Firebase Crashlytics + Analytics no flavor dev

- Estado: Concluído
- Aprovação manual: 30/09/2026 (dono: "Quero que você passe todos os planos que estão pendentes de validação manual para completo.")
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (Gradle, `src/main` interface de telemetria + interceptor, `src/dev` Firebase, `src/prod` no-op), `.mcp.json`, `.gitignore`, docs. Fora do repo: projeto Firebase `nutri-bot-dev`.
- Pré-requisitos: [A10](a10-flavors-dev-prod.md) concluído (ADR-014 aceito). [S6](../../../server/plans/completed/s6-log-conversa-dev.md) recomendado antes, mas não obrigatório: sem ele, o `X-Request-Id` do app só aparece no Firebase.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a11-firebase-dev.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O "Nutri Dev" reporta crashes, falhas não fatais (incluindo cada "nao deu pra estimar") e eventos de uso ao Firebase `nutri-bot-dev`. Cada chamada à API carrega um `X-Request-Id` que liga o Crashlytics ao log de conversa do server (S6). O prod continua sem nenhuma dependência Firebase.

## Fontes de verdade

- [ADR-014](../../adrs/ADR-014-flavors-firebase-dev.md), [ADR-015](../../../server/adrs/ADR-015-log-conversa-dev.md).
- Código: `core/network/NetworkModule.kt`, `feature/chat/ChatViewModel.kt`, `NutriApplication.kt`, navegação em `MainActivity.kt`.

## Decisões fixas

| Item | Valor |
|---|---|
| Projeto Firebase | ID `nutri-bot-dev` (ou `nutri-bot-dev-<nnnn>` se estiver ocupado; `_` não é aceito em IDs). Nome "Nutri Dev". Plano Spark (grátis, sem billing) |
| App Firebase | Android `com.nutri.android.dev`, apelido "Nutri Dev" |
| Config | `apps/android/app/src/dev/google-services.json`, no `.gitignore`. O plugin usa `missingGoogleServicesStrategy = WARN`, então clone limpo e prod constroem sem ele |
| Produtos | Crashlytics + Analytics. Nada de Firestore, Remote Config, FCM, Gemini/AI Logic |
| Dependências | Firebase BoM + `firebase-crashlytics` + `firebase-analytics`, só em `devImplementation`. Plugins `com.google.gms.google-services` e `com.google.firebase.crashlytics` |
| Anúncios | `google_analytics_adid_collection_enabled=false`; permissão `AD_ID` removida do manifest dev |
| MCP | `firebase` → `cmd /c npx -y firebase-tools@latest mcp`, no `.mcp.json` |

### Telemetria

Interface `Telemetry` em `main` (`event`, `breadcrumb`, `nonFatal`, `setKey`), injetada por Hilt. `FirebaseTelemetry` em `src/dev/`, `NoopTelemetry` em `src/prod/`.

| Sinal | Onde | Conteúdo |
|---|---|---|
| Crash | automático | stack trace + breadcrumbs + chaves `env`, `last_request_id` |
| Non-fatal `ApiFailure` | interceptor | rota, código HTTP ou exceção de rede, `request_id` |
| Non-fatal `ChatFallback` | ChatViewModel | resposta `nao deu pra estimar` sem estimativa, `request_id`, se tinha foto |
| Evento `screen_view` | navegação | rota (`home`, `chat`, `cfg`, `o1`…) |
| Evento `api_call` | interceptor | `route`, `status`, `latency_ms` |
| Evento `chat_send` | ChatViewModel | `has_photo`, `text_len` em faixa (`<20`, `20-100`, `>100`) |
| Evento `chat_result` | ChatViewModel | `outcome` (`ok`/`fallback`/`error`), `has_estimate`, `confidence` |
| Eventos `meal_saved`, `meal_skipped`, `onboarding_complete`, `push_action` | onde já acontecem | só enums e números |

Nenhum texto do usuário, foto, memória ou convite vai para o Firebase. O texto completo fica só no log do server (S6).

### Request id

`RequestIdInterceptor` em `main` (os dois flavors): em cada chamada, gera um UUID e envia `X-Request-Id`, `X-App-Version` (`versionName`) e `X-App-Env` (`BuildConfig.ENV`). Mede a latência, chama a `Telemetry` e guarda o id como `last_request_id`.

## Divisão de trabalho

### Fase 0 — MCP e contas

> Adiantado em 28/09/2026, antes da aprovação: login no CLI confirmado (`login:list`, `projects:list` ok, termos aceitos), servidor `firebase` no `.mcp.json` e `claude mcp list` = Connected. Na implementação, só o passo 5 (sanidade) roda.

1. **[DONO]** Login no Firebase CLI (abre o navegador):
   ```
   ! npx -y firebase-tools@latest login
   ```
2. **[DONO]** Se for a primeira vez desta conta no Firebase: abrir `console.firebase.google.com` uma vez e aceitar os termos.
3. **[AGENTE]** Adicionar o servidor `firebase` ao `.mcp.json`.
4. **[DONO]** Reiniciar o Claude Code e aprovar o MCP `firebase`. `/mcp` deve mostrar `gcloud` e `firebase` conectados.
5. **[AGENTE]** Sanidade pelo MCP: ambiente/conta ativa, listagem de projetos. Confirmar quais ferramentas de Crashlytics o MCP expõe e registrar aqui. Se faltar alguma, usar o `firebase` CLI via `npx` como fallback.

### Fase 1 — Projeto e app (AGENTE, via MCP; CLI como fallback)

6. Criar o projeto `nutri-bot-dev` ("Nutri Dev").
7. Registrar o app Android `com.nutri.android.dev` e baixar o `google-services.json` para `apps/android/app/src/dev/`.
8. **[DONO]** No console: Configurações do projeto → Integrações → **Google Analytics → Ativar**, com uma conta do Analytics nova ou existente. Isso não é feito por CLI/MCP.
9. Registrar no plano o ID do projeto e o App ID (`1:…:android:…`).

### Fase 2 — Código (AGENTE)

10. Versões atuais do BoM e dos plugins compatíveis com o AGP 9.4 no `libs.versions.toml`. Se houver incompatibilidade, parar e voltar ao Planning.
11. Gradle: plugins aplicados; `googleServices { missingGoogleServicesStrategy = WARN }`; `devImplementation` do BoM + Crashlytics + Analytics.
12. `src/main`: `Telemetry`, `RequestIdInterceptor` (ligado no `NetworkModule`), chamadas em `ChatViewModel`, navegação, onboarding, registrar/pular e push.
13. `src/dev`: `FirebaseTelemetry` + módulo Hilt; `AndroidManifest.xml` dev com os meta-data de anúncio, `AD_ID` removido e o receiver de teste (item 14).
14. `src/dev`: `TestCrashReceiver` exportado, só no dev, ação `com.nutri.android.dev.TEST_CRASH`, que lança `RuntimeException("crashlytics test")`. Não é tela, e serve só para validar.
15. `src/prod`: `NoopTelemetry` + módulo Hilt.
16. `.gitignore`: `google-services.json`.
17. Testes JVM: interceptor põe os três headers e chama `Telemetry.event("api_call")`; o `ChatViewModel` chama `nonFatal(ChatFallback)` e `event("chat_result", outcome=fallback)` na resposta de fallback, usando um `FakeTelemetry`.

### Fase 3 — Validação (AGENTE, emulador + MCP)

18. `testDevDebugUnitTest` + `verifyRoborazziDevDebug` verdes; nenhuma tela mudou (`capture-chat.sh light` igual ao A10).
19. APK prod: `apkanalyzer dex packages` sem `com.google.firebase`.
20. Emulador com `assembleDevDebug`: logcat mostra o Crashlytics inicializado; com `adb shell setprop debug.firebase.analytics.app com.nutri.android.dev`, o logcat `FA` mostra `screen_view`, `chat_send`, `api_call` e `chat_result`.
21. `adb shell am broadcast -a com.nutri.android.dev.TEST_CRASH -n com.nutri.android.dev/…TestCrashReceiver` → reabrir o app → a issue "crashlytics test" aparece pelo MCP em alguns minutos.
22. Forçar um fallback contra o `fake-chat-server` (modo que devolve `reply` vazio) → non-fatal `ChatFallback` com `request_id` no Crashlytics. Com o S6 feito, repetir contra o server GCP e achar a linha pelo mesmo id com `pull-conversations.ps1 -RequestId`.

### Fase 4 — Celular (DONO) e docs (AGENTE)

23. **[AGENTE]** `assembleDevRelease` e instalar por `adb` se o celular estiver conectado. **[DONO]** Senão, instalar por arquivo.
24. **[DONO]** Usar normalmente por um dia; no dia seguinte, avisar o agente.
25. **[AGENTE]** Ler Crashlytics pelo MCP e o log do server, e fazer um resumo curto do que apareceu. O Analytics o dono vê no console, porque a leitura de Analytics por API/MCP exige export para BigQuery (Blaze), o que está fora de escopo.
26. Docs: `AGENTS.md` (item "Firebase" em *Do not* → "Firebase outside ADR-014"; linha de stack), `docs/android/README.md` (seção Telemetria), `SETUP.md` (login Firebase, `google-services.json`).

## Arquivos e áreas afetadas

- `apps/android/gradle/libs.versions.toml`, `apps/android/build.gradle.kts` (plugins), `apps/android/app/build.gradle.kts`.
- `apps/android/app/src/main/java/com/nutri/android/core/telemetry/` (novo), `core/network/NetworkModule.kt`, `feature/chat/ChatViewModel.kt`, pontos de evento em onboarding, home e push.
- `apps/android/app/src/dev/` (Firebase, manifest, receiver), `apps/android/app/src/prod/` (no-op).
- Testes JVM novos; `tools/fake-chat-server.mjs` (modo fallback, se ainda não existir).
- `.mcp.json`, `.gitignore`, `AGENTS.md`, `SETUP.md`, `docs/android/README.md`.

## Validação planejada

Os passos 18 a 22 e 25. Manual: passos 8, 23 e 24.

## Fora de escopo

- **App Distribution** para testers: plano próprio depois deste, porque exige `versionCode` crescente e lista de testers.
- Firebase no prod, BigQuery export, Performance Monitoring, Remote Config, FCM (o push continua local, via A7).
- Mudança de UI, de prompt ou do contrato do server (o S6 cobre o header).

## Riscos e controles

- **Plugins incompatíveis com o AGP 9.4:** passo 10 com parada obrigatória.
- **Firebase vazar para o prod:** `devImplementation` + passo 19.
- **Dado pessoal no Analytics:** a tabela de eventos só aceita enums e números; os testes cobrem os params do `chat_send`.
- **Login interativo:** o passo 1 é do dono; o agente não mexe em credenciais.
- **ID `nutri-bot-dev` ocupado:** sufixo numérico, registrado no passo 9.

## Critérios de aceite

- Crash de teste e `ChatFallback` visíveis no Crashlytics pelo MCP.
- Eventos no Analytics (DebugView/console).
- APK prod sem classes Firebase.
- O `request_id` do Crashlytics encontra a linha do log do server (com o S6 feito).

## Registro de execução

- 28/09/2026 — MCP `firebase` (sessão iniciada com o servidor aprovado): conta `nicolasteixeira3856@gmail.com`, termos aceitos.
  - O MCP **não expõe ferramentas de Crashlytics** por padrão. `.mcp.json` passou a iniciar com `mcp --only core,crashlytics`, válido a partir do próximo reinício do Claude Code.
- Projeto `nutri-bot-dev` ("Nutri Dev", número `823717355877`): o ID estava livre. App Android `com.nutri.android.dev` = `1:823717355877:android:d01b29a0b20b0674bd818c`. SHA-256 da chave do A9 cadastrado no app.
- `google-services.json` → `apps/android/app/src/dev/`; `.gitignore` com `google-services.json`. Nenhum `firebase.json`/`.firebaserc` criado no repo.
- Versões (Google Maven, estáveis): BoM `34.19.0`, `com.google.gms.google-services` `4.5.0`, `com.google.firebase.crashlytics` `3.0.8`. Configuram e compilam com AGP 9.4 sem ajuste.
- Código:
  - `core/telemetry/` em `main`: `Telemetry`, `NoopTelemetry`, `ApiFailure`, `ChatFallback`, `TelemetryEvents` (inclui `lengthBucket`) e `RequestIds` + `RequestIdInterceptor`.
  - `src/dev`: `FirebaseTelemetry` + `TelemetryModule`, `TestCrashReceiver` e o manifest sem `AD_ID`, com `google_analytics_adid_collection_enabled=false`.
  - `src/prod`: `TelemetryModule` → `NoopTelemetry`.
- **Desvio:** `NoopTelemetry` ficou em `main` (e não em `src/prod`). ViewModels e `PushHandler` recebem `telemetry: Telemetry = NoopTelemetry` com valor padrão, então os testes existentes não precisaram mudar.
- **Achado:** o fallback do server chega como `reply` preenchido (não era falha para o client). `ChatFallback` é detectado por `reply == "nao deu pra estimar"` sem estimativa, e a resposta continua exibida como antes.
- Eventos ligados: `screen_view` (listener de navegação, ids ADR-012), `api_call`, `chat_send`, `chat_result`, `meal_saved` (Chat), `meal_skipped` (Chat, Home, push), `onboarding_complete`, `push_action` (`shown`, `skip`, `open`).
- `TestCrashReceiver` exportado só no dev e protegido por `android.permission.DUMP`: só o `adb shell` consegue disparar.
- `tools/fake-chat-server.mjs`: modo `{"fallback": true}` e `/__calls` com o último `requestId`.

### Resultados da validação

1. `testDevDebugUnitTest` → **195** testes, 0 falhas (186 + 9 novos: interceptor ×3, buckets, nomes de tela, `ChatFallback`, `chat_send` sem texto, erro de rede, `meal_saved`). `verifyRoborazziDevDebug` verde; baselines sem mudança. ✅
2. APK prod (com `prod.*` temporários, restaurados depois): **0** referências `Lcom/google/firebase/` e 0 a `gms/measurement` nos `.dex`. O dev tem 1113. A falta do `google-services.json` no prod vira só aviso. ✅
3. Emulador (`Medium_Phone`, fake server): `capture-chat.sh light` → 21 ✓ / 0 ✗. Pixel diff igual ao do A10 (só data, hora e cursor). `diff-gold` light splash/o1–o4 ✓. Nenhuma tela mudou. ✅
4. Logcat: `Initializing Firebase Crashlytics 20.1.1 for com.nutri.android.dev`. Analytics em debug: `screen_view` ×22, `api_call` ×8, `chat_send` ×8, `chat_result` ×7, `meal_saved` ×1, `onboarding_complete` ×1. ✅
5. Fallback forçado no fake: a tela mostra "nao deu pra estimar", o FA registra `chat_result {outcome=fallback, has_estimate=false}` e o fake recebeu `X-Request-Id: c489966b-17d9-46c5-ab7f-481c802ef8a9`. ✅
6. Crash de teste por `adb`: `FATAL EXCEPTION … crashlytics test`, Crashlytics tratou a exceção. Na reabertura, upload para `crashlyticsreports-pa.googleapis.com` → `Status Code: 200` (Analytics em `firebaselogging-pa` → 200). ✅ Leitura da issue pelo MCP pendente do reinício (ferramentas de Crashlytics).
7. APK final: `app-dev-release.apk` (URL GCP, `ENV=dev`, chave A9). ⏳ Instalação pelo dono.
8. Manual: passo 8 feito pelo dono em 28/09/2026 (Google Analytics ativado no console). O `google-services.json` baixado depois é idêntico ao do repo, então não houve troca. ✅ Passos 23–24 e leitura do passo 25. ⏳

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`. Fica em `pending_manual_validation/` até o passo 25.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
