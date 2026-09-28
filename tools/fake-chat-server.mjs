#!/usr/bin/env node
// QA-only fake of POST /v1/chat (no OpenAI). Answers the Stitch chatE estimate, suggesting the
// profile slot whose name starts with "Caf" (else the first). POST /__mode {"hang": true} makes
// /v1/chat never answer (loading state, then the client's 60 s timeout). compact=true answers a
// fixed digest like the real server (S3); empty messages -> 422. GET /__calls also counts compacts
// and echoes the `memory` of the last request (A8) and what came in `image_b64` (A6: bytes, JPEG?).
// POST /__mode {"fallback": true} answers the real server's fallback ("nao deu pra estimar", no
// estimate) to exercise the A11 ChatFallback non-fatal; /__calls echoes the last X-Request-Id.
// Build the app against it: ./gradlew :app:assembleDevDebug -PAPI_PUBLIC_URL=http://10.0.2.2:8765
// Usage: node tools/fake-chat-server.mjs [port]
import http from "http";

const port = Number(process.argv[2] ?? 8765);
let hang = false;
let fallback = false;
let lastRequestId = "";
let calls = 0;
let compacts = 0;
let lastMemory = "";
let image = { bytes: 0, jpeg: false, photos: 0 };
const DIGEST = "Resumo QA: cafe da manha 380 kcal registrado.";

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
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ hang, fallback, calls }));
    return;
  }
  if (req.method === "GET" && req.url === "/__calls") {
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ calls, compacts, memory: lastMemory, image, requestId: lastRequestId }));
    return;
  }
  if (req.method === "POST" && req.url === "/v1/chat") {
    calls++;
    lastRequestId = req.headers["x-request-id"] ?? "";
    if (hang) return; // never answers
    const input = JSON.parse(body || "{}");
    lastMemory = input.memory ?? "";
    if (input.image_b64) {
      const bytes = Buffer.from(input.image_b64, "base64");
      image = { bytes: bytes.length, jpeg: bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff, photos: image.photos + 1 };
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
    const slot = slots.find((s) => s.name.startsWith("Caf")) ?? slots[0];
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
      reply: "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:",
      estimate: {
        kcal: 380, p: 22, c: 36, g: 16, confidence: "high", question: null,
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
