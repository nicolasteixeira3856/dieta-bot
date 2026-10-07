"""Plan budget (ADR-039): the model reports reservations and the user's choice, the server does the arithmetic."""
from __future__ import annotations

import math
from typing import Any

CHOICES = ("over_ok", "fit")
RESERVED_MAX = 3
LABEL_MAX = 40
RESERVED_KCAL_MAX = 3000
FIT_KCAL_MAX = 5000

SCHEMA: dict[str, Any] = {
    "anyOf": [
        {
            "type": "object",
            "properties": {
                "reserved": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {"label": {"type": "string"}, "kcal": {"type": "number"}},
                        "required": ["label", "kcal"],
                        "additionalProperties": False,
                    },
                },
                "choice": {"type": ["string", "null"], "enum": [*CHOICES, None]},
            },
            "required": ["reserved", "choice"],
            "additionalProperties": False,
        },
        {"type": "null"},
    ]
}


def shape_model_budget(raw: Any) -> dict[str, Any]:
    """Model plan_budget → {reserved, choice}. Invalid entries are dropped, an unknown choice is null."""
    reserved: list[dict[str, Any]] = []
    entries = raw.get("reserved") if isinstance(raw, dict) else None
    for entry in entries if isinstance(entries, list) else []:
        if not isinstance(entry, dict):
            continue
        label, kcal = entry.get("label"), entry.get("kcal")
        if not isinstance(label, str) or isinstance(kcal, bool) or not isinstance(kcal, (int, float)):
            continue
        label = label.strip()
        if not math.isfinite(kcal) or not 1 <= len(label) <= LABEL_MAX:
            continue
        kcal = round(kcal)
        if 1 <= kcal <= RESERVED_KCAL_MAX:
            reserved.append({"label": label, "kcal": kcal})
        if len(reserved) == RESERVED_MAX:
            break
    choice = raw.get("choice") if isinstance(raw, dict) else None
    return {"reserved": reserved, "choice": choice if choice in CHOICES else None}


def plan_kcal(payload: Any) -> float | None:
    """kcal of an in-scope plan for today with an estimate, else None: nothing to check."""
    if not isinstance(payload, dict) or payload.get("intent") != "plan" or payload.get("meal_day") == "other":
        return None
    estimate = payload.get("estimate")
    kcal = estimate.get("kcal") if isinstance(estimate, dict) else None
    if isinstance(kcal, bool) or not isinstance(kcal, (int, float)) or not math.isfinite(kcal) or kcal <= 0:
        return None
    return kcal


def check(
    kcal: float, remaining_kcal: int, model_budget: dict[str, Any], fit_kcal: int | None, *, window: Any = None,
) -> dict[str, Any]:
    """limit = fit_kcal, else the meal window (ADR-043), else remaining minus the stated reservations.

    window: a meal_window.Window; its reservations (computed and stated) replace the model's list. Any excess counts
    as at least 1 kcal over.
    """
    reserved = window.reserved if window is not None else model_budget["reserved"]
    default = window.window_kcal if window is not None else remaining_kcal - sum(r["kcal"] for r in reserved)
    limit = fit_kcal if fit_kcal is not None else default
    return {
        "limit_kcal": limit,
        "over_kcal": max(0, math.ceil(kcal - limit)),
        "reserved": reserved,
        "choice": model_budget["choice"],
    }


def needs_adjustment(budget: dict[str, Any], fit_kcal: int | None) -> bool:
    adjusting = fit_kcal is not None or budget["choice"] == "fit"
    return adjusting and budget["limit_kcal"] >= 1 and budget["over_kcal"] > 0
