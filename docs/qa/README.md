# docs/qa

Padrão oficial de Qualidade Visual e Validação do Nutri.

## Folders

- `stitch/dark/` — Gold PNGs oficiais exportados do projeto Google Stitch `Nutri` (Dark theme, 18 telas)
- `stitch/light/` — Gold PNGs oficiais exportados do projeto Google Stitch `Nutri` (Light theme, 18 telas)
- `android/current/dark/` — Screencaps do emulador Android (Dark theme). Só emulador: renders JVM ficam em `build/`.
- `android/current/light/` — Screencaps do emulador Android (Light theme)
- `_legacy/` — Telas legadas e wires antigos depreciados. **Nunca comparar contra esta pasta.**

> **Regra estrita:** Nenhum arquivo PNG/JPG pode ficar na raiz de `docs/qa/`.

---

## Filenames (18 telas oficiais por tema)

```text
splash.png · o1.png · o2.png · o3.png · o4.png
home0.png · home1.png · homeX.png
chat0.png · chatL.png · chatE.png · chatT.png · chatP.png · chatF.png · chatG.png
cfg.png · wipe.png · push.png
```

O nome base (`<id>.png`) é rigorosamente idêntico em `stitch/{dark,light}/` e `android/current/{dark,light}/`.

*Nota sobre a Splash:* É tela de cold start rápido (≤2s), não um travamento. Nunca trate splash visível como crash.

---

## Exportação das Telas Stitch

Para re-exportar os PNGs gold diretamente do Google Stitch:

```bash
node tools/export-stitch.mjs
```

Para verificar se todas as 36 telas estão presentes e íntegras:

```bash
node tools/check-stitch.mjs
```

---

## Loop de Implementação Visual (Padrão Ouro)

A implementação de qualquer tela no client Android deve seguir este ciclo:

1. Garantir que o Gold PNG da tela está em `docs/qa/stitch/{dark,light}/<id>.png` (`node tools/check-stitch.mjs` falha em miniatura < 780 px).
2. Emulador na geometria do gold (390 dp @ 2x):
   ```bash
   adb shell wm size 780x1688 && adb shell wm density 320
   ```
3. Capturar a tela pelo fluxo real (testTags viram resource-id) em `docs/qa/android/current/{theme}/<id>.png`. Onboarding: `tools/capture-onboarding.sh dark|light`. Home (inclui interações): `tools/capture-home.sh dark|light`. Config (cfg, wipe + checagens de wipe/treino no Room): `tools/capture-config.sh dark|light`. Foto (chatF + galeria, câmera, 16 MB, WebP→JPEG): fake + `tools/capture-photo.sh dark|light`. Push (alarmes reais, Registrar/Pular, tela de bloqueio): `tools/capture-push.sh dark|light` (~6 min). Chat: `node tools/fake-chat-server.mjs` + APK com `-PAPI_PUBLIC_URL=http://10.0.2.2:8765` + `tools/capture-chat.sh dark|light`. Manual:
   ```bash
   adb exec-out screencap -p > docs/qa/android/current/{theme}/<id>.png
   ```
4. Comparar contra o Gold:
   ```bash
   node tools/diff-gold.mjs            # splash + O1..O4, ou: node tools/diff-gold.mjs dark/o1 light/o1
   ```
   Na JVM, sem emulador: `StitchGoldTest` (`./gradlew.bat :app:testDevDebugUnitTest`), renders e máscaras em `apps/android/app/build/outputs/stitch-gold/`.
5. Escrever a lista de diffs (layout, tokens, tipo, raio, ButtonGroup, CTA, timeline, macros semânticos) no plano da tela.
6. Ajustar a UI Compose e repetir 3–5 até passar no gate.

### Gate

- Ambas as imagens borradas (box blur, 3 passes, raio 3 px): diferença de rasterização de glifo não conta (AGENTS: ignorar raster de fonte). Deslocamento de layout, tamanho e cor contam.
- Pixel diverge se o maior delta de canal > 40. Ignora 40 dp do topo (relógio, bateria) e 40 dp da base (nav, pílula home).
- Conteúdo alinhado pelo topo e rodapé (CTA) pela base, cada um com o melhor deslocamento em ±24 dp (altura de status/nav varia por aparelho).
- Presença de conteúdo: tinta da captura entre 0,8× e 1,25× a do gold.
- **Aprovado: ≤ 2%.** Pixel a pixel sem borrão não serve de gate: a splash fica em ~1,2% só por raster.
- Golds que contradizem o canônico do grupo (hoje `home0`, `homeX` × `home1`; `chat0`, `chatL`, `chatG`, `chatF` × `chatE`; `push`, que é tela de bloqueio do sistema) ficam em `GOLD_CONFLICTS`: medidos e reportados (`~`), sem bloquear, até serem regenerados no Stitch. Parte nova de um gold conflitante tem gate por região (`REGIONS` / `region`, melhor deslocamento vertical): hoje, a bolha de foto do chatF (A6).

### Regressão

Baseline Roborazzi (render JVM contra ele mesmo) em `apps/android/app/src/test/snapshots/`: `recordRoborazziDevDebug` grava, `verifyRoborazziDevDebug` falha em divergência. Não é comparação com o gold.

> **Sem screenshot comparado e validado contra o Stitch, a UI NÃO está pronta.**
