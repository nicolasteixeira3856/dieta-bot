# Respostas de segurança

Textos fixos para as bolhas existentes. Não criam tela nem controle novo. Em uso no server (`server/shaping.py`).

| Situação | Código interno | Resposta |
| --- | --- | --- |
| Fora do escopo | `out_of_scope` | Posso ajudar com refeições, porções e o orçamento alimentar do dia. |
| Conteúdo bloqueado | `policy_blocked` | Não posso analisar esse conteúdo. Envie uma descrição de refeição ou uma foto de alimentos. |
| Imagem sem informação alimentar | `out_of_scope` | Não identifiquei alimentos ou informações de um rótulo nessa imagem. Envie outra foto ou descreva a refeição. |
| Risco de transtorno alimentar ou autolesão | `safety_support` | Não posso orientar práticas alimentares que possam causar dano. Se quiser conversar com alguém agora, o CVV atende 24 horas pelo 188. Procure também um profissional de saúde. |
| Relato de violência ou risco pessoal | `safety_support` | Não consigo atender essa situação pelo app. Se houver risco imediato, ligue 190 ou 192. |

Nenhuma resposta acusa o usuário de crime, afirma que houve denúncia, expõe categorias internas ou descreve o conteúdo bloqueado. Falha técnica usa o estado de erro já existente.

Conferido em 30/09/2026: CVV 188 (24 horas, gratuito), 190 (Polícia Militar) e 192 (SAMU) vigentes. Confira de novo antes de qualquer revisão desta tabela.

## Proveniência

- [CP2](../plans/completed/cp2-server-content-controls.md) — Server scope and content controls
