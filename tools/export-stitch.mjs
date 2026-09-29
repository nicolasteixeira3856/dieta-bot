#!/usr/bin/env node
import path from "path";
import fs from "fs";
import https from "https";
import { fileURLToPath } from "url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const outDark = path.join(root, "docs", "qa", "stitch", "dark");
const outLight = path.join(root, "docs", "qa", "stitch", "light");

export const STITCH_PROJECT_ID = "6282733070135794645";

export const DARK_SCREENS = {
  splash: "5a803993e6834bdeb8e8b8e098a1226b",
  o1: "268a0d329a034b60b78545a722394dc7",
  o2: "c97ade760dcb4c3b8c55a80faab7510f",
  o3: "14440390e2d94582af1efa4e8aa4573b",
  o4: "9b9a9ab5dd9e471f9ff600ef6273cc6a",
  home0: "9e798987e4794692a03d42e2fb7cb249",
  home1: "fad15337390b4638b5d51fdac191a010",
  homeX: "1df72fbb44824b6a861d334672f31c6c",
  homeW: "df7066fb62474c2aa9aea3804c2ba548",
  chat0: "be092fe5db2f40b4bca8da18ae9ce580",
  chatL: "e021044511b44e25b8a8de6433b8dbad",
  chatE: "a91c63f2a624456f9d5e30ef14422578",
  chatT: "8537f8a0e82a4529b3fb0efb9ea89c38",
  chatP: "e64a7a52e1a54259b2e488e7dfdf7413",
  chatF: "ac502aadad004c18bc271e0c74355960",
  chatG: "5e95d8451bf447989bd62a2e9dd38909",
  chatA: "242efa973e9e4bf38f570319af81eed3",
  cfg: "ffb8e640dff34e8b9015e4936f36ebe5",
  wipe: "f2797b013a714413ac252918753dffa9",
  push: "f9c469937daa43849a07deb36ac0610d"
};

export const LIGHT_SCREENS = {
  splash: "7a34dd2615cd4e2d850db7cac3803b5d",
  o1: "9f5820b4990f4c13b4b84a8860209bee",
  o2: "8ab9ffbf98294b9c834d5c7c480f0419",
  o3: "87f1e6f1b9334faeae8f40f6cebf56e5",
  o4: "9e763f0f8a6b4245894f38044e6d48f4",
  home0: "9f2849ad1ae9413f8ef23ae7777051a0",
  home1: "83adedc45ff1441ea398414b7f41dede",
  homeX: "74299a8d5cff46f790fab34d5626b5a7",
  homeW: "cad05772505e480c997b722296b64473",
  chat0: "84f166d46b6f4a6d847a3c64977e07d3",
  chatL: "611e2752cdd04d2fb103c251b6d21c7b",
  chatE: "2811a76b200a447aad6d5233cbb5cce4",
  chatT: "9feb6e7d276a41d6956df9fc001f445a",
  chatP: "66c301014a5f4fdc8b002756faa87396",
  chatF: "35a756967193481fa84c48e05e33e0be",
  chatG: "c226508d6c9b4830952a42005f1cf2f4",
  chatA: "f2ea449a4ed44d439a35115d0ac1bd1e",
  cfg: "d582887ce63242a9ab48b882647ddbb9",
  wipe: "bacd8c4d917040d687ef6a4e9ddedad2",
  push: "038a997a4f2247c9a12da72aea01f73c"
};

// Bare lh3 URLs serve a 226px thumbnail. "=s0" requests the original size.
function fullSize(url) {
  return url.replace(/=[^/]*$/, "") + "=s0";
}

function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, (res) => {
      if (res.statusCode !== 200) {
        return reject(new Error(`Failed to download ${url}: status ${res.statusCode}`));
      }
      res.pipe(file);
      file.on("finish", () => file.close(resolve));
    }).on("error", (err) => {
      fs.unlink(dest, () => reject(err));
    });
  });
}

// Screens re-saved by the Stitch web editor get a 1x JPEG screenshot (390 px, and the renderer may miss the
// web fonts). The gold contract is a 2x PNG of the phone frame (780 px), so those screens are rendered from
// their HTML instead: 390 CSS px phone frame, deviceScaleFactor 2, fonts loaded, clipped to the frame.
function isFullSizePng(file) {
  const b = fs.readFileSync(file);
  const png = b.length > 24 && b.readUInt32BE(0) === 0x89504e47;
  return png && b.readUInt32BE(16) >= 780;
}

let browser = null;
async function renderHtml(htmlUrl, dest, cssHeight) {
  if (!browser) {
    const { chromium } = await import("playwright");
    browser = await chromium.launch({ channel: "chrome" }).catch(() => chromium.launch());
  }
  const html = await (await fetch(htmlUrl)).text();
  // Viewport height = the screen height Stitch declares (2x px / 2): min-h-screen frames take it.
  const page = await browser.newPage({ viewport: { width: 1280, height: cssHeight }, deviceScaleFactor: 2 });
  try {
    await page.setContent(html, { waitUntil: "networkidle" });
    await page.evaluate(() => document.fonts.ready);
    // The phone frame: the largest element 385–415 CSS px wide and at least 700 tall.
    const findFrame = async () => (await page.evaluateHandle(() => {
      let best = null, area = 0;
      for (const el of document.body.querySelectorAll("*")) {
        const r = el.getBoundingClientRect();
        if (r.width >= 385 && r.width <= 415 && r.height >= 700 && r.width * r.height > area) { best = el; area = r.width * r.height; }
      }
      return best;
    })).asElement();
    let frame = await findFrame();
    // Responsive frames (w-full max-w-[412px]) are rendered at the 390 px gold width.
    if (frame && (await frame.boundingBox()).width > 395) {
      await page.setViewportSize({ width: 390, height: cssHeight });
      frame = await findFrame();
    }
    if (!frame) throw new Error("phone frame (≈390 px wide) not found in the screen HTML");
    // Frames taller than the declared screen: grow the viewport to the frame, as the Stitch canvas shows it.
    // Otherwise fixed elements (the Home "Chat" FAB) sit at the short viewport bottom, over the content.
    const tall = Math.ceil((await frame.boundingBox()).height);
    if (tall > cssHeight) {
      await page.setViewportSize({ width: page.viewportSize().width, height: tall });
      frame = await findFrame();
    }
    // Same framing as the Stitch screenshots: the declared screen height, with 20 dp of page above the phone
    // frame when the frame is shorter than the screen (844 dp frame in a 884 dp screen). StitchGoldTest relies on it.
    const box = await frame.boundingBox();
    const top = Math.max(0, box.y - Math.min(20, Math.max(0, cssHeight - box.height)));
    await page.screenshot({ path: dest, fullPage: true, clip: { x: box.x, y: top, width: box.width, height: Math.max(cssHeight, box.height) } });
  } finally {
    await page.close();
  }
}

// Screens whose full-size Stitch screenshot is stale (older than the HTML): always rendered from the HTML.
const RENDER_FROM_HTML = new Set([
  "cad05772505e480c997b722296b64473" // light/homeW: screenshot predates the ST2 row fix
]);

async function exportOne(theme, name, screen, outDir) {
  const dest = path.join(outDir, `${name}.png`);
  await downloadFile(fullSize(screen.screenshot.downloadUrl), dest);
  if (!RENDER_FROM_HTML.has(screen.name.split("/").pop()) && isFullSizePng(dest)) return "screenshot";
  if (!screen.htmlCode?.downloadUrl) throw new Error(`${theme}/${name}: small screenshot and no HTML to render`);
  await renderHtml(screen.htmlCode.downloadUrl, dest, Math.round(Number(screen.height || 1768) / 2));
  return "rendered from HTML (2x)";
}

export async function exportStitch() {
  fs.mkdirSync(outDark, { recursive: true });
  fs.mkdirSync(outLight, { recursive: true });

  const apiKey = process.env.STITCH_API_KEY;
  if (!apiKey) {
    throw new Error("STITCH_API_KEY environment variable is required to fetch screens.");
  }

  console.log(`Fetching screen metadata from Stitch project ${STITCH_PROJECT_ID}...`);
  const res = await fetch(`https://stitch.googleapis.com/v1/projects/${STITCH_PROJECT_ID}/screens?key=${apiKey}`);
  if (!res.ok) {
    throw new Error(`Failed to list screens: ${res.status} ${await res.text()}`);
  }
  const data = await res.json();
  const screensById = new Map();
  for (const s of data.screens || []) {
    const id = s.name.split("/").pop();
    screensById.set(id, s);
  }

  console.log("Exporting Dark screens (20)...");
  for (const [name, id] of Object.entries(DARK_SCREENS)) {
    const screen = screensById.get(id);
    if (!screen || !screen.screenshot?.downloadUrl) {
      throw new Error(`Dark screen ${name} (ID: ${id}) not found or has no screenshot URL.`);
    }
    const how = await exportOne("dark", name, screen, outDark);
    console.log(`  ✓ dark/${name}.png (${id}) ${how}`);
  }

  console.log("Exporting Light screens (20)...");
  for (const [name, id] of Object.entries(LIGHT_SCREENS)) {
    const screen = screensById.get(id);
    if (!screen || !screen.screenshot?.downloadUrl) {
      throw new Error(`Light screen ${name} (ID: ${id}) not found or has no screenshot URL.`);
    }
    const how = await exportOne("light", name, screen, outLight);
    console.log(`  ✓ light/${name}.png (${id}) ${how}`);
  }

  if (browser) await browser.close();
  console.log("\nDone! 40 screens exported to docs/qa/stitch/{dark,light}/");
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) {
  exportStitch().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
