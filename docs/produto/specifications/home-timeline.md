# Especificação — Home painel

## Estado

Home hoje: restante 34pt, composer, CTA “o que cabe agora”, lista de logs. O doodle do dono é outro app.

## Contexto e objetivo

Relógio do dia. Registro mora no Chat.

## Escopo

- Home + navegação para Config e Chat.
- Timeline comeu / pulado.

## Fora de escopo

- Composer. Chip de janela. T3 “o que cabe agora”. BottomNav.

## Regras funcionais

1. Círculo grande = kcal **consumidas** no dia (não o restante).
2. Linha P C G = consumido/alvo. Estouro usa token `bad`.
3. Data `DD/MM/YYYY` em America/Sao_Paulo.
4. Timeline: um bloco por slot do perfil, ordem da hora. Slot com logs: cada linha é o texto gravado (descrição da IA se veio de foto). Slot skip: “Pulado”. Slot vazio: linha neutra; tap oferece Pular {nome}.
5. Segundo log no mesmo slot empilha (soma no contador).
6. Config: ícone no topo direito → tela Config.
7. FAB canto inferior direito → Chat. Único caminho de registro.
8. Tap num log não abre Chat neste corte.
9. Rollover 00:00 SP: contador e timeline do novo dia, vazios. Fio do chat (UI) não apaga.
10. Disclaimer visível em uma linha.

## Estados e falhas

- Sem perfil / onboardingDone=0: splash → O1.
- Sem logs: círculo 0, slots vazios, nenhum “Pulado” automático.

## Fronteiras e ownership

Comportamento: `produto`. UI: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)

## Planos relacionados

- `docs/android/plans/a4-home-painel.md`

## Critérios de aceite funcionais

- Zero campo de texto na Home.
- FAB e Config visíveis.
- Pular na timeline grava skip e redesenha.
