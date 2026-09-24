---
name: nutri-feature
description: Debate uma feature do Nutri antes de código. Use quando o pedido for funcionalidade nova, fluxo, copy, "e se", "vale a pena" ou "como funcionaria". Dispara só. Não use em correção de uma linha nem no calculator.
---

Não implemente nesta skill. Leia docs/TEAM.md.

1. Se a decisão depende de fato que não está no repo, spawn o pesquisador com o briefing. Ele busca fonte. Não peça `/deep-research` ao dono.
2. No mesmo passo, se ainda couber em 3 filhos, spawn Harper e Iris, sem write, com o mesmo briefing mais AGENTS.md e wires/nutri-wires.html.
3. Se o pesquisador ocupou uma vaga e só couber mais um, Harper primeiro. Iris na rodada seguinte, ainda sem código.
4. Confronta os resumos. Entrega:
   - 3 linhas por papel que falou
   - 1 decisão
   - 2 alternativas
   - MVP ou v2
   - pergunta única se ainda faltar um call do dono
5. Pare. Benjamin, Lucas, QA e revisor só depois de um sim explícito.

Máximo 3 filhos por rodada. Sem loop. Sem ler .env.
