# docs/qa

Padrão oficial de Qualidade Visual e Validação do Dieta Bot.

## Folders

- `figma/dark/` e `figma/light/` — Gold PNGs exportados dos frames do arquivo Figma `Design` (2x, 780 px), para os ids de fonte `figma` ([ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md))
- `stitch/dark/` e `stitch/light/` — Gold PNGs exportados do projeto Google Stitch `Nutri`, congelado, para os ids de fonte `stitch`
- `android/current/dark/` — Screencaps do emulador Android (Dark theme). Só emulador: renders JVM ficam em `build/`.
- `android/current/light/` — Screencaps do emulador Android (Light theme)
- `_legacy/` — Telas legadas e wires antigos depreciados. **Nunca comparar contra esta pasta.**

> **Regra estrita:** Nenhum arquivo PNG/JPG pode ficar na raiz de `docs/qa/`.

---

## Golds

Inventário oficial: dono único da lista de golds, igual nos dois temas. Cada linha começa pela **fonte** dos seus ids (`stitch` ou `figma`): o gold de um id está em `docs/qa/<fonte>/{dark,light}/<id>.png`. A fonte de um fluxo passa a `figma` quando o plano de **client** do fluxo conclui; até lá o app ainda desenha o visual antigo e segue comparado com o gold Stitch ([ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md) § 7). Quem cria, remove ou troca a fonte de um gold atualiza esta lista e o mapa da fonte (`tools/export-stitch.mjs` ou `tools/export-figma.mjs`) na mesma entrega; `node tools/check-docs.mjs` (C7) confere que cada id está no mapa da fonte declarada.

```text
figma: splash.png · o1.png · o1e.png · o2.png · o3.png · o3t.png · o3s.png · o4.png
figma: home0.png · home1.png · homeX.png · homeW.png
stitch: chat0.png · chatL.png · chatE.png · chatT.png · chatP.png · chatF.png · chatG.png · chatA.png · chatX.png
stitch: chatR.png · chatM.png · chatS.png · chatQ.png · chatU.png · chatD.png
stitch: cfg.png · cfgS.png · wipe.png · push.png
```

O nome base (`<id>.png`) é rigorosamente idêntico em `stitch/{dark,light}/`, `figma/{dark,light}/` e `android/current/{dark,light}/`.

*Nota sobre a Splash:* É tela de cold start rápido (≤2s), não um travamento. Nunca trate splash visível como crash.

---

## Exportação dos golds

Figma (fonte `figma`): frames mapeados em `tools/export-figma.mjs` (`DARK_FRAMES` / `LIGHT_FRAMES`, preenchidos pelo plano de design de cada fluxo), exportados a 2x pela API REST do Figma. Precisa de `FIGMA_TOKEN` no ambiente do usuário (token pessoal, escopo File content: Read-only); o script nunca imprime o token.

```bash
node tools/export-figma.mjs --only home0,home1
```

```bash
node tools/check-figma.mjs
```

`--dry-run [--out <pasta>]` exporta para uma pasta temporária, nunca para `docs/qa/`, e aceita também um node id cru (`9:2`) para conferir token e frame. Mesmo filtro de ruído do Stitch: PNG com menos de 0,05% dos pixels mudados volta à versão do git.

Stitch (fonte `stitch`, congelado: sem gates novos): re-exportar e conferir os golds dos fluxos ainda não migrados.

```bash
node tools/export-stitch.mjs
```

```bash
node tools/check-stitch.mjs
```

---

## Loop de Implementação Visual (Padrão Ouro)

A implementação de qualquer tela no client Android deve seguir este ciclo:

1. Achar a fonte do id no inventário acima e garantir que o Gold PNG está em `docs/qa/<fonte>/{dark,light}/<id>.png` (`node tools/check-stitch.mjs` / `node tools/check-figma.mjs` falham em PNG com menos de 780 px).
2. Emulador na geometria do gold (390 dp @ 2x):
   ```bash
   adb shell wm size 780x1688 && adb shell wm density 320
   ```
3. Capturar a tela pelo fluxo real (testTags viram resource-id) em `docs/qa/android/current/{theme}/<id>.png`. Onboarding: `tools/capture-onboarding.sh dark|light`. Home (inclui interações): `tools/capture-home.sh dark|light`. Config (cfg, wipe + checagens de wipe/treino no Room): `tools/capture-config.sh dark|light`. Foto (chatA: anexo sem POST, ✕, troca, sair do Chat; chatF + galeria, câmera, 50 MP com EXIF → 2048 px < 2 MB, arquivo de 16 MB passa, WebP→JPEG): fake + `tools/capture-photo.sh dark|light` (AVD com `-camera-back virtualscene`). Substituir refeição dentro da conversa + Enter (A34, sem captura própria: o gold é o `chatU`): fake + `tools/capture-replace.sh dark|light`. Push (alarmes reais, Registrar/Pular, tela de bloqueio): `tools/capture-push.sh dark|light` (~6 min). Chat: `node tools/fake-chat-server.mjs` + APK com `-PAPI_PUBLIC_URL=http://10.0.2.2:8765` + `tools/capture-chat.sh dark|light` (`SCENES=v2` roda só onboarding + `chatS`, `chatR`, `chatM` do A29; `SCENES=a30` só onboarding + perguntas antes da estimativa, Forçar estimativa, teto de 3 rodadas, `chatQ` e `chatE` do A30, com o fake em `{"clarify": true}`; `SCENES=a32` só onboarding + histórico longo do A32: abre no fim sem rolagem (screenrecord), páginas de 20 até 60 dias, resposta com o fio rolado para cima sem pulo, câmera fecha o teclado; `SCENES=a34` só onboarding + registro autônomo do A34, com o fake em `{"record": "auto"|"ask"|"none"}` e `{"skip": "Jan"}`: registro sem toque, Excluir, Trocar refeição e Desfazer, Substituir dentro da conversa e Desfazer, Editar, Registrar e `Não registrado`, pulo por texto, outro dia sem registro, e `chatG`, `chatU`, `chatD` semeados; muda o relógio do aparelho por `cmd alarm set-time` para gravar o café em 3 dias passados e devolve o relógio automático no fim). Manual, com o Android CLI (skill `android-cli`):
   ```bash
   android screen capture -o docs/qa/android/current/{theme}/<id>.png
   ```
   Alternativa sem o CLI: `adb exec-out screencap -p > docs/qa/android/current/{theme}/<id>.png`.
4. Comparar contra o Gold:
   ```bash
   node tools/diff-gold.mjs            # splash + O1..O4, ou: node tools/diff-gold.mjs dark/o1 light/o1
   ```
   O `diff-gold.mjs` lê o gold da pasta da fonte declarada no inventário.
   Na JVM, sem emulador: `StitchGoldTest` (`./gradlew.bat :app:testDevDebugUnitTest`), renders e máscaras em `apps/android/app/build/outputs/stitch-gold/`.
5. Escrever a lista de diffs (layout, tokens, tipo, raio, ButtonGroup, CTA, timeline, macros semânticos) no plano da tela. Posição, tamanho e texto vêm medidos do `android layout --flat -o <scratchpad>/<id>.json` (testTag = `resource-id`, `bounds` na mesma grade de pixels do gold; ler o JSON como UTF-8). Cor, tamanho de fonte e raio vêm da imagem contra o gold: o `layout` não os traz. Elemento que não aparece no `layout`: `android screen capture --annotate` no scratchpad, nunca em `docs/qa/`.
6. Ajustar a UI Compose e repetir 3–5 até passar no gate.

### Gate

- Ambas as imagens borradas (box blur, 3 passes, raio 3 px): diferença de rasterização de glifo não conta (AGENTS: ignorar raster de fonte). Deslocamento de layout, tamanho e cor contam.
- Pixel diverge se o maior delta de canal > 40. Ignora 40 dp do topo (relógio, bateria) e 40 dp da base (nav, pílula home).
- Conteúdo alinhado pelo topo e rodapé (CTA) pela base, cada um com o melhor deslocamento em ±24 dp (altura de status/nav varia por aparelho).
- Presença de conteúdo: tinta da captura entre 0,8× e 1,25× a do gold.
- **Aprovado: ≤ 2%.** Pixel a pixel sem borrão não serve de gate: a splash fica em ~1,2% só por raster.
- Golds que divergem entre si ou do plano ([ADR-027](../android/adrs/ADR-027-golds-divergentes.md), regra até um plano futuro mudá-la): gold de estado (ex.: `o1e`) com geometria diferente do gold base (`o1`) → cada estado segue o próprio gold, o base não muda, refluxo na troca de estado é aceito; dark × light do mesmo gold → um layout com a média dos dois, desde que os dois temas passem no gate (senão: segue o dark e o light fica só reportado); valor normativo do plano/spec/tokens (opacidade, cor) × render do gold → vale o plano, a diferença vai para a lista de diffs; esmaecimento de desabilitado por save layer (`Modifier.disabledAlpha`), não `Modifier.alpha`, que o render JVM não desenha.
- Golds Stitch que contradizem o canônico do grupo (hoje `chat0`, `chatL`, `chatG`, `chatF`, `chatA` × `chatE`; `push`, que é tela de bloqueio do sistema) ficam em `GOLD_CONFLICTS`: medidos e reportados (`~`), sem bloquear, até serem regenerados no Stitch. Parte nova de um gold conflitante tem gate por região (`REGIONS` / `region`, melhor deslocamento vertical): hoje, a bolha de foto do chatF (A6) e o composer com anexo do chatA (A19; light só reportado: o gold pinta o composer na cor da página). `light/chatQ` (A30): tela só reportada (o gold light desenha as bolhas de pergunta mais justas que o dark a partir do mesmo prompt do ST7; o app segue o dark), com a barra do Forçar estimativa em gate nos dois temas.
- Golds Figma ([ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md)) são o frame puro, sem barra de status nem de navegação, com a página inteira: as exceções acima valem só para ids `stitch`. Na JVM, `StitchGoldTest` desenha o id `figma` na altura do frame, sem insets, e compara a página toda. No emulador, a captura de 844 dp é comparada com o topo do frame. `homeW` (sheet no fim da página de 1414 dp): tela só reportada, com gate na Home desfocada do topo (40–450 dp) e no sheet ancorado pela base.

### Regressão

`o3t`: the Figma frame is 780×2536 with `Dialog/TimeWheel` 300 dp from its top, while the app centres the dialog
on the 780×1688 capture. `diff-gold.mjs` aligns the dialog box (gold px 48, 600 – 732, 1388) on the capture centre,
then gates its border, title, five wheel rows and both actions at the same 2% limit and 0.8–1.25 content-presence
ratio. The O3 behind it is checked by `o3`. Gold inputs remain read-only.

Baseline Roborazzi (render JVM contra ele mesmo) em `apps/android/app/src/test/snapshots/`: `recordRoborazziDevDebug` grava, `verifyRoborazziDevDebug` falha em divergência. Não é comparação com o gold.

> **Sem screenshot comparado e validado contra o gold da fonte do id, a UI NÃO está pronta.**
