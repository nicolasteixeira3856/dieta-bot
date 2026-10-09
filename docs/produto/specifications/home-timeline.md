# Especificação — Home painel

## Contexto e objetivo

Relógio do dia. Registro mora no Chat. Layout: gold `home1` (canônico) da fonte declarada no [inventário](../../qa/README.md#golds), desenhado no Figma `Design` com o design system Aero ([ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md)); valores em [`docs/tokens.md`](../../tokens.md) § Aero.

## Escopo

- Home + navegação para Config e Chat.
- Timeline comeu / pulado.

## Fora de escopo

- Composer. Chip de janela. T3 “o que cabe agora”. BottomNav.

## Regras funcionais

1. Anel grande = kcal **consumidas** no dia (não o restante), progresso sobre a meta do dia (teto efetivo com crédito de treino). Pílula "Meta {n} kcal". Acima da meta: anel `status/bad` + "Meta excedida (+{n} kcal)".
2. Linha P C G = consumido/alvo. Estouro usa token `status/bad`.
3. Cabeçalho `DIA {n}` (dias desde o primeiro dia) + data `{d} de {mês}` pt-BR, America/Sao_Paulo.
4. Timeline: um bloco por slot cujo bitmask days contém o dia corrente em America/Sao_Paulo, ordem da hora. Slot com logs: cada linha é o texto gravado (descrição da IA se veio de foto) + kcal, e um resumo consolidado `{kcal} kcal · {P}P · {C}C · {G}G`; nó ✓ (câmera se veio de foto). Slot skip: "Refeição pulada". Slot vazio: "Nenhum registro · Toque para registrar, segura para pular" (pode quebrar em duas linhas); toque abre o Chat, igual ao FAB (sem texto preenchido, sem slot enviado); toque longo vibra e abre a confirmação "Pular {nome}?" ([ADR-040](../adrs/ADR-040-home-card-gestures-app-reset.md)). Ações de acessibilidade: "Registrar" e "Pular". Cards com log, pulados e "Outros" não reagem a toque nem a toque longo. O 1º slot vazio depois do último preenchido fica em destaque. O slot em que o acumulado passa da meta (e os seguintes com log) ficam em `status/bad`. Logs sem slot ou associados a um slot que não pertence ao dia: bloco "Outros" no fim. Editar refeições nunca apaga esses logs.
5. Um registro por refeição: gravar pelo Chat num slot com registro pede confirmação e substitui ([ADR-017](../adrs/ADR-017-registro-consolidado.md)). Registros duplicados de antes do A18 continuam somando.
6. Config: ícone no topo direito → tela Config.
7. FAB canto inferior direito → Chat. FAB e card de slot vazio abrem o Chat; o Chat é o único lugar que registra.
8. Tap num log não abre Chat neste corte.
9. Rollover 00:00 SP: contador e timeline do novo dia, vazios. Fio do chat (UI) não apaga.
10. Disclaimer visível no rodapé com a copy do Stitch ("Estimativa nutricional, não substitui consulta médica ou nutricional."). No fim da rolagem ele fica inteiro acima do FAB: 20 dp de respiro (gold `home1`) + inset de navegação.
11. Linha "Treino de hoje" abaixo dos macros, antes da timeline (gold `homeW`). Sem treino hoje: valor `Informar` na cor `accent/default`. Com treino: `{kcal} kcal · +{crédito} na meta`, crédito pela fórmula do `AGENTS.md` (0% → `+0 na meta`, a linha continua). Toque abre o sheet "Treino de hoje": campo numérico (estilo `Field/Number`) com sufixo `kcal`, uma linha de crédito ao vivo (`+{n} kcal na meta de hoje (compensação {pct}%)`; 0%: `Compensação desativada na Config`) e o par Salvar / Cancelar. Salvar grava o mesmo `day.workoutKcal` da Config; campo vazio + Salvar = sem treino (crédito 0). Cancelar ou back não grava. Mesmo editor da Config. O número também vem do Chat (Chat, regra 23): a linha e o sheet mostram o valor que o Chat gravou, e o sheet continua podendo mudá-lo.

12. Fechamentos (`homeC`, `homeK`, [ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md)): entre a linha de treino e a timeline, o card da semana acima do card do dia. Dia: `Fechamento de {d} de {mês}`, `{kcal} de {teto} kcal`, `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}` nas cores dos macros, as refeições puladas e sem registro numa linha (`Pulado: … · Sem registro: …`; reservada conta como sem registro), `Treino: {kcal} kcal` quando houver e o texto do servidor (`Sem o texto: sem rede.`, `Nenhum registro.` sem ele). Um registro do dia depois das 22:00 atualiza os números do card a partir do Room, não o texto. No dia seguinte o card de ontem fica aberto até o primeiro registro e então vira uma linha `Ontem: {kcal} de {teto} kcal`; o toque abre. Semana: `Semana de {d} a {d}`, `{total} kcal · média {média} kcal/dia` (média sobre os dias com registro), `Proteína: média {p} g/dia · Dias sem registro: {n}`, a linha da refeição que mais passou e o texto; aberto até terça 00:00, depois uma linha até o domingo seguinte.
13. Refeição reservada (`homeP`, [ADR-046](../adrs/ADR-046-planned-meal-reservation.md)): nó tracejado com o calendário, card esmaecido com o prato e `planejado · {kcal} kcal`; não conta no anel nem nos macros. Toque abre o Chat como uma refeição vazia; toque longo pula depois da confirmação de sempre e limpa a reserva. A virada do dia, o wipe e o reset limpam as reservas do dia.

## Estados e falhas

- Sem perfil / onboardingDone=0: splash → O1.
- Sem logs: círculo 0, slots vazios, nenhum “Pulado” automático.

## Fronteiras e ownership

Comportamento: `produto`. UI: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../adrs/ADR-017-registro-consolidado.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)
- [ADR-040](../adrs/ADR-040-home-card-gestures-app-reset.md)
- [ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md), [ADR-046](../adrs/ADR-046-planned-meal-reservation.md)

## Critérios de aceite funcionais

- Zero campo de texto no painel da Home (o único campo é o numérico do sheet de treino).
- Da Home ao treino salvo em 3 toques (linha, digitar, Salvar); o kcal e o crédito aparecem na linha.
- FAB e Config visíveis.
- Toque num slot vazio abre o Chat sem gravar nada; toque longo + Pular grava skip e redesenha.
- A Home oferece a captura de tela com rolagem do sistema (Android 12+), com a linha do tempo e os cartões de fechamento ([ADR-048](../adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md)).

## Proveniência

- [A4](../../android/plans/completed/a4-home-painel.md) — Home painel
- [A18](../../android/plans/completed/a18-chat-registro-foto.md) — Chat: refeição consolidada, teclado e foto 2048 px
- [A20](../../android/plans/completed/a20-polimento-geral.md) — Polimento: feedback de toque, botões dos sheets, respiro de scroll, Config
- [A22](../../android/plans/completed/a22-treino-home.md) — Treino na Home
- [ST2](../../stitch/plans/completed/st2-home-treino.md) — Home: atalho de treino
- [A24](../../android/plans/completed/a24-refeicoes-por-dia.md) — Refeições por dia da semana
- [A40](../../android/plans/completed/a40-home-aero.md) — Home no Aero
- [A52](../../android/plans/completed/a52-home-card-gestures.md) — Home: toque registra, toque longo pula
- [A60](../../android/plans/completed/a60-tone-formatting-planned-skips.md) — fechamentos do dia e da semana, refeição reservada
- [A61](../../android/plans/completed/a61-chat-copy-scroll-capture-inline-actions.md) — captura de tela com rolagem
- [A65](../../android/plans/completed/a65-workout-via-chat.md) — treino informado pelo Chat
