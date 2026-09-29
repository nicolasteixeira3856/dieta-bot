# stitch

## Propósito

Gate de design. Guarda os prompts que o dono executa no Google Stitch `Nutri` (`6282733070135794645`) e a conferência dos golds que eles geram. Regra: [docs/sdd/README.md § Gate Stitch](../sdd/README.md#gate-stitch).

Verificação de um gate, numa rodada: `node tools/verify-stitch.mjs st<n> --report` ([detalhes](#verificação-automática-sv1)).

## Tipo e ownership

- Tipo: gate de design.
- Executor do prompt: **o dono** (passo manual). Verificação: o agente.
- Código: nenhum código de app. Toca só `docs/qa/stitch/`, `tools/export-stitch.mjs`, `tools/check-stitch.mjs`, `tools/verify-stitch.mjs`, os `plans/**/*.checks.json` e a lista de golds (`AGENTS.md`, `docs/qa/README.md`).

## Escopo

- Prompt exato para o Stitch no topo de cada plano, com as instruções extras (telas a selecionar, anexos) logo abaixo.
- Checklist visual do que o prompt precisa produzir.
- Registro dos IDs de tela novos.

## Fora de escopo

- Layout ou comportamento do app: [android](../android/README.md).
- Decisão de produto: [produto](../produto/README.md). Um gate só executa uma decisão já tomada.

## Fronteiras

- Um gate bloqueia um ou mais planos do android. Eles o listam em Pré-requisitos.
- Um gate falho (o Stitch não produziu o esperado) para tudo: o agente reporta o que falta e não segue.

## Nomes das telas (regra do dono, 29/09/2026)

No Stitch, as telas têm título ("Chat vazio (V2 Expressive)"), não o id de gold (`chat0`). Por isso:

1. Toda instrução para o dono (selecionar, duplicar, renomear) e toda tela citada **dentro** de um prompt usa o **título exato do Stitch**, copiado da tabela abaixo. O id de gold (`chatE`) só aparece entre parênteses, para o agente.
2. Tema: `V2 Expressive` = dark, `V2 Light` = light. O prompt vem em dois blocos prontos, um por tema, cada um com os títulos daquele tema.
3. Tela nova: o gate define o título final no mesmo padrão ("… (V2 Expressive)" / "… (V2 Light)"). O dono renomeia a cópia para esse título exato, e o agente acha a tela por ele no `list_screens`.
4. Quando um gate cria ou renomeia uma tela, a tabela abaixo é atualizada na mesma entrega.

| Gold | Dark (Stitch) | Light (Stitch) |
|---|---|---|
| `splash` | Nutri Splash Screen (V2 Expressive) | Nutri Splash Screen (V2 Light) |
| `o1` | Onboarding 1/4 - Teto do dia (V2 Expressive) | Onboarding 1/4 - Teto do dia (V2 Light) |
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
| `cfg` | Configurações do perfil e dia (V2 Expressive) | Configurações do perfil e dia (V2 Light) |
| `cfgS` | Configurações com refeições por dia (V2 Expressive) | Configurações com refeições por dia (V2 Light) |
| `wipe` | Reiniciar registros de hoje - Diálogo Wipe (V2 Expressive) | Reiniciar registros de hoje - Diálogo Wipe (V2 Light) |
| `push` | Notificação do sistema - Lembrete de refeição (V2 Expressive) | Notificação do sistema - Lembrete de refeição (V2 Light) |

Títulos lidos pelo MCP do Stitch (`list_screens`) em 29/09/2026.

## Verificação automática (SV1)

Cada gate tem, ao lado do plano, `plans/<gate>.checks.json` (depois de concluído, em `plans/completed/`). `node tools/verify-stitch.mjs <st1|st2|…>`:

1. Acha cada tela do gate, nos dois temas: pelo ID de `tools/export-stitch.mjs` (e confere o título) ou, para tela nova, pelo título exato da tabela acima.
2. Renderiza o HTML da tela com o mesmo render do exportador (`loadFrame`: 390 px, altura do frame).
3. Roda as checagens e imprime uma linha por checagem (`ok` / `FALHA` / `aviso` + valor medido). Sai com código 1 se alguma falhar.

Checagens declaradas por tela (`screens[].checks`):

| Tipo | Campos | Passa quando |
|---|---|---|
| `text` | `has`, `not` | cada texto de `has` aparece e nenhum de `not` aparece no texto visível (inclui o valor de campos) |
| `fits` | `text`, `with` (opcional), `maxHeight` (opcional) | a linha que contém `text` (e `with`) não passa da borda nem do frame de 390 px, e tem no máximo `maxHeight` px |
| `gap` | `a`, `b`, `min` | a distância entre os dois textos é de pelo menos `min` px |
| `visibleAbove` | `text`, `fixed` | o texto não fica coberto pelo elemento fixo que contém `fixed` (o FAB "Chat") |

Sempre, sem declarar: imagens (`<img>` responde 200), coerência dark × light (mesmos textos visíveis nos dois temas; `coherence.exceptions` para diferença de propósito, `coherence.known` para defeito anterior ao gate, que vira `aviso`) e títulos repetidos no `list_screens` (`aviso`: a API mostra versões ocultas). `keep` (do gate e da tela) entra na lista "keep unchanged" do prompt de correção. `fix` numa checagem troca a frase padrão do prompt.

- `--report [--out <arquivo>]`: exporta as telas do gate para uma pasta temporária e gera um HTML (padrão: `stitch-report.html` na pasta temporária do sistema; nunca em `docs/qa/`) com o gold antigo (git) e o novo lado a lado, recortados na região que mudou, as checagens e, por tela que falhou, o prompt de correção por tema com o título exato.
- `node tools/export-stitch.mjs --only home0,homeW`: exporta só esses golds. Todo export passa pelo filtro de ruído: PNG com menos de 0,05% dos pixels com Δ > 40 (`NOISE_MAX_PCT`, `NOISE_DELTA`) volta à versão do git; o log mostra o percentual de cada arquivo.
- Regressão sem Stitch: `node --test tools/verify-stitch.test.mjs` (fixtures em `tools/fixtures/verify-stitch/`).

## Ciclo de vida

| Situação | Estado | Local |
|---|---|---|
| Esperando o dono rodar o prompt | `Aguardando o dono no Stitch` | `plans/<gate>.md` |
| Dono rodou; agente verificando | `Em verificação` | `plans/<gate>.md` |
| Checklist inteiro passou, golds exportados | `Concluído` | `plans/completed/<gate>.md` |
| Cancelado pelo dono | `Cancelado` | `plans/cancelled/<gate>.md` |

Não há "aprovação" de gate: o prompt já é o que foi decidido nos ADRs. O dono rodar o prompt e avisar o agente é o gatilho da verificação.

## Estado atual

ST1 e ST2 concluídos (29/09/2026). Três gates esperando o dono, cada um já com o seu `checks.json`. Ferramenta [SV1](plans/completed/sv1-verificacao-automatica.md) concluída: verificar com `node tools/verify-stitch.mjs st<n> --report`. Antes de verificar um gate, leia a skill `dieta-bot-stitch` (limites conhecidos do agente e do MCP).

## Índice

### Planos

1. ✅ [ST1 Chat: bolhas, pergunta separada, anexo (`chatA`)](plans/completed/st1-chat.md) — concluído; libera [A19](../android/plans/a19-chat-visual.md).
2. ✅ [ST2 Home: atalho de treino (`homeW`)](plans/completed/st2-home-treino.md) — concluído; libera [A22](../android/plans/a22-treino-home.md).
3. ✅ [ST3 Seletor de horário em rodas (`o3t`)](plans/completed/st3-seletor-horario.md) — concluído; libera [A21](../android/plans/a21-seletor-horario.md).
4. ✅ [ST4 Refeições por dia da semana (`o3s`, `cfgS`)](plans/completed/st4-refeicoes-por-dia.md) — concluído; libera [A24](../android/plans/a24-refeicoes-por-dia.md).
5. ✅ [ST5 Chat: texto longo demais no composer (`chatX`)](plans/completed/st5-chat-texto-longo.md) — concluído; libera [A25](../android/plans/a25-limite-texto-composer.md).
6. ✅ [SV1 Verificação automática dos gates](plans/completed/sv1-verificacao-automatica.md) — concluído; `tools/verify-stitch.mjs`, usado na verificação do ST3–ST5.

ADRs que os gates executam: [ADR-020](../produto/adrs/ADR-020-estados-novos-chat-home-horario.md), [ADR-021](../produto/adrs/ADR-021-refeicoes-por-dia.md), [ADR-022](../produto/adrs/ADR-022-limite-texto-chat.md).
