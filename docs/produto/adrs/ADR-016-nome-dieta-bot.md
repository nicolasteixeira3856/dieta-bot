# ADR-016 — Nome visível "Dieta Bot"; IDs técnicos continuam "nutri"

- Estado: Aceito (aprovação do plano A13, 2026-09-28)
- Data: 2026-09-28
- Contexto: `produto`
- Substitui: parcialmente o nome "Nutri" em `AGENTS.md` e nas specs vivas. ADRs aceitos não se editam.

## Contexto

O app nasceu como "Nutri". O dono passou a chamá-lo de "Dieta Bot" (o repo já se chama `dieta-bot`), e esse nome ainda é **temporário**. "Nutri" aparece em cerca de 170 arquivos, em três camadas: nome visível, docs/código e IDs técnicos.

## Decisão

- **Muda para "Dieta Bot"** (duas palavras, D e B maiúsculos):
  - Tudo o que o usuário vê: nome no launcher, wordmark da splash, cabeçalho do onboarding, "Chat Dieta Bot", "Dieta Bot AI" e o nome que o modelo usa para se apresentar.
  - Docs vivas (`AGENTS.md`, READMEs, specs, SETUP, skills).
  - Nomes de classe, arquivo e estilo no Kotlin: `Nutri*` → `DietaBot*` (ex.: `NutriTheme` → `DietaBotTheme`, `Theme.Nutri` → `Theme.DietaBot`).
- **Fica "nutri"** (identidade técnica): pacote e `applicationId` `com.nutri.android(.dev)`, projeto Firebase `nutri-bot-dev`, banco `nutri.db`, VM `nutri-api` e `/opt/nutri`, loggers `nutri`, projeto Stitch `Nutri` (o ID), `nutri-release.jks`, extras e actions `com.nutri.*`.
- ADRs aceitos e planos concluídos não se editam: são histórico.

## Motivação

- Trocar o `applicationId` cria outro app: reinstalar, perder dados locais e registrar outro app no Firebase. Com o nome ainda temporário, esse custo poderia se repetir.
- O nome visível e as docs são o que confunde no dia a dia.

## Consequências

### Positivas

- App e docs falam "Dieta Bot"; nenhum dado se perde; Firebase, VM e assinatura continuam iguais.

### Negativas

- Convivem os dois nomes: "Dieta Bot" na superfície, `nutri` nos IDs. Fica documentado aqui e no `AGENTS.md`.
- Os golds do Stitch que mostram o nome precisam ser atualizados antes da UI (regra de QA visual).
- O pacote definitivo fica para quando o nome for final, antes de publicar o prod (ADR futuro).

## Alternativas consideradas

### Trocar tudo, incluindo pacote, Firebase e banco

Rejeitada pelo dono (opção b): custo alto para um nome temporário.

### Só o nome no launcher

Deixa a confusão nas docs e no código.

## Relações

- Planos: [A13](../../android/plans/completed/a13-rename-dieta-bot.md) (app, docs, skills), [S7](../../server/plans/completed/s7-rename-prompt.md) (prompt do server).
- ADRs relacionados: [ADR-012](ADR-012-chat-home-perfil.md), [ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md).

Depois de aceito, este ADR não se edita. Mudança posterior exige ADR novo que declare a substituição.
