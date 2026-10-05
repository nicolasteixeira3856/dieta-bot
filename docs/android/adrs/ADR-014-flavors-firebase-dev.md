# ADR-014 — Flavors dev/prod e Firebase só no dev

- Estado: Aceito (aprovação do plano A10, 2026-09-28; a parte Firebase é implementada no A11); `applicationId` do dev e projeto `nutri-bot-dev` substituídos pelo [ADR-036](ADR-036-fibrai-technical-identity.md) (A48, 2026-10-05)
- Data: 2026-09-28
- Contexto: `android`
- Substitui: parcialmente `AGENTS.md` (item "Firebase" em *Do not*). Crashlytics e Analytics passam a ser permitidos, **só no flavor dev**. Gemini/Firebase AI continuam proibidos.

## Contexto

O app só tinha um ambiente: debug assinado com a chave do PC, apontando para o server GCP ([ADR-013](../../server/adrs/ADR-013-gcp-host.md)). O dono quer crash reports e telemetria para analisar falhas reais, e declarou que o server GCP e esse Firebase são **ambiente de dev**. Não existe prod ainda.

## Decisão

- Dimensão `env` com dois flavors:
  - `dev`: `applicationId com.nutri.android.dev`, nome "Nutri Dev", aponta para o server GCP, com Firebase.
  - `prod`: `applicationId com.nutri.android`, sem Firebase e sem telemetria. `API_PUBLIC_URL`/`INVITE_CODE` próprios. Sem eles, o build de prod falha com mensagem clara.
- Firebase projeto `nutri-bot-dev` (plano Spark), app Android `com.nutri.android.dev`, com **Crashlytics + Analytics**.
- Dependências Firebase só em `devImplementation`. O código chama uma interface `Telemetry` em `main`; a implementação Firebase fica em `src/dev/` e a no-op em `src/prod/`.
- Analytics recebe só eventos e métricas (sem texto livre do usuário). O conteúdo das conversas fica no log do server ([ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md)), correlacionado por `X-Request-Id`.
- Os dois flavors são assinados na release com a chave própria do app ([A9](../plans/completed/a9-assinatura-release.md)).

## Motivação

- Dev e prod instalam lado a lado, e nada de telemetria vaza para prod por construção (a dependência nem entra no APK de prod).
- O ID limpo `com.nutri.android` fica reservado para o prod futuro.
- Crashlytics dá stack trace e breadcrumbs, que o agente lê pelo MCP do Firebase. Analytics dá o funil de uso.

## Consequências

### Positivas

- Falhas do app ficam observáveis sem cabo USB nem logcat.
- Os ambientes ficam separados desde já, e o prod não herda dívida de dev.

### Negativas

- A troca de assinatura + ID exige instalar o app dev do zero. Os dados locais do app atual (perfil, dia, refeições) não migram.
- Nomes de tasks e APKs mudam (`assembleDevDebug`, `app-dev-debug.apk`, `testDevDebugUnitTest`, `verifyRoborazziDevDebug`). Scripts, skills e docs precisam acompanhar.
- Os plugins Google Services e Crashlytics entram no build. A compatibilidade com o AGP 9.4 tem que ser confirmada na implementação.

## Alternativas consideradas

### Firebase no app inteiro, sem flavors

Mistura ambientes e leva telemetria para um prod futuro. Rejeitada pelo dono.

### Firestore com cópia das conversas

Não teria a saída crua do modelo nem o prompt montado pelo server, e duplicaria dados pessoais em outro lugar. Rejeitada em favor do log no server.

### dev = ID base, prod = `.prod`

Mantém o ID atual para o uso diário, mas deixa o prod com um ID "sujo". Rejeitada pelo dono.

## Relações

- Planos: [A9](../plans/completed/a9-assinatura-release.md), [A10](../plans/completed/a10-flavors-dev-prod.md), [A11](../plans/completed/a11-firebase-dev.md).
- ADRs relacionados: [ADR-013](../../server/adrs/ADR-013-gcp-host.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md), [002](../../decisions/002-android-client.md).
- Contextos consumidores: [server](../../server/README.md) (header `X-Request-Id`).

Depois de aceito, este ADR não se edita. Mudança posterior exige ADR novo que declare a substituição.
