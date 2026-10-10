# Especificação — Home painel

## Contexto e objetivo

Relógio do dia e dos 30 dias anteriores. Registro mora no Chat. Layout: gold `home1` (canônico; `homeH` um dia anterior, `homeE` o dia 1) da fonte declarada no [inventário](../../qa/README.md#golds), desenhado no Figma `Design` com o design system Aero ([ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md)); valores em [`docs/tokens.md`](../../tokens.md) § Aero.

## Escopo

- Home + navegação para Config e Chat.
- Timeline comeu / pulado / extra, por horário.
- Faixa dos últimos 30 dias e um dia anterior só para leitura ([ADR-058](../adrs/ADR-058-extras-and-history.md)).

## Fora de escopo

- Composer. Chip de janela. T3 “o que cabe agora”. BottomNav.
- Editar um dia anterior pela Home; reservar, pular ou o card `O de sempre` num dia anterior; dias além de 30 ou antes do primeiro dia do app.

## Regras funcionais

1. Anel grande = kcal **consumidas** no dia (não o restante), progresso sobre a meta do dia (teto efetivo com crédito de treino). Pílula "Meta {n} kcal". Acima da meta: anel `status/bad` + "Meta excedida (+{n} kcal)".
2. Linha P C G = consumido/alvo. Estouro usa token `status/bad`.
3. Cabeçalho `DIA {n}` (dias desde o primeiro dia até o dia mostrado) + data `{d} de {mês}` pt-BR do dia mostrado, America/Sao_Paulo.
4. Timeline: um bloco por slot cujo bitmask days contém o dia mostrado em America/Sao_Paulo e um bloco por extra, todos na ordem da hora (o slot pela hora do perfil, o extra pela sua; no empate, a refeição antes) ([ADR-058](../adrs/ADR-058-extras-and-history.md) decisão 2). Extra: nó próprio (o anel de feito com o relógio) e o card `Card/Extra` com `Extra · {HH:mm}`, o texto e `{kcal} kcal · {P}P · {C}C · {G}G` nas cores dos macros, sem número de refeição e sem toque; um extra conta no anel, nos macros e na regra do acima da meta, que percorre a lista misturada. O cabeçalho continua `{n} Refeições` (só os slots). Slot com logs: cada linha é o texto gravado (descrição da IA se veio de foto) + kcal, e um resumo consolidado `{kcal} kcal · {P}P · {C}C · {G}G`; nó ✓ (câmera se veio de foto). Slot skip: "Refeição pulada". Slot vazio: "Nenhum registro · Toque para registrar, segura para pular" (pode quebrar em duas linhas); toque abre o Chat, igual ao FAB (sem texto preenchido, sem slot enviado); toque longo vibra e abre a confirmação "Pular {nome}?" ([ADR-040](../adrs/ADR-040-home-card-gestures-app-reset.md)). Ações de acessibilidade: "Registrar" e "Pular". Cards com log, pulados e "Outros" não reagem a toque nem a toque longo. O 1º slot vazio depois do último preenchido fica em destaque. O slot em que o acumulado passa da meta (e os seguintes com log) ficam em `status/bad`. Logs sem slot (que não são extras) ou associados a um slot que não pertence ao dia: bloco "Outros" no fim. Editar refeições nunca apaga esses logs.
5. Um registro por refeição: gravar pelo Chat num slot com registro pede confirmação e substitui ([ADR-017](../adrs/ADR-017-registro-consolidado.md)). Registros duplicados de antes do A18 continuam somando.
6. Config: ícone no topo direito → tela Config.
7. FAB canto inferior direito → Chat. FAB e card de slot vazio abrem o Chat; o Chat é o único lugar que registra.
8. Tap num log não abre Chat neste corte.
9. Rollover 00:00 SP: contador e timeline do novo dia, vazios. Fio do chat (UI) não apaga.
10. Disclaimer visível no rodapé com a copy do Stitch ("Estimativa nutricional, não substitui consulta médica ou nutricional."). No fim da rolagem ele fica inteiro acima do FAB: 20 dp de respiro (gold `home1`) + inset de navegação.
11. Linha "Treino de hoje" abaixo dos macros, antes da timeline (gold `homeW`). Sem treino hoje: valor `Informar` na cor `accent/default`. Com treino: `{kcal} kcal · +{crédito} na meta`, crédito pela fórmula do `AGENTS.md` (0% → `+0 na meta`, a linha continua). Toque abre o sheet "Treino de hoje": campo numérico (estilo `Field/Number`) com sufixo `kcal`, uma linha de crédito ao vivo (`+{n} kcal na meta de hoje (compensação {pct}%)`; 0%: `Compensação desativada na Config`) e o par Salvar / Cancelar. Salvar grava o mesmo `day.workoutKcal` da Config; campo vazio + Salvar = sem treino (crédito 0). Cancelar ou back não grava. Mesmo editor da Config. O número também vem do Chat (Chat, regra 23): a linha e o sheet mostram o valor que o Chat gravou, e o sheet continua podendo mudá-lo.

12. Fechamentos (`homeC`, `homeK`, [ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md)): entre a linha de treino e a timeline, o card da semana acima do card do dia. Dia: `Fechamento de {d} de {mês}`, `{kcal} de {teto} kcal`, `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}` nas cores dos macros, as refeições puladas e sem registro numa linha (`Pulado: … · Sem registro: …`; reservada conta como sem registro), `Treino: {kcal} kcal` quando houver e o texto do servidor (`Sem o texto: sem rede.`, `Nenhum registro.` sem ele). O card nasce no horário do fechamento do perfil (padrão 22:00, [memoria-push](memoria-push.md) push regra 7); um registro do dia depois dele atualiza os números do card a partir do Room, não o texto. No dia seguinte o card de ontem fica aberto até o primeiro registro e então vira uma linha `Ontem: {kcal} de {teto} kcal`; o toque abre. Semana: `Semana de {d} a {d}`, `{total} kcal · média {média} kcal/dia` (média sobre os dias com registro), `Proteína: média {p} g/dia · Dias sem registro: {n}`, a linha da refeição que mais passou e o texto; aberto até terça 00:00, depois uma linha até o domingo seguinte.
13. Refeição reservada (`homeP`, [ADR-046](../adrs/ADR-046-planned-meal-reservation.md)): nó tracejado com o calendário, card esmaecido com o prato e `planejado · {kcal} kcal`; não conta no anel nem nos macros. Toque abre o Chat como uma refeição vazia; toque longo pula depois da confirmação de sempre e limpa a reserva. A virada do dia, o wipe e o reset limpam as reservas do dia.
14. Faixa de dias (`home1`, `homeE`, [ADR-058](../adrs/ADR-058-extras-and-history.md) decisão 3): entre o cabeçalho e o anel, um círculo de 36 dp por dia dos últimos 30 (nunca antes do primeiro dia do app), 10 dp entre eles, hoje no fim à direita, rolando para a esquerda; acima do círculo, a abreviação do mês no primeiro dia da faixa e em todo dia 1 (`SET`, `OUT`). Estados (`Home/DayCircle`): hoje selecionado (`accent/default`), hoje com outro dia selecionado (vidro com anel `accent/default`), dia anterior selecionado (`surface/selected` com anel `border/selected`), dia anterior com registro (vidro) e sem registro (anel tracejado `border/line`, número `text/dim`). Rótulo de acessibilidade `{d} de {mês}, {kcal} kcal`. O toque num dia anterior mostra esse dia; o toque em hoje volta a hoje. Telemetria `home_day_selected` (`offset` `1` | `2-7` | `8-30`) e `screen_view` `home_past`.
15. Dia anterior (`homeH`, decisão 4): o anel, os macros, a linha de treino (`Treino do dia`, sem a seta e sem toque; sem número, `Sem treino`), o card do fechamento daquele dia aberto quando existe (os números guardados) e a timeline daquele dia com o grupo de refeições do dia da semana, lidos do Room, com o teto e o crédito daquele dia. Cards sem toque e sem toque longo; refeição vazia diz só `Nenhum registro`; sem destaque de próxima refeição. FAB e Config continuam; o Chat é sempre o de hoje, e abrir o Chat ou a Config volta a Home para hoje, assim como a virada do dia.

## Estados e falhas

- Dia 1 (`homeE`): a faixa só com hoje.
- Sem perfil / onboardingDone=0: splash → onboarding: boas-vindas, ou o ponto em que parou ([perfil-onboarding](perfil-onboarding.md) regra 10).
- Sem logs: círculo 0, slots vazios, nenhum “Pulado” automático.

## Fronteiras e ownership

Comportamento: `produto`. UI: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../adrs/ADR-017-registro-consolidado.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)
- [ADR-040](../adrs/ADR-040-home-card-gestures-app-reset.md)
- [ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md), [ADR-046](../adrs/ADR-046-planned-meal-reservation.md)
- [ADR-058](../adrs/ADR-058-extras-and-history.md)

## Critérios de aceite funcionais

- Zero campo de texto no painel da Home (o único campo é o numérico do sheet de treino).
- Da Home ao treino salvo em 3 toques (linha, digitar, Salvar); o kcal e o crédito aparecem na linha.
- FAB e Config visíveis.
- Toque num slot vazio abre o Chat sem gravar nada; toque longo + Pular grava skip e redesenha.
- Um extra aparece no horário dele entre as refeições; um dia anterior da faixa mostra o dia sem gestos.
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
- [A71](../../android/plans/pending_manual_validation/a71-conversational-onboarding.md) — onboarding no lugar da O1, fechamento no horário do perfil
- [D28](../../design/plans/completed/d28-home-extras-and-history.md) — golds `home1`, `homeH`, `homeE`
- [A72](../../android/plans/pending_manual_validation/a72-extras-and-history.md) — extras na timeline, faixa de 30 dias, dia anterior
