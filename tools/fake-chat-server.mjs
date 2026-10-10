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
// A54: POST /__mode {"a54": true} answers an S18 addition into the slot whose name starts with "Jan": intent "log",
// record "auto", meal_change add with base_slot null, the reply rewritten as the server does. /__calls reports
// `day`, the DAY slots of the last turn.
// A59: POST /__mode {"skips": ["Alm"]} lists the slots whose names start with those prefixes as `skip_slots` on the
// answer of a client that sent skip_slots (S29), next to the estimate, the skip or any other answer. /__calls
// reports the last skip_slots flag.
// A60 part A: POST /__mode {"plan_budget": "over"|"still"|"zero"} answers the chatRB recipe (macarrão com atum, 620 kcal)
// to a client that sent plan_budget: over its 310 kcal window with a 250 kcal reservation; a turn with fit_kcal answers the
// adjusted plan, at the target ("over") or still 40 kcal over ("still"); "zero" has no window left (limit_kcal 0).
// /__calls reports the last plan_budget flag and fit_kcal.
// A60 part B: POST /v1/close answers canned seco and duro texts for a day or a week; POST /__mode {"close_fail": true}
// drops the connection (the app's offline state). /__calls reports the last profile tone and the last close request.
// A60 part C: POST /__mode {"format": "plan"|"recipe"|"log"|"malformed"} answers the formatted chatR plan, the chatRK
// recipe, the chatE log with a bold total, or a reply with broken markup.
// A60 part D: POST /__mode {"planned": true} answers a dinner log into the slot whose name starts with "Jan", with the
// server's difference line against the plan the request carried in DAY (`status: planned`).
// A64: /__calls reports `recentDays`, the `recent_days` of the last turn (S33), and `facts` carries their macros.
// A65: POST /__mode {"workout": {"kcal": 450, "mode": "replace"}} answers a workout-only turn (S35: intent "question",
// no estimate, record "auto") to a client that sent workout: true; /__calls reports the last `workout` flag.
// A66: POST /__mode {"actions": "day"|"plan"|"held"} answers typed actions (S36) to a client that sent actions: true:
// "day" = log café 380 + log almoço 640 + skip lanche; "plan" = log jantar 610 + plan lanche 180; "held" = log café 380 +
// a held log of the almoço with its question. /__calls reports the last `actions` flag.
// A67: POST /__mode {"discovery": true} answers a client that sent discovery: true: the first turn (no assistant message in
// HISTORY) asks the routines as - lines; the next proposes a declared breakfast routine with its numbers and an equipment
// fact. /__calls reports the last `discovery` flag.
// A68: POST /__mode {"recipe": "plan"|"log"} answers a client that sent actions: true: "plan" = the chatRK recipe as a plan
// action with its `recipe` (Salvar receita); "log" = a log of the first saved recipe in `recipes` (recipe_id, its totals).
// /__calls reports `recipes` (the index ids) and `recipeFull` (the id sent complete, or null).
// A69: /__calls reports `factTexts`, the text of each fact of the last turn (the correction reaches the prompt).
// A70: POST /__mode {"fixture": "<path>"} answers every chat turn with the `response` of a saved evaluator fixture
// (server `--save-fixture`: {request, response}), its slot ids mapped by name from the fixture's profile to the client's.
// A71: POST /v1/profile (S41) echoes the profile, accepts the goal and answers declared facts built from the `nicolas` persona
// of S40 (air fryer and micro-ondas, the scale at home, no fish, routines for the first and third meal) when the answers
// mention food (`foods` with "ovo"), none otherwise. POST /__mode {"profile_fail": 500, "profile_delay": 3000} answers that
// HTTP status (or 0: drops the connection) after the delay; {"profile_goal_refused": true} refuses the goal. /__calls
// reports `profile`, the last profile request.
// Build the app against it: ./gradlew :app:assembleDevDebug -PAPI_PUBLIC_URL=http://10.0.2.2:8765
// Usage: node tools/fake-chat-server.mjs [port]
import fs from "fs";
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
let a54 = false;
let skipPrefixes = [];
let planBudget = null;
let closeFail = false;
let format = null;
let planned = false;
let lastPlanBudget = null;
let lastFit = null;
let lastTone = null;
let lastClose = null;
let lastMessages = [];
let lastSkipSlots = null;
let lastRecentDays = null;
let workoutMode = null;
let actionsMode = null;
let discoveryMode = false;
let recipeMode = null;
let fixturePath = null;
let profileFail = null;
let profileDelay = 0;
let profileGoalRefused = false;
let lastProfile = null;
let lastRecipes = [];
let lastRecipeFull = null;
let lastDiscovery = null;
let lastActions = null;
let lastWorkout = null;
let lastDay = [];
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
const NL = String.fromCharCode(10);

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
    a54 = Boolean(mode.a54);
    skipPrefixes = Array.isArray(mode.skips) ? mode.skips : [];
    planBudget = mode.plan_budget ?? null;
    closeFail = Boolean(mode.close_fail);
    format = mode.format ?? null;
    planned = Boolean(mode.planned);
    workoutMode = mode.workout ?? null;
    actionsMode = mode.actions ?? null;
    discoveryMode = Boolean(mode.discovery);
    recipeMode = mode.recipe ?? null;
    fixturePath = mode.fixture ?? null;
    profileFail = mode.profile_fail ?? null;
    profileDelay = Number(mode.profile_delay ?? 0);
    profileGoalRefused = Boolean(mode.profile_goal_refused);
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ hang, fallback, slot: slotPrefix, kcal: kcalOverride, plan, routine, clarify, record, skip: skipPrefix, calls }));
    return;
  }
  if (req.method === "GET" && req.url === "/__calls") {
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ calls, compacts, memory: lastMemory, image, requestId: lastRequestId, textLen, facts: lastFacts, clarify: lastClarify, autoRecord: lastAutoRecord, day: lastDay, skipSlots: lastSkipSlots, planBudget: lastPlanBudget, fit: lastFit, tone: lastTone, close: lastClose, messages: lastMessages, recentDays: lastRecentDays, workout: lastWorkout, actions: lastActions, discovery: lastDiscovery, recipes: lastRecipes, recipeFull: lastRecipeFull, factTexts: lastFacts.map((f) => f.text), profile: lastProfile }));
    return;
  }
  if (req.method === "POST" && req.url === "/v1/profile") {
    calls++;
    lastRequestId = req.headers["x-request-id"] ?? "";
    const input = JSON.parse(body || "{}");
    lastProfile = input;
    if (profileDelay > 0) await new Promise((r) => setTimeout(r, profileDelay));
    if (profileFail === 0) {
      req.socket.destroy();
      return;
    }
    if (profileFail) {
      res.writeHead(profileFail, { "content-type": "application/json" }).end(JSON.stringify({ detail: profileFail === 400 ? "content_policy_blocked" : "profile_unavailable" }));
      return;
    }
    const slots = input.slots ?? [];
    const foods = String(input.answers?.foods ?? "");
    const facts = !foods.includes("ovo") ? [] : [
      { kind: "permanent", category: "equipment", key: "air fryer", text: "Tem air fryer e micro-ondas", slot: null, declared: true },
      { kind: "dynamic", category: "routine", key: "café", text: "pão com ovo", slot: slots[0]?.id ?? null, declared: true, kcal: 380, p: 22, c: 34, g: 16 },
      { kind: "dynamic", category: "routine", key: "lanche", text: "iogurte natural com whey e banana", slot: slots[2]?.id ?? null, declared: true, kcal: 330, p: 32, c: 38, g: 5 },
      { kind: "permanent", category: "portion", key: "balança", text: "Em casa pesa na balança; fora estima pelo prato", slot: null, declared: true },
      { kind: "permanent", category: "preference", key: "peixe", text: "Não come peixe", slot: null, declared: true },
    ].filter((f) => f.category !== "routine" || f.slot);
    const notifications = input.notifications ?? { enabled: true, closure_time: null };
    const profile = { ...input, notifications: { enabled: notifications.enabled, closure_time: notifications.enabled ? (notifications.closure_time ?? "22:00") : null } };
    delete profile.goal; delete profile.answers; delete profile.local_time;
    const goal = profileGoalRefused ? null : (input.goal?.weight_kg ? input.goal : null);
    const summary = `Teto ${input.ceiling_kcal} kcal · P ${input.p_target} · C ${input.c_target} · G ${input.g_target}. Refeições: ${slots.map((s) => `${s.name} ${s.time}`).join(", ")}.`;
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
      profile, goal, goal_refused: profileGoalRefused && Boolean(input.goal?.weight_kg), facts, summary, request_id: lastRequestId, model: "gpt-6-luna",
    }));
    return;
  }
  if (req.method === "POST" && req.url === "/v1/close") {
    calls++;
    lastRequestId = req.headers["x-request-id"] ?? "";
    const input = JSON.parse(body || "{}");
    lastClose = input;
    if (closeFail) {
      req.socket.destroy();
      return;
    }
    const n = input.numbers ?? {};
    const duro = input.tone === "duro";
    let text;
    if (input.period === "week") {
      text = duro
        ? "Média de 2.350 kcal com teto de 2.200; o Jantar passou da conta em 4 dias e 2 dias ficaram sem registro." + NL +
          "Semana que vem: jantares de omelete com salada, frango com legumes e peixe com arroz; lanches de iogurte e fruta; teto de 2.200 no fim de semana."
        : "Semana: 16.450 kcal, média de 2.350 de 2.200; proteína média de 128; 2 dias sem registro." + NL +
          "Jantares: omelete com salada, frango com legumes, peixe com arroz.";
    } else {
      text = duro
        ? "Passou 340 kcal do teto: o Jantar levou 980 kcal. Faltaram 42 g de proteína." + NL +
          "Amanhã: ovos no café, frango no almoço e fruta no lanche."
        : `${n.kcal ?? 0} de ${n.ceiling_kcal ?? 0} kcal. Proteína: ${n.p ?? 0} de ${input.profile?.p_target ?? 0} g.`;
    }
    res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({ text, model: "gpt-6-luna" }));
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
    if (!input.compact) lastDay = input.day?.slots ?? [];
    if (!input.compact) lastSkipSlots = input.skip_slots ?? null;
    if (!input.compact) lastRecentDays = input.recent_days ?? null;
    if (!input.compact) lastWorkout = input.workout ?? null;
    if (!input.compact) lastActions = input.actions ?? null;
    if (!input.compact) lastDiscovery = input.discovery ?? false;
    if (!input.compact) lastRecipes = (input.recipes ?? []).map((r) => r.id);
    if (!input.compact) lastRecipeFull = input.recipe_full?.id ?? null;
    if (!input.compact) lastPlanBudget = input.plan_budget ?? null;
    if (!input.compact) lastFit = input.fit_kcal ?? null;
    if (!input.compact) lastMessages = input.messages ?? [];
    lastTone = input.profile?.tone ?? null;
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
    // S29: only a client that sent skip_slots gets the list.
    const listed = () => (input.skip_slots === true
      ? { skip_slots: skipPrefixes.map((p) => slots.find((s) => s.name.startsWith(p))?.id).filter(Boolean) }
      : {});
    if (fixturePath) {
      const fixture = JSON.parse(fs.readFileSync(fixturePath, "utf8"));
      const names = Object.fromEntries((fixture.request?.profile?.slots ?? []).map((s) => [s.id, s.name]));
      const ids = Object.fromEntries(Object.entries(names).map(([id, name]) => [id, slots.find((s) => s.name === name)?.id ?? id]));
      const SLOT_KEYS = new Set(["slot", "suggested_slot", "skip_slot", "skip_slots", "question_slot"]);
      const remap = (v, key) => Array.isArray(v) ? v.map((x) => remap(x, key))
        : v && typeof v === "object" ? Object.fromEntries(Object.entries(v).map(([k, x]) => [k, remap(x, k)]))
        : SLOT_KEYS.has(key) && typeof v === "string" && v in ids ? ids[v] : v;
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify(remap(fixture.response, null)));
      return;
    }
    if (recipeMode && input.actions === true) {
      const jantar = slots.find((s) => s.name.startsWith("Jan")) ?? slots[slots.length - 1];
      const base = { question: null, record_intent: "unsure", meal_day: "today", workout: null, options: null, plan_budget: null, recipe: null, recipe_id: null };
      const saved = (input.recipes ?? [])[0];
      const est = { kcal: 520, p: 46, c: 41, g: 17, confidence: "high", question: null, items: [], suggested_slot: jantar.id, meal_text: "Frango com brócolis e arroz" };
      const action = recipeMode === "log" && saved
        ? { ...base, id: "a1", type: "log", slot: jantar.id, estimate: { ...est, kcal: saved.kcal }, record: "auto", record_intent: "clear",
            meal_change: { operation: "new", base_slot: null, addition: null }, recipe_id: saved.id }
        : { ...base, id: "a1", type: "plan", slot: jantar.id, estimate: est, record: "none", meal_change: null,
            recipe: { name: "Frango com brócolis e arroz", steps: ["Corte o frango em cubos e grelhe por 8 min.", "Refogue o alho no azeite e junte o brócolis por 3 min.", "Misture o arroz e o frango e finalize com o queijo."],
              ingredients: [{ name: "Peito de frango", g: 120, kcal: 198 }, { name: "Arroz cozido", g: 120, kcal: 154 }, { name: "Brócolis", g: 100, kcal: 34 },
                { name: "Azeite", g: 5, kcal: 44 }, { name: "Alho", g: 5, kcal: 7 }, { name: "Queijo ralado (opcional)", g: 15, kcal: 60 }] } };
      const reply = action.type === "log" ? "Jantar: frango com brócolis e arroz, 520 kcal."
        : ["Frango com brócolis e arroz", "| Item | Gramas |", "| --- | --- |", "| Peito de frango | 120 g |", "| Arroz cozido | 120 g |", "| Brócolis | 100 g |",
          "1. Corte o frango em cubos e grelhe por 8 min.", "Total: ~**520 kcal** · 46P · 41C · 17G"].join(NL);
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply, actions: [action], memory_updates: [], memory_used: [], digest: null, model: "gpt-6-luna",
      }));
      return;
    }
    if (discoveryMode && input.discovery === true) {
      const cafe = slots.find((s) => s.name.startsWith("Caf")) ?? slots[0];
      const answered = (input.messages ?? []).some((m) => m.role === "assistant");
      const updates = answered ? [
        { op: "add", id: null, kind: "dynamic", category: "routine", key: "cafe", text: "2 ovos mexidos e 1 pão francês", slot: cafe.id,
          kcal: 320, p: 17, c: 29, g: 16, declared: true },
        { op: "add", id: null, kind: "permanent", category: "equipment", key: "air fryer", text: "Tem air fryer", slot: null,
          kcal: null, p: null, c: null, g: null, declared: false },
      ] : [];
      const reply = answered
        ? ["Vou lembrar:", "- Café: 2 ovos mexidos e 1 pão francês", "- Air fryer"].join(NL)
        : ["Oi! Para eu acertar desde hoje, me conta (pode pular qualquer uma):", "- O que você costuma tomar no café?",
          "- E no almoço?", "- E no jantar?", "- Alguma preferência fixa ou equipamento (leite, whey, air fryer)?"].join(NL);
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply, intent: "question", estimate: null, memory_updates: updates, memory_used: [], digest: null, model: "gpt-6-luna",
        ...marks({ record: "none", skip_slot: null }),
      }));
      return;
    }
    if (actionsMode && input.actions === true) {
      const by = (p, i) => slots.find((s) => s.name.startsWith(p)) ?? slots[i];
      const cafe = by("Caf", 0), almoco = by("Alm", 1), lanche = by("Lan", 2), jantar = by("Jan", slots.length - 1);
      const est = (kcal, slot, text) => ({ kcal, p: 20, c: 30, g: 10, confidence: "high", question: null,
        items: [{ name: text, g: 200, kcal }], suggested_slot: slot.id, meal_text: text });
      const base = { estimate: null, question: null, record: "none", record_intent: "unsure", meal_day: "today", meal_change: null,
        workout: null, recipe_id: null, options: null, plan_budget: null, recipe: null };
      const log = (id, kcal, slot, text) => ({ ...base, id, type: "log", slot: slot.id, estimate: est(kcal, slot, text), record: "auto",
        record_intent: "clear", meal_change: { operation: "new", base_slot: null, addition: null } });
      const answers = {
        day: { reply: "Café, almoço e lanche de hoje: café 380 kcal, almoço 640 kcal, lanche fora.", actions: [
          log("a1", 380, cafe, "2 pães e 2 ovos"), log("a2", 640, almoco, "arroz, feijão e frango"),
          { ...base, id: "a3", type: "skip", slot: lanche.id, record: "auto" }] },
        plan: { reply: "Jantar: 610 kcal. Para o lanche: iogurte natural com banana, 180 kcal.", actions: [
          log("a1", 610, jantar, "frango e arroz"), { ...base, id: "a2", type: "plan", slot: lanche.id, estimate: est(180, lanche, "iogurte natural com banana") }] },
        held: { reply: "Café anotado. E no almoço, quanto de arroz?", actions: [
          log("a1", 380, cafe, "2 pães e 2 ovos"), { ...base, id: "a2", type: "log", slot: almoco.id, question: "Quanto de arroz no almoço?" }] },
      };
      const a = answers[actionsMode] ?? answers.day;
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: a.reply, actions: a.actions, memory_updates: [], memory_used: [], digest: null, model: "gpt-6-luna",
        intent: "log", estimate: a.actions[0].estimate, record: "auto", skip_slot: null, skip_slots: [], meal_change: null,
      }));
      return;
    }
    if (workoutMode && input.workout === true) {
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: `Treino de hoje: ${workoutMode.kcal} kcal.`, intent: "question", estimate: null, digest: null, model: "gpt-6-luna",
        ...marks({ record: "auto", skip_slot: null }), workout: workoutMode,
      }));
      return;
    }
    if (skipPrefix) {
      const skipped = slots.find((s) => s.name.startsWith(skipPrefix)) ?? slots[0];
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: "Tudo bem, essa refeição não aconteceu hoje.", intent: "skip", estimate: null, digest: null, model: "gpt-6-luna",
        ...marks({ record: "auto", skip_slot: skipped ? skipped.id : null }),
        ...listed(),
      }));
      return;
    }
    if (a54) {
      const jantar = slots.find((s) => s.name.startsWith("Jan")) ?? slots[slots.length - 1];
      const text = "2 omeletes de queijo (160 g), feitas com 5 g de manteiga";
      const items = [{ name: "Omelete de queijo", g: 160.0, kcal: 219 }, { name: "Manteiga", g: 5.0, kcal: 44 }];
      const addition = { meal_text: text, kcal: 263, p: 20, c: 1, g: 24, items };
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: `${text}: +263 kcal` + String.fromCharCode(10) + `Total do ${jantar.name}: 263 kcal`,
        intent: "log",
        estimate: { ...addition, confidence: "medium", question: null, suggested_slot: jantar.id },
        digest: null, model: "gpt-6-luna",
        ...marks({ record: "auto", skip_slot: null }),
        meal_change: { operation: "add", base_slot: null, addition },
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
    if (planBudget) {
      const jantar = slots.find((s) => s.name.startsWith("Jan")) ?? slots[slots.length - 1];
      const fit = input.fit_kcal ?? null;
      const kcal = fit == null ? 620 : planBudget === "still" ? fit + 40 : fit;
      const limit = planBudget === "zero" ? 0 : fit ?? 310;
      const reply = fit == null
        ? ["Macarrão com atum ao sugo:", "• 80 g de macarrão cru", "• 1 lata de atum em água (120 g)", "• 150 g de molho de tomate",
          "• 20 g de queijo ralado (opcional)", "1. Cozinhe o macarrão por 9 min.", "2. Aqueça o molho com o atum por 5 min.",
          "3. Misture e finalize com o queijo.", "Total: ~620 kcal · 42P · 70C · 18G"].join(NL)
        : ["Macarrão com atum ajustado:", "• 50 g de macarrão cru", "• 1 lata de atum em água (120 g)", "• 120 g de molho de tomate",
          `Total: ~${kcal} kcal · 36P · 40C · 6G`].join(NL);
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply, intent: "plan",
        estimate: {
          kcal, p: fit == null ? 42 : 36, c: fit == null ? 70 : 40, g: fit == null ? 18 : 6, confidence: "medium", question: null,
          items: [{ name: "macarrão cru", g: 80, kcal: 290 }], suggested_slot: jantar ? jantar.id : null, meal_text: "Macarrão com atum ao sugo",
        },
        digest: null, model: "gpt-6-luna",
        ...marks({ record: "none", skip_slot: null }),
        ...(input.plan_budget === true ? {
          plan_budget: {
            limit_kcal: limit, over_kcal: Math.max(0, Math.ceil(kcal - limit)),
            reserved: fit == null ? [{ label: "fatia de bolo", kcal: 250 }] : [], choice: fit == null ? null : "fit",
          },
        } : {}),
      }));
      return;
    }
    if (format) {
      const jantar = slots.find((s) => s.name.startsWith("Jan")) ?? slots[slots.length - 1];
      const cafe = slots.find((s) => s.name.startsWith("Caf")) ?? slots[0];
      const answers = {
        plan: {
          reply: ["Duas opções para o jantar:",
            "- **Pizza de pão sírio**: 1 pão sírio (60 g), 30 g de molho de tomate, 100 g de frango desfiado, 30 g de milho e 30 g de muçarela · **420 kcal**",
            "- **Omelete de forno**: 3 ovos, 50 g de ricota e 1 fatia de pão integral (25 g) · **360 kcal**",
            "Primeira opção: ~**420 kcal** · 40P · 38C · 12G"].join(NL),
          intent: "plan", kcal: 420, p: 40, c: 38, g: 12, slot: jantar, text: "Pizza de pão sírio",
        },
        recipe: {
          reply: ["Frango com brócolis e arroz", "| Item | Gramas |", "| --- | --- |", "| Peito de frango | 120 g |", "| Arroz cozido | 120 g |",
            "| Brócolis | 100 g |", "| Azeite | 5 g |", "| Alho | 5 g |", "| Queijo ralado (opcional) | 15 g |",
            "1. Corte o frango em cubos e grelhe por 8 min.", "2. Refogue o alho no azeite e junte o brócolis por 3 min.",
            "3. Misture o arroz e o frango e finalize com o queijo.", "Total: ~**520 kcal** · 46P · 41C · 17G"].join(NL),
          intent: "plan", kcal: 520, p: 46, c: 41, g: 17, slot: jantar, text: "Frango com brócolis e arroz",
        },
        log: {
          reply: "Identifiquei 2 pães franceses (100 g) e 2 ovos mexidos (100 g). A estimativa total é de **380 kcal**:",
          intent: "log", kcal: 380, p: 22, c: 36, g: 16, slot: cafe, text: "2 pães franceses e 2 ovos mexidos",
        },
        malformed: {
          reply: ["**Arroz com feijão", "| Item | Gramas |", "| --- | --- |", "| arroz |", "Total: 400 kcal"].join(NL),
          intent: "plan", kcal: 400, p: 14, c: 70, g: 4, slot: jantar, text: "Arroz com feijão",
        },
      };
      const a = answers[format] ?? answers.plan;
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: a.reply, intent: a.intent,
        estimate: {
          kcal: a.kcal, p: a.p, c: a.c, g: a.g, confidence: "high", question: null,
          items: [{ name: a.text, g: 200, kcal: a.kcal }], suggested_slot: a.slot ? a.slot.id : null, meal_text: a.text,
        },
        digest: null, model: "gpt-6-luna",
        ...marks({ record: a.intent === "log" ? (record ?? "ask") : "none", skip_slot: null }),
        ...(a.intent === "log" && input.meal_changes === true ? { meal_change: { operation: "new", base_slot: null, addition: null } } : {}),
      }));
      return;
    }
    if (planned) {
      const jantar = slots.find((s) => s.name.startsWith("Jan")) ?? slots[slots.length - 1];
      const plannedDay = (input.day?.slots ?? []).find((s) => s.id === jantar?.id && s.status === "planned");
      const kcal = 610;
      const diff = plannedDay ? kcal - plannedDay.kcal : null;
      const clause = diff == null ? null : diff > 0 ? `+${diff} kcal sobre o plano.` : diff < 0 ? `−${-diff} kcal abaixo do plano.` : "Igual ao plano.";
      res.writeHead(200, { "content-type": "application/json" }).end(JSON.stringify({
        reply: ["Jantar: 150 g de frango grelhado, 150 g de arroz e salada: **610 kcal** · P 52 g.", clause].filter(Boolean).join(NL),
        intent: "log",
        estimate: {
          kcal, p: 52, c: 60, g: 14, confidence: "high", question: null,
          items: [{ name: "frango grelhado", g: 150, kcal: 240 }, { name: "arroz", g: 150, kcal: 370 }],
          suggested_slot: jantar ? jantar.id : null, meal_text: "150 g de frango grelhado, 150 g de arroz e salada",
        },
        digest: null, model: "gpt-6-luna",
        ...marks({ record: "auto", skip_slot: null }),
        ...(input.meal_changes === true ? { meal_change: { operation: "new", base_slot: null, addition: null } } : {}),
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
      ...listed(),
    }));
    return;
  }
  res.writeHead(404).end();
}).listen(port, () => console.log(`fake /v1/chat on :${port}`));
