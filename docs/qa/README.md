# docs/qa

Padrão oficial de Qualidade Visual e Validação do Dieta Bot.

## Folders

- `figma/dark/` e `figma/light/` — Gold PNGs exportados dos frames do arquivo Figma `Design` (2x, 780 px), a única fonte de UI ([ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md))
- `android/current/dark/` — Screencaps do emulador Android (Dark theme). Só emulador: renders JVM ficam em `build/`.
- `android/current/light/` — Screencaps do emulador Android (Light theme)
- `site/current/dark/` e `site/current/light/` — Capturas do navegador da landing (`land`, `landM`, `priv`), feitas por `npm --prefix web run capture` ([site](../site/README.md))
- `_legacy/` — Telas legadas, wires antigos e os golds e o design system do Google Stitch arquivados (`stitch/`, `stitch-design-system/`; [histórico](../stitch/README.md)). **Nunca comparar contra esta pasta.**

> **Regra estrita:** Nenhum arquivo PNG/JPG pode ficar na raiz de `docs/qa/`.

---

## Golds

Inventário oficial: dono único da lista de golds, igual nos dois temas. O gold de um id está em `docs/qa/figma/{dark,light}/<id>.png`. Quem cria ou remove um gold atualiza esta lista e o mapa de `tools/export-figma.mjs` na mesma entrega; `node tools/check-docs.mjs` (C7) confere que a lista e o mapa dos dois temas são iguais.

```text
splash.png · o1.png · o1e.png · o2.png · o3.png · o3t.png · o3s.png · o4.png
home0.png · home1.png · homeX.png · homeW.png
chat0.png · chatL.png · chatQ.png · chatE.png · chatT.png · chatP.png · chatX.png
chatF.png · chatG.png · chatA.png · chatR.png · chatM.png · chatS.png · chatU.png · chatD.png
cfg.png · cfgS.png · wipe.png · push.png
land.png · landM.png · priv.png
```

A última linha é o site (landing de `fibrai.app`, [ADR-037](../site/adrs/ADR-037-landing-site.md)), não o app: `land` e `priv` são frames desktop de 1440 px (PNG de 2880 px) e `landM` é mobile de 390 px. Eles são comparados com capturas do navegador (Chromium a 2x, página inteira) em `site/current/`, pelo mesmo gate abaixo, sem as faixas de status e navegação; a captura tem a altura exata do gold. As telas dentro dos celulares são o gold do app; a deriva de até 4 px do clone reescalado no Figma é aceita (W1, em [`site/plans/completed/`](../site/plans/completed/)).

O nome base (`<id>.png`) é rigorosamente idêntico em `figma/{dark,light}/` e `android/current/{dark,light}/`.

*Nota sobre a Splash:* É tela de cold start rápido (≤2s), não um travamento. Nunca trate splash visível como crash.

---

## Exportação dos golds

Frames mapeados em `tools/export-figma.mjs` (`DARK_FRAMES` / `LIGHT_FRAMES`, preenchidos pelo plano de design de cada fluxo), exportados a 2x pela API REST do Figma. Precisa de `FIGMA_TOKEN` no ambiente do usuário (token pessoal, escopo File content: Read-only); o script nunca imprime o token.

```bash
node tools/export-figma.mjs --only home0,home1
```

```bash
node tools/check-figma.mjs
```

`--dry-run [--out <pasta>]` exporta para uma pasta temporária, nunca para `docs/qa/`, e aceita também um node id cru (`9:2`) para conferir token e frame. Filtro de ruído: PNG com menos de 0,05% dos pixels com Δ > 40 (`NOISE_MAX_PCT`, `NOISE_DELTA`) volta à versão do git; o log mostra o percentual de cada arquivo.

---

## Loop de Implementação Visual (Padrão Ouro)

A implementação de qualquer tela no client Android deve seguir este ciclo:

1. Garantir que o id está no inventário acima e o Gold PNG em `docs/qa/figma/{dark,light}/<id>.png` (`node tools/check-figma.mjs` falha em PNG com menos de 780 px).
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
   O `diff-gold.mjs` lê o gold de `docs/qa/figma/`.
   Na JVM, sem emulador: `GoldTest` (`./gradlew.bat :app:testDevDebugUnitTest`), lê só `docs/qa/figma/`; renders e máscaras em `apps/android/app/build/outputs/gold/`.
5. Escrever a lista de diffs (layout, tokens, tipo, raio, ButtonGroup, CTA, timeline, macros semânticos) no plano da tela. Posição, tamanho e texto vêm medidos do `android layout --flat -o <scratchpad>/<id>.json` (testTag = `resource-id`, `bounds` na mesma grade de pixels do gold; ler o JSON como UTF-8). Cor, tamanho de fonte e raio vêm da imagem contra o gold: o `layout` não os traz. Elemento que não aparece no `layout`: `android screen capture --annotate` no scratchpad, nunca em `docs/qa/`.
6. Ajustar a UI Compose e repetir 3–5 até passar no gate.

### Gate

- Ambas as imagens borradas (box blur, 3 passes, raio 3 px): diferença de rasterização de glifo não conta (AGENTS: ignorar raster de fonte). Deslocamento de layout, tamanho e cor contam.
- Pixel diverge se o maior delta de canal > 40. Ignora 40 dp do topo (relógio, bateria) e 40 dp da base (nav, pílula home).
- Conteúdo alinhado pelo topo e rodapé (CTA) pela base, cada um com o melhor deslocamento em ±24 dp (altura de status/nav varia por aparelho).
- Presença de conteúdo: tinta da captura entre 0,8× e 1,25× a do gold.
- **Aprovado: ≤ 2%.** Pixel a pixel sem borrão não serve de gate: a splash fica em ~1,2% só por raster.
- Golds que divergem entre si ou do plano ([ADR-027](../android/adrs/ADR-027-golds-divergentes.md), regra até um plano futuro mudá-la): gold de estado (ex.: `o1e`) com geometria diferente do gold base (`o1`) → cada estado segue o próprio gold, o base não muda, refluxo na troca de estado é aceito; dark × light do mesmo gold → um layout com a média dos dois, desde que os dois temas passem no gate (senão: segue o dark e o light fica só reportado); valor normativo do plano/spec/tokens (opacidade, cor) × render do gold → vale o plano, a diferença vai para a lista de diffs; esmaecimento de desabilitado por save layer (`Modifier.disabledAlpha`), não `Modifier.alpha`, que o render JVM não desenha.
- `push` é uma tela de bloqueio desenhada (`Notification/Push`): o app só posta a notificação (ícone, título, ações e accent) e o SystemUI desenha a tela e o cartão. Medido e reportado (`~`), sem bloquear.
- Os golds ([ADR-031](../design/adrs/ADR-031-figma-source-of-truth.md)) são o frame puro, sem barra de status nem de navegação, com a página inteira. Na JVM, `GoldTest` desenha o id na altura do frame, sem insets, e compara a página toda. No emulador, a captura de 844 dp é comparada com o topo do frame; um diálogo centrado no app (`o3t`, `chatP`) é comparado só na caixa do diálogo, alinhada ao centro da captura. `homeW` (sheet no fim da página de 1414 dp): tela só reportada, com gate na Home desfocada do topo (40–450 dp) e no sheet ancorado pela base. Conversas que enchem a tela do aparelho (`chatF`, `chatG`, `chatD`, `chatR`, `chatM`, `chatU`): tela reportada, com gate no cabeçalho (pelo topo) e na cauda da conversa até o composer (pela base); no `chatF` a foto fica fora, porque o recorte da amostra é do próprio frame. `chatS`: a cena semeia outro dia (meta 2.000, 100 %), então a tela é reportada e o gate fica no cabeçalho e no cartão da rotina.

### Regressão

`o3t`: the Figma frame is 780×2536 with `Dialog/TimeWheel` 300 dp from its top, while the app centres the dialog
on the 780×1688 capture. `diff-gold.mjs` aligns the dialog box (gold px 48, 600 – 732, 1388) on the capture centre,
then gates its border, title, five wheel rows and both actions at the same 2% limit and 0.8–1.25 content-presence
ratio. The O3 behind it is checked by `o3`. Gold inputs remain read-only.

Baseline Roborazzi (render JVM contra ele mesmo) em `apps/android/app/src/test/snapshots/`: `recordRoborazziDevDebug` grava, `verifyRoborazziDevDebug` falha em divergência. Não é comparação com o gold.

> **Sem screenshot comparado e validado contra o gold, a UI NÃO está pronta.**
