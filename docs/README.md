# Documentação — matriz

Índice global do Dieta Bot. Política: [`sdd/README.md`](sdd/README.md).

Constituição: [`../AGENTS.md`](../AGENTS.md). Não duplicar regras aqui. Este arquivo roteia: estado, versão e data ficam no arquivo dono, não aqui.

## Contextos

| Contexto | Tipo | Código | Spec viva | ADR local | Planos | Validação |
|---|---|---|---|---|---|---|
| [produto](produto/README.md) | produto | — | [specifications/](produto/specifications/) | [adrs/](produto/adrs/) | — | [qa/](qa/) |
| [android](android/README.md) | client | `apps/android/` | [Room](android/specifications/room-v2.md) | [adrs/](android/adrs/) | [plans/](android/plans/) | [qa/android/](qa/android/), [validation/](android/validation/) |
| [server](server/README.md) | contrato HTTP | `server/` | [v1-chat](server/specifications/v1-chat.md) + [api-contract.md](api-contract.md) | [adrs/](server/adrs/) | [plans/](server/plans/) | `server/tests/` |
| [stitch](stitch/README.md) | gate de design | — (golds + `tools/export-stitch.mjs`) | — | — | [plans/](stitch/plans/) | `tools/check-stitch.mjs`, `tools/verify-stitch.mjs st<n>` |
| [content-policy](content-policy/README.md) | cross-cutting policy | um diretório por plano: server, Android ou infra GCP | [content handling](content-policy/specifications/content-policy.md), [identity/audit](content-policy/specifications/identity-and-audit.md) | [adrs/](content-policy/adrs/) | [índice](content-policy/plans/README.md) | [matriz](content-policy/validation/README.md) |
| [sdd](sdd/README.md) | manutenção do fluxo | `tools/check-skills.mjs`, skills | — | — | [índice](sdd/plans/README.md) | — |

`specifications/`, `adrs/`, `plans/` e `validation/` nascem no primeiro artefato. Não criar vazias. Pastas de estado do plano nascem no primeiro plano que as ocupar; vazias são removidas com `rmdir`.

## Planos

- Ativos: arquivos na raiz de cada `plans/` da matriz (e o índice de `plans/`, quando existe).
- Pendentes de aprovação manual: `plans/pending_manual_validation/`.
- Fora de escopo: `plans/out_of_scope/`. Os que bloqueiam produção estão no [production gate](content-policy/production-gate.md), dono da lista de bloqueios.
- Histórico: `plans/completed/` e `plans/cancelled/`. Só para proveniência, conferência de gate Stitch ou pergunta explícita sobre histórico.

## ADRs

Status: a linha `Status`/`Estado` de cada ADR. Fonte histórica: [`decisions/`](decisions/). Novos: `docs/<contexto>/adrs/`.

| ADR | Contexto dono | Título |
|---|---|---|
| [001](decisions/001-monorepo.md) | produto | monorepo |
| [002](decisions/002-android-client.md) | android | client Android |
| [003](decisions/003-rn-client.md) | produto | client RN |
| [004](decisions/004-m3-expressive.md) | android | Material 3 Expressive |
| [005](decisions/005-android-only.md) | produto | Android only |
| [007](decisions/007-english-identifiers.md) | produto | identificadores EN |
| [008](decisions/008-visual-qa.md) | android | visual QA |
| [009](decisions/009-visual-match.md) | android | visual match |
| [010](decisions/010-room.md) | android | Room |
| [011](decisions/011-t2-t3-actions.md) | android | T2/T3 actions |
| [012](produto/adrs/ADR-012-chat-home-perfil.md) | produto | Chat tela, Home painel, perfil nomeado |
| [013](server/adrs/ADR-013-gcp-host.md) | server | host GCP e2-micro |
| [014](android/adrs/ADR-014-flavors-firebase-dev.md) | android | flavors dev/prod, Firebase só no dev |
| [015](server/adrs/ADR-015-log-conversa-dev.md) | server | log de conversa no server de dev |
| [016](produto/adrs/ADR-016-nome-dieta-bot.md) | produto | nome visível "Dieta Bot", IDs técnicos `nutri` |
| [017](produto/adrs/ADR-017-registro-consolidado.md) | produto | um registro por refeição, confirmação ao substituir |
| [018](android/adrs/ADR-018-foto-2048.md) | android | foto reduzida a 2048 px no client |
| [019](produto/adrs/ADR-019-ferramentas-dev.md) | produto | telas de ferramenta só no dev, sem gold |
| [020](produto/adrs/ADR-020-estados-novos-chat-home-horario.md) | produto | golds novos `chatA`, `homeW`, `o3t` |
| [021](produto/adrs/ADR-021-refeicoes-por-dia.md) | produto | refeições por dia da semana |
| [022](produto/adrs/ADR-022-limite-texto-chat.md) | produto | mensagem do Chat até 2000 caracteres, estado de erro `chatX` |
| [023](produto/adrs/ADR-023-chat-v2-memoria-v2.md) | produto | Chat v2 (intenção, texto da refeição, plano) e Memória v2 (permanente + dinâmica), golds `chatR`, `chatM`, `chatS` |
| [024](content-policy/adrs/ADR-024-content-safety-boundaries.md) | content-policy | limites de conteúdo e segurança do Chat |
| [025](content-policy/adrs/ADR-025-safety-correlation-audit.md) | content-policy | correlação de segurança pseudônima |
| [026](produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) | produto | perguntas antes da estimativa, até 3 rodadas, Forçar estimativa; `chatE` alterado, gold `chatQ` |
| [027](android/adrs/ADR-027-golds-divergentes.md) | android | QA visual com golds divergentes: estado segue o próprio gold, média dark/light, spec vence o gold, esmaecimento por save layer |
| [028](produto/adrs/ADR-028-registro-autonomo.md) | produto | registro autônomo no Chat, recibo com Desfazer/Excluir/Trocar refeição/Editar, Registrar na dúvida, substituição confirmada no Chat; golds `chatE`, `chatF`, `chatG` alterados, `chatU`, `chatD` novos |

## Outros docs

| Arquivo | Papel |
|---|---|
| [api-contract.md](api-contract.md) | contrato HTTP vigente |
| [tokens.md](tokens.md) | valores dos tokens visuais (dono único) |
| [server/deploy-gcp.md](server/deploy-gcp.md) | runbook do host GCP (ADR-013) |
| [qa/](qa/README.md) | inventário de golds (dono único), capturas e fluxo de QA visual |
| [`../.stitch/`](../.stitch/) | Design System oficial Google Stitch (`Nutri`) |

## Como começar uma entrega

1. Discovery nesta matriz e no README do contexto dono.
2. Planning: plano em `docs/<contexto>/plans/`, com a mudança pretendida de spec descrita nele, + ADR se couber.
3. Approval explícita do arquivo do plano.
4. Implementation via `/goal` do plano aprovado.
5. Completion: specs reescritas no lugar + Results do plano + ciclo de vida do plano + `rmdir` de pasta de estado vazia.
