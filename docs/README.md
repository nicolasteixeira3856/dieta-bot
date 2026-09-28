# Documentação — matriz

Índice global do Nutri. Política: [`sdd/README.md`](sdd/README.md).

Constituição: [`../AGENTS.md`](../AGENTS.md). Não duplicar regras aqui.

## Contextos

| Contexto | Tipo | Código | Spec viva | ADR local | Plano ativo | Validação |
|---|---|---|---|---|---|---|
| [produto](produto/README.md) | produto | — | [specifications/](produto/specifications/) | [ADR-012](produto/adrs/ADR-012-chat-home-perfil.md) | nenhum (Planning fechado) | [qa/](qa/) |
| [android](android/README.md) | client | `apps/android/` | [room-v2](android/specifications/room-v2.md) | histórico em [decisions/](decisions/) | [A0 arch, tokens, Roborazzi, A1, A2, A3, A4, A5, A5b, A6, A7, A8, A8b](android/plans/completed/) concluídos | [qa/android/](qa/android/) |
| [server](server/README.md) | contrato HTTP | `server/` | [v1-chat](server/specifications/v1-chat.md) + [api-contract.md](api-contract.md) | [ADR-013](server/adrs/ADR-013-gcp-host.md) | [S1, S4, S2, S3, S5](server/plans/completed/) concluídos | `server/tests/` |

`specifications/`, `adrs/`, `plans/` e `validation/` nascem no primeiro artefato. Não criar vazias. Pastas de estado do plano nascem no primeiro plano que as ocupar; vazias são removidas com `rmdir`.

## ADRs vigentes

Fonte histórica: [`decisions/`](decisions/). Novos: `docs/<contexto>/adrs/`.

| ADR | Contexto dono | Título |
|---|---|---|
| [001](decisions/001-monorepo.md) | produto | monorepo |
| [002](decisions/002-android-client.md) | android | client Android |
| [003](decisions/003-rn-client.md) | produto | client RN (morto, ADR 005) |
| [004](decisions/004-m3-expressive.md) | android | Material 3 Expressive |
| [005](decisions/005-android-only.md) | produto | Android only |
| [007](decisions/007-english-identifiers.md) | produto | identificadores EN |
| [008](decisions/008-visual-qa.md) | android | visual QA |
| [009](decisions/009-visual-match.md) | android | visual match |
| [010](decisions/010-room.md) | android | Room |
| [011](decisions/011-t2-t3-actions.md) | android | T2/T3 actions (vale até A5) |
| [012](produto/adrs/ADR-012-chat-home-perfil.md) | produto | Chat tela, Home painel, perfil nomeado |
| [013](server/adrs/ADR-013-gcp-host.md) | server | host GCP e2-micro |

## Planos aguardando aprovação

Ordem de `/goal` depois da frase de aprovação:

Nenhum no momento.

## Planos concluídos

1. [S1 timeout + cap 16 MB](server/plans/completed/s1-timeout-photo-cap.md)
2. [S4 Hardening de segurança da API](server/plans/completed/s4-security-hardening.md)
3. [S2 POST /v1/chat](server/plans/completed/s2-v1-chat.md)
4. [A0 Refatoração arquitetural feature-first & Kotlin puro](android/plans/completed/a0-arch-refactor.md)
5. [A0 Tokens semânticos P/C/G & Material 3 Expressive](android/plans/completed/a0-tokens-expressive.md)
6. [A0 Roborazzi setup (regressão visual)](android/plans/completed/a0-roborazzi-setup.md)
7. [A1 Room v2](android/plans/completed/a1-room-v2.md)
8. [A2 Onboarding perfil](android/plans/completed/a2-onboarding-perfil.md)
9. [A4 Home painel](android/plans/completed/a4-home-painel.md)
10. [A5 Chat](android/plans/completed/a5-chat.md)
11. [A3 Config + wipe + treino](android/plans/completed/a3-config-wipe-treino.md)
12. [S3 compact digest](server/plans/completed/s3-compact.md)
13. [A5b ligar compact](android/plans/completed/a5b-ligar-compact.md)
14. [A8 memória](android/plans/completed/a8-memoria.md)
15. [A8b memória com gravação atômica](android/plans/completed/a8b-memoria-gravacao-atomica.md)
16. [A6 foto](android/plans/completed/a6-foto.md)
17. [A7 push](android/plans/completed/a7-push.md)
18. [S5 deploy GCP e2-micro](server/plans/completed/s5-gcp-deploy.md)

## Outros docs

| Arquivo | Papel |
|---|---|
| [api-contract.md](api-contract.md) | contrato HTTP vigente |
| [tokens.md](tokens.md) | tokens visuais |
| [TEAM.md](TEAM.md) | time |
| [HERMES.md](HERMES.md) | troca de modelo no agente de código |
| [server/deploy-gcp.md](server/deploy-gcp.md) | runbook do host GCP (ADR-013) |
| [MIGRACAO-VPS.md](MIGRACAO-VPS.md) | substituído pelo ADR-013 |
| [qa/](qa/) | Stitch gold (`stitch/{dark,light}/`) + capturas (`android/current/`) |
| [`../.stitch/`](../.stitch/) | Design System oficial Google Stitch (`Nutri`) |
| [`../wires/`](../wires/) | wires preliminares (criação de layout apenas) |

## Como começar uma entrega

1. Discovery nesta matriz e no README do contexto dono.
2. Planning: spec + ADR se couber + plano em `docs/<contexto>/plans/`.
3. Approval explícita do arquivo do plano.
4. Implementation via `/goal` do plano aprovado.
5. Completion: evidência + ciclo de vida do plano + `rmdir` de pasta de estado vazia.
