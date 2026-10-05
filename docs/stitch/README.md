# stitch

## Propósito

Histórico. O projeto Google Stitch `Nutri` (`6282733070135794645`) foi a fonte de layout do app até o [ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md); hoje a única fonte de UI é o arquivo Figma `Design` ([design](../design/README.md)). Este contexto não tem trabalho ativo, não recebe planos e não é fonte de gold.

## Onde está o histórico

- Gates `ST<n>`, a verificação automática `SV1` e os `*.checks.json`: [`plans/completed/`](plans/completed/).
- Golds Stitch exportados: `docs/qa/_legacy/stitch/{dark,light}/`. Design system do Stitch: `docs/qa/_legacy/stitch-design-system/`. Pela regra de `_legacy`, nenhum dos dois serve de comparação.
- Tooling (exportador, conferência, verificador e seus testes) e a skill `dieta-bot-stitch`: só no histórico do git.

## Leitura dos gates

Os gates citam as telas pelo título do Stitch. `V2 Expressive` = dark, `V2 Light` = light. Títulos ↔ id de gold:

| Gold | Dark (Stitch) | Light (Stitch) |
|---|---|---|
| `splash` | Nutri Splash Screen (V2 Expressive) | Nutri Splash Screen (V2 Light) |
| `o1` | Onboarding 1/4 - Teto do dia (V2 Expressive) | Onboarding 1/4 - Teto do dia (V2 Light) |
| `o1e` | Onboarding 1/4 - Teto do dia sem perfil (V2 Expressive) | Onboarding 1/4 - Teto do dia sem perfil (V2 Light) |
| `o2` | Onboarding 2/4 - Compensação de treinos (V2 Expressive) | Onboarding 2/4 - Compensação de treinos (V2 Light) |
| `o3` | Onboarding 3/4 - Distribuição das refeições (V2 Expressive) | Onboarding 3/4 - Distribuição das refeições (V2 Light) |
| `o3t` | Onboarding 3/4 - Seletor de horário (V2 Expressive) | Onboarding 3/4 - Seletor de horário (V2 Light) |
| `o3s` | Onboarding 3/4 - Refeições Sáb e Dom (V2 Expressive) | Onboarding 3/4 - Refeições Sáb e Dom (V2 Light) |
| `o4` | Onboarding 4/4 - Alvos de macronutrientes (V2 Expressive) | Onboarding 4/4 - Alvos de macronutrientes (V2 Light) |
| `home0` | Home vazia - Day 1 (V2 Expressive Timeline) | Home vazia - Day 1 (V2 Light Timeline) |
| `home1` | Home no dia - 1300 kcal (V2 Expressive Timeline) | Home no dia - 1300 kcal (V2 Light Timeline) |
| `homeX` | Home meta excedida - 2280 / 2000 kcal (V2 Expressive Timeline) | Home meta excedida - 2280 / 2000 kcal (V2 Light Timeline) |
| `homeW` | Home com treino de hoje - Bottom Sheet (V2 Expressive Timeline) | Home com treino de hoje - Bottom Sheet (V2 Light Timeline) |
| `chat0` | Chat vazio (V2 Expressive) | Chat vazio (V2 Light) |
| `chatL` | Chat loading - Estimando (V2 Expressive) | Chat loading - Estimando (V2 Light) |
| `chatE` | Estimate com botões de ação (V2 Expressive) | Estimate com botões de ação (V2 Light) |
| `chatT` | Selecionar refeição - Bottom Sheet (V2 Expressive) | Selecionar refeição - Bottom Sheet (V2 Light) |
| `chatP` | Diálogo de confirmação para pular refeição (V2 Expressive) | Diálogo de confirmação para pular refeição (V2 Light) |
| `chatF` | Foto de refeição e estimativa no Chat (V2 Expressive) | Foto de refeição e estimativa no Chat (V2 Light) |
| `chatG` | Confirmação pós-gravação com recibo duplo-check (V2 Expressive) | Confirmação pós-gravação com recibo duplo-check (V2 Light) |
| `chatA` | Chat com foto anexada (V2 Expressive) | Chat com foto anexada (V2 Light) |
| `chatX` | Chat com texto longo demais (V2 Expressive) | Chat com texto longo demais (V2 Light) |
| `chatR` | Chat com plano de refeição (V2 Expressive) | Chat com plano de refeição (V2 Light) |
| `chatM` | Chat com memória atualizada (V2 Expressive) | Chat com memória atualizada (V2 Light) |
| `chatS` | Chat com sugestão da rotina (V2 Expressive) | Chat com sugestão da rotina (V2 Light) |
| `chatQ` | Chat com pergunta antes da estimativa (V2 Expressive) | Chat com pergunta antes da estimativa (V2 Light) |
| `chatU` | Chat com substituição pendente (V2 Expressive) | Chat com substituição pendente (V2 Light) |
| `chatD` | Chat com registro desfeito (V2 Expressive) | Chat com registro desfeito (V2 Light) |
| `cfg` | Configurações do perfil e dia (V2 Expressive) | Configurações do perfil e dia (V2 Light) |
| `cfgS` | Configurações com refeições por dia (V2 Expressive) | Configurações com refeições por dia (V2 Light) |
| `wipe` | Reiniciar registros de hoje - Diálogo Wipe (V2 Expressive) | Reiniciar registros de hoje - Diálogo Wipe (V2 Light) |
| `push` | Notificação do sistema - Lembrete de refeição (V2 Expressive) | Notificação do sistema - Lembrete de refeição (V2 Light) |
