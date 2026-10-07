# Execução noturna — 06 para 07/10/2026

Resumo da noite, a pedido do dono (aprovações na mensagem de 06/10/2026). Os detalhes e a evidência de cada plano ficam na seção Results do próprio plano, linkado abaixo.

## Em uma frase

O A54 foi entregue e a versão **0.0.17** está no App Tester. Os quatro planos de design estão desenhados no Figma e esperam a sua revisão. O **S24 não fechou** e ficou parado em implementação, sem deploy, e por isso **S25, S26 e S27 não começaram**.

## Planos

| Plano | Estado | Entrega |
|---|---|---|
| [A54](../../android/plans/completed/a54-auto-record-addition-empty-slot.md) — registro automático de acréscimo | Concluído | PR #152 |
| [S24](../plans/s24-protein-first-plan.md) — plano com proteína dentro da janela | **Em implementação (parado)** | PR #153 em rascunho, sem merge; status na master pelo PR #154 |
| [S25](../plans/s25-tone-and-closures.md) — tom e fechamentos (servidor) | Não iniciado | depende do S24 |
| [S26](../plans/s26-reply-formatting-subset.md) — formatação das respostas | Não iniciado | depende do S24 |
| [S27](../plans/s27-planned-slot.md) — refeição planejada no DAY | Não iniciado | depende do S24 |
| [D16](../../design/plans/completed/d16-tone-and-closures.md) — tom e fechamentos | Pendente aprovação manual | PR #155 |
| [D12](../../design/plans/completed/d12-plan-budget-choice.md) — plano acima do orçamento (`chatRB`) | Pendente aprovação manual | PR #156 |
| [D17](../../design/plans/completed/d17-rich-replies.md) — negrito, listas e tabela | Pendente aprovação manual | PR #157 |
| [D18](../../design/plans/completed/d18-planned-meal.md) — refeição planejada | Pendente aprovação manual | PR #158 |

ADRs aceitos pelas aprovações da noite, conforme a linha de status de cada um: ADR-043 (S24; o ADR-039 registra a substituição parcial), ADR-044 (D16 e S25), ADR-045 (D17 e S26) e ADR-046 (D18 e S27).

### A54: a causa não era a que o plano supunha

Os breadcrumbs do Crashlytics da sessão de 06/10 mostram que o acréscimo de 263 kcal **foi gravado** (`meal_auto_recorded`). Treze segundos depois, o tester tocou em **Excluir** no recibo. O texto da resposta, "Total do Jantar: 263 kcal", continuou no HISTORY com o Jantar vazio no DAY, e o modelo passou a mandar um `base_slot` velho, que o servidor recusou duas vezes. Correção entregue: uma resposta de acréscimo cujo destino não está comido vai ao HISTORY só com a primeira linha. Além disso, toda resposta `auto` sem registro agora mostra `Não registrado` e manda `record_guard` com o motivo.

### S24: por que parou

Pela regra da noite, uma validação que não passa depois de duas tentativas para o plano. Foram cinco rodadas dos casos `s24`, cada uma com uma correção diferente, e uma rodada completa:

- O caso do prato nomeado com espaço na janela ficou 0/3 em todas as rodadas: o modelo não acrescenta os itens `(opcional)` de proteína (ADR-043, decisão 4).
- A rodada completa deu **201/230** (S23: 215/223). **Onze falhas** são casos de receita/orçamento do ADR-039 contra a nova janela. A reserva padrão de cada refeição vazia (teto ÷ refeições) zera o `limit_kcal` em casos da noite.
- Fechamento do dia (linhas por refeição), reservas calculadas, prato nomeado sem espaço, sem balança e memória de marca passaram 3/3 em pelo menos uma rodada. Os números das linhas de fechamento são reescritos pelo servidor.

O código está no branch `feat/s24-protein-first-plan` (PR #153 em rascunho), com 425 testes passando.

## Custo na OpenAI (teto da noite: US$ 1,00)

| Item | Custo |
|---|---|
| S24, `--tag s24 --repeat 3`, 5 rodadas | US$ 0,0332 |
| S24, rodada completa `--repeat 1` | US$ 0,0841 |
| A54, dois turnos reais no servidor dev (validação 4) | ≈ US$ 0,001 (estimado, chave do servidor) |
| **Total** | **≈ US$ 0,118** |

Nenhuma avaliação com `--moderation all`, nenhum pilot runner e nenhuma repetição para desempatar.

## App e servidor

- **Versão distribuída: 0.0.17** (tag `dev-v0.0.17`, grupo `testers` no Firebase App Distribution). Notas para o tester: correção do Chat depois de excluir uma refeição registrada pelo Chat, e o aviso `Não registrado` quando uma refeição não pode ser registrada sozinha.
- **Deploys: nenhum.** O único plano de servidor desta noite (S24) não fechou, e o servidor dev segue com o S23, como estava ontem.
- Figma: 22 das 120 chamadas do MCP (D16: 12, contando 4 de preparação; D12: 2; D17: 4; D18: 4). As imagens de revisão saíram pelo export REST em `--dry-run`, sem gastar chamadas.

## O que fazer de manhã, em ordem

1. **Revisar no Figma** (`Design` → `Release 1`). É o único passo manual dos planos de design. Cada plano lista nos Results o que olhar:
   1. **D16**: `o5`, `cfg` (linha `Tom da Tali`), `cfgT`, `homeC`, `homeK`, mais `Card/Closure` e `Option/Tone` em Componentes. Decidir a palavra `TOM` no eyebrow do O5 e a falta de subtítulo. Os contadores `n/4` de `o1`–`o4` ficaram fora do plano. Os frames `cfgR` foram movidos para dentro da seção Config, onde deveriam estar.
   2. **D12**: `chatRB` na seção "Chat — Plano acima do orçamento". Com texto a 130%, o rótulo "Ajustar para caber" precisa quebrar em duas linhas (anotado para o A50).
   3. **D17**: `chatR` (agora pedido aberto com duas opções em bullets), `chatE` (gramas em negrito), `chatRK` (tabela e passos).
   4. **D18**: `chatR` (pílula **Reservar para o Jantar**), `chatRL`, `homeP`. O marcador planejado usa o contorno tracejado do pulado, e "planejado · 360 kcal" estreita a descrição.
   Depois de cada OK, o agente exporta os golds e conclui o plano.
2. **Decidir o S24**: (a) a reserva padrão das refeições vazias e os onze casos do ADR-039; (b) o que fazer com a decisão 4 do ADR-043 (prato nomeado com `(opcional)`), que o modelo não segue só com regras de prompt. Com isso decidido, o S24 fecha e S25, S26 e S27 podem rodar.
3. **Instalar a 0.0.17** no App Tester e conferir o Chat depois de excluir um registro.
4. Os planos de cliente A50, A55, A57 e A58 continuam esperando os golds (depois dos OKs no Figma) e a sua aprovação.
