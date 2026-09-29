# Plano — A15 Ajustar o app aos golds novos (splash, o3, o4)

- Estado: Pendente aprovação manual (tinta do `diff-gold` dark/o4 no emulador × gold novo; A13/A14 no celular)
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (Compose de splash, o3, o4), `tools/export-stitch.mjs`, golds `docs/qa/stitch/`, docs. Nenhuma regra de produto muda.
- Aprovação: 28/09/2026, `/goal` do dono ("Aprovo os planos … a15 … e … a16 …").
- Pré-requisitos: golds novos exportados (rename do A13 + logo do A14 feitos pelo dono no Stitch web, exportados pelo Gemini em 28/09/2026).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a15-ajuste-visual-golds-novos.md`. Implemente o plano aprovado.

## Objetivo

`StitchGoldTest` e `diff-gold` verdes contra os golds novos, fechando o A13 e o A14 (hoje em `pending_manual_validation/`).

## Estado de partida (não commitado, fica neste plano)

- Golds novos em `docs/qa/stitch/` (28 telas mudaram, 36 PNG 780 px).
- `tools/export-stitch.mjs`: quando o Stitch devolve captura 1x/JPEG (telas salvas pelo editor web), renderiza o HTML da tela no Chrome em 2x, com o enquadramento dos golds antigos.
- Splash: logo de 60 dp dentro de uma caixa de 120 dp, 24 dp acima do wordmark (medido no HTML do gold).
- `StitchGoldTest`: os 2 `@Ignore` de splash foram retirados.
- Resultado: 171 testes, **5 falhas** acima de 2% (blurred):

| Tela | Diferença | Causa observada |
|---|---|---|
| splash light / dark | 2,05% / 2,45% | logo + "Dieta Bot" (posição e peso do wordmark) |
| o3 dark | 2,08% | espaçamento de texto (render Chrome × renderizador antigo) |
| o4 dark / light | 3,19% / 6,51% | textos deslocados ~4 dp a partir do parágrafo de descrição; título "Dieta Bot" mais largo |

## Escopo de implementação

1. Para cada tela: comparar a render JVM (`build/outputs/stitch-gold/render|diff`) com o gold e escrever a lista de diffs (layout, tipo, espaçamento, raio), como manda o `AGENTS.md`.
2. Ajustar o Compose até cada tela ficar ≤ 2%: espaçamentos, alturas de linha e tamanhos (`DietaBotType`/`SplashBoot`/telas de onboarding), sem mudar cores, textos nem regras.
3. Se um ajuste para uma tela quebrar outra (tipografia compartilhada), preferir o ajuste local da tela.
4. Emulador: `capture-onboarding.sh` e `capture-chat.sh` dark/light sem ✗; `diff-gold` splash/o1–o4 ✓; o chat comparado com os golds novos, com a lista de diffs.
5. Roborazzi: regravar só as baselines das telas ajustadas, registrando o motivo.
6. Encerramento: A13 e A14 → `completed/` (validação manual do celular registrada como feita quando o dono confirmar), commit por etapa e push.

## Validação planejada

1. `testDevDebugUnitTest` → 171 testes, **0 falhas, 0 pulados**.
2. `verifyRoborazziDevDebug` verde.
3. Emulador: capturas e `diff-gold` como no item 4.

## Fora de escopo

- Mudar os golds ou o limite de 2% do teste.
- Telas que já passam.

## Riscos e controles

- **Correr atrás de diferença de renderizador:** se uma tela não descer de 2% sem distorcer o layout, parar e voltar ao dono com a lista de diffs (não afrouxar o teste por conta própria).

## Critérios de aceite

- Gate visual verde contra os golds novos; A13 e A14 concluídos.

## Resultados (28/09/2026)

### Lista de diffs e ajustes (px a 2x, render JVM × gold)

| Tela | Diff medido | Ajuste no Compose |
|---|---|---|
| splash (ambos) | logo 24 px acima, wordmark 9 px acima, barra e copy 3 px abaixo; vãos logo→wordmark 137 × 122 px e wordmark→barra 53 × 41 px | caixa do logo `padding(bottom)` 24 → 16 dp; barra `padding(top)` 16 → 10 dp; offset da coluna −12 → −6,5 dp. Cores, textos e tamanhos iguais. |
| o3 (ambos, mesma geometria) | seletor 2–6: contêiner 48 × 46 dp, pílulas 40 × 36 dp, rótulo→seletor 4 dp a mais, seletor→cartões 2 dp a menos; cartão de refeição com 14 × 15 dp de padding (passo 128 × 130 dp) | `SectionLabel(bottom = 10.dp)`, `CountStepper(itemHeight = 36.dp, inset = 5.dp)`, espaço 26 → 28 dp, `SlotCard(pad = 15.dp)`. Parâmetros novos com o valor antigo como padrão: Config não muda. |
| o4 (ambos) | cabeçalho de marca 4 px acima | `OnboardingBar.Brand` topo 5 → 7 dp (só o O4 usa). |
| o4 dark | título (fallback 16 sp do gold) 3 px e descrição 5 px abaixo; cartão de distribuição 2 px acima | título `lineHeight` 20 → 18 sp; espaços em volta do cartão 20 → 21 dp. |
| o4 light | título "Alvos de macronutrientes" ~8% mais largo e mais alto (26 × 24 sp), 3 dp mais alto; cartão de distribuição 2 dp mais alto (barra 1 dp abaixo) | título 26 sp, `titleTop` 8 → 5 dp, `subtitleTop` 8 → 9 dp (parâmetro novo no `ScreenTitle`, padrão 8 dp); cartão com `padding(bottom = 13.dp)` e barra `padding(top = 9.dp)` no light. `lineHeight` menor que a métrica da fonte não muda nada no Compose, por isso o ajuste foi por espaçamento. |

### Validação

1. `StitchGoldTest` (blurred, máx. 2%): splash dark 0,04% / light 0,03%; o3 dark 1,40% / light 0,83%; o4 dark 1,57% / light 1,49%; o1, o2, cfg sem mudança. ✅
2. `verifyRoborazziDevDebug` (roda o `testDevDebugUnitTest`): **171 testes, 0 falhas, 0 pulados**. ✅
3. Roborazzi: só as baselines `splash` dark/light regravadas (motivo: espaçamento do logo e do wordmark acima). O3 e O4 não têm baseline. ✅
4. Emulador (`Medium_Phone`, `wm size 780x1688`, `density 320`, devDebug com o fake em `10.0.2.2:8765`): `capture-onboarding.sh` dark/light com relaunch ✓; `capture-chat.sh` dark/light **0 ✗** (duas execuções por tema).
5. `diff-gold` splash/o1–o4: dark splash 0,42%, o1 1,16%, o2 0,85%, o3 1,07%, light splash 0,41%, o1 0,82%, o2 1,13%, o3 0,81%, o4 1,02% ✓. **dark/o4 ✗ só na tinta: pixel 1,28% (< 2%), tinta 0,76 (< 0,8).** ⏳
6. Chat × golds novos (`diff-gold`): chatE dark 1,50% / light 1,21%, chatT 1,32% / 1,75%, chatP 0,36% / 0,51%, região da foto do chatF 1,98% / 1,66% ✓. O nome "Chat Dieta Bot" / "Dieta Bot AI" agora bate com o gold. Seguem só reportadas (conflitos de gold já registrados no A4/A5, sem gate): chat0 5,16% / 4,92%, chatL 5,59% / 5,15%, chatF inteira 11,24% / 11,11%, chatG 10,37% / 9,15%.

### Pendência (dono)

- **dark/o4, tinta no emulador:** a mesma captura dá 0,85 contra o gold antigo e 0,76 contra o novo; a do A14 (antes deste plano) também dá 0,76. A causa é o gold: no render Chrome do export novo, o texto muted sai `#9ca2aa` e mais grosso, e o token do `AGENTS.md` é `#8b939c` (app e JVM usam o token). Depois do blur, a descrição e a nota caem abaixo do limiar de tinta no emulador. Não mudei cor nem o gate (fora de escopo). Opções do dono: (a) aceitar como diferença de render de fonte, (b) reexportar o o4 dark no Stitch, (c) outro plano para o limiar de tinta.
- A13 e A14: validação manual no celular (ícone, splash e nome). Sobem para `completed/` quando o dono confirmar.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.
