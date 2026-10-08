"""Protein boost of a named dish (ADR-043 decision 4, S24): server arithmetic over a short TACO table.

When a plan for today covers too little of the protein still missing and its meal window has room, the server
appends one or two protein foods marked `(opcional)` to the estimate (items, totals, meal_text) and to the reply.
The model never does this itself: at effort none it did not follow the rule. Pure functions, no I/O.

Values per 100 g from the Tabela Brasileira de Composição de Alimentos (TACO, 4th ed.), the same rows the
reference-portions rule carries: peito de frango grelhado 159/32/0/2.5 (served shredded), ovo cozido
146/13/0.6/9.5 (1 unit 50 g), patinho grelhado 219/36/0/7 (served as lean ground beef).
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal
from typing import Any

# Below this share of the missing protein a named dish gets the boost (ADR-043 decision 4).
FLOOR_PCT = 30
# Room in the window the boost needs to be worth adding, and the most grams of one food.
MIN_ROOM_KCAL = 60
MAX_GRAMS = 150
MAX_FOODS = 2
# S33: a plan of one food under this many kcal answers "posso adicionar X?" with that food alone: not a dish.
MIN_DISH_KCAL = 150


@dataclass(frozen=True)
class Food:
    name: str
    kcal: float  # per 100 g
    p: float
    c: float
    g: float
    step: int  # grams per step (an egg is 50 g)
    keys: tuple[str, ...]  # words that mean the dish already has this food


FOODS: tuple[Food, ...] = (
    Food("frango desfiado", 159, 32, 0, 2.5, 25, ("frango", "chicken")),
    Food("ovo cozido", 146, 13, 0.6, 9.5, 50, ("ovo", "ovos", "omelete", "mexido")),
    Food("patinho moído cozido", 219, 36, 0, 7, 25, ("carne", "bife", "patinho", "acém", "moída", "moido")),
)


def _rounded(value: float) -> int:
    return int(Decimal(repr(float(value))).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def _number(value: Any) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        return None
    return float(value)


def _dish_text(estimate: dict[str, Any]) -> str:
    parts = [str(estimate.get("meal_text") or "")]
    parts += [str(item.get("name") or "") for item in estimate.get("items") or [] if isinstance(item, dict)]
    return " ".join(parts).casefold()


def _grams(food: Food, room_kcal: float, gap_p: float) -> int:
    """The most grams that fit the room and do not exceed the gap, in the food's step; 0 when a step does not fit."""
    by_room = room_kcal / food.kcal * 100
    by_gap = gap_p / food.p * 100
    grams = min(MAX_GRAMS, by_room, by_gap)
    steps = int(grams // food.step)
    return steps * food.step


def floor_p(remaining_p: float) -> int:
    return math.ceil(max(0.0, remaining_p) * FLOOR_PCT / 100)


def apply(payload: Any, window_kcal: int | None, remaining_p: int | None) -> dict[str, Any] | None:
    """Append `(opcional)` protein foods to payload.estimate and the reply when the dish needs them.

    Applies only to an in-scope plan with an estimate, a known window and missing protein. Returns the record
    `{gap_p, room_kcal, added: [{name, g, kcal, p}]}` when something was added, else None. Mutates payload.
    """
    if not isinstance(payload, dict) or payload.get("intent") != "plan" or window_kcal is None or remaining_p is None:
        return None
    estimate = payload.get("estimate")
    if not isinstance(estimate, dict) or remaining_p <= 0:
        return None
    kcal, p = _number(estimate.get("kcal")), _number(estimate.get("p"))
    if kcal is None or p is None or kcal <= 0:
        return None
    if p >= floor_p(remaining_p):
        return None
    if len(estimate.get("items") or []) == 1 and kcal < MIN_DISH_KCAL:
        return None
    room = window_kcal - kcal
    gap = remaining_p - p
    if room < MIN_ROOM_KCAL or gap <= 0:
        return None
    dish = _dish_text(estimate)
    added: list[dict[str, Any]] = []
    for food in FOODS:
        if len(added) == MAX_FOODS or p >= floor_p(remaining_p):
            break
        if any(key in dish for key in food.keys):
            continue
        grams = _grams(food, room, gap)
        if grams <= 0:
            continue
        item_kcal = _rounded(food.kcal * grams / 100)
        item_p = _rounded(food.p * grams / 100)
        added.append({"name": food.name, "g": grams, "kcal": item_kcal, "p": item_p,
                      "c": _rounded(food.c * grams / 100), "gfat": _rounded(food.g * grams / 100)})
        room -= item_kcal
        gap -= item_p
        p += item_p
    if not added:
        return None
    items = list(estimate.get("items") or [])
    for entry in added:
        items.append({"name": f"{entry['name']} (opcional)", "g": entry["g"], "kcal": entry["kcal"]})
    estimate["items"] = items
    estimate["kcal"] = _rounded(kcal + sum(e["kcal"] for e in added))
    estimate["p"] = _rounded((_number(estimate.get("p")) or 0) + sum(e["p"] for e in added))
    estimate["c"] = _rounded((_number(estimate.get("c")) or 0) + sum(e["c"] for e in added))
    estimate["g"] = _rounded((_number(estimate.get("g")) or 0) + sum(e["gfat"] for e in added))
    text = str(estimate.get("meal_text") or "").rstrip()
    extra = "; ".join(f"{e['g']} g de {e['name']} (opcional)" for e in added)
    estimate["meal_text"] = f"{text}; {extra}" if text else extra
    line = " e ".join(f"{e['g']} g de {e['name']}" for e in added)
    boost_kcal = sum(e["kcal"] for e in added)
    boost_p = sum(e["p"] for e in added)
    reply = payload.get("reply")
    note = f"Para a proteína (opcional): {line}, +{boost_kcal} kcal · P {boost_p} g."
    payload["reply"] = f"{reply.rstrip()}\n{note}" if isinstance(reply, str) and reply.strip() else note
    return {
        "gap_p": _rounded(remaining_p - (p - boost_p)),
        "room_kcal": _rounded(window_kcal - kcal),
        "added": [{"name": e["name"], "g": e["g"], "kcal": e["kcal"], "p": e["p"]} for e in added],
    }
