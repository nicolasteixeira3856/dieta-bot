# docs/qa

Padrão oficial de Qualidade Visual e Validação do Nutri.

## Folders

- `stitch/dark/` — Gold PNGs oficiais exportados do projeto Google Stitch `Nutri` (Dark theme, 18 telas)
- `stitch/light/` — Gold PNGs oficiais exportados do projeto Google Stitch `Nutri` (Light theme, 18 telas)
- `android/current/dark/` — Capturas de tela do emulador Android (Dark theme)
- `android/current/light/` — Capturas de tela do emulador Android (Light theme)
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

1. Garantir que o Gold PNG da tela está em `docs/qa/stitch/{dark,light}/<id>.png`.
2. Capturar a tela do emulador:
   ```bash
   adb exec-out screencap -p > docs/qa/android/current/{theme}/<id>.png
   ```
3. Listar as diferenças visuais contra o Gold do Stitch:
   - Layout e proporção (390×844)
   - Tokens literais e cores de macronutrientes (Proteína menta `#4ec994`, Carbo âmbar `#e58e42`, Gordura ouro `#e8b86d`, Estouro `#e07a6a`)
   - Tipografia (Plus Jakarta Sans para títulos, Inter para números e corpo)
   - Linha do tempo e nós conectados
   - Raios de borda (Sheet 22dp, cards 16dp)
   - Estilo do FAB e botões
4. Ajustar a UI Compose no Kotlin.
5. Recapturar a tela no emulador.
6. Repetir as iterações até que o emulador esteja equivalente ao Gold do Stitch.

> **Sem screenshot comparado e validado contra o Stitch, a UI NÃO está pronta.**
