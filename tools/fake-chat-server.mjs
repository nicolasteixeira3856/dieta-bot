#!/usr/bin/env node
// QA-only fake of POST /v1/chat (no OpenAI). Answers the chatE estimate, suggesting the
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
// A29: POST /__mode {"plan": true} answers the chatR plan (intent "plan", suggested slot "Jan…").
// {"routine": true} answers the usual breakfast (intent "log", slot "Caf…") with memory_updates: a
// routine "cafe" (add, or reinforce of the stored one) and, once, a permanent "leite" preference;
// memory_used echoes every fact id the request carried. /__calls reports the facts it got.
// A30: POST /__mode {"clarify": true} answers a dinner turn with a question only (estimate null,
// `question` on top, like S13) while clarify_rounds < 3 and force_estimate is false; then the
// estimate. /__calls reports the last clarify_rounds and force_estimate.
// A32: POST /__mode {"delay": 4000} answers each turn (not compacts) that many ms later, so a reply
// can land while the thread is scrolled up.
// A34: POST /__mode {"record": "auto"|"ask"|"none"} adds `record` to the estimate answers of a client
// that sent auto_record (S14); {"skip": "Jan"} answers "pulei" as intent "skip" for the slot whose name
// starts with "Jan" (record auto, no estimate); {"reply": "..."} replaces the default reply text.
// /__calls reports the last auto_record.
// Build the app against it: ./gradlew :app:assembleDevDebug -PAPI_PUBLIC_URL=http://10.0.2.2:8765
// Usage: node tools/fake-chat-server.mjs [port]
import http from "http";

const port = Number(process.argv[2] ?? 8765);
let hang = false;
let delayMs = 0;
let fallback = false;
let slotPrefix = "Caf";
let kcalOverride = null;
let plan = false;
let routine = false;
let clarify = false;
let record = null;
let skipPrefix = null;
let replyOverride = null;
let lastAutoRecord = null;
let lastClarify = { rounds: null, force: false };
let lastFacts = [];
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
    delayMs = Number(mode.delay ?? 0);
    fallback = Boolean(mode.fallback);
    slotPrefix = mode.slot ?? "Caf";
    kcalOverride = mode.kcal ?? null;
    plan = Boolean(mode.plan);
    routine = Boolean(mode.routine);
    clarify = Boolean(mode.clarify);
    record = mode.record ?? null;
    skipPrefix = mode.skip ?? null;
    replyOverride = mode.reply ?? null;
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ hang, fallback, slot: slotPrefix, kcal: kcalOverride, plan, routine, clarify, record, skip: skipPrefix, calls }));
    return;
  }
  if (req.method === "GET" && req.url === "/__calls") {
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ calls, compacts, memory: lastMemory, image, requestId: lastRequestId, textLen, facts: lastFacts, clarify: lastClarify, autoRecord: lastAutoRecord }));
    return;
  }
  if (req.method === "POST" && req.url === "/v1/chat") {
    calls++;
    lastRequestId = req.headers["x-request-id"] ?? "";
    if (hang) return; // never answers
    const input = JSON.parse(body || "{}");
    if (delayMs > 0 && !input.compact) await new Promise((r) => setTimeout(r, delayMs));
    lastMemory = input.memory ?? "";
    if (!input.compact) lastFacts = input.facts ?? [];
    if (!input.compact) textLen = [...(input.text ?? "")].length;
    if (!input.compact) lastClarify = { rounds: input.clarify_rounds ?? null, force: Boolean(input.force_estimate) };
    if (!input.compact) lastAutoRecord = input.auto_record ?? null;
    // S14: only a client that sent auto_record gets the mark.
    const marks = (fields) => (input.auto_record === true ? fields : {});
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
    const rounds = input.clarify_rounds;
    if (skipPrefix) {
      const skipped = slots.find((s) => s.name.startsWith(skipPrefix)) ?? slots[0];
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: "Tudo bem, essa refeição não aconteceu hoje.", intent: "skip", estimate: null, digest: null, model: "gpt-6-luna",
        ...marks({ record: "auto", skip_slot: skipped ? skipped.id : null }),
      }));
      return;
    }
    if (clarify && Number.isInteger(rounds) && rounds < 3 && !input.force_estimate) {
      const question = [
        "O molho branco levou creme de leite ou requeijão? E o macarrão, foi 1 prato raso ou fundo?",
        "O frango foi grelhado ou empanado?",
        "O macarrão levou queijo por cima?",
      ][rounds];
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: "Entendi: macarrão com frango ao molho branco." + String.fromCharCode(10) + question,
        intent: "log", estimate: null, question, digest: null, model: "gpt-6-luna",
      }));
      return;
    }
    if (plan) {
      const jantar = slots.find((s) => s.name.startsWith("Jan")) ?? slots[slots.length - 1];
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: [
          "Para caber nas 560 kcal que sobram hoje:",
          "• 1 pão sírio (60 g)",
          "• 2 colheres de sopa de molho de tomate (30 g)",
          "• 100 g de frango desfiado",
          "• 30 g de milho",
          "• 30 g de muçarela",
          "Monte e leve ao forno a 200 °C por 8 a 10 min.",
          "Total: ~420 kcal · 40P · 38C · 12G",
        ].join(String.fromCharCode(10)),
        intent: "plan",
        estimate: {
          kcal: 420, p: 40, c: 38, g: 12, confidence: "high", question: null,
          items: [{ name: "pão sírio", g: 60, kcal: 160 }], suggested_slot: jantar ? jantar.id : null,
          meal_text: "Pizza de pão sírio: 60 g pão sírio, 30 g molho de tomate, 100 g frango, 30 g milho, 30 g muçarela",
        },
        digest: null, model: "gpt-6-luna",
      }));
      return;
    }
    if (routine) {
      const cafe = slots.find((s) => s.name.startsWith("Caf")) ?? slots[0];
      const text = "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café";
      const stored = lastFacts.find((f) => f.category === "routine" && f.key === "cafe");
      const updates = [stored
        ? { op: "reinforce", id: stored.id, kind: stored.kind, category: "routine", key: "cafe", text, slot: cafe.id }
        : { op: "add", id: null, kind: "dynamic", category: "routine", key: "cafe", text, slot: cafe.id }];
      if (!lastFacts.some((f) => f.key === "leite")) {
        updates.push({ op: "add", id: null, kind: "permanent", category: "preference", key: "leite", text: "Usa leite semidesnatado", slot: null });
      }
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: "Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar.",
        intent: "log",
        estimate: {
          kcal: 440, p: 25, c: 38, g: 22, confidence: "high", question: null,
          items: [{ name: "2 ovos mexidos", g: 100, kcal: 180 }], suggested_slot: cafe ? cafe.id : null, meal_text: text,
        },
        memory_used: lastFacts.map((f) => f.id),
        memory_updates: updates,
        digest: null, model: "gpt-6-luna",
      }));
      return;
    }
    const slot = slots.find((s) => s.name.startsWith(slotPrefix)) ?? slots[0];
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
      reply: replyOverride ?? "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:",
      ...(record ? { intent: "log" } : {}),
      estimate: {
        kcal: kcalOverride ?? 380, p: 22, c: 36, g: 16, confidence: "high", question: null,
        items: [{ name: "2 pães franceses", g: 100, kcal: 270 }, { name: "2 ovos mexidos", g: 100, kcal: 110 }],
        suggested_slot: slot ? slot.id : null,
      },
      digest: null,
      model: "gpt-6-luna",
      ...marks(record ? { record, skip_slot: null } : {}),
    }));
    return;
  }
  res.writeHead(404).end();
}).listen(port, () => console.log(`fake /v1/chat on :${port}`));
