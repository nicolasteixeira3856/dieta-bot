# Plano — A3 Config + wipe + treino do dia (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A1 + A2 + [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a3-config-wipe-treino.md. Analise e implemente o plano`

## Objetivo

Tela Config. Edita teto, slots, alvos, eat-back, treino hoje. Wipe de hoje se mudar teto.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{cfg,wipe}.png` e `docs/qa/stitch/light/{cfg,wipe}.png`
- spec perfil-onboarding / memoria-push
- ADR-012

## Escopo de implementacao

1. Rota Config. Icone topo dir na Home.
2. Secoes: teto 3 modos, eat-back, alvos P/C/G, slots 2-6, treino hoje NumberField vazio=null.
3. Mudou teto: dialogo Apaga os logs de hoje? O chat fica. Confirmar wipeToday. Cancelar nao salva teto.
4. Mudou nome/hora: saveSlots. Sem wipe.
5. Treino hoje → day.workoutKcal. Rollover SP zera.
6. Back → Home.

## Validacao planejada

- Teste wipeToday. Mudar hora nao chama wipe.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/{cfg,wipe}.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/{cfg,wipe}.png` com diff list aprovada.

## Fora de escopo

Push A7. Chat. Memoria A8.

## Criterios de aceite

- Treino vazio ⇒ credito 0.
- Wipe nao apaga chat_message nem profile.

## Resultado da implementação

### Entregue

- **`feature/config`.** `ConfigScreen` (gold cfg): cabeçalho com voltar, seções "Metas e limites" (meta de calorias com o teto base de hoje, compensação de treinos, P · C · G), "Horários das refeições" (um item por slot) e "Treino de hoje" (kcal + "Crédito atual"), nota do wipe. Cada linha abre um sheet de edição (raio 22 no topo) com os componentes do onboarding: `ModeGroup` + `KcalField` (3 modos), `EatCard` + `PctField`, `MacroCard`, `CountStepper` + `SlotCard` + relógio (2–6 slots, nome obrigatório), `NumberField` do treino. Nada vai ao Room antes de Salvar.
- **Wipe (gold wipe).** Salvar com teto diferente fecha o sheet e abre "Reiniciar registros de hoje?". Confirmar → `DayRepository.changeCeiling` (perfil + `wipeToday` na mesma transação). Cancelar → nada é gravado, o teto antigo fica. Teto igual salva sem diálogo.
- **Sem wipe:** eat-back (`saveEatBack`), alvos (`saveMacroTargets`), slots (`saveSlots`, ids preservados, logs continuam no slot), treino (`setWorkout`; vazio = `null` = crédito 0; a linha `day` é por data SP, então o dia seguinte começa vazio).
- **"Prompt do dia recomeça" (spec regra 5, ADR-012).** `wipeToday` grava em `chat_message` um marcador `role = "wiped"`: nenhuma linha do chat é apagada; `PromptBuilder` só envia raw depois do último marcador; o Chat não desenha o marcador e ele fecha as ações da última estimativa.
- **Navegação.** Engrenagem da Home → `RouteConfig`; back → Home. `ConfigStubScreen` removida.
- **QA.** `tools/capture-config.sh dark|light` (fluxo real + checagens no sqlite do emulador).

### Decisões tomadas na implementação

- **Marcador `wiped` em `chat_message`** em vez de coluna nova: cumpre "Fio do chat de hoje fica na UI e sai do prompt" sem migração de Room.
- **Edição em sheets.** O gold cfg é uma lista com chevrons; os editores não têm gold. Sheets são camadas da tela Config (como o sheet do Chat), não telas novas.
- **Cor secundária no dark.** No gold cfg dark, subtítulos, valores e rótulos são `#c0c7d0` (texto a ~78%), não `muted`. Light usa `muted`, igual ao gold.
- **Detalhe do teto:** "Mesmo valor todos os dias" / "Úteis {a} · fim de semana {b}" / "Valor diferente por dia"; valor = teto base de hoje.

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 132/132. Novos: `ConfigViewModelTest` (linhas refletem o perfil; teto novo pede confirmação e só grava no Confirmar, wipe apaga `meal_log` de hoje e mantém chat, slots e eat-back; Cancelar não salva teto; teto igual não abre diálogo; mudar hora/nome do slot não chama wipe e mantém o `slotId` do log; slot sem nome bloqueia Salvar; treino 400 → crédito 200 com 50%, vazio → `null` → crédito 0; rollover SP zera o treino; eat-back/alvos sem wipe), `RoomV2Test.changeCeiling_storesCeilingAndWipesOnlyToday` (dia anterior, perfil, treino e chat ficam), `PromptBuilderTest` (marcador corta o prompt). `wipeToday_preservesChatProfileSlotsAndWorkout` atualizado para o marcador.
2. Emulador (`wm size 780x1688`, `density 320`), `tools/capture-config.sh dark` e `light`: 22/22 checagens em cada tema — engrenagem abre Config; renomear slot não apaga log nem grava marcador; treino 400 grava e mostra, com política 0% o crédito fica 0; treino vazio → `NULL`; teto 1800 abre o diálogo; Cancelar mantém 2000 e o log; Confirmar grava 1800, `meal_log` de hoje = 0, mensagem do user mantida, 1 marcador, `onboardingDone` = 1; back volta para a Home.
3. Gate visual (borrado ≤ 2% + tinta 0,8–1,25):

| Tela | dark emulador | light emulador | dark JVM | light JVM |
|---|---|---|---|---|
| cfg | 0,40% ✓ | 0,37% ✓ | 1,55% ✓ | 0,61% ✓ |
| wipe | 0,76% ✓ | 1,26% ✓ | 0,93% ✓ | 1,80% ✓ |

4. Splash e O1–O4 recapturados no mesmo fluxo e ainda no gate (0,69–1,80%).

## Lista de diffs (gold × app)

- **Layout:** igual (margem 25 dp, linhas 71/77/77, slots 57, treino 93, nota 72 dp). Gold cfg é captura de página inteira (936 dp dark, 930 dp light); a JVM renderiza nessa altura.
- **Tokens:** fundo `phone` + brilho dourado no topo; grupos `surf2` (dark) / `panel` (light); nota `surf` / `surf2`; valor do teto em `gold` (acento único). Wipe: CTA `bad`, texto `ctaText` no dark e `surf` no light; botão Cancelar `surf2` com borda no dark, `panel` sem borda no light; ícone em quadrado arredondado (dark) / círculo (light), como no gold.
- **Tipo:** título 24,5 sp Jakarta; linha 16 sp; valor 14 sp; detalhe 12 sp; nota 11,5 sp (a fonte do gold é mais estreita que a Inter); corpo do diálogo 13,7 sp. No emulador, o corpo do diálogo quebra uma palavra antes (raster da fonte, ignorado pelo AGENTS).
- **Raio:** cards 16 dp como no gold (AGENTS diz 14; segue o gold, igual ao onboarding); diálogo 24 dp; sheet 22 dp no topo.
- **Sem ButtonGroup/timeline/macros semânticos no gold:** macros coloridos só no sheet de alvos (`MacroCard`).
- **Dados:** relógio e barra de status do emulador.

## Encerramento

Ciclo SDD.
