"""Meal window of a plan and the windows of the remaining slots (ADR-043, S24).

Server arithmetic over the request: the model never computes these numbers. Pure functions, no I/O.
"""

from __future__ import annotations

import math
import re
import unicodedata
from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal
from typing import Any, Iterable, Sequence

# A slot has its own history once it has records on this many distinct days of RECENT.
HISTORY_DAYS = 2
# ADR-043 decision 4: below this share of the missing protein a named dish gets (opcional) protein foods.
PROTEIN_FLOOR_PCT = 30


@dataclass(frozen=True)
class Slot:
    id: str
    name: str
    status: str  # empty | eaten | skipped
    kcal: float = 0
    p: float = 0


@dataclass(frozen=True)
class Window:
    window_kcal: int
    reserved_upcoming: int
    reserved: list[dict[str, Any]]

    def log(self) -> dict[str, Any]:
        return {"window_kcal": self.window_kcal, "reserved_upcoming": self.reserved_upcoming, "reserved": self.reserved}


def rounded(value: float) -> int:
    """Round once, ties upward (the meal-change and estimate-total rule)."""
    return int(Decimal(repr(float(value))).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def _key(label: str) -> str:
    """Case- and accent-insensitive label, so `jantar` and `Jantar` name the same slot."""
    text = unicodedata.normalize("NFKD", label.strip().casefold())
    return "".join(ch for ch in text if not unicodedata.combining(ch))


def expected_kcal(slot_id: str, recent: Iterable[Any], ceiling_kcal: float, slot_count: int) -> int:
    """Mean kcal per distinct day of the slot's RECENT records when it has at least two days; else an equal share."""
    days: dict[Any, float] = {}
    for meal in recent:
        if getattr(meal, "slot_id", None) == slot_id:
            days[meal.date] = days.get(meal.date, 0.0) + float(meal.kcal)
    if len(days) >= HISTORY_DAYS:
        return rounded(sum(days.values()) / len(days))
    return rounded(ceiling_kcal / slot_count) if slot_count else 0


def expected_by_slot(slots: Sequence[Slot], recent: Iterable[Any], ceiling_kcal: float) -> dict[str, int]:
    recent = list(recent)
    return {s.id: expected_kcal(s.id, recent, ceiling_kcal, len(slots)) for s in slots}


def window(
    slots: Sequence[Slot],
    expected: dict[str, int],
    remaining_kcal: int,
    target: str | None,
    stated: Sequence[dict[str, Any]] = (),
) -> Window:
    """window_kcal = max(0, remaining_kcal - reserved_upcoming) for a plan of [target].

    reserved_upcoming: the expected kcal of every other empty slot, a stated reservation whose label names one of
    them replacing its value, plus every stated reservation that names no such slot. No target: only the stated
    reservations (the meal is unknown, so no slot is "other").
    """
    others = [s for s in slots if s.status == "empty" and s.id != target] if target is not None else []
    values = {s.id: expected[s.id] for s in others}
    by_name = {_key(s.name): s.id for s in others}
    extra: list[dict[str, Any]] = []
    for entry in stated:
        slot_id = by_name.get(_key(entry["label"]))
        if slot_id is not None:
            values[slot_id] = entry["kcal"]
        else:
            extra.append({"label": entry["label"], "kcal": entry["kcal"]})
    reserved = [{"label": s.name, "kcal": values[s.id]} for s in others] + extra
    upcoming = sum(r["kcal"] for r in reserved)
    return Window(max(0, remaining_kcal - upcoming), upcoming, reserved)


def _shares(total: int, weights: Sequence[int]) -> list[int]:
    """[total] split by [weights], whole numbers summing exactly to total (largest remainder); 0 when total <= 0."""
    if total <= 0 or not weights:
        return [0] * len(weights)
    weight = sum(weights)
    if weight <= 0:
        weights, weight = [1] * len(weights), len(weights)
    raw = [Decimal(total) * w / weight for w in weights]
    out = [int(r) for r in raw]
    order = sorted(range(len(raw)), key=lambda i: (raw[i] - out[i], -i), reverse=True)
    for i in order[: total - sum(out)]:
        out[i] += 1
    return out


def windows(slots: Sequence[Slot], expected: dict[str, int], remaining_kcal: int, remaining_p: int) -> list[tuple[str, int, int]]:
    """(name, kcal, protein) of every listed slot: expected kcal scaled to sum to remaining_kcal, protein in the same shares."""
    kcal = _shares(remaining_kcal, [expected[s.id] for s in slots])
    protein = _shares(remaining_p, kcal) if remaining_kcal > 0 else [0] * len(slots)
    return [(s.name, k, p) for s, k, p in zip(slots, kcal, protein)]


def empty(slots: Sequence[Slot]) -> list[Slot]:
    return [s for s in slots if s.status == "empty"]


def budget_line(slots: Sequence[Slot], expected: dict[str, int], remaining_kcal: int, remaining_p: int) -> str | None:
    """`BUDGET` for the model: the protein floor of a named dish (30% of remaining_p, rounded up) and the window
    of a plan for each empty meal of today and for any other meal."""
    free = empty(slots)
    if not free:
        return None
    parts = []
    for slot in free:
        w = window(slots, expected, remaining_kcal, slot.id)
        names = ", ".join(r["label"] for r in w.reserved) or "-"
        parts.append(f"{slot.name}={w.window_kcal} (reserved_upcoming={w.reserved_upcoming}: {names})")
    upcoming = sum(expected[s.id] for s in free)
    parts.append(
        f"any other meal={max(0, remaining_kcal - upcoming)} "
        f"(reserved_upcoming={upcoming}: {', '.join(s.name for s in free)})"
    )
    floor = math.ceil(max(0, remaining_p) * PROTEIN_FLOOR_PCT / 100)
    return f"BUDGET: protein_floor={floor}; window_kcal by meal: " + "; ".join(parts)


def windows_line(slots: Sequence[Slot], expected: dict[str, int], remaining_kcal: int, remaining_p: int) -> str | None:
    """`WINDOWS` for the model: at least two empty slots, so a log or plan of one leaves another to close."""
    free = empty(slots)
    if len(free) < 2:
        return None
    body = " · ".join(f"{name} ~{k} kcal P {p}" for name, k, p in windows(free, expected, remaining_kcal, remaining_p))
    return f"WINDOWS: {body} | faltam {max(0, remaining_p)} g P"


_CLOSING = re.compile(r"^(?P<name>[^:\n]{1,40}): (?P<food>.+?) ~\d+ kcal · P \d+(?: g)?[.;]?\s*$")


def close_reply(
    reply: str,
    slots: Sequence[Slot],
    expected: dict[str, int],
    remaining_kcal: int,
    remaining_p: int,
    target: str | None,
    kcal: float,
    p: float,
) -> str:
    """Closing lines `{slot}: {food} ~{kcal} kcal · P {p}` with the server's numbers (ADR-043 decision 6).

    The answer logs or plans [target] at [kcal]/[p]; the other empty slots share what is left after it, in
    proportion to their expected kcal. A closing line for another slot of the day (the answered meal, an eaten or
    skipped slot) is dropped. Every other line, including one that names no slot, stays as it is.
    """
    by_key = {_key(s.name): s for s in slots}
    answered = next((s for s in slots if s.id == target), None)
    before_kcal = answered.kcal if answered is not None and answered.status == "eaten" else 0
    before_p = answered.p if answered is not None and answered.status == "eaten" else 0
    others = [s for s in empty(slots) if s.id != target]
    left_kcal = rounded(remaining_kcal - (kcal - before_kcal))
    left_p = rounded(remaining_p - (p - before_p))
    numbers = {s.id: (k, q) for s, (_, k, q) in zip(others, windows(others, expected, left_kcal, left_p))}
    lines = []
    for line in reply.split("\n"):
        m = _CLOSING.match(line)
        slot = by_key.get(_key(m.group("name"))) if m else None
        if m is None or slot is None:
            lines.append(line)
            continue
        if slot.id not in numbers:
            continue
        k, q = numbers[slot.id]
        lines.append(f"{slot.name}: {m.group('food')} ~{k} kcal · P {q}")
    return "\n".join(lines).rstrip("\n")
