# Plano — A28 Memória v2: fatos permanentes e dinâmicos

- Estado: Aguardando aprovação
- Data: 30/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`core/memory`, `core/network/ChatModels.kt`, `core/database` — Room v5 → v6, `feature/chat/ChatViewModel.kt`, `feature/chat/PromptBuilder.kt`, `app/src/dev/…/feature/devtools/DevMemoryScreen.kt`)
- Pré-requisitos: [A27](a27-chat-v2-texto-intencao.md) concluído (Room v5, `meal_text`). [S11](../../server/plans/pending_manual_validation/s11-chat-v2.md) no ar. Aceita o [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) (decisão 4). **Sem gate Stitch**: o selo e os chips são do [A29](a29-chat-v2-interface.md); a tela dev não tem gold ([ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md)).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a28-memoria-v2.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

A memória deixa de ser um diário e vira uma lista curta de fatos que cresce e encolhe com o uso: permanente (explícita ou promovida, nunca some sozinha) e dinâmica (observada, some em 21 dias). O app manda os fatos ao server, aplica o que a IA propõe com regras fixas e guarda, por mensagem, se a memória mudou e de onde veio o que a IA usou (base do selo e dos chips do A29).

## Fontes de verdade

- [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) decisão 4, [ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md).
- [memoria-push](../../produto/specifications/memoria-push.md), [api-contract.md](../../api-contract.md) (depois do S11).

## Escopo de implementação

### 1. Formato (`core/memory`)

- Mesmo arquivo `filesDir/memory.bin`, mesma cifra (AES-256-GCM, chave no Keystore) e gravação atômica do A8b. Conteúdo passa a ser JSON:

```json
{
  "v": 2,
  "next": {"P": 4, "D": 13},
  "facts": [
    {"id": "P1", "kind": "permanent", "category": "preference", "key": "leite",
     "text": "Leite semidesnatado", "slot": null, "source": "explicit",
     "days": ["2026-09-30"], "created": "2026-09-30",
     "kcal": null, "p": null, "c": null, "g": null}
  ]
}
```

- `source`: `explicit` | `promoted` | `observed`. `days`: datas distintas (America/Sao_Paulo) em que o fato apareceu, só as dos últimos 21 dias. `kcal`/`p`/`c`/`g`: só rotina, do último registro.
- **Leitura de conteúdo que não é `v: 2` (a memória de texto do A8) → memória vazia**, e o arquivo é reescrito vazio na primeira gravação. Sem conversão (ADR-023: memória inicial vazia para todo mundo).
- `MemoryStore` vira `FactMemory` com `read()`, `apply(updates, today, recorded: RecordedMeal?)`, `reinforceRoutine(factId, meal)`, `replaceAll(facts)` (editor dev). O `append` do A8 some.

### 2. Regras (`domain/MemoryRules.kt`, Kotlin puro, testável sem Android)

Constantes: `PERMANENT_MAX = 30`, `DYNAMIC_MAX = 40`, `DYNAMIC_TTL_DAYS = 21`, `PROMOTE_DAYS = 5`, `STRONG_ROUTINE_DAYS = 3`, `TEXT_MAX = 160`, `KEY_MAX = 40`.

| Operação | Regra |
|---|---|
| `add permanent` | Já existe fato com a mesma `key` (qualquer tipo) → vira esse fato, permanente, com o texto novo (contradição). Senão, com vaga (< 30) → novo `P{n}`, `source: explicit`. Sem vaga → ignorado (a IA deveria ter perguntado). |
| `add dynamic` | Mesma `key` existe → trata como `reinforce` dela, com o texto novo se for dinâmica. Senão → novo `D{n}`, `source: observed`. Acima de 40 → sai a dinâmica com o `last_seen` mais antigo. |
| `reinforce` | Acrescenta hoje em `days` (sem repetir). Dinâmica: texto novo, se veio. Permanente: texto só muda por `replace`. |
| `replace` | Troca o `text`. Com `kind: permanent` numa dinâmica → vira permanente (`explicit`), se houver vaga. |
| `remove` | Apaga o fato (pedido explícito do usuário). |
| Promoção | Depois de cada aplicação: dinâmica com ≥ 5 `days` nos últimos 21 → permanente (`P{n}` novo, `source: promoted`, `days` preservados), se houver vaga; senão espera. |
| Expiração | Em toda leitura: `days` fora dos 21 dias saem; dinâmica sem nenhum dia restante sai. Permanente nunca expira. |
| Rotina forte | Rotina permanente, ou dinâmica com ≥ 3 `days`, com `slot` e `kcal`. Usada pelo A29. |

### 3. Quando aplicar (`ChatViewModel`)

- Na resposta: `memory_updates` de `preference` e `portion`, e todo `replace`/`remove` (inclusive de rotina), aplicados na hora.
- `add`/`reinforce` de `routine`: guardados na mensagem da IA (`pendingMemory`, JSON) e aplicados só quando **aquela** estimativa for gravada (Gravar, Confirmar do Trocar, Substituir e, no A29, Registrar assim), com `slot`, `kcal`, P/C/G do registro. Estimativa não gravada não vira hábito.
- A mensagem que teve pelo menos uma mudança **efetivamente aplicada** ganha `memoryUpdated = 1`: a da IA (na resposta) ou o recibo (no registro). Mudança ignorada (sem vaga, id sumido) não marca.
- `memory_used`: ids resolvidos para o tipo no momento da resposta e guardados em `memoryUsedKinds` (`permanent`, `dynamic` ou os dois). Id que não existe mais é ignorado.
- Somem: a linha `Respondeu "…": …` e as linhas `{slot}: …` do A8.

### 4. Request (`PromptBuilder`, `ChatModels`)

- `ChatIn.facts`: todos os fatos depois da expiração (≤ 70), com `days_seen` = tamanho de `days` e `last_seen` = último dia. `memory` = `""`.
- Com `facts` presente, o server trata o client como v2: planos voltam com estimativa (guardada, sem ações até o A29), e chegam `memory_updates`/`memory_used`.
- `ChatOut.memoryUpdates`, `ChatOut.memoryUsed`.

### 5. Room v5 → v6 (`chat_message`)

- `pendingMemory TEXT` (nullable), `memoryUsedKinds TEXT` (nullable), `memoryUpdated INTEGER NOT NULL DEFAULT 0`.
- `MIGRATION_5_6` só com `ADD COLUMN`. Schema exportado e `room-v2.md`.

### 6. Editor dev (`Memória da IA (dev)`, A23)

- A área **Memória** mostra um fato por linha, editável: `P1 | preference | leite | Leite semidesnatado`. Rotina: `D2 | routine | cafe | slot=1 | 2 ovos mexidos, … | 440 kcal 25P 38C 22G`.
- Acima dela, só leitura: `Permanente 3/30 · Dinâmica 5/40` e, por fato, `visto {n} dias · último {data}`.
- Salvar: mesmas regras de limite e tamanho. Pode editar texto, `key`, tipo e adicionar linha nova (`novo | preference | queijo | Queijo minas`). **Não apaga** ([ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md)): linha removida é recusada com o número da linha, e nada é salvo. Esquecer um fato é pelo Chat ("esquece X").
- Conferir a duplicação que o dono viu em 30/09 (o texto da memória aparece duas vezes, colado em "(65 kcal)Café:"): se vier do editor, corrigir aqui e registrar a causa.

### 7. Telemetria

- Evento `memory_changed`: contagem por operação aplicada (`add`, `reinforce`, `replace`, `remove`, `promote`, `expire`) e totais `permanent`/`dynamic`. Só números.

### 8. Testes

- `MemoryRulesTest` (JVM): cada linha da tabela da seção 2, inclusive contradição, sem vaga, promoção com e sem vaga, expiração no dia 22, limite de 40, rotina forte.
- `FactMemoryTest`: memória de texto antiga → vazia; ida e volta do JSON cifrado; gravação atômica mantida.
- `ChatViewModelTest`: preferência aplicada na resposta (`memoryUpdated` na IA); rotina só no Gravar (`memoryUpdated` no recibo); estimativa não gravada não reforça; `memory_used` → `memoryUsedKinds`; nenhuma linha `Respondeu`.
- `PromptBuilderTest`: `facts` com `days_seen`/`last_seen`, `memory` vazio.
- `MigrationTest` 5 → 6.
- Roborazzi sem mudança no Chat.

## Arquivos e áreas afetadas

- `core/memory/*` (`MemoryStore` → `FactMemory`, `LegacyMemory`), `domain/MemoryRules.kt` (novo).
- `core/network/ChatModels.kt`, `feature/chat/ChatViewModel.kt`, `feature/chat/PromptBuilder.kt`.
- `core/database/ChatMessageEntity.kt`, `DietaBotDatabase.kt` (v6), `Migrations.kt`, schema.
- `app/src/dev/…/feature/devtools/DevMemoryScreen.kt` (+ ViewModel).
- `core/telemetry/TelemetryEvents`.
- Docs: `docs/produto/specifications/memoria-push.md` (regras da memória), `docs/android/specifications/room-v2.md`, este plano.

## Validação planejada

1. `:app:testDevDebugUnitTest` e `:app:verifyRoborazziDevDebug` verdes.
2. `:app:assembleDevRelease`, instalado **por cima** do build do A27 com memória antiga: a memória aparece vazia no editor dev, nada mais muda.
3. Emulador com o server de dev:
   - "Sempre uso leite semidesnatado" → `P1 leite` no editor.
   - Café com leite → não pergunta o tipo de leite; a mensagem guarda `memoryUsedKinds = permanent`.
   - "Agora uso leite integral" → `P1` com o texto novo, mesmo id.
   - "Esquece o leite" → `P1` sai.
   - Café gravado 3 dias seguidos (data do emulador avançada) → rotina `D…` com 3 dias; no 5º, vira `P…`.
4. Manual (dono): uma semana de uso real, olhando o editor dev.

## Fora de escopo

- Selo `Memória atualizada`, chips de origem, sugestão da rotina: [A29](a29-chat-v2-interface.md) (gold do [ST6](../../stitch/plans/completed/st6-chat-v2.md)).
- Memória visível ao usuário final fora do dev (ADR-023: até o dono decidir).
- Server: [S11](../../server/plans/pending_manual_validation/s11-chat-v2.md).

## Riscos e controles

- **Apagar a memória dos testers:** decisão do dono (ADR-023). O `recent` do A27 cobre o "mesmo de ontem".
- **A IA propor fato errado:** regras fixas no app, no máximo 5 por turno (server), editor dev para corrigir.
- **Permanente encher de promoções:** promoção só com vaga e depois de 5 dias; a frase explícita tem a pergunta de "memória cheia".
- **Arquivo cifrado corrompido:** mesma política do A8b (leitura falha → vazia, gravação atômica).

## Critérios de aceite

- Memória antiga some na atualização. Memória nova começa vazia.
- Frase explícita entra como permanente; contradição substitui; "esquece" remove.
- Rotina só conta com registro; 5 dias em 21 promovem; 21 dias sem aparecer expiram a dinâmica.
- Cada mensagem sabe se mudou a memória e que tipo de memória usou.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
