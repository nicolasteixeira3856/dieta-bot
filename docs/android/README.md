# android

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
- Visual QA: `docs/qa/android/current/{dark,light}/` vs gold em `docs/qa/wire/`.

## Fora de escopo

- Contrato HTTP e LLM — [server](../server/README.md).
- Job, copy, o que entra no prompt — [produto](../produto/README.md).
- Flutter / RN. Mortos.

## Fronteiras e dependencias

- Teto e orcamento: BudgetCalculator no domain. API nao calcula teto.
- Header X-Invite + BuildConfig.API_PUBLIC_URL.
- ADRs vigentes em docs/decisions/: 002, 004, 005, 007, 008, 009, 010, 011.

## Cobertura documental atual

Sem specifications/ neste contexto. Telas descritas por AGENTS.md + wires + ADR 010/011.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. [matriz](../README.md) e este README
2. AGENTS.md (stack, tokens, visual QA)
3. ADRs 010 e 011 se o assunto for Room ou T2/T3
4. Codigo em `apps/android/`

## Estado atual

Client vivo. Room v1: profile, day, meal_log. T1/T3 sheet, T2 rota cheia. Sem tela Chat, sem Config, sem push.

## Indice

### Especificacoes

Nenhuma especificacao criada ate o momento.

### ADRs

Nenhum ADR local. Historico: [002](../decisions/002-android-client.md), [004](../decisions/004-m3-expressive.md), [005](../decisions/005-android-only.md), [007](../decisions/007-english-identifiers.md), [008](../decisions/008-visual-qa.md), [009](../decisions/009-visual-match.md), [010](../decisions/010-room.md), [011](../decisions/011-t2-t3-actions.md).

### Planos e validacao

Nenhum plano criado ate o momento.
