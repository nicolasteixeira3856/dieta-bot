"""Typed actions per Chat message (ADR-050, S36). Pure functions, no I/O.

The meal-change branch answers with `actions[]`. `normalize` keeps at most six valid actions in the order of
ADR-050 (logs, plans, workouts and recalls as stated; skips last in profile order; a slot both eaten and skipped
is a log). `view` turns one action into the single-estimate payload the rest of the server already shapes
(ADR-042 totals, ADR-032 add/revise, ADR-043 window and closing lines, the protein boost, the clarify and record
gates), so every rule applies per action unchanged.
"""

from __future__ import annotations

from typing import Any, Iterable

TYPES = ("log", "plan", "skip", "workout", "recipe_recall", "question")
MAX_ACTIONS = 6
# The single-estimate intent each action type is shaped as.
INTENT = {"log": "log", "plan": "plan", "skip": "skip", "workout": "question", "recipe_recall": "question",
          "question": "question"}


def _slot_of(action: dict[str, Any]) -> Any:
    estimate = action.get("estimate")
    if isinstance(estimate, dict) and estimate.get("suggested_slot") is not None:
        return estimate.get("suggested_slot")
    return action.get("slot")


def from_legacy(payload: dict[str, Any]) -> list[dict[str, Any]]:
    """A single-estimate payload as actions (benchmark `baseline_view`): the log or plan, a stated workout,
    then one skip per listed slot; a question when nothing else."""
    out: list[dict[str, Any]] = []
    intent = payload.get("intent")
    estimate = payload.get("estimate") if isinstance(payload.get("estimate"), dict) else None
    if intent in ("log", "plan"):
        out.append({"type": intent, "slot": estimate.get("suggested_slot") if estimate else None,
                    "estimate": estimate, "record_intent": payload.get("record_intent"),
                    "meal_day": payload.get("meal_day"), "meal_change": payload.get("meal_change"),
                    "plan_budget": payload.get("plan_budget")})
    if isinstance(payload.get("workout"), dict):
        out.append({"type": "workout", "workout": payload["workout"]})
    for slot in payload.get("skip_slots") or []:
        out.append({"type": "skip", "slot": slot})
    return out or [{"type": "question"}]


def normalize(payload: Any, slot_ids: Iterable[str]) -> list[dict[str, Any]]:
    """The model's actions, valid and ordered, ids a1…; one question action when nothing usable is left."""
    order = list(slot_ids)
    raw = payload.get("actions") if isinstance(payload, dict) else None
    if raw is None and isinstance(payload, dict) and "intent" in payload:
        raw = from_legacy(payload)
    items = [a for a in raw if isinstance(a, dict) and a.get("type") in TYPES] if isinstance(raw, list) else []
    main = [a for a in items if a["type"] != "skip"]
    logged = {_slot_of(a) for a in main if a["type"] == "log"}
    skips: dict[str, dict[str, Any]] = {}
    for action in items:
        slot = action.get("slot")
        if action["type"] == "skip" and slot in order and slot not in logged and slot not in skips:
            skips[slot] = action
    ordered = main + [skips[s] for s in order if s in skips]
    if not ordered:
        ordered = [{"type": "question"}]
    out = []
    for index, action in enumerate(ordered[:MAX_ACTIONS], start=1):
        out.append({**action, "id": f"a{index}"})
    return out


def dropped(payload: Any) -> int:
    """How many valid things the model listed beyond six (the reply names them; the server keeps six)."""
    raw = payload.get("actions") if isinstance(payload, dict) else None
    count = sum(1 for a in raw if isinstance(a, dict) and a.get("type") in TYPES) if isinstance(raw, list) else 0
    return max(0, count - MAX_ACTIONS)


def question_with_others(actions: list[dict[str, Any]]) -> bool:
    """ADR-050 decision 3: a question action never sits next to another action."""
    return len(actions) > 1 and any(a.get("type") == "question" for a in actions)


def view(payload: dict[str, Any], action: dict[str, Any], *, reply: Any, first: bool) -> dict[str, Any]:
    """The single-estimate payload of one action. Memory lists travel with the first action only."""
    kind = action.get("type")
    estimate = action.get("estimate") if kind in ("log", "plan") else None
    return {
        "reply": reply,
        "scope": payload.get("scope"),
        "intent": INTENT.get(kind, "question"),
        "estimate": estimate,
        "record_intent": action.get("record_intent") if kind == "log" else "unsure",
        "meal_day": action.get("meal_day") if kind in ("log", "plan") else "today",
        "meal_change": action.get("meal_change") if kind == "log" else None,
        "skip_slots": [action.get("slot")] if kind == "skip" and action.get("slot") is not None else [],
        "memory_updates": payload.get("memory_updates") if first else [],
        "memory_used": payload.get("memory_used") if first else [],
        "digest": None,
        "plan_budget": action.get("plan_budget") if kind == "plan" else None,
        "workout": action.get("workout") if kind == "workout" else None,
    }
