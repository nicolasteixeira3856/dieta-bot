# Documentação — matriz

Índice global do Dieta Bot. Política: [`sdd/README.md`](sdd/README.md).

Constituição: [`../AGENTS.md`](../AGENTS.md). Não duplicar regras aqui.

## Contextos

| Contexto | Tipo | Código | Spec viva | ADR local | Plano ativo | Validação |
|---|---|---|---|---|---|---|
| [produto](produto/README.md) | produto | — | [specifications/](produto/specifications/) | [ADR-012](produto/adrs/ADR-012-chat-home-perfil.md); [ADR-026](produto/adrs/ADR-026-perguntas-antes-da-estimativa.md); [ADR-028](produto/adrs/ADR-028-registro-autonomo.md) (proposto) | nenhum | [qa/](qa/) |
| [android](android/README.md) | client | `apps/android/` | [room-v2](android/specifications/room-v2.md) | [ADR-014](android/adrs/ADR-014-flavors-firebase-dev.md), [ADR-027](android/adrs/ADR-027-golds-divergentes.md) | [A0 arch, tokens, Roborazzi, A1–A33](android/plans/completed/) concluídos (A11, A17–A22, A24, A25, A27–A30 aprovado pelo dono em 30/09/2026; A31–A33 em 01/10/2026); [A34](android/plans/a34-registro-autonomo.md) aguardando aprovação; [A35](android/plans/out_of_scope/a35-registro-retroativo.md) fora de escopo | [qa/android/](qa/android/) |
| [stitch](stitch/README.md) | gate de design | — (golds + `tools/export-stitch.mjs`) | — | — | [ST1–ST9](stitch/README.md#planos) concluídos (ST9 em 02/10/2026, golds `chatU` e `chatD`); [SV1](stitch/plans/completed/sv1-verificacao-automatica.md) concluído | `tools/check-stitch.mjs`, `tools/verify-stitch.mjs st<n>` |
| [server](server/README.md) | contrato HTTP | `server/` | [v1-chat](server/specifications/v1-chat.md) + [api-contract.md](api-contract.md) | [ADR-013](server/adrs/ADR-013-gcp-host.md), [ADR-015](server/adrs/ADR-015-log-conversa-dev.md) | [S1–S13](server/plans/completed/) concluídos (S6, S8, S11, S12 aprovado pelo dono em 30/09/2026; [S13](server/plans/completed/s13-perguntas-antes-da-estimativa.md) no ar no dev em 30/09/2026); [S14](server/plans/s14-registro-autonomo.md) aguardando aprovação | `server/tests/` |

`specifications/`, `adrs/`, `plans/` e `validation/` nascem no primeiro artefato. Não criar vazias. Pastas de estado do plano nascem no primeiro plano que as ocupar; vazias são removidas com `rmdir`.

## Content policy — planning, 2026-09-30

| Context | Type | Code ownership | Specifications | Proposed ADRs | Plans | Validation |
| --- | --- | --- | --- | --- | --- | --- |
| [content-policy](content-policy/README.md) | Cross-cutting policy | One folder per plan: server, Android or GCP infra | [Content handling](content-policy/specifications/content-policy.md), [identity/audit](content-policy/specifications/identity-and-audit.md) | [024](content-policy/adrs/ADR-024-content-safety-boundaries.md) accepted; [025](content-policy/adrs/ADR-025-safety-correlation-audit.md) accepted | [CP2 → CP1 → CP3 → CP4 → CP5](content-policy/plans/README.md) (closed test): CP1–CP5 `Concluído` (2026-10-01), PG1 `Sanado`; [production gate](content-policy/production-gate.md) | [Matrix](content-policy/validation/README.md) |

Out of scope: [CP6](content-policy/plans/out_of_scope/cp6-production-readiness.md), [CP8](content-policy/plans/out_of_scope/cp8-public-legal-pack.md) and [CP9](content-policy/plans/out_of_scope/cp9-production-audit-and-containment.md) because the app is in a closed test; [CP7](content-policy/plans/out_of_scope/cp7-specialist-detection.md) for budget. All four block production through the [production gate](content-policy/production-gate.md). The owner-authorized [SDD state](sdd/README.md#fora-de-escopo) preserves the reasons and requires explicit reactivation. CP2–CP5 are deployed to dev; CP1 is a documentation delivery.

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
| [011](decisions/011-t2-t3-actions.md) | android | T2/T3 actions (histórico: fluxo removido no A12) |
| [012](produto/adrs/ADR-012-chat-home-perfil.md) | produto | Chat tela, Home painel, perfil nomeado |
| [013](server/adrs/ADR-013-gcp-host.md) | server | host GCP e2-micro |
| [014](android/adrs/ADR-014-flavors-firebase-dev.md) | android | flavors dev/prod, Firebase só no dev |
| [015](server/adrs/ADR-015-log-conversa-dev.md) | server | log de conversa no server de dev |
| [016](produto/adrs/ADR-016-nome-dieta-bot.md) | produto | nome visível "Dieta Bot", IDs técnicos `nutri` |
| [017](produto/adrs/ADR-017-registro-consolidado.md) | produto | um registro por refeição, confirmação ao substituir (aceito) |
| [018](android/adrs/ADR-018-foto-2048.md) | android | foto reduzida a 2048 px no client (aceito) |
| [019](produto/adrs/ADR-019-ferramentas-dev.md) | produto | telas de ferramenta só no dev, sem gold (aceito) |
| [020](produto/adrs/ADR-020-estados-novos-chat-home-horario.md) | produto | golds novos `chatA`, `homeW`, `o3t` (aceito) |
| [021](produto/adrs/ADR-021-refeicoes-por-dia.md) | produto | refeições por dia da semana (aceito) |
| [022](produto/adrs/ADR-022-limite-texto-chat.md) | produto | mensagem do Chat até 2000 caracteres, estado de erro `chatX` (aceito) |
| [023](produto/adrs/ADR-023-chat-v2-memoria-v2.md) | produto | Chat v2 (intenção, texto da refeição, plano) e Memória v2 (permanente + dinâmica), golds `chatR`, `chatM`, `chatS` (aceito) |
| [026](produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) | produto | perguntas antes da estimativa, até 3 rodadas, Forçar estimativa; `chatE` alterado, gold `chatQ` (aceito com o A30) |
| [027](android/adrs/ADR-027-golds-divergentes.md) | android | QA visual com golds divergentes: estado segue o próprio gold, média dark/light, spec vence o gold, esmaecimento por save layer (aceito) |
| [028](produto/adrs/ADR-028-registro-autonomo.md) | produto | registro autônomo no Chat, recibo com Desfazer/Excluir/Trocar refeição/Editar, Registrar na dúvida, substituição confirmada no Chat; golds `chatE`, `chatF`, `chatG` alterados, `chatU`, `chatD` novos (proposto) |

## Planos aguardando aprovação

Registro autônomo ([ADR-028](produto/adrs/ADR-028-registro-autonomo.md), proposto em 01/10/2026), nesta ordem: [S14](server/plans/s14-registro-autonomo.md) (server, opt-in, aguardando aprovação) ∥ [ST9](stitch/plans/completed/st9-registro-autonomo.md) (gate, concluído em 02/10/2026) → [A34](android/plans/a34-registro-autonomo.md) (client, aguardando aprovação). Fora de escopo: [A35 registro retroativo](android/plans/out_of_scope/a35-registro-retroativo.md).

Perguntas antes da estimativa ([ADR-026](produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), 30/09/2026), nesta ordem: [S13](server/plans/completed/s13-perguntas-antes-da-estimativa.md) (server, opt-in, concluído e no ar no dev) ∥ [ST7](stitch/plans/completed/st7-pergunta-antes-da-estimativa.md) (gate concluído: `chatE`, `chatQ`) → [A30](android/plans/completed/a30-perguntas-antes-da-estimativa.md) (client, concluído).

Concluídos: Chat v2 + Memória v2 ([ADR-023](produto/adrs/ADR-023-chat-v2-memoria-v2.md): S10, S11, S12, A27, A28, ST6, A29), o lote do feedback dos testers (A19–A24, ST1–ST4) e o limite de texto ([ADR-022](produto/adrs/ADR-022-limite-texto-chat.md): ST5, A25, S9) estão concluídos.

## Planos pendentes de aprovação manual

Nenhum. Em 30/09/2026 o dono aprovou todos os que estavam pendentes: [A11](android/plans/completed/a11-firebase-dev.md), [A17](android/plans/completed/a17-changelog-deploy.md), [A18](android/plans/completed/a18-chat-registro-foto.md), [A19](android/plans/completed/a19-chat-visual.md), [A20](android/plans/completed/a20-polimento-geral.md), [A21](android/plans/completed/a21-seletor-horario.md), [A22](android/plans/completed/a22-treino-home.md), [A24](android/plans/completed/a24-refeicoes-por-dia.md), [A25](android/plans/completed/a25-limite-texto-composer.md), [A27](android/plans/completed/a27-chat-v2-texto-intencao.md), [A28](android/plans/completed/a28-memoria-v2.md), [A29](android/plans/completed/a29-chat-v2-interface.md), [S6](server/plans/completed/s6-log-conversa-dev.md), [S8](server/plans/completed/s8-chat-json-slot-consolidado.md), [S11](server/plans/completed/s11-chat-v2.md) e [S12](server/plans/completed/s12-slot-nomeado.md).

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
19. [A10 flavors dev/prod](android/plans/completed/a10-flavors-dev-prod.md)
20. [A12 remover legado T1/T2/T3](android/plans/completed/a12-remover-legado-t123.md)
21. [S7 "Dieta Bot" no prompt do server](server/plans/completed/s7-rename-prompt.md)
22. [A13 rename visível "Dieta Bot"](android/plans/completed/a13-rename-dieta-bot.md)
23. [A14 marca: ícone e splash com o logo](android/plans/completed/a14-marca-icone-splash.md)
24. [A15 ajuste aos golds novos](android/plans/completed/a15-ajuste-visual-golds-novos.md) — tinta do dark/o4: mantido o token do `AGENTS.md` (decisão do dono).
25. [A16 Firebase App Distribution, versão 0.0.N](android/plans/completed/a16-app-distribution.md)
26. [A9 chave de assinatura do release](android/plans/completed/a9-assinatura-release.md)
27. [S9 `/v1/chat` aceita 2000 caracteres](server/plans/completed/s9-limite-texto-2000.md)
28. [A26 Android CLI no loop de QA visual](android/plans/completed/a26-android-cli-qa.md) — `android screen capture` e `android layout` nas skills e no QA; gate `diff-gold.mjs` igual.

## Outros docs

Shared workflow maintenance: [SD1 — Repository skill alignment](sdd/plans/completed/sd1-skills-alignment.md), `Concluído`; [SDD maintenance index](sdd/plans/README.md).

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
