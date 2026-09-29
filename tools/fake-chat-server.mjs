#!/usr/bin/env node
// QA-only fake of POST /v1/chat (no OpenAI). Answers the Stitch chatE estimate, suggesting the
// profile slot whose name starts with "Caf" (else the first). POST /__mode {"hang": true} makes
// /v1/chat never answer (loading state, then the client's 60 s timeout). compact=true answers a
// fixed digest like the real server (S3); empty messages -> 422. GET /__calls also counts compacts
// and echoes the `memory` of the last request (A8) and what came in `image_b64` (A6: bytes, JPEG?).
// POST /__mode {"fallback": true} answers the real server's fallback ("nao deu pra estimar", no
// estimate) to exercise the A11 ChatFallback non-fatal; /__calls echoes the last X-Request-Id.
// A18: /__calls also reports the JPEG width/height (ADR-018, longest side 2048). POST /__mode
// {"slot": "Jan", "kcal": 1220} suggests the slot whose name starts with "Jan" and answers 1220 kcal
// (the replace flow on a taken slot, ADR-017). Every /__mode call resets what it does not name.
// A25: /__calls reports `textLen`, the code points of the last turn text; above 2000 answers 422
// like the server (ADR-022, S9).
// Build the app against it: ./gradlew :app:assembleDevDebug -PAPI_PUBLIC_URL=http://10.0.2.2:8765
// Usage: node tools/fake-chat-server.mjs [port]
import http from "http";

const port = Number(process.argv[2] ?? 8765);
let hang = false;
let fallback = false;
let slotPrefix = "Caf";
let kcalOverride = null;
let lastRequestId = "";
let calls = 0;
let compacts = 0;
let lastMemory = "";
let textLen = 0;
let image = { bytes: 0, jpeg: false, width: 0, height: 0, photos: 0 };
const DIGEST = "Resumo QA: cafe da manha 380 kcal registrado.";

// Width/height from the first SOF marker of a baseline or progressive JPEG.
const jpegSize = (b) => {
  let i = 2;
  while (i + 9 < b.length) {
    if (b[i] !== 0xff) { i++; continue; }
    const marker = b[i + 1];
    if (marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker)) {
      return { height: b.readUInt16BE(i + 5), width: b.readUInt16BE(i + 7) };
    }
    i += 2 + b.readUInt16BE(i + 2);
  }
  return { width: 0, height: 0 };
};

const read = (req) => new Promise((resolve) => {
  let body = "";
  req.on("data", (c) => (body += c));
  req.on("end", () => resolve(body));
});

http.createServer(async (req, res) => {
  const body = await read(req);
  if (req.method === "POST" && req.url === "/__mode") {
    const mode = JSON.parse(body || "{}");
    hang = Boolean(mode.hang);
    fallback = Boolean(mode.fallback);
    slotPrefix = mode.slot ?? "Caf";
    kcalOverride = mode.kcal ?? null;
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ hang, fallback, slot: slotPrefix, kcal: kcalOverride, calls }));
    return;
  }
  if (req.method === "GET" && req.url === "/__calls") {
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ calls, compacts, memory: lastMemory, image, requestId: lastRequestId, textLen }));
    return;
  }
  if (req.method === "POST" && req.url === "/v1/chat") {
    calls++;
    lastRequestId = req.headers["x-request-id"] ?? "";
    if (hang) return; // never answers
    const input = JSON.parse(body || "{}");
    lastMemory = input.memory ?? "";
    if (!input.compact) textLen = [...(input.text ?? "")].length;
    if (textLen > 2000) {
      res.writeHead(422, { "content-type": "application/json" }).end(JSON.stringify({ detail: "text_too_long" }));
      return;
    }
    if (input.image_b64) {
      const bytes = Buffer.from(input.image_b64, "base64");
      const jpeg = bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;
      image = { bytes: bytes.length, jpeg, ...(jpeg ? jpegSize(bytes) : { width: 0, height: 0 }), photos: image.photos + 1 };
    }
    if (input.compact) {
      compacts++;
      if (!input.messages?.length) {
        res.writeHead(422, { "content-type": "application/json" }).end(JSON.stringify({ detail: "compact_needs_messages" }));
        return;
      }
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: "", estimate: null, digest: DIGEST, model: "gpt-6-luna",
      }));
      return;
    }
    if (fallback) {
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: "nao deu pra estimar", estimate: null, digest: null, model: "gpt-6-luna",
      }));
      return;
    }
    const slots = input.profile?.slots ?? [];
    const slot = slots.find((s) => s.name.startsWith(slotPrefix)) ?? slots[0];
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
      reply: "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:",
      estimate: {
        kcal: kcalOverride ?? 380, p: 22, c: 36, g: 16, confidence: "high", question: null,
        items: [{ name: "2 pães franceses", g: 100, kcal: 270 }, { name: "2 ovos mexidos", g: 100, kcal: 110 }],
        suggested_slot: slot ? slot.id : null,
      },
      digest: null,
      model: "gpt-6-luna",
    }));
    return;
  }
  res.writeHead(404).end();
}).listen(port, () => console.log(`fake /v1/chat on :${port}`));
