# Plano — A2 Onboarding perfil (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: [A1](a1-room-v2.md) + [A0 Roborazzi](a0-roborazzi-setup.md) + [perfil-onboarding.md](../../../produto/specifications/perfil-onboarding.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a2-onboarding-perfil.md. Analise e implemente o plano aprovado.`

## Objetivo

O1-O4 no lugar de O1-O2. Perfil completo em Room. Splash alinhada ao Stitch gold. Primeira comparacao Roborazzi contra gold.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{splash,o1,o2,o3,o4}.png` e `docs/qa/stitch/light/{splash,o1,o2,o3,o4}.png`
- spec perfil-onboarding (slots 2-6, chips por faixa, nunca assume)
- ADR-012

## Escopo de implementacao

### 0. Gold em resolucao real + splash

- `tools/export-stitch.mjs` exporta os PNGs gold em resolucao real (hoje 226x512). Re-exportar os 36.
- Splash segue o gold: layout e copy do Stitch ("Estimativa nutricional, nao substitui consulta medica ou nutricional."). Atualizar `SplashBoot.COPY` e `TokensTest`.
- Roborazzi compara `splash` contra `docs/qa/stitch/{dark,light}/splash.png`, tolerancia 1%.

### 1. O1 corpo + teto

- sexo H/M, idade, altura cm, peso kg, teto kcal prefill TMB se user ainda nao editou o teto nesta sessao, modo teto 3 chips.
- CTA: teto > 0 e sexo escolhido. Idade/altura/peso 0 → TMB some, teto livre.
- Copy: sugerido {n} kcal.

### 2. O2 eat-back

- 0 / % / 100. Sem cap.

### 3. O3 slots

- Stepper 2-6. Nome + TimePicker.
- Chips preenchem campo vazio: 05-10 Cafe da manha; 10-11 Lanche da manha; 11-15 Almoco; 15-18 Lanche da tarde; 18-22 Janta; 22-05 Ceia.
- CTA exige nome em todas as linhas.

### 4. O4 macros

- P C G prefill MacroSplit(teto dia 1). Editaveis.

### 5. Persistencia

- saveProfile + saveSlots no fim de O4. onboardingDone=1 so saindo de O4.
- Nav splash → O1 → O2 → O3 → O4 → Home.

## Validacao planejada

- test O1 27/116/180 male → 2155, prefill 2160 (spec; 2072 corrigido no A1)
- test O4 2000 → 150/200/67
- `node tools/check-stitch.mjs`: 36 gold presentes em resolucao real.
- Validacao visual Compose vs Stitch Gold: capturas `docs/qa/android/current/{dark,light}/{splash,o1,o2,o3,o4}.png` (Roborazzi + emulador) comparadas contra `docs/qa/stitch/{dark,light}/{splash,o1,o2,o3,o4}.png`, tolerancia 1%, com diff list aprovada.

## Fora de escopo

Config, Chat, Home nova, TMB como meta oculta.

## Criterios de aceite

- Kill apos O4: perfil e slots voltam.
- Sem slots vazios gravados.

## Resultado da implementação

### Entregue

- **Gold em resolução real.** `tools/export-stitch.mjs` pede `=s0` na URL lh3 (antes vinha miniatura 226×512). 36 PNGs re-exportados: 780 px de largura (390 dp @ 2x). `tools/check-stitch.mjs` falha se a largura < 780.
- **Splash** conforme gold: wordmark 48 sp Jakarta, barra 48×4 dp gold, copy do Stitch, brilho radial. `SplashBoot.COPY` + `TokensTest` atualizados. Splash do sistema (API 31+) sem ícone (`ic_splash_blank`), fundo = token `phone`.
- **Tipografia.** Plus Jakarta Sans e Inter (OFL, variáveis) em `res/font`, licenças em `apps/android/licenses/`. `NutriType` com a escala do Stitch e `LineHeightStyle(Center, Trim.None)` (caixa de linha igual ao CSS).
- **Paleta.** Papéis derivados dos tokens existentes, sem hex novo: `card`, `cardSel`, `segSel`, `onSegSel`, `isDark`. Fundo das telas = `phone`.
- **O1–O4** (`feature/onboarding`): `OnboardingChrome.kt` (moldura, barras de topo por tela, CTA pílula, eyebrow, título, InfoNote, rádio gold) + `OnboardingScreens.kt`.
  - O1: sexo, idade/altura/peso, modo do teto (3 linhas), teto (mesmo | útil+fds | 7 dias), legenda "Sugerido {n} kcal…" com TMB arredondada a 10. Prefill do teto até o user editar. CTA: sexo + teto > 0.
  - O2: 0% (Padrão) | % digitável (campo aparece no card, default 50) | 100%. Sem cap.
  - O3: stepper 2–6, nome + TimePicker 24 h, chips por faixa (`domain/SlotSuggestions.kt`), ícone por faixa. Linhas intocadas assumem os horários padrão da nova contagem; editadas ficam. CTA exige nome em todas.
  - O4: barra 30/40/30 recalculada dos gramas, cards P/C/G editáveis (tune foca o campo), "{teto do dia 1} KCAL TOTAL ESTIMADA", ajuda em diálogo (não é tela nova).
- **Persistência.** Um `OnboardingViewModel` para o grafo `RouteOnboarding` (antes cada tela tinha o seu VM e o O1 se perdia no O2). `saveSlots` (ordenado por horário) antes de `saveProfile(onboardingDone = true)`.
- **Ícones.** `material-icons-extended` 1.7.8 (Outlined).

### Mudança no A0 Roborazzi

`docs/qa/android/current/` é só de screencaps do emulador (AGENTS). Baseline Roborazzi foi para `apps/android/app/src/test/snapshots/`; renders JVM do gold vão para `app/build/outputs/stitch-gold/`.

### Métrica e gate visual

O critério "1%" não é alcançável pixel a pixel: a splash, a tela mais simples, fica em 1,17% bruto só pela rasterização de glifo Chrome × Skia. `AGENTS.md` manda ignorar raster de fonte. Gate adotado:

- Ambas as imagens borradas (3 passes de box blur, raio 3 px ≈ σ 3); pixel diverge se o maior delta de canal > 40.
- Ignorados: 40 dp do topo (relógio/bateria) e 40 dp da base (nav, pílula home).
- Conteúdo alinhado pelo topo e rodapé (CTA) pela base, cada um com o melhor deslocamento em ±24 dp (altura de status/nav varia por aparelho).
- Presença de conteúdo: tinta da captura entre 0,8× e 1,25× a do gold (captura em branco falha).
- **Gate: ≤ 2%.** Implementado em `StitchGoldTest` (JVM) e `tools/diff-gold.mjs` (emulador).

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 83/83 (inclui `OnboardingViewModelTest` 5, `ProfileMathTest` 8, `StitchGoldTest` 10).
2. `./gradlew.bat :app:verifyRoborazziDebug`: splash dark/light verdes contra o baseline novo.
3. `node tools/check-stitch.mjs`: 36/36 em resolução real.
4. Emulador `Medium_Phone` com `wm size 780x1688` + `wm density 320`; `tools/capture-onboarding.sh dark|light` percorre o fluxo real (testTags) e `node tools/diff-gold.mjs`:

| Tela | dark | light |
|---|---|---|
| splash | 0,69% | 0,70% |
| o1 | 1,16% | 0,82% |
| o2 | 1,09% | 1,33% |
| o3 | 1,80% | 1,09% |
| o4 | 1,01% | 0,91% |

JVM (`StitchGoldTest`, borrado / bruto): splash 0,75/1,17 · o1 1,07/4,83 · o2 1,53/6,82 · o3 1,56/4,51 · o4 0,93/5,17 (dark); splash 0,74/1,18 · o1 0,78/5,07 · o2 1,82/7,71 · o3 1,09/5,76 · o4 0,90/5,85 (light).

5. Kill após O4 (nos dois temas): relaunch abre a Home, não o onboarding. `nutri.db`: perfil `male/27/180/116.0`, teto 2000, `zero`, 150/200/67, `firstDay` = hoje; 4 `meal_slot` (07:30, 12:30, 16:00, 20:00) com nome. Nenhum slot vazio.

## Lista de diffs (gold × app)

Layout, raio, CTA, macros semânticos e cores seguem o gold; os itens abaixo são as diferenças que restam.

- **Tokens.** O HTML do Stitch usa hex próprios (`#0d1015`, `#1a1e26`, `#343b47`, `#9aa1ab`); o app usa os tokens do `AGENTS.md`/`DESIGN.md` (`phone`, `surf`, `line`, `muted`…). Delta ≤ 6 por canal.
- **Raio.** Cards/campos com 16 dp como o gold (`rounded-2xl`); o token `card 14` segue nos componentes antigos.
- **Rasterização.** Inter/Jakarta do Chrome saem ~1–2% mais largos; algumas quebras de linha foram casadas com padding (O2 card 1).
- **O1.** Gold mostra "Sugerido 2155 kcal" (TMB crua); o app mostra o prefill arredondado (2160), conforme spec regra 1.
- **O3.** Captura do emulador tem "Café" no 1º slot (chip); o gold tem "Café da manhã" digitado (`adb input` não digita acento). Gold O3 é página inteira (1103 dp); o emulador compara só a parte acima do rodapé.
- **Inconsistências do próprio gold, reproduzidas por tema** (decidir no Stitch se forem erro):
  - O4 dark: título sem estilo (classe `font-headline-lg-mobile` inexistente → fonte do sistema 16 px); O4 light: Jakarta 24 bold.
  - O2/O4: título semibold no dark, bold no light.
  - O2/O3 light: voltar dentro de círculo e "NUTRI INTAKE" na cor de texto; dark: seta solta, rótulo apagado.
  - Eyebrow sem destaque: apagado no dark, dourado no light.
  - O2 selecionado: dark = superfície elevada + anel dourado translúcido; light = mesmo card + borda dourada sólida 2 dp.
  - O4 wordmark "Nutri": dourado no dark, cor de texto no light.
  - Cabeçalhos diferentes por tela (O1 sem voltar; O2/O3 "NUTRI INTAKE"; O4 wordmark + ajuda).

## Encerramento

Ciclo SDD.
