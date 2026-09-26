# Documentação — matriz

Índice global do Nutri. Política: [`sdd/README.md`](sdd/README.md).

Constituição: [`../AGENTS.md`](../AGENTS.md). Não duplicar regras aqui.

## Contextos

| Contexto | Tipo | Código | Spec viva | ADR local | Plano ativo | Validação |
|---|---|---|---|---|---|---|
| [produto](produto/README.md) | produto | — | não | histórico em [decisions/](decisions/) | nenhum | [qa/](qa/) |
| [android](android/README.md) | client | `apps/android/` | não | histórico em [decisions/](decisions/) | nenhum | [qa/android/](qa/android/) |
| [server](server/README.md) | contrato HTTP | `server/` | [api-contract.md](api-contract.md) | histórico em [decisions/](decisions/) | nenhum | `server/tests/` |

`specifications/`, `adrs/`, `plans/` e `validation/` nascem no primeiro artefato. Não criar vazias. Pastas de estado do plano nascem no primeiro plano que as ocupar; vazias são removidas com `rmdir`.

## ADRs vigentes (histórico)

Fonte atual: [`decisions/`](decisions/). Novos ADRs: `docs/<contexto>/adrs/`.

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
| [011](decisions/011-t2-t3-actions.md) | android | T2/T3 actions |

## Outros docs

| Arquivo | Papel |
|---|---|
| [api-contract.md](api-contract.md) | contrato HTTP vigente até existir spec em `server/specifications/` |
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
