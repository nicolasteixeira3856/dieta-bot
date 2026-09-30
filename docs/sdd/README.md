# Spec-Driven Development

Política global de SDD do Dieta Bot. Governa todos os contextos da [matriz](../README.md).

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

Decisão explícita que muda comportamento documentado atualiza a spec viva na etapa de Planning. Decisão arquitetural nova não reescreve ADR aceito: cria ADR sucessor e declara substituição total ou parcial.

ADRs aceitos em `docs/decisions/` (001–011) continuam lá. ADRs novos nascem em `docs/<contexto>/adrs/`.

## Tipos de contexto

### Produto

Comportamento visível: job, telas, copy, onboarding, slots, o que entra no prompt. Código não mora aqui.

### Client

`apps/android/`. Compose, Room, push, foto, navegação.

### Contrato HTTP

`server/`. Rotas, auth, LLM, shaping, timeout. Infra S0 (compose, tunnel, migração) indexada neste contexto. Sem contexto `infra` separado.

### Gate de design

`stitch`. Prompts que o dono executa no Stitch `Nutri` e a conferência dos golds resultantes. Não tem código de app. Ver [Gate Stitch](#gate-stitch).

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
| Validação | Evidências e pendências | Viva | Não |

Planos não são fonte de verdade do produto. Se a implementação descobrir comportamento novo: para, atualiza spec e artefatos, pede nova aprovação.

## Fluxo obrigatório

### 1. Discovery

1. Comece em `docs/README.md`.
2. Resolva o contexto dono.
3. Leia o README do contexto e o que ele indicar.
4. Consulte planos ativos, ADRs vigentes e validações pendentes.
5. Confirme no código atual.
6. Feche decisões de produto ou arquitetura ainda abertas (A/B/C se precisar).

### 2. Planning

1. Crie a fundação do contexto se não existir.
2. Atualize as specs vivas afetadas.
3. Crie um ADR quando a decisão for duradoura e ainda não coberta.
4. Crie ou atualize um plano ativo em `docs/<contexto>/plans/<plano>.md`.
5. Registre validações automatizadas e manuais previstas.
6. Atualize índices, matriz e links.

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

1. Registre resultados reais e pendências na validação.
2. Atualize specs e índices.
3. Aplique o ciclo de vida do plano na mesma entrega.
4. Atualize links que apontavam para o caminho anterior.
5. Remova pastas de estado que ficaram vazias, pela regra de limpeza abaixo.
6. Entregue pelo fluxo git abaixo.

### 6. Entrega git (decisão do dono, 29/09/2026)

Ao fim da implementação de qualquer plano, o agente faz sozinho, sem pedir:

1. `git switch -c <tipo>/<id-do-plano>` a partir da `master` atualizada (ex.: `fix/s8-chat-json-slot`, `feat/a22-treino-home`, `docs/st1-stitch-chat`).
2. Commit(s) só com os arquivos do plano. Nada de `git add -A` com lixo de fora do escopo.
3. `git push -u origin <branch>` e `gh pr create --base master` com resumo, validação executada e pendências.
4. `gh pr merge --merge --delete-branch` (merge commit, sem squash).
5. `git switch master` e `git pull --ff-only`. O repositório local termina igual ao remote.

Exceções: o commit `chore(release)` + tag do `tools/distribute-dev.ps1` (A16) continua indo direto na `master`, porque faz parte do deploy e não de um plano. PR com CI vermelho ou conflito não é mergeado: o agente para e reporta.

## Gate Stitch

Mudança de layout que precisa de gold novo ou alterado no Stitch `Nutri` é um passo manual do dono. Ela vive no contexto [stitch](../stitch/README.md), num plano de gate (`ST<n>`), separado da implementação.

Um plano de gate:

- Abre com o **prompt exato** que o dono cola no Stitch, marcado como bloqueio do dono. Logo abaixo, as instruções extras: quais telas selecionar, o que anexar.
- Chama as telas pelo **título exato do Stitch**, em toda instrução ao dono (selecionar, duplicar, renomear) e dentro do próprio prompt. Nunca pelo id de gold (`chatE`), que só aparece entre parênteses, para o agente. `V2 Expressive` = dark, `V2 Light` = light, com um bloco de prompt pronto por tema. Tela nova ganha no gate o título final, que o dono aplica na cópia. A tabela título ↔ gold fica em [stitch/README.md § Nomes das telas](../stitch/README.md#nomes-das-telas-regra-do-dono-29092026) e é atualizada a cada gate.
- Não muda layout nem funcionalidade do app. Mexe só em gold PNG (`docs/qa/stitch/`), no mapa de IDs de `tools/export-stitch.mjs` e `tools/check-stitch.mjs`, na lista de golds do `AGENTS.md` e de `docs/qa/README.md`.
- Tem uma verificação: o agente lê o projeto pelo MCP do Stitch, exporta os golds e confere um checklist. Se algo do prompt não aparece no Stitch, o agente **para na hora**, lista o que falta e não segue para nenhum plano dependente.
- Termina `Concluído` só quando o checklist inteiro passou.

Planos de implementação que dependem de um gate declaram o `ST<n>` em Pré-requisitos. O primeiro passo da implementação confere que o gate está em `stitch/plans/completed/`. Se não estiver, a implementação nem começa.

Template: [plano de gate Stitch](templates/stitch-gate.md).

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

ADR aceito é imutável. Para mudar:

1. Crie outro ADR.
2. Declare a substituição total ou parcial.
3. Atualize a spec viva.
4. Atualize índices.
5. Preserve o ADR anterior.

## Validação

Separe: automação executada, comandos reais, cobertura, manual executada, manual pendente, falhas e justificativa do que não rodou.

Cobertura pequena pode viver no próprio plano. Crie `validation/` quando houver matriz viva, validação recorrente, pendência manual ou evidência de mais de um plano.

UI no client: DONE só com captura vs gold do corte, regra de `AGENTS.md`.

## Manutenção de links

Criação, movimentação ou substituição:

1. README do contexto.
2. README de `plans/`, se existir.
3. Matriz global se o contexto ou a cobertura mudou.
4. Specs, planos, validações.
5. Links relativos e referências ao caminho antigo.

## Shared workflow maintenance

Scope: repository agent instructions and their supporting validation tools; no client or server implementation ownership. Existing product and architecture authorities remain unchanged.

Plans: [maintenance index](plans/README.md). Completed: [SD1 — Repository skill alignment](plans/completed/sd1-skills-alignment.md), `Concluído`.

## Templates

- [README de contexto](templates/context-readme.md)
- [Especificação](templates/specification.md)
- [ADR](templates/adr.md)
- [Plano](templates/plan.md)
- [Deferred plan — Fora de escopo](templates/out-of-scope-plan.md)
- [Plano de gate Stitch](templates/stitch-gate.md)
- [Matriz de validação](templates/validation-matrix.md)
