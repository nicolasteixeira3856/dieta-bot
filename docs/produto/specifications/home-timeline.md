# Especificação — Home painel

## Estado

Vigente desde o [A4](../../android/plans/completed/a4-home-painel.md): painel conforme Stitch gold home1 (canônico).

Mudanças planejadas (29/09/2026, aguardando aprovação): linha "Treino de hoje" abaixo dos macros ([A22](../../android/plans/a22-treino-home.md), gate [ST2](../../stitch/plans/st2-home-treino.md)); disclaimer inteiro acima do FAB ([A20](../../android/plans/a20-polimento-geral.md)); slots do dia da semana ([A24](../../android/plans/a24-refeicoes-por-dia.md)).

## Contexto e objetivo

Relógio do dia. Registro mora no Chat.

## Escopo

- Home + navegação para Config e Chat.
- Timeline comeu / pulado.

## Fora de escopo

- Composer. Chip de janela. T3 “o que cabe agora”. BottomNav.

## Regras funcionais

1. Anel grande = kcal **consumidas** no dia (não o restante), progresso sobre a meta do dia (teto efetivo com crédito de treino). Pílula "Meta {n} kcal". Acima da meta: anel `bad` + "Meta excedida (+{n} kcal)".
2. Linha P C G = consumido/alvo. Estouro usa token `bad`.
3. Cabeçalho `DIA {n}` (dias desde o primeiro dia) + data `{d} de {mês}` pt-BR, America/Sao_Paulo.
4. Timeline: um bloco por slot do perfil, ordem da hora. Slot com logs: cada linha é o texto gravado (descrição da IA se veio de foto) + kcal, e um resumo consolidado `{kcal} kcal · {P}P · {C}C · {G}G`; nó ✓ (câmera se veio de foto). Slot skip: "Refeição pulada". Slot vazio: "Nenhum registro · Toque para pular"; tap abre confirmação "Pular {nome}?". O 1º slot vazio depois do último preenchido fica em destaque. O slot em que o acumulado passa da meta (e os seguintes com log) ficam em `bad`. Logs sem slot: bloco "Outros" no fim.
5. Um registro por refeição: gravar pelo Chat num slot com registro pede confirmação e substitui ([ADR-017](../adrs/ADR-017-registro-consolidado.md), [A18](../../android/plans/pending_manual_validation/a18-chat-registro-foto.md)). Registros duplicados de antes do A18 continuam somando.
6. Config: ícone no topo direito → tela Config.
7. FAB canto inferior direito → Chat. Único caminho de registro.
8. Tap num log não abre Chat neste corte.
9. Rollover 00:00 SP: contador e timeline do novo dia, vazios. Fio do chat (UI) não apaga.
10. Disclaimer visível no rodapé com a copy do Stitch ("Estimativa nutricional, não substitui consulta médica ou nutricional.").

## Estados e falhas

- Sem perfil / onboardingDone=0: splash → O1.
- Sem logs: círculo 0, slots vazios, nenhum “Pulado” automático.

## Fronteiras e ownership

Comportamento: `produto`. UI: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)

## Planos relacionados

- [A4 (Concluído)](../../android/plans/completed/a4-home-painel.md)

## Critérios de aceite funcionais

- Zero campo de texto na Home.
- FAB e Config visíveis.
- Pular na timeline grava skip e redesenha.
