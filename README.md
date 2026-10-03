# Dieta Bot

Android app that fits the next meal into today's remaining budget, dinner first. Log a meal in natural language or with a photo; the app shows kcal and macros against the day's target. An estimate, not advice.

Product name: Dieta Bot. Technical IDs stay `nutri` ([ADR-016](docs/produto/adrs/ADR-016-nome-dieta-bot.md)). The app is in a closed test.

## Start here

- [AGENTS.md](AGENTS.md): constitution (product rules, formulas, stack, gates). Read first.
- [docs/README.md](docs/README.md): documentation matrix. Routes to each context, its specifications, ADRs and plans.
- [docs/server/deploy-gcp.md](docs/server/deploy-gcp.md): dev server runbook.
- [docs/content-policy/production-gate.md](docs/content-policy/production-gate.md): what blocks production.

## Folders

```text
apps/android/   Android client (Kotlin, Compose)
server/         HTTP API (FastAPI)
infra/          host configuration
docs/           specifications, ADRs, plans, visual QA golds
design/         brand sources
tools/          QA, export, deploy and distribution scripts
.stitch/        Google Stitch design system (Nutri)
```

Agent skills live in `.agents/skills`, `.grok/skills`, `.hermes/skills` and `.claude/skills` (kept identical).
