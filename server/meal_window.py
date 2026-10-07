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
# An empty slot whose time passed less than this many minutes ago is still the meal in progress.
GRACE_MINUTES = 90
# ADR-043 decision 4: below this share of the missing protein a named dish gets (opcional) protein foods.
PROTEIN_FLOOR_PCT = 30


@dataclass(frozen=True)
class Slot:
    id: str
    name: str
    status: str  # empty | eaten | skipped
    kcal: float = 0
    p: float = 0
    time: str | None = None  # HH:MM of the profile slot; None reserves regardless of the clock


@dataclass(frozen=True)
class Window:
    window_kcal: int  # floored at 0: what the model is told
    reserved_upcoming: int
    reserved: list[dict[str, Any]]
    limit_kcal: int = 0  # remaining - reserved, may be negative: the ADR-039 budget limit

    def log(self) -> dict[str, Any]:
        return {"window_kcal": self.window_kcal, "reserved_upcoming": self.reserved_upcoming, "reserved": self.reserved}


def rounded(value: float) -> int:
    """Round once, ties upward (the meal-change and estimate-total rule)."""
    return int(Decimal(repr(float(value))).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def _key(label: str) -> str:
    """Case- and accent-insensitive label, so `jantar` and `Jantar` name the same slot."""
    text = unicodedata.normalize("NFKD", label.strip().casefold())
    return "".join(ch for ch in text if not unicodedata.combining(ch))


_LEAD = re.compile(r"^(?:para|pra|pro|no|na|ao|a|o)\s+(?:a|o)?\s*", re.IGNORECASE)


def _slot_key(label: str) -> str:
    """_key of a label with a leading "para a", "na", "pro" dropped: `Para a ceia` names the slot `Ceia`."""
    return _key(_LEAD.sub("", label.strip()))


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


def _minutes(hhmm: str | None) -> int | None:
    m = re.fullmatch(r"(\d{1,2}):(\d{2})", (hhmm or "").strip())
    return int(m.group(1)) * 60 + int(m.group(2)) if m else None


def upcoming(slots: Sequence[Slot], now: str | None = None) -> list[Slot]:
    """The empty slots still to be eaten: with a clock, only those whose time has not passed (S24 owner decision).

    An empty slot whose time is behind the clock was skipped in practice and reserves nothing. A slot without a
    time, or no clock, is reserved regardless.
    """
    clock = _minutes(now)
    out = []
    for s in slots:
        if s.status != "empty":
            continue
        at = _minutes(s.time)
        if clock is None or at is None or at >= clock - GRACE_MINUTES:
            out.append(s)
    return out


def _capped(remaining_kcal: int, target_expected: int, others: Sequence[Slot], expected: dict[str, int]) -> dict[str, int]:
    """The reservation of each other upcoming slot: its expected kcal, capped at its share of the remainder.

    The share splits remaining_kcal among the target and the others by expected kcal, so the target keeps at
    least its proportional part and the window never reaches zero while something remains (S24 owner decision).
    """
    total = target_expected + sum(expected[s.id] for s in others)
    out = {}
    for s in others:
        share = rounded(remaining_kcal * expected[s.id] / total) if total > 0 and remaining_kcal > 0 else 0
        out[s.id] = min(expected[s.id], max(0, share))
    return out


def window(
    slots: Sequence[Slot],
    expected: dict[str, int],
    remaining_kcal: int,
    target: str | None,
    stated: Sequence[dict[str, Any]] = (),
    now: str | None = None,
    typed: set[int] | None = None,
) -> Window:
    """window_kcal = max(0, remaining_kcal - reserved_upcoming) for a plan of [target].

    reserved_upcoming: the capped expected kcal of every other upcoming empty slot, a stated reservation whose
    label names one of them replacing its value, plus every stated reservation that names no such slot. No
    target: only the stated reservations (the meal is unknown, so no slot is "other").

    typed: the whole numbers the user wrote in the conversation. With it, a stated reservation that names a slot
    counts only when its kcal is one of them: the model reports what the user said, and a figure the user never
    typed is the model's own guess, not a statement. Without it (None) the number is not checked.
    """
    others = [s for s in upcoming(slots, now) if s.id != target] if target is not None else []
    target_expected = expected.get(target, 0) if target is not None else 0
    values = _capped(remaining_kcal, target_expected, others, expected)
    by_name = {_key(s.name): s.id for s in others}
    any_slot = {_key(s.name) for s in slots}
    echo = served_numbers(slots, expected, remaining_kcal, now)
    extra: list[dict[str, Any]] = []
    for entry in stated:
        label = _slot_key(entry["label"])
        slot_id = by_name.get(label)
        if slot_id is None and label in any_slot:
            # The target, an eaten, skipped or past meal: a meal name is never an extra reservation.
            continue
        if slot_id is not None:
            # A number the server itself served (BUDGET/WINDOWS) is an echo, not a statement; a smaller
            # figure is usually the model's own closing suggestion. Only a larger stated reservation counts.
            said = typed is None or int(entry["kcal"]) in typed
            if said and entry["kcal"] not in echo and entry["kcal"] > values[slot_id]:
                values[slot_id] = entry["kcal"]
        else:
            extra.append({"label": entry["label"], "kcal": entry["kcal"]})
    reserved = [{"label": s.name, "kcal": values[s.id]} for s in others] + extra
    total_reserved = sum(r["kcal"] for r in reserved)
    limit = remaining_kcal - total_reserved
    return Window(max(0, limit), total_reserved, reserved, limit)


def served_numbers(slots: Sequence[Slot], expected: dict[str, int], remaining_kcal: int, now: str | None) -> set[int]:
    """Every kcal figure the server writes about the upcoming slots (expected, capped shares, windows)."""
    free = upcoming(slots, now)
    out = {expected[s.id] for s in free}
    out.update(k for _, k, _ in windows(free, expected, remaining_kcal, 0))
    for s in free:
        out.update(_capped(remaining_kcal, expected[s.id], [o for o in free if o.id != s.id], expected).values())
    out.update(_capped(remaining_kcal, 0, free, expected).values())
    return {n for n in out if n > 0}


def typed_numbers(texts: Iterable[str]) -> set[int]:
    """Every whole number the user wrote (`300`, `1.200`, `250kcal`): the only figures a stated reservation may carry."""
    out: set[int] = set()
    for text in texts:
        for m in re.finditer(r"\d{1,3}(?:\.\d{3})+|\d+", text or ""):
            out.add(int(m.group(0).replace(".", "")))
    return out


def nearest_upcoming(slots: Sequence[Slot], now: str | None) -> str | None:
    """The empty slot whose time is closest to the clock (the meal a plan without a slot is for), else None."""
    free = upcoming(slots, now)
    if not free:
        return None
    clock = _minutes(now)
    if clock is None:
        return free[0].id
    return min(free, key=lambda s: abs((_minutes(s.time) if _minutes(s.time) is not None else clock) - clock)).id


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


def empty(slots: Sequence[Slot], now: str | None = None) -> list[Slot]:
    return upcoming(slots, now)


def budget_line(
    slots: Sequence[Slot], expected: dict[str, int], remaining_kcal: int, remaining_p: int, now: str | None = None,
) -> str | None:
    """`BUDGET` for the model: the protein floor of a named dish (30% of remaining_p, rounded up) and the window
    of a plan for each empty meal of today and for any other meal."""
    free = empty(slots, now)
    if not free:
        return None
    parts = []
    for slot in free:
        w = window(slots, expected, remaining_kcal, slot.id, now=now)
        names = ", ".join(r["label"] for r in w.reserved) or "-"
        parts.append(f"{slot.name}={w.window_kcal} (reserved_upcoming={w.reserved_upcoming}: {names})")
    reserved_all = sum(_capped(remaining_kcal, 0, free, expected).values())
    parts.append(
        f"any other meal={max(0, remaining_kcal - reserved_all)} "
        f"(reserved_upcoming={reserved_all}: {', '.join(s.name for s in free)})"
    )
    floor = math.ceil(max(0, remaining_p) * PROTEIN_FLOOR_PCT / 100)
    return f"BUDGET: protein_floor={floor}; window_kcal by meal: " + "; ".join(parts)


def windows_line(
    slots: Sequence[Slot], expected: dict[str, int], remaining_kcal: int, remaining_p: int, now: str | None = None,
) -> str | None:
    """`WINDOWS` for the model: at least two upcoming empty slots, so a log or plan of one leaves another to close."""
    free = empty(slots, now)
    if len(free) < 2:
        return None
    body = " · ".join(f"{name} ~{k} kcal P {p}" for name, k, p in windows(free, expected, remaining_kcal, remaining_p))
    return f"WINDOWS: {body} | faltam {max(0, remaining_p)} g P"


_CLOSING = re.compile(
    r"(?:(?<=^)|(?<=\n)|(?<=\. )|(?<=; ))(?P<name>[^.:\n;]{1,40}?): (?P<food>(?:(?!kcal)[^\n])+?) ~\d+ kcal · P \d+(?: g)?[.;]?(?=\s|$)",
    re.MULTILINE,
)


def close_reply(
    reply: str,
    slots: Sequence[Slot],
    expected: dict[str, int],
    remaining_kcal: int,
    remaining_p: int,
    target: str | None,
    kcal: float,
    p: float,
    now: str | None = None,
    usual: dict[str, str] | None = None,
    complete: bool = False,
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
    others = [s for s in empty(slots, now) if s.id != target]
    left_kcal = rounded(remaining_kcal - (kcal - before_kcal))
    left_p = rounded(remaining_p - (p - before_p))
    numbers = {s.id: (k, q) for s, (_, k, q) in zip(others, windows(others, expected, left_kcal, left_p))}
    closings: dict[str, str] = {}
    body = reply
    for m in list(_CLOSING.finditer(reply))[::-1]:
        slot = by_key.get(_slot_key(m.group("name")))
        if slot is None:
            continue
        if slot.id in numbers and slot.id not in closings:
            closings[slot.id] = m.group("food").strip().rstrip(".;")
        body = body[: m.start()] + body[m.end():]
    prose = "\n".join(line.rstrip() for line in body.split("\n") if line.strip()).strip()
    # With complete (a WINDOWS line was served), a meal the model left out still gets its line: the slot's
    # latest RECENT food when there is one, else "a definir"; the numbers are the server's either way (ADR-043 decision 6).
    lines = []
    for s in others:
        food = closings.get(s.id) or ((usual or {}).get(s.id) or "a definir" if complete else None)
        if food is None:
            continue
        lines.append(f"{s.name}: {food} ~{numbers[s.id][0]} kcal · P {numbers[s.id][1]}")
    return "\n".join([prose, *lines] if prose else lines).rstrip("\n")
