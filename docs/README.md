# Documentação — matriz

Índice global do Dieta Bot. Política: [`sdd/README.md`](sdd/README.md).

Constituição: [`../AGENTS.md`](../AGENTS.md). Não duplicar regras aqui.

## Contextos

| Contexto | Tipo | Código | Spec viva | ADR local | Plano ativo | Validação |
|---|---|---|---|---|---|---|
| [produto](produto/README.md) | produto | — | [specifications/](produto/specifications/) | [ADR-012](produto/adrs/ADR-012-chat-home-perfil.md) | nenhum (Planning fechado) | [qa/](qa/) |
| [android](android/README.md) | client | `apps/android/` | [room-v2](android/specifications/room-v2.md) | [ADR-014](android/adrs/ADR-014-flavors-firebase-dev.md) | [A23 e A24](android/README.md#planos-e-validacao) aguardando aprovação; [A21](android/plans/pending_manual_validation/a21-seletor-horario.md) pendente de validação manual; [A11, A17, A18](android/plans/pending_manual_validation/) pendentes de aprovação manual; [A0 arch, tokens, Roborazzi, A1, A2, A3, A4, A5, A5b, A6, A7, A8, A8b, A10, A12, A26](android/plans/completed/) concluídos | [qa/android/](qa/android/) |
| [stitch](stitch/README.md) | gate de design | — (golds + `tools/export-stitch.mjs`) | — | — | [ST1–ST5](stitch/README.md#planos) concluídos; [SV1](stitch/plans/completed/sv1-verificacao-automatica.md) concluído | `tools/check-stitch.mjs`, `tools/verify-stitch.mjs st<n>` |
| [server](server/README.md) | contrato HTTP | `server/` | [v1-chat](server/specifications/v1-chat.md) + [api-contract.md](api-contract.md) | [ADR-013](server/adrs/ADR-013-gcp-host.md), [ADR-015](server/adrs/ADR-015-log-conversa-dev.md) | [S8](server/plans/pending_manual_validation/s8-chat-json-slot-consolidado.md) e [S6](server/plans/pending_manual_validation/s6-log-conversa-dev.md) pendentes aprovação manual; [S1, S4, S2, S3, S5, S7, S9](server/plans/completed/) concluídos | `server/tests/` |

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
| [011](decisions/011-t2-t3-actions.md) | android | T2/T3 actions (histórico: fluxo removido no A12) |
| [012](produto/adrs/ADR-012-chat-home-perfil.md) | produto | Chat tela, Home painel, perfil nomeado |
| [013](server/adrs/ADR-013-gcp-host.md) | server | host GCP e2-micro |
| [014](android/adrs/ADR-014-flavors-firebase-dev.md) | android | flavors dev/prod, Firebase só no dev |
| [015](server/adrs/ADR-015-log-conversa-dev.md) | server | log de conversa no server de dev |
| [016](produto/adrs/ADR-016-nome-dieta-bot.md) | produto | nome visível "Dieta Bot", IDs técnicos `nutri` |
| [017](produto/adrs/ADR-017-registro-consolidado.md) | produto | um registro por refeição, confirmação ao substituir (aceito) |
| [018](android/adrs/ADR-018-foto-2048.md) | android | foto reduzida a 2048 px no client (aceito) |
| [019](produto/adrs/ADR-019-ferramentas-dev.md) | produto | telas de ferramenta só no dev, sem gold (proposto) |
| [020](produto/adrs/ADR-020-estados-novos-chat-home-horario.md) | produto | golds novos `chatA`, `homeW`, `o3t` (proposto) |
| [021](produto/adrs/ADR-021-refeicoes-por-dia.md) | produto | refeições por dia da semana (aceito) |
| [022](produto/adrs/ADR-022-limite-texto-chat.md) | produto | mensagem do Chat até 2000 caracteres, estado de erro `chatX` (aceito) |

## Planos aguardando aprovação

Ordem de `/goal` depois da frase de aprovação:

Lote do feedback dos testers (29/09/2026). A ordem respeita as dependências; um gate Stitch (⛔) é passo do dono, não aprovação.

1. ✅ [A20 polimento: toque/vibração, botões dos sheets, respiro, Config](android/plans/pending_manual_validation/a20-polimento-geral.md) — implementado, vibração pendente de validação manual.
2. [A23 editor de memória e perfil (dev)](android/plans/a23-editor-memoria-dev.md).
3. ✅ [ST1](stitch/plans/completed/st1-chat.md) → ✅ [A19 chat visual](android/plans/pending_manual_validation/a19-chat-visual.md) — implementado, anexar e enviar no APK pendente de validação manual.
4. ✅ [ST2](stitch/plans/completed/st2-home-treino.md) → ✅ [A22 treino na Home](android/plans/pending_manual_validation/a22-treino-home.md) — implementado, informar o treino pela Home no APK pendente de validação manual.
5. ✅ [ST3](stitch/plans/completed/st3-seletor-horario.md) → [A21 seletor de horário](android/plans/pending_manual_validation/a21-seletor-horario.md) — implementado; validação manual de 07:30 e 21:45 pendente.
6. ✅ [ST4](stitch/plans/completed/st4-refeicoes-por-dia.md) → [A24 refeições por dia](android/plans/pending_manual_validation/a24-refeicoes-por-dia.md) — implementado, pendente aprovação manual.

Limite de texto do Chat (29/09/2026, [ADR-022](produto/adrs/ADR-022-limite-texto-chat.md)). Independe do lote acima:

1. ✅ [ST5](stitch/plans/completed/st5-chat-texto-longo.md) → ✅ [A25 composer: limite de 2000 com estado de erro](android/plans/pending_manual_validation/a25-limite-texto-composer.md) — implementado, colar texto longo no APK pendente de validação manual. O [S9](server/plans/completed/s9-limite-texto-2000.md) já está no ar.


## Planos pendentes de aprovação manual

1. [A11 Firebase Crashlytics + Analytics no dev](android/plans/pending_manual_validation/a11-firebase-dev.md) — Crashlytics e Analytics validados no emulador; Analytics ativado no console; falta o dono instalar o APK e usar por 1 dia.
2. [S6 log de conversa no server de dev](server/plans/pending_manual_validation/s6-log-conversa-dev.md) — no ar; o "nao deu para estimar" já foi explicado pelo log (29/09, virou o S8); falta o dono aprovar.
3. [S8 chat: JSON garantido, slot sugerido, refeição consolidada](server/plans/pending_manual_validation/s8-chat-json-slot-consolidado.md) — no ar no dev (29/09); 64 testes e 4 replays do log OK; falta o dono ver "Gravar café" no APK 0.0.2.
4. [A17 changelog humano no deploy](android/plans/pending_manual_validation/a17-changelog-deploy.md) — `-Notes` obrigatório e validado antes do build; falta o primeiro deploy real com notas no App Tester e seção no `CHANGELOG.md`.
5. [A18 chat: refeição consolidada, Enter, foto 2048 px](android/plans/pending_manual_validation/a18-chat-registro-foto.md) — substituir com confirmação, Enter pula linha, foto 2048 px q85 sem EXIF; unit, Roborazzi e emulador OK; falta o dono repetir esfihas + suco no APK distribuído.
6. [A25 composer: limite de 2000 com estado de erro](android/plans/pending_manual_validation/a25-limite-texto-composer.md) — nada é cortado; acima de 2000 borda vermelha e "Texto muito longo", enviar e câmera desligados; unit, gold e emulador OK; falta o dono colar um texto longo no APK distribuído.

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
