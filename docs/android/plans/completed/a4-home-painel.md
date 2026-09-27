# Plano — A4 Home painel (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: [A1](a1-room-v2.md) [A2](a2-onboarding-perfil.md) + [home-timeline.md](../../../produto/specifications/home-timeline.md). A3 ainda não existe: ver "Config" abaixo.

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a4-home-painel.md. Analise e implemente o plano aprovado.`

## Objetivo

Home painel oficial Stitch Gold. Sem composer. Consumidas + P/C/G com cores semânticas + data + timeline contínua com refeição consolidada + Config + FAB.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{home0,home1,homeX}.png` e `docs/qa/stitch/light/{home0,home1,homeX}.png`
- spec [home-timeline.md](../../../produto/specifications/home-timeline.md)
- ADR-012

## Escopo de implementacao

1. Remove composer, CTA, chips de janela.
2. Circulo central de saldo/consumo com tipografia de destaque (34pt w590).
3. Linha P/C/G consumido/alvo com cores semânticas de macronutrientes (P: menta, C: âmbar, G: ouro, estouro: bad).
4. Data dd/MM/yyyy SP.
5. Timeline contínua com linha guia vertical e marcadores de nó. Refeições consolidadas em linha única (ex: "520 kcal · 28P · 52C · 22G"). Skip = Pulado. Vazio: "Nenhum registro · Toque para pular".
6. Config topo dir. FAB → Chat.
7. ChatScreen stub scaffold + back. A5 substitui o corpo.
8. T1/T2/T3 rotas ficam. Home nao aponta pra elas.
9. reservedUpcoming = 0.
10. Disclaimer 1 linha: "Estimativa, não consulta."

## Validacao planejada

- test groups by slotId; skip addSkip.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/{home0,home1,homeX}.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/{home0,home1,homeX}.png` com diff list aprovada.

## Fora de escopo

Chat real A5. Foto. Push. Apagar T1/T2/T3.

## Criterios de aceite

- Zero TextField na Home.
- Circulo = soma kcal hoje.
- FAB visivel.
- Tap slot vazio → skip.

## Resultado da implementação

### Entregue

- `feature/home/HomePanel.kt`: estado + `HomePanelMapper` puro (snapshot Room → painel). Meta = teto efetivo do dia (`BudgetCalculator`, crédito de treino, `reservedUpcoming = 0`). Dia `DIA n` (desde `firstDay`), data `d 'de' MMMM` pt-BR SP.
- Timeline: um bloco por slot, ordem da hora. Estados: `LOGGED` (✓; câmera se algum log veio de foto), `OVER` (slot em que o acumulado passou da meta, e os seguintes com log: nó "!", borda/kcal/chip `bad`), `SKIPPED` ("Refeição pulada"), `NEXT` (1º vazio depois do último preenchido: anel gold), `EMPTY`. Logs sem slot → bloco "Outros" no fim. Várias linhas por slot empilham; resumo consolidado `520 kcal · 28P · 52C · 22G`.
- `HomePanelScreen`: sem campo de texto, sem composer/CTA/chips. Anel 204 dp (traço 16) = consumidas/meta; estouro → anel `bad` + "Meta excedida (+N kcal)". P/C/G consumido/alvo com cores semânticas; estouro → valor e barra `bad`. Tap em slot vazio → confirmação "Pular {nome}?" → `addSkip`. Config (engrenagem) e FAB "Chat". Disclaimer com a copy do Stitch.
- `HomePanelViewModel`: `observeToday` → mapper; `skip(slotId)`.
- `feature/stub`: `ChatStubScreen` (A5 substitui) e `ConfigStubScreen` (A3 substitui), só título + voltar.
- Navegação: `RouteHome` → painel; `RouteChat`, `RouteConfig`. `RouteT2` e o código T1/T3 antigo (`HomeViewModel`, `HomeSheets`) ficam, sem link a partir da Home.
- Paleta: papel `onGold` (texto do FAB: `ctaText` no dark, `surf` no light).
- `tools/capture-home.sh`: onboarding + interações + capturas home0/home1/homeX (logs semeados no Room via sqlite até o A5).

### Decisões tomadas na implementação

- **Config.** O plano lista A3 como pré-requisito, mas a matriz ordena A4 antes do A3. A engrenagem abre um stub de Config (mesmo padrão do stub de Chat, item 7); o A3 troca o corpo.
- **Gold canônico = home1.** As três Homes do Stitch são gerações diferentes e se contradizem (ver lista). home1 cobre registrado, foto, pulado e próxima; é a referência. home0 e homeX: estados semânticos implementados (vazio; estouro), estilo segue home1.
- **Data.** Gold: `DIA 1` + `25 de setembro`, não `DD/MM/YYYY` do plano. Segue o gold (spec atualizada).
- **Disclaimer.** Gold usa a copy do Stitch, não "Estimativa, não consulta.".
- **Pular.** Spec: "tap oferece Pular {nome}" → diálogo de confirmação (não é tela nova).
- **Número central.** Consumidas em 36 sp bold (gold + `.stitch/DESIGN.md`). A regra "Remaining 34pt w590" do `AGENTS.md` é da Home antiga (restante); **pendente de decisão do dono** atualizar o `AGENTS.md`.

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: suíte verde (inclui `HomePanelMapperTest` 4 e `StitchGoldTest` 16).
2. `HomePanelMapperTest`: vazio sem destaque; agrupa por slot e empilha; pulado; próxima; foto; estouro no slot que cruza a meta; macros over; "Outros"; meta com crédito de treino; `DIA n`.
3. Emulador (`wm size 780x1688`, `density 320`), `tools/capture-home.sh dark|light`, nos dois temas:
   - tap em slot vazio → "Pular Café da manhã?" → "Pular" → card vira "Refeição pulada", `slot_skip` gravado (data de hoje) e o próximo slot vira `NEXT`;
   - FAB → Chat → voltar → Home; engrenagem → Config → voltar → Home;
   - zero `EditText` na Home.
4. Gate visual (`node tools/diff-gold.mjs`, borrado ≤ 2% + tinta 0,8–1,25):

| Tela | dark emulador | light emulador | dark JVM | light JVM |
|---|---|---|---|---|
| home1 (canônica) | 1,29% ✓ | 1,35% ✓ | 0,92% ✓ | 1,12% ✓ |
| home0 | 3,54% (conflito) | 4,22% (conflito) | 4,94% | 7,11% |
| homeX | 7,23% (conflito) | 9,27% (conflito) | 8,69% | 12,91% |

   home0/homeX são reportadas, não bloqueiam (`GOLD_CONFLICTS` em `StitchGoldTest` e `diff-gold.mjs`) até o gold ser regenerado.
5. Onboarding (A2) segue no gate após as mudanças: splash/O1–O4 0,69–1,80%.

## Lista de diffs (gold × app)

- **Tokens, raio 16 dp, rasterização:** como no A2.
- **FAB:** fica 24 dp acima do gold (respeita a barra de navegação do Android; o gold é iOS sem inset). Rodapé fora da comparação em página inteira.
- **Data do emulador:** "27 de setembro" (hoje); gold "25 de setembro".
- **Conflitos entre golds (decidir no Stitch; hoje segue home1):**
  - home0: data semibold; letra P/C/G colorida e separada do nome regular; nós vazios de 16 dp; chevron na linha do texto; timeline mais junta; FAB com outro rótulo de acessibilidade.
  - homeX: data 24 px; nós ✓ vazados; cards sem divisória, kcal em negrito branco, chip em pílula escura; espaçamentos maiores; card estourado com brilho `bad`.
  - home1 × homeX: nó ✓ preenchido (home1) × vazado (homeX).

## Encerramento

Ciclo SDD.
