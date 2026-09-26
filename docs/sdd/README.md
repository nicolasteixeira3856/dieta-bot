# Spec-Driven Development

Política global de SDD do Nutri. Governa todos os contextos da [matriz](../README.md).

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
│   └── cancelled/
└── validation/
```

O README declara: propósito, tipo, ownership de código, escopo, fora de escopo, fronteiras, cobertura, estado, ordem de leitura, índice.

Não crie diretórios vazios. `completed/`, `pending_manual_validation/` e `cancelled/` nascem no primeiro plano que precisar delas.

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

## Criação de um novo contexto

1. Nome em `snake_case`.
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

Regras:

- Plano ativo mora direto em `plans/`.
- Depois da implementação e das validações automatizadas, o plano sai da raiz de `plans/` na mesma entrega.
- Sucesso automatizado não conclui plano com validação manual pendente.
- Aprovação manual: estado `Concluído` e move para `completed/`.
- Plano implementado não permanece em `Aguardando aprovação`.
- Toda movimentação atualiza índices, pré-requisitos, specs, validações e links.

### Limpeza segura das pastas de estado

Após mover um plano, confira `plans/completed/`, `plans/pending_manual_validation/` e `plans/cancelled/` no contexto afetado. Remova somente diretórios desse conjunto que estejam vazios, inclusive de ocultos.

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

## Templates

- [README de contexto](templates/context-readme.md)
- [Especificação](templates/specification.md)
- [ADR](templates/adr.md)
- [Plano](templates/plan.md)
- [Matriz de validação](templates/validation-matrix.md)
