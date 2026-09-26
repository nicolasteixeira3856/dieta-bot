# Documentação — matriz

Índice global do Nutri. Política: [`sdd/README.md`](sdd/README.md).

Constituição: [`../AGENTS.md`](../AGENTS.md). Não duplicar regras aqui.

## Contextos

| Contexto | Tipo | Código | Spec viva | ADR local | Plano ativo | Validação |
|---|---|---|---|---|---|---|
| [produto](produto/README.md) | produto | — | [specifications/](produto/specifications/) | [ADR-012](produto/adrs/ADR-012-chat-home-perfil.md) | nenhum (Planning fechado) | [qa/](qa/) |
| [android](android/README.md) | client | `apps/android/` | [room-v2](android/specifications/room-v2.md) | histórico em [decisions/](decisions/) | [A1–A8](android/plans/) aguardando aprovação | [qa/android/](qa/android/) |
| [server](server/README.md) | contrato HTTP | `server/` | [v1-chat](server/specifications/v1-chat.md) + [api-contract.md](api-contract.md) | histórico em [decisions/](decisions/) | [S1–S3](server/plans/) aguardando aprovação | `server/tests/` |

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

## Planos aguardando aprovação

Ordem de `/goal` depois da frase de aprovação:

1. [S1 timeout + cap 16 MB](server/plans/s1-timeout-photo-cap.md)
2. [S2 POST /v1/chat](server/plans/s2-v1-chat.md)
3. [S3 compact digest](server/plans/s3-compact.md)
4. [A1 Room v2](android/plans/a1-room-v2.md)
5. [A2 Onboarding perfil](android/plans/a2-onboarding-perfil.md)
6. [A3 Config + wipe + treino](android/plans/a3-config-wipe-treino.md)
7. [A4 Home painel](android/plans/a4-home-painel.md)
8. [A5 Chat](android/plans/a5-chat.md) (pré-req S2 + A4)
9. [A6 foto](android/plans/a6-foto.md)
10. [A7 push](android/plans/a7-push.md)
11. [A8 memória](android/plans/a8-memoria.md)

Nenhum plano aprovado. Sem código desta refatoração.

## Outros docs

| Arquivo | Papel |
|---|---|
| [api-contract.md](api-contract.md) | contrato HTTP vigente até S2 atualizar |
| [tokens.md](tokens.md) | tokens visuais |
| [TEAM.md](TEAM.md) | time |
| [HERMES.md](HERMES.md) | troca de modelo no agente de código |
| [MIGRACAO-VPS.md](MIGRACAO-VPS.md) | migração futura; não executar |
| [SETUP-WINDOWS.md](SETUP-WINDOWS.md) | toolchain Windows |
| [qa/](qa/) | gold + capturas |
| [`../GOALS.md`](../GOALS.md) | G1–G8 histórico. Trabalho novo = plano SDD, depois `/goal` |
| [`../wires/`](../wires/) | wires |

## Como começar uma entrega

1. Discovery nesta matriz e no README do contexto dono.
2. Planning: spec + ADR se couber + plano em `docs/<contexto>/plans/`.
3. Approval explícita do arquivo do plano.
4. Implementation via `/goal` do plano aprovado.
5. Completion: evidência + ciclo de vida do plano + `rmdir` de pasta de estado vazia.
