# Export gold PNGs

Wire: wires/nutri-wires-expressive.html

## Browser (preferred on Windows)

1. Open the HTML in Edge
2. Click Export PNGs
3. Allow multiple downloads
4. Move splash.png … t3ideia.png into docs/qa/wire/ (if downloads are wire-*.png, strip the wire- prefix)

Unattended: open `wires/nutri-wires-expressive.html?export=all`

Needs network once (html2canvas CDN). If blocked, use the node script.

## Node

```
cd <repo>
npm install --prefix tools
npx --yes playwright install chromium
node tools/export-wires.mjs
```

Writes straight into docs/qa/wire/.
