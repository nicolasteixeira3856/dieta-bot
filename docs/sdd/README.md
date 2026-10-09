# Spec-Driven Development

Política global de SDD do Fibrai. Governa todos os contextos da [matriz](../README.md).

READMEs de contexto complementam ownership, estado e ordem de leitura. Não redefinem gates, estados ou regras globais.

Esta política não substitui `AGENTS.md`. Constituição e SDD convivem. Conflito de produto aberto pelo dono vira ADR sucessor + spec viva, não um commit solto.

## Princípios

- Documentação nasce sob demanda. Sem pasta vazia para antecipar trabalho.
- Um contexto é um domínio de produto, o client, ou o contrato HTTP. Não precisa ser um package.
- Cada decisão tem um contexto dono e uma fonte de verdade.
- Especificações descrevem comportamento vigente. ADRs preservam decisão arquitetural. Planos descrevem uma entrega. Validações registram evidência.
- Planejamento não autoriza alterar código de produção.
- Implementação fica dentro do plano aprovado explicitamente.
- grok-cli: 1 agente. `/goal` é a fase Implementation, não Planning.

## Precedência

1. Decisão explícita mais recente do dono.
2. `AGENTS.md` no que ainda estiver vigente.
3. Especificação viva do contexto.
4. ADR vigente aplicável.
5. Plano aprovado.
6. Implementação atual.

Decisão explícita que muda comportamento documentado fica registrada no plano (e num ADR, se for arquitetural) na etapa de Planning. A spec viva só muda no Completion, quando o comportamento está entregue. Decisão arquitetural nova não reescreve o corpo de ADR aceito: cria ADR sucessor e declara substituição total ou parcial.

ADRs aceitos em `docs/decisions/` (001–011) continuam lá. ADRs novos nascem em `docs/<contexto>/adrs/`.

## Tipos de contexto

### Produto

Comportamento visível: job, telas, copy, onboarding, slots, o que entra no prompt. Código não mora aqui.

### Client

`apps/android/`. Compose, Room, push, foto, navegação.

### Contrato HTTP

`server/`. Rotas, auth, LLM, shaping, timeout. Infra S0 (compose, tunnel, migração) indexada neste contexto. Sem contexto `infra` separado.

### Fonte de design

`design`. O arquivo Figma `Design`: design system Aero e todas as telas e estados, que o agente desenha pelo MCP do Figma. Um plano de design cobre um fluxo e termina pelo gate de revisão no Figma. Não tem código de app; toca só `docs/qa/figma/`, o tooling de design em `tools/` e o espelho gerado de tokens. Ver [Gate Figma](#gate-figma).

### Cross-cutting policy

Owner-authorized on 2026-09-30: [content-policy](../content-policy/README.md) owns content policy, correlation/audit requirements, incident procedures and their delivery plans. It does not replace the code ownership of server, Android or infrastructure.

Each plan declares one executable folder boundary. A client goal never edits server code; a server goal never edits client/infra code. Related specifications, indexes and validation evidence may be updated with their owning delivery. A documentation-only `/goal` is permitted for an explicitly approved documentation plan and does not authorize production code. Existing code plans are not duplicated in multiple contexts.

## Estrutura de um contexto

Todo contexto começa por um `README.md`. O resto é opcional e só existe com conteúdo:

```text
docs/<contexto>/
├── README.md
├── specifications/
├── adrs/
├── plans/
│   ├── README.md
│   ├── completed/
│   ├── pending_manual_validation/
│   ├── cancelled/
│   └── out_of_scope/
└── validation/
```

O README declara: propósito, tipo, ownership de código, escopo, fora de escopo, fronteiras, cobertura, estado, ordem de leitura, índice.

Não crie diretórios vazios. `completed/`, `pending_manual_validation/`, `cancelled/` e `out_of_scope/` nascem no primeiro plano que precisar delas.

## Responsabilidade dos artefatos

| Artefato | Responsabilidade | Natureza | Autoriza código? |
| --- | --- | --- | --- |
| README do contexto | Escopo, ownership, navegação | Vivo | Não |
| Especificação | Comportamento vigente | Viva | Não |
| ADR aceito | Histórico de uma decisão | Imutável | Não |
| Plano ativo | Escopo executável de uma entrega | Mutável até aprovação | Só após aprovação explícita |
| Validação | Evidência que cobre mais de um plano, matriz viva ou pendência recorrente | Viva | Não |

Evidência de um único plano (testes, evals, confirmação do dono) fica na seção Results do próprio plano.

### Estado atual × histórico (decisão do dono, 02/10/2026)

- **Arquivos de estado atual**: `AGENTS.md`, `README.md`, READMEs de contexto e de `plans/`, especificações, `docs/api-contract.md`, `docs/tokens.md`, `docs/qa/README.md`. São reescritos no lugar. Não carregam histórico ("desde o plano X"), propostas, avisos de pendência nem status de outro artefato.
- **Arquivos de histórico**: planos, corpo de ADR, evidência de validação. Imutáveis depois do encerramento. A pasta (`plans/completed/`, `plans/cancelled/`) ou a linha de status é o rótulo.
- Uma especificação cita os planos que a formaram só numa seção final `## Provenance` (ou `## Proveniência`), uma linha por plano: `- [ID](caminho) — título`, sem palavra de status.

### Dono único (decisão do dono, 02/10/2026)

Cada fato (status, versão, data, lista, valor) tem um arquivo dono. Os outros arquivos linkam para ele em vez de repeti-lo. Exemplos: rotas em `docs/api-contract.md`; schema do Room na spec do Room; valores de token em `docs/tokens.md`; inventário de golds em `docs/qa/README.md`; status de um ADR na linha de status do próprio ADR; estado de um plano no próprio plano e na pasta onde ele está.

Planos não são fonte de verdade do produto. Se a implementação descobrir comportamento novo: para, atualiza spec e artefatos, pede nova aprovação.

## Fluxo obrigatório

### 1. Discovery

1. Comece em `docs/README.md`.
2. Resolva o contexto dono.
3. Leia o README do contexto e o que ele indicar.
4. Consulte planos ativos (raiz de `plans/`), `pending_manual_validation/`, `out_of_scope/`, ADRs vigentes e validações pendentes. `completed/` e `cancelled/` são histórico: abra só para proveniência ou pergunta explícita sobre histórico.
5. Confirme no código atual.
6. Feche decisões de produto ou arquitetura ainda abertas (A/B/C se precisar).

### 2. Planning

1. Crie a fundação do contexto se não existir.
2. Descreva no plano a mudança pretendida em cada spec viva afetada. A spec não muda nesta etapa.
3. Crie um ADR quando a decisão for duradoura e ainda não coberta.
4. Crie ou atualize um plano ativo em `docs/<contexto>/plans/<plano>.md`.
5. Registre validações automatizadas e manuais previstas.
6. Atualize índices, matriz e links, só com o estado ativo.

Etapa só documental. Exceção: o dono autorizou outra alteração não produtiva.

### 3. Approval

O plano ativo precisa de aprovação explícita que identifique o arquivo:

> Aprovo o plano `docs/<contexto>/plans/<plano>.md`. Implemente o plano aprovado.

Pedido para analisar, documentar, criar spec, criar ADR ou criar plano **não** autoriza implementação.

### 4. Implementation

1. Implemente somente o plano aprovado.
2. 1 `/goal` = 1 pasta. Client e server não no mesmo `/goal`.
3. Preserve fora de escopo.
4. Rode a validação prevista.
5. Decisão não coberta: interrompa e volte ao Planning.

### 5. Completion

1. Registre resultados reais e pendências na seção Results do plano (ou em `validation/`, se a evidência cobre mais de um plano).
2. Reescreva no lugar as regras das specs afetadas, no presente. Entrega parcial (ex.: server no ar antes do client) entra como ressalva de rollout na própria regra ("Sem cliente v4, …"). Acrescente o plano à `## Provenance` da spec. Tire qualquer texto que a entrega deixou velho. READMEs listam só planos ativos, pendentes e fora de escopo.
3. Aplique o ciclo de vida do plano na mesma entrega.
4. Atualize links que apontavam para o caminho anterior.
5. Remova pastas de estado que ficaram vazias, pela regra de limpeza abaixo.
6. Rode `node tools/check-docs.mjs` (e `node tools/check-skills.mjs` se mexeu em skills). Falha bloqueia a entrega.
7. Entregue pelo fluxo git abaixo.

### 6. Entrega git (decisão do dono, 29/09/2026; branches em 08/10/2026)

Branches: `develop` é a branch de desenvolvimento (todo agente começa nela, atualizada; todo PR aponta para ela; deploy de dev e build de teste saem dela). `master` é produção e fica travada: sem push direto, sem merge e sem deploy salvo pedido explícito do dono naquela mensagem, e só depois da validação completa ponta a ponta (§ Ritmo de entrega). Ao fim da implementação de qualquer plano, o agente faz sozinho, sem pedir:

1. `git switch -c <tipo>/<id-do-plano>` a partir da `develop` atualizada (ex.: `fix/s8-chat-json-slot`, `feat/a22-treino-home`, `docs/d8-archive-stitch`).
2. Commit(s) só com os arquivos do plano. Nada de `git add -A` com lixo de fora do escopo.
3. `git push -u origin <branch>` e `gh pr create --base develop` com resumo, validação executada e pendências.
4. `gh pr merge --merge --delete-branch` (merge commit, sem squash).
5. `git switch develop` e `git pull --ff-only`. O repositório local termina igual ao remote.

Exceções: o commit `chore(release)` + tag do `tools/distribute-dev.ps1` (A16) continua indo direto na `develop`, porque faz parte do deploy e não de um plano. PR com CI vermelho ou conflito não é mergeado: o agente para e reporta. Promoção para `master` (produção): só por pedido explícito do dono, por PR `develop` → `master`, depois da validação completa.

## Execução autônoma

Vários planos aprovados numa única mensagem e entregues em sequência sem o dono: regras, ordem, teto de custo de avaliação (US$ 0,50 por sessão) e relatório em [autonomous-run.md](autonomous-run.md) (decisão do dono, 07/10/2026).

## Gate Figma

Mudança de layout que precisa de gold novo ou alterado é desenhada pelo agente no arquivo Figma `Design`, num plano de design (`docs/design/plans/D<n>`, um fluxo por plano, dentro de um dia de orçamento do MCP do Figma). Regra: [ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md); passos: [design/plans/README.md § Figma review gate](../design/plans/README.md#figma-review-gate).

Um plano de design:

- Começa por um discovery só de leitura: cada elemento visível do gold de referência é classificado contra o código e as specs vivas (paridade de funcionalidade). Elemento que só existe no gold sai; elemento da spec que falta no gold entra.
- Lê o arquivo antes de escrever, constrói o Light e clona o Dark pelo modo de variável, segue a regra de legibilidade do ADR-031 § 3 e conta as chamadas do MCP nos Results.
- Tira um screenshot por frame composto e vai para `Pendente aprovação manual`. A revisão visual do dono no Figma é o único passo manual: não há prompt para colar.
- Depois do OK do dono, mapeia os frames em `tools/export-figma.mjs`, exporta os golds para `docs/qa/figma/{dark,light}/` e roda `node tools/check-figma.mjs`.
- Não muda layout nem funcionalidade do app.

Planos de client que dependem de um plano de design o declaram em Pré-requisitos e só começam com ele em `design/plans/completed/`.

Skill: `dieta-bot-figma`. Template: [plano de fluxo Figma](templates/figma-flow.md).

## Criação de um novo contexto

1. Nome em `snake_case`. Owner-requested exception: `content-policy` (2026-09-30).
2. `docs/<contexto>/README.md` a partir do template.
3. Ownership, fronteiras, cobertura inicial.
4. Só os diretórios dos artefatos atuais.
5. Inclua o contexto na [matriz](../README.md).
6. Crie spec, ADR ou plano necessário.
7. Espere a aprovação do plano antes de código.

## Trabalho entre contextos

- Um contexto dono por decisão e por plano.
- Referencie o outro contexto por link relativo. Não duplique decisão.
- Separe planos quando contrato, risco, validação ou aprovação puderem andar sozinhos.
- Declare pré-requisito entre planos.
- Atualize o README de todo contexto cujo estado documental mudou.

## Ciclo de vida dos planos

| Situação | Estado | Local |
| --- | --- | --- |
| Planejado, sem autorização | `Aguardando aprovação` | `plans/<plano>.md` |
| Aprovado e em execução | `Em implementação` | `plans/<plano>.md` |
| Código feito, validação manual pendente | `Pendente aprovação manual` | `plans/pending_manual_validation/<plano>.md` |
| Implementação e validações concluídas | `Concluído` | `plans/completed/<plano>.md` |
| Cancelado pelo dono | `Cancelado` | `plans/cancelled/<plano>.md` |
| Deferred from the current cut by owner decision, with an explicit reason | `Fora de escopo` | `plans/out_of_scope/<plano>.md` |

Regras:

- Plano ativo mora direto em `plans/`.
- Depois da implementação e das validações automatizadas, o plano sai da raiz de `plans/` na mesma entrega.
- Sucesso automatizado não conclui plano com validação manual pendente.
- Aprovação manual: estado `Concluído` e move para `completed/`.
- Plano implementado não permanece em `Aguardando aprovação`.
- Toda movimentação atualiza índices, pré-requisitos, specs, validações e links.

### Limpeza segura das pastas de estado

Após mover um plano, confira `plans/completed/`, `plans/pending_manual_validation/`, `plans/cancelled/` e `plans/out_of_scope/` no contexto afetado. Remova somente diretórios desse conjunto que estejam vazios, inclusive de ocultos.

- Confira o caminho. Tem que estar no escopo autorizado.
- Use remoção de diretório vazio (`rmdir` / `Path.rmdir()`), sem recursão e sem apagar os pais. Se deixar de estar vazio, pare.
- Não siga symlink. Não apague arquivo, `.gitkeep`, evidência, `plans/`, a raiz do contexto ou outro diretório por esta regra.
- Não crie marcador só para manter pasta de estado vazia. Recrie a pasta só quando outro plano for ocupá-la.
- Registre o que removeu e confira links. Pasta vazia não é versionada; ausência de diff não prova a limpeza.

Essa higiene não altera estado, não cancela plano e não autoriza limpeza global.

### Cancelamento

Só declaração explícita do dono, identificando o plano. Ausência de aprovação, demora ou avaliação do agente não cancelam.

1. Estado `Cancelado`, data e a frase do dono. Motivo só se ele informar.
2. Mova para `plans/cancelled/` do contexto dono.
3. Preserve ID, histórico, evidência e pendência. Cancelado não é concluído.
4. Atualize índices e links.

Crie `cancelled/` só quando houver plano cancelado para guardar. Cancelar não faz rollback de código.

### Fora de escopo

Owner-authorized lifecycle state added on 2026-09-30. Use it for a future delivery explicitly excluded from the current cut for a recorded financial, business, technical or legal reason. It is not `Cancelado`, `Concluído`, a missing approval, or an excuse to hide unfinished required work.

1. Record `Fora de escopo`, date, owning context, the owner's decision/constraint, specific reason, residual risk and conditions for reconsideration. The owner may authorize a defined class of deferrals; record that authority without requiring a duplicate confirmation for every plan in that class.
2. Place the plan in `plans/out_of_scope/`, retaining its ID, scope and history. Create the directory only when it contains a plan. Update context/global indexes, dependencies and relative links.
3. Exclude it from active execution order. Do not implement it or start `/goal` from that state. Deferral does not waive a legal duty or make a dependent delivery complete; any required deferred capability remains an explicit rollout blocker.
4. Reactivate only on an explicit owner decision: refresh scope, assumptions, costs, dependencies and validation; preserve the dated deferral history; move the same ID to `plans/` as `Aguardando aprovação`; update all links and obtain explicit approval of that active plan before code. One owner message can reactivate and approve a fully reviewed, named plan when its scope is already concrete.
5. If implementation has already started, do not relabel the whole plan to hide incomplete work. Record delivered/remaining work and obtain an explicit owner scope decision; split a separately identified future plan when necessary. Moving a document does not roll back code.

Template: [deferred plan](templates/out-of-scope-plan.md). Safe empty-directory cleanup applies to `out_of_scope/` under the same rules as the other state directories.

## ADRs

Nome: `ADR-NNNN-<decisao>.md` em `docs/<contexto>/adrs/`.
Mínimo: estado, data, contexto, decisão, motivação, consequências, alternativas.

O corpo de ADR aceito é imutável. A linha `- Status:` (ou `- Estado:`) é a única parte que muda: ela registra a substituição total ou parcial quando um sucessor é aceito. Para mudar uma decisão:

1. Crie outro ADR.
2. Declare a substituição total ou parcial e o escopo que sobrevive do anterior.
3. Atualize a linha de status do ADR anterior (ex.: `Status: Accepted; partially superseded by ADR-028 (record trigger)`).
4. Atualize a spec viva no Completion do plano que entrega a decisão.
5. Atualize índices sem copiar o status.
6. Preserve o corpo do ADR anterior.

## Ritmo de entrega (decisão do dono, 08/10/2026)

Iterar rápido, codificar rápido, testar minimamente e entregar rápido. Testes massivos e chamadas pagas ao modelo não fazem parte do desenvolvimento.

- App: captura no emulador e comparação de gold só nas telas que o plano toca (regra de validação parcial abaixo); a regressão JVM cobre o resto. Nunca o fluxo inteiro para um build ou deploy de dev; nunca o fluxo inteiro numa feature pequena.
- Server: smoke mínimo que prove a mudança, no máximo **12 disparos ao modelo por plano** (casos novos uma vez; sem conjunto sentinela, sem suíte, sem repetição para decidir caso instável). Testes unitários e o smoke HTTP de três turnos depois do deploy de dev não custam e continuam.
- Produção (`master`, futuro): a validação completa ponta a ponta de server e app roda uma vez antes de qualquer deploy de produção, nunca durante o desenvolvimento.
- Cada plano declara o seu teto na seção Validação; um plano que precise de mais para e pergunta ao dono.

## Validação

Separe: automação executada, comandos reais, cobertura, manual executada, manual pendente, falhas e justificativa do que não rodou.

Evidência de um plano vive na seção Results dele, com os números escritos no texto (logs locais não ficam no repositório). Crie `validation/` só quando houver matriz viva, validação recorrente ou evidência de mais de um plano.

UI no client: DONE só com captura vs gold do corte, regra de `AGENTS.md`.

Validação parcial no emulador (decisão do dono, 04/10/2026): quando a entrega muda só algumas telas ou fluxos, e não o app inteiro, basta rodar os scripts de captura e o `diff-gold` dos fluxos que usam o código alterado (telas e componentes compartilhados que elas usam). Os demais fluxos podem ficar de fora se a regressão JVM (`testDevDebugUnitTest`, com o `GoldTest` de todos os ids, e `verifyRoborazziDevDebug`) passar sem mudança neles. Results lista os fluxos capturados, os que ficaram de fora e por quê. Mudança que atravessa o app (tema, tokens, design system inteiro, navegação raiz) continua pedindo todos os scripts.

## Manutenção de links

Criação, movimentação ou substituição:

1. README do contexto. Planos concluídos e cancelados entram só como link para a pasta (`plans/completed/`), nunca um por um.
2. README de `plans/`, se existir, com a mesma regra.
3. Matriz global se o contexto ou a cobertura mudou.
4. Specs, planos, validações.
5. Links relativos e referências ao caminho antigo.

## Shared workflow maintenance

Scope: repository agent instructions and their supporting validation tools; no client or server implementation ownership. Existing product and architecture authorities remain unchanged.

Plans and their state: [maintenance index](plans/README.md).

## Templates

- [README de contexto](templates/context-readme.md)
- [Especificação](templates/specification.md)
- [ADR](templates/adr.md)
- [Plano](templates/plan.md)
- [Deferred plan — Fora de escopo](templates/out-of-scope-plan.md)
- [Plano de fluxo Figma](templates/figma-flow.md)
- [Matriz de validação](templates/validation-matrix.md)
