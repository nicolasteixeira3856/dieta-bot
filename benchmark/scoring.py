"""Deterministic checks of one model answer against a case's `expect` (benchmark only).

`expect` keys (all optional except summary):
  refusal: "out_of_scope" | "safety_support" | "policy_blocked"   (default: in_scope)
  actions_count: int | [min, max]
  actions: [ {type, slot, kcal_range, kcal_exact, macros_exact [p,c,g], p_min, confidence, confidence_in,
              question: present|absent, estimate: present, record_intent, meal_day, meal_change, base_slot,
              meal_text_has, meal_text_not, meal_text_regex, workout_kcal, workout_mode, recipe_id, options,
              items_min, item_min_g} ]                                   greedy one-to-one match, order-free
  reply_has / reply_not: regex list (re.I | re.M, accent-insensitive)   reply_max_chars   reply_bullets_max
  memory_updates_has / memory_updates_not: [ {op, op_in, id, kind, category, key_has, slot, has_macros} ]
  memory_updates_empty: bool          memory_used_has: [fact ids]
  baseline_unsupported: [check names the current server cannot satisfy by construction]

Every answer of the new arms also passes INVARIANTS (1–6 actions, unique ids, field/type consistency,
unique option ids). `items_sum` (estimate.kcal equals the sum of its items) is recorded as a soft check:
the server recomputes that total (ADR-042), so it is reported, never counted in pass.

The baseline arm (current server schema) is scored through `baseline_view`, which maps its single estimate,
skip_slots and intent to the actions shape. Checks named in `baseline_unsupported`, expected action types the
baseline cannot express (workout, recipe_recall, options, recipe ids) and routine macros are reported as
`unsupported`, so `det_supported` shows execution apart from capability.
"""
from __future__ import annotations

import re
import unicodedata
from typing import Any

FLAGS = re.I | re.M
UNSUPPORTED_BASELINE = {"workout", "recipe_recall"}
MAX_ACTIONS = 6


def _fold(text: str) -> str:
    return "".join(ch for ch in unicodedata.normalize("NFKD", text or "") if not unicodedata.combining(ch))


def _rx(pattern: str, text: str) -> bool:
    return re.search(_fold(pattern), _fold(text), FLAGS) is not None


def _action(id_: str, type_: str, **kw: Any) -> dict[str, Any]:
    base = {"id": id_, "type": type_, "slot": None, "estimate": None, "record_intent": "unsure", "meal_day": "today",
            "meal_change": None, "workout": None, "recipe_id": None, "options": None, "plan_budget": None}
    base.update(kw)
    return base


def baseline_view(raw: dict[str, Any]) -> dict[str, Any]:
    """Current server schema → actions shape."""
    actions: list[dict[str, Any]] = []
    intent = raw.get("intent")
    est = raw.get("estimate") if isinstance(raw.get("estimate"), dict) else None
    if intent in ("log", "plan"):
        actions.append(_action("a1", intent, slot=est.get("suggested_slot") if est else None, estimate=est,
                               record_intent=raw.get("record_intent"), meal_day=raw.get("meal_day"),
                               meal_change=raw.get("meal_change"), plan_budget=raw.get("plan_budget")))
    for s in raw.get("skip_slots") or []:
        actions.append(_action(f"a{len(actions) + 1}", "skip", slot=s))
    if not actions:
        actions.append(_action("a1", "question"))
    return {"reply": raw.get("reply", ""), "scope": raw.get("scope", "in_scope"), "actions": actions,
            "memory_updates": raw.get("memory_updates") or [], "memory_used": raw.get("memory_used") or [], "digest": None}


def _match_action(exp: dict[str, Any], got: dict[str, Any]) -> tuple[bool, str]:
    est = got.get("estimate") if isinstance(got.get("estimate"), dict) else None
    if exp.get("type") and got.get("type") != exp["type"]:
        return False, f"type {got.get('type')}≠{exp['type']}"
    if "slot" in exp and got.get("slot") != exp["slot"]:
        return False, f"slot {got.get('slot')}≠{exp['slot']}"
    if "estimate" in exp and exp["estimate"] == "present" and not est:
        return False, "estimate missing"
    if "kcal_range" in exp:
        if not est:
            return False, "no estimate for kcal_range"
        lo, hi = exp["kcal_range"]
        if not (lo <= float(est.get("kcal", -1)) <= hi):
            return False, f"kcal {est.get('kcal')} not in [{lo},{hi}]"
    if "kcal_exact" in exp:
        if not est or abs(float(est.get("kcal", -1)) - exp["kcal_exact"]) > 0.5:
            return False, f"kcal {(est or {}).get('kcal')}≠{exp['kcal_exact']}"
    if "macros_exact" in exp:
        if not est:
            return False, "no estimate for macros"
        for key, want in zip(("p", "c", "g"), exp["macros_exact"]):
            if abs(float(est.get(key, -1)) - want) > 0.5:
                return False, f"{key} {est.get(key)}≠{want}"
    if "p_min" in exp and (not est or float(est.get("p", 0)) < exp["p_min"]):
        return False, "protein below minimum"
    if "confidence" in exp and (not est or est.get("confidence") != exp["confidence"]):
        return False, f"confidence {est.get('confidence') if est else None}"
    if "confidence_in" in exp and (not est or est.get("confidence") not in exp["confidence_in"]):
        return False, "confidence not in set"
    if "question" in exp:
        if not est:
            return False, "no estimate to carry the question"
        q = est.get("question")
        if exp["question"] == "present" and not q:
            return False, "question expected"
        if exp["question"] == "absent" and q:
            return False, f"unexpected question: {q[:60]}"
    if "record_intent" in exp and got.get("record_intent") != exp["record_intent"]:
        return False, f"record_intent {got.get('record_intent')}"
    if "meal_day" in exp and got.get("meal_day") != exp["meal_day"]:
        return False, f"meal_day {got.get('meal_day')}"
    mc = got.get("meal_change") if isinstance(got.get("meal_change"), dict) else None
    if "meal_change" in exp and (mc or {}).get("operation") != exp["meal_change"]:
        return False, f"meal_change {(mc or {}).get('operation')}≠{exp['meal_change']}"
    if "base_slot" in exp and (mc or {}).get("base_slot") != exp["base_slot"]:
        return False, f"base_slot {(mc or {}).get('base_slot')}"
    text = (est or {}).get("meal_text", "") or ""
    for pat in exp.get("meal_text_has", []):
        if not _rx(pat, text):
            return False, f"meal_text lacks /{pat}/"
    for pat in exp.get("meal_text_not", []):
        if _rx(pat, text):
            return False, f"meal_text has /{pat}/"
    if "meal_text_regex" in exp and not _rx(exp["meal_text_regex"], text):
        return False, "meal_text regex"
    w = got.get("workout") if isinstance(got.get("workout"), dict) else None
    if "workout_kcal" in exp and (not w or abs(float(w.get("kcal", -999)) - exp["workout_kcal"]) > 1):
        return False, f"workout kcal {(w or {}).get('kcal')}"
    if "workout_mode" in exp and (not w or w.get("mode") != exp["workout_mode"]):
        return False, f"workout mode {(w or {}).get('mode')}"
    if "recipe_id" in exp and got.get("recipe_id") != exp["recipe_id"]:
        return False, f"recipe_id {got.get('recipe_id')}≠{exp['recipe_id']}"
    if "options" in exp:
        opts = got.get("options") or []
        if len(opts) != exp["options"]:
            return False, f"options {len(opts)}≠{exp['options']}"
        if len({o.get("id") for o in opts}) != len(opts):
            return False, "option ids repeat"
    items = (est or {}).get("items") or []
    if "items_min" in exp and len(items) < exp["items_min"]:
        return False, f"items {len(items)}<{exp['items_min']}"
    for name, g in (exp.get("item_min_g") or {}).items():
        if not any(_fold(name).lower() in _fold(i.get("name") or "").lower() and float(i.get("g", 0)) >= g for i in items):
            return False, f"item {name} < {g} g"
    return True, "ok"


def invariants(view: dict[str, Any]) -> list[dict[str, Any]]:
    """Structural rules the strict schema cannot express. Hard unless marked soft."""
    out: list[dict[str, Any]] = []
    actions = view.get("actions") or []
    out.append({"name": "inv:actions_1_to_6", "ok": 1 <= len(actions) <= MAX_ACTIONS, "detail": f"{len(actions)} actions", "unsupported": False, "soft": False})
    ids = [a.get("id") for a in actions]
    out.append({"name": "inv:unique_ids", "ok": len(set(ids)) == len(ids), "detail": ",".join(map(str, ids)), "unsupported": False, "soft": False})
    types_ok = True
    detail = []
    for a in actions:
        t = a.get("type")
        est = isinstance(a.get("estimate"), dict)
        if t in ("log", "plan") and not est and a.get("recipe_id") is None:
            types_ok = False
            detail.append(f"{a.get('id')}:{t} without estimate")
        if t not in ("log", "plan") and est:
            types_ok = False
            detail.append(f"{a.get('id')}:{t} with estimate")
        if (t == "workout") != isinstance(a.get("workout"), dict):
            types_ok = False
            detail.append(f"{a.get('id')}:workout field")
        if t != "plan" and a.get("options"):
            types_ok = False
            detail.append(f"{a.get('id')}:options outside plan")
        if t == "question" and len(actions) > 1:
            types_ok = False
            detail.append(f"{a.get('id')}:question next to other actions")
    out.append({"name": "inv:type_fields", "ok": types_ok, "detail": "; ".join(detail) or "ok", "unsupported": False, "soft": False})
    sums_ok = True
    for a in actions:
        est = a.get("estimate") if isinstance(a.get("estimate"), dict) else None
        items = (est or {}).get("items") or []
        if est and items and abs(sum(float(i.get("kcal", 0)) for i in items) - float(est.get("kcal", 0))) > 1:
            sums_ok = False
    out.append({"name": "items_sum", "ok": sums_ok, "detail": "estimate.kcal equals the sum of items (soft, ADR-042 server arithmetic)", "unsupported": False, "soft": True})
    return out


def evaluate(expect: dict[str, Any], view: dict[str, Any], *, baseline: bool = False) -> list[dict[str, Any]]:
    checks: list[dict[str, Any]] = []
    unsupported_names = set(expect.get("baseline_unsupported", [])) if baseline else set()

    def add(name: str, ok: bool, detail: str = "", unsupported: bool = False) -> None:
        base_name = name.split(" ", 1)[0]
        checks.append({"name": name, "ok": bool(ok), "detail": detail, "soft": False,
                       "unsupported": (unsupported or base_name in unsupported_names) and not ok})

    want_scope = expect.get("refusal", "none")
    scope = view.get("scope", "in_scope")
    if want_scope == "none":
        add("scope", scope == "in_scope", f"scope={scope}")
    else:
        add("refusal", scope == want_scope, f"scope={scope} want {want_scope}")

    actions = view.get("actions") or []
    if "actions_count" in expect:
        n = expect["actions_count"]
        lo, hi = (n, n) if isinstance(n, int) else n
        add("actions_count", lo <= len(actions) <= hi, f"{len(actions)} actions, want {n}")

    used: set[int] = set()
    for i, exp in enumerate(expect.get("actions", [])):
        unsupported = baseline and (exp.get("type") in UNSUPPORTED_BASELINE or "options" in exp or "recipe_id" in exp)
        best = "no candidate"
        hit = False
        for j, got in enumerate(actions):
            if j in used:
                continue
            ok, why = _match_action(exp, got)
            if ok:
                used.add(j)
                hit = True
                break
            best = why
        add(f"action[{i}] {exp.get('type', '*')}", hit, "ok" if hit else best, unsupported=unsupported)

    reply = view.get("reply", "") or ""
    for pat in expect.get("reply_has", []):
        add(f"reply_has /{pat}/", _rx(pat, reply))
    for pat in expect.get("reply_not", []):
        add(f"reply_not /{pat}/", not _rx(pat, reply))
    if "reply_max_chars" in expect:
        add("reply_max_chars", len(reply) <= expect["reply_max_chars"], f"{len(reply)} chars")
    if "reply_bullets_max" in expect:
        n = len(re.findall(r"^- ", reply, re.M))
        add("reply_bullets_max", n <= expect["reply_bullets_max"], f"{n} bullets")

    ups = view.get("memory_updates") or []

    def up_match(exp: dict[str, Any], up: dict[str, Any]) -> bool:
        if "op" in exp and up.get("op") != exp["op"]:
            return False
        if "op_in" in exp and up.get("op") not in exp["op_in"]:
            return False
        for k in ("id", "kind", "category", "slot"):
            if k in exp and up.get(k) != exp[k]:
                return False
        if "key_has" in exp and _fold(exp["key_has"]).lower() not in _fold(up.get("key") or "").lower():
            return False
        if exp.get("has_macros") and any(up.get(k) is None for k in ("kcal", "p", "c", "g")):
            return False
        return True

    for i, exp in enumerate(expect.get("memory_updates_has", [])):
        unsupported = baseline and bool(exp.get("has_macros"))
        add(f"memory_updates_has[{i}]", any(up_match(exp, u) for u in ups), str(exp), unsupported=unsupported)
    for i, exp in enumerate(expect.get("memory_updates_not", [])):
        add(f"memory_updates_not[{i}]", not any(up_match(exp, u) for u in ups), str(exp))
    if expect.get("memory_updates_empty"):
        add("memory_updates_empty", not ups, f"{len(ups)} updates")
    usedids = set(view.get("memory_used") or [])
    for fid in expect.get("memory_used_has", []):
        add(f"memory_used_has {fid}", fid in usedids)

    if not baseline:
        checks.extend(invariants(view))
    return checks


def passed(checks: list[dict[str, Any]]) -> bool:
    return all(c["ok"] or c.get("soft") for c in checks)


def passed_supported(checks: list[dict[str, Any]]) -> bool:
    """Pass ignoring the checks a baseline cannot satisfy by construction."""
    return all(c["ok"] or c.get("soft") or c["unsupported"] for c in checks)
