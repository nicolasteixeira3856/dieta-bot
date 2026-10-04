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
4. Timeline: um bloco por slot cujo bitmask days contém o dia corrente em America/Sao_Paulo, ordem da hora. Slot com logs: cada linha é o texto gravado (descrição da IA se veio de foto) + kcal, e um resumo consolidado `{kcal} kcal · {P}P · {C}C · {G}G`; nó ✓ (câmera se veio de foto). Slot skip: "Refeição pulada". Slot vazio: "Nenhum registro · Toque para pular"; tap abre confirmação "Pular {nome}?". O 1º slot vazio depois do último preenchido fica em destaque. O slot em que o acumulado passa da meta (e os seguintes com log) ficam em `status/bad`. Logs sem slot ou associados a um slot que não pertence ao dia: bloco "Outros" no fim. Editar refeições nunca apaga esses logs.
5. Um registro por refeição: gravar pelo Chat num slot com registro pede confirmação e substitui ([ADR-017](../adrs/ADR-017-registro-consolidado.md)). Registros duplicados de antes do A18 continuam somando.
6. Config: ícone no topo direito → tela Config.
7. FAB canto inferior direito → Chat. Único caminho de registro.
8. Tap num log não abre Chat neste corte.
9. Rollover 00:00 SP: contador e timeline do novo dia, vazios. Fio do chat (UI) não apaga.
10. Disclaimer visível no rodapé com a copy do Stitch ("Estimativa nutricional, não substitui consulta médica ou nutricional."). No fim da rolagem ele fica inteiro acima do FAB: 20 dp de respiro (gold `home1`) + inset de navegação.
11. Linha "Treino de hoje" abaixo dos macros, antes da timeline (gold `homeW`). Sem treino hoje: valor `Informar` na cor `accent/default`. Com treino: `{kcal} kcal · +{crédito} na meta`, crédito pela fórmula do `AGENTS.md` (0% → `+0 na meta`, a linha continua). Toque abre o sheet "Treino de hoje": campo numérico (estilo `Field/Number`) com sufixo `kcal`, uma linha de crédito ao vivo (`+{n} kcal na meta de hoje (compensação {pct}%)`; 0%: `Compensação desativada na Config`) e o par Salvar / Cancelar. Salvar grava o mesmo `day.workoutKcal` da Config; campo vazio + Salvar = sem treino (crédito 0). Cancelar ou back não grava. Mesmo editor da Config.

## Estados e falhas

- Sem perfil / onboardingDone=0: splash → O1.
- Sem logs: círculo 0, slots vazios, nenhum “Pulado” automático.

## Fronteiras e ownership

Comportamento: `produto`. UI: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../adrs/ADR-017-registro-consolidado.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)

## Critérios de aceite funcionais

- Zero campo de texto no painel da Home (o único campo é o numérico do sheet de treino).
- Da Home ao treino salvo em 3 toques (linha, digitar, Salvar); o kcal e o crédito aparecem na linha.
- FAB e Config visíveis.
- Pular na timeline grava skip e redesenha.

## Proveniência

- [A4](../../android/plans/completed/a4-home-painel.md) — Home painel
- [A18](../../android/plans/completed/a18-chat-registro-foto.md) — Chat: refeição consolidada, teclado e foto 2048 px
- [A20](../../android/plans/completed/a20-polimento-geral.md) — Polimento: feedback de toque, botões dos sheets, respiro de scroll, Config
- [A22](../../android/plans/completed/a22-treino-home.md) — Treino na Home
- [ST2](../../stitch/plans/completed/st2-home-treino.md) — Home: atalho de treino
- [A24](../../android/plans/completed/a24-refeicoes-por-dia.md) — Refeições por dia da semana
- [A40](../../android/plans/pending_manual_validation/a40-home-aero.md) — Home no Aero
