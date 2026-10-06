"""Estimate total as server arithmetic over the items (ADR-042, S23).

The model names the foods, their grams and their energy; the server adds. Pure functions, no I/O.
"""

from __future__ import annotations

import math
import re
from decimal import ROUND_HALF_UP, Decimal
from typing import Any

MACROS = ("p", "c", "g")


def _finite(value: Any) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        return None
    return float(value)


def rounded(value: float) -> int:
    """Round once to a whole number, ties upward (same rule as the meal-change contract)."""
    return int(Decimal(repr(value)).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def items_total(items: Any) -> int | None:
    """Sum of the rounded item kcal, or None when no item carries a usable positive energy.

    Items with a nonfinite or negative kcal, or nonpositive grams, do not count.
    """
    if not isinstance(items, list):
        return None
    total = 0
    counted = False
    for item in items:
        if not isinstance(item, dict):
            continue
        kcal = _finite(item.get("kcal"))
        grams = _finite(item.get("g"))
        if kcal is None or kcal < 0 or grams is None or grams <= 0:
            continue
        total += rounded(kcal)
        counted = counted or kcal > 0
    return total if counted else None


def apply(estimate: Any) -> dict[str, int] | None:
    """Make estimate.kcal the sum of its items, in place; scale p, c and g in the same ratio.

    Item kcal are rounded to whole numbers in place as well. Returns {model_kcal, items_kcal} when the
    total changed, None when nothing applied (no estimate, no usable items or an equal total).
    """
    if not isinstance(estimate, dict):
        return None
    total = items_total(estimate.get("items"))
    if total is None:
        return None
    for item in estimate["items"]:
        if isinstance(item, dict):
            kcal = _finite(item.get("kcal"))
            if kcal is not None and kcal >= 0:
                item["kcal"] = rounded(kcal)
    model_kcal = _finite(estimate.get("kcal"))
    if model_kcal is not None and rounded(model_kcal) == total and model_kcal == total:
        return None
    ratio = total / model_kcal if model_kcal is not None and model_kcal > 0 else None
    for key in MACROS:
        value = _finite(estimate.get(key))
        if value is None or value < 0:
            continue
        estimate[key] = rounded(value * ratio) if ratio is not None else rounded(value)
    estimate["kcal"] = total
    return {"model_kcal": rounded(model_kcal) if model_kcal is not None else None, "items_kcal": total}


def apply_turn(payload: Any, keep: frozenset[float] | set[float] = frozenset()) -> dict[str, int] | None:
    """Chat turn: apply to payload.estimate and rewrite the quoted total in reply. Returns the record.

    keep: kcal of the supplied RECENT rows and eaten DAY slots. A model total equal to one of them is a
    copied record (habitual meal, correction of a recorded meal) and keeps the record's numbers.
    """
    if not isinstance(payload, dict):
        return None
    estimate = payload.get("estimate")
    if isinstance(estimate, dict):
        model_kcal = _finite(estimate.get("kcal"))
        if model_kcal is not None and any(model_kcal == float(k) for k in keep):
            return None
    change = apply(estimate)
    if change and change["model_kcal"] is not None and isinstance(payload.get("reply"), str):
        payload["reply"] = rewrite_reply(payload["reply"], change["model_kcal"], change["items_kcal"])
    return change


_KCAL = re.compile(r"(?<![\d.,])(\d{1,3}(?:\.\d{3})*|\d+)(?:,0)?(\s*kcal)", re.IGNORECASE)


def rewrite_reply(reply: str, model_kcal: int, items_kcal: int) -> str:
    """Replace the figure the model quoted as its total (`589 kcal`, `1.050 kcal`, `589,0 kcal`).

    Other figures, including macros and item energies, stay. A total quoted in another form is left as is.
    """
    def swap(match: re.Match[str]) -> str:
        digits = match.group(1).replace(".", "")
        if digits.isdigit() and int(digits) == model_kcal:
            return _format_like(match.group(1), items_kcal) + match.group(2)
        return match.group(0)

    return _KCAL.sub(swap, reply)


def _format_like(sample: str, value: int) -> str:
    if "." in sample:
        return f"{value:,}".replace(",", ".")
    return str(value)
