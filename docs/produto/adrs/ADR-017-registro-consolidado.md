# ADR-017 — Um registro por refeição, com confirmação ao substituir

- Estado: Aceito (29/09/2026, com a aprovação do S8)
- Data: 2026-09-29
- Contexto: `produto`
- Substitui: parcialmente o [ADR-012](ADR-012-chat-home-perfil.md), regra 5 ("Segundo Gravar no mesmo slot **soma**"), e a regra 5 da [home-timeline](../specifications/home-timeline.md) ("Segundo log no mesmo slot empilha").

## Contexto

Um tester registrou 4 esfihas na Ceia e depois mandou "também tomei 2 copos de suco". O segundo Gravar criou um segundo registro na mesma Ceia. Depois ele pediu "corrija, na minha ceia eu consumi aproximadamente 1220 kcal", e a IA devolveu 1220 kcal com P/C/G zerados, que viraram um terceiro registro possível. Com o ADR-012, somar é o único caminho: não dá para corrigir nem completar uma refeição.

## Decisão

1. Cada refeição (slot) do dia tem **no máximo um registro**. A timeline já mostra um resumo consolidado; agora o dado também é um só.
2. Quando a fala completa, corrige ou remove algo de uma refeição que já tem registro, a IA reestima **a refeição inteira** (itens antigos + novos) e sugere aquele slot.
3. Tocar em Gravar num slot que já tem registro abre a confirmação (decisão do dono: "perguntar antes"):
   - Título: `Substituir {slot}?`
   - Texto: `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`
   - Ações: **Substituir** (troca o registro, P/C/G e texto) | **Outra refeição** (abre o sheet Trocar).
4. Slot vazio: Gravar grava direto, como hoje.
5. Um total de kcal sem comida ("comi 1220 kcal") **não gera estimativa**. A IA pergunta o que foi comido. Estimativa só sai com alimentos, e com P/C/G.
6. Registros duplicados que já existem no Room ficam como estão: a Home continua somando. A regra vale para gravações novas.

## Motivação

Uma refeição, um número. Corrigir não pode inflar o dia. A confirmação evita substituir por engano quando o slot sugerido está errado.

Custo de token: a refeição gravada já vai no snapshot do dia (`day.slots[].text/kcal/p/c/g`). A IA reestima a partir dali, sem reenviar o histórico.

## Consequências

### Positivas

- Home e Room sempre com uma linha por refeição.
- "Esqueci o suco" vira um toque em Substituir.

### Negativas

- A qualidade depende da IA juntar os itens antigos com os novos. Mitigação: o snapshot leva os itens da refeição (S8/A18).
- Mais um passo (confirmação) quando o slot já tem registro. Aceito pelo dono.

## Alternativas consideradas

- **Atualizar direto, sem perguntar:** rejeitada pelo dono.
- **Continuar somando:** é o bug relatado.
- **A IA manda só o delta (o suco) e o app soma:** exige coordenar dois números e não resolve remoção ou correção.

## Relações

- Planos: [S8](../../server/plans/completed/s8-chat-json-slot-consolidado.md), [A18](../../android/plans/completed/a18-chat-registro-foto.md).
- Specs: [chat](../specifications/chat.md), [home-timeline](../specifications/home-timeline.md), [v1-chat](../../server/specifications/v1-chat.md).

Depois de aceito, este ADR não se edita.
