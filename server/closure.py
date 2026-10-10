"""Day and week closure text (ADR-044, S30): the app's numbers as model input, the shaping and the fallback.

The app computes every number from Room; the server only adds the differences it serializes next to them, and
the model writes prose over those numbers. Pure functions, no I/O.
"""

from __future__ import annotations

import json
import re
from datetime import date
from typing import Any

from meal_window import rounded

TEXT_MAX_CHARS = 400
TEXT_MAX_LINES = 3
# Counts up to this value ("três jantares", "2 lanches", the list number "1.") are words, not figures.
COUNT_MAX = 10

_WEEKDAYS = ("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")
_NUMBER = re.compile(r"\d{1,3}(?:\.\d{3})+(?![\d,])|\d+(?:,\d+)?")
_SENTENCE = re.compile(r"(?<=[.!?;])\s+")
# Markers of the reply subset and any other markup: the closure text is plain (ADR-045 does not apply here).
_MARKUP = re.compile(r"\*\*|__|`+|^#{1,6}\s+|^\s*(?:[-*+•]\s+|\d+[.)]\s+)", re.MULTILINE)


def _quoted(name: str) -> str:
    return json.dumps(name, ensure_ascii=False)


def _weekday(day: date) -> str:
    return _WEEKDAYS[day.weekday()]


def numbers_text(body: Any) -> str:
    """NUMBERS for the model: the request values plus the differences the server derives from them."""
    profile = body.profile
    targets = f"p={profile.p_target}, c={profile.c_target}, g={profile.g_target}"
    if body.period == "day":
        lines = _day_lines(body.numbers, profile, targets)
    else:
        lines = _week_lines(body.numbers, profile, targets)
    if profile.goal is not None:
        lines.append(_goal_line(body))
    return "\n".join(lines)


def _goal_line(body: Any) -> str:
    """ADR-057 (S41): the accepted goal; a week adds the kcal over the ceiling summed over the recorded days."""
    goal = body.profile.goal
    line = f"GOAL: weight_kg={goal.weight_kg:g}" + (f", date={goal.date.isoformat()}" if goal.date else "")
    if body.period == "week":
        over = sum(max(0, d.kcal - d.ceiling_kcal) for d in body.numbers.days if d.recorded)
        line += f", week_over_kcal={over}"
    return line


def _day_lines(day: Any, profile: Any, targets: str) -> list[str]:
    workout = f", workout_kcal={day.workout_kcal}" if day.workout_kcal else ""
    lines = [
        "PERIOD: day",
        f"DATE: {day.date.isoformat()} ({_weekday(day.date)})",
        f"TARGETS: ceiling_kcal={day.ceiling_kcal}{workout}, {targets}",
        f"EATEN: kcal={day.kcal}, p={day.p}, c={day.c}, g={day.g}",
        "DIFFERENCES: "
        f"over_kcal={max(0, day.kcal - day.ceiling_kcal)}, left_kcal={max(0, day.ceiling_kcal - day.kcal)}, "
        f"p_missing={max(0, profile.p_target - day.p)}, c_missing={max(0, profile.c_target - day.c)}, "
        f"g_missing={max(0, profile.g_target - day.g)}, c_over={max(0, day.c - profile.c_target)}, "
        f"g_over={max(0, day.g - profile.g_target)}",
    ]
    if day.slots:
        lines.append("MEALS: " + "; ".join(_slot(s) for s in day.slots))
        last = day.slots[-1]
        if last.status == "empty":
            lines.append(f"LAST_MEAL_EMPTY: {_quoted(last.name)}")
        eaten = [s for s in day.slots if s.status == "eaten" and s.kcal]
        if eaten:
            top = max(eaten, key=lambda s: s.kcal)
            lines.append(f"TOP_MEAL: {_quoted(top.name)} {top.kcal} kcal")
    else:
        lines.append("MEALS: none")
    return lines


def _slot(slot: Any) -> str:
    if slot.status in ("eaten", "planned") and slot.kcal is not None:
        return f"{_quoted(slot.name)} {slot.status} {slot.kcal} kcal"
    return f"{_quoted(slot.name)} {slot.status}"


def _week_lines(week: Any, profile: Any, targets: str) -> list[str]:
    days = sorted(week.days, key=lambda d: d.date)
    recorded = [d for d in days if d.recorded]
    total = sum(d.kcal for d in recorded)
    lines = [
        "PERIOD: week",
        f"WEEK: {days[0].date.isoformat()} to {days[-1].date.isoformat()}",
        f"TARGETS: ceiling_kcal={profile.ceiling_kcal}, {targets}",
        "DAYS: " + "; ".join(_week_day(d) for d in days),
        f"TOTALS: days_recorded={len(recorded)}, days_without_record={len(days) - len(recorded)}, "
        f"total_kcal={total}",
    ]
    if recorded:
        n = len(recorded)
        lines.append(
            f"MEANS (recorded days): mean_kcal={rounded(total / n)}, mean_p={rounded(sum(d.p for d in recorded) / n)}, "
            f"days_over_ceiling={sum(1 for d in recorded if d.kcal > d.ceiling_kcal)}"
        )
    if week.over_slot is not None and week.over_slot.days > 0:
        lines.append(f"OVER_MEAL: {_quoted(week.over_slot.name)} over its share on {week.over_slot.days} days")
    return lines


def _week_day(day: Any) -> str:
    head = f"{day.date.isoformat()} {_weekday(day.date)}"
    if not day.recorded:
        return f"{head}: no record"
    workout = f", workout {day.workout_kcal}" if day.workout_kcal else ""
    return f"{head}: {day.kcal} kcal of {day.ceiling_kcal}, p {day.p}, c {day.c}, g {day.g}{workout}"


def allowed_numbers(text: str) -> set[int]:
    """Every whole number written in NUMBERS (dates included): the only figures the closure text may carry."""
    return {n for n in (_value(m.group(0)) for m in _NUMBER.finditer(text)) if n is not None}


def _value(token: str) -> int | None:
    if "," in token:
        return None
    return int(token.replace(".", ""))


def shape_text(raw: Any, allowed: set[int]) -> tuple[str | None, int]:
    """(text, sentences dropped). Plain text, a sentence with a number not in [allowed] is dropped whole,
    at most TEXT_MAX_LINES lines and TEXT_MAX_CHARS characters. None when nothing is left."""
    if not isinstance(raw, str):
        return None, 0
    dropped = 0
    lines: list[str] = []
    for line in _MARKUP.sub("", raw).splitlines():
        kept = []
        for sentence in _SENTENCE.split(" ".join(line.split())):
            if not sentence:
                continue
            if all(_allowed(m.group(0), allowed) for m in _NUMBER.finditer(sentence)):
                kept.append(sentence)
            else:
                dropped += 1
        if kept:
            lines.append(" ".join(kept))
    lines = lines[:TEXT_MAX_LINES]
    while lines and len("\n".join(lines)) > TEXT_MAX_CHARS:
        if len(lines) > 1:
            lines.pop()
            continue
        lines = [_cut(lines[0])] if _cut(lines[0]) else []
    text = "\n".join(lines).strip()
    return (text or None), dropped


def _allowed(token: str, allowed: set[int]) -> bool:
    value = _value(token)
    return value is not None and (value <= COUNT_MAX or value in allowed)


def _cut(line: str) -> str:
    """The longest run of whole sentences of [line] within TEXT_MAX_CHARS, or ""."""
    out = ""
    for sentence in _SENTENCE.split(line):
        candidate = f"{out} {sentence}".strip()
        if len(candidate) > TEXT_MAX_CHARS:
            break
        out = candidate
    return out


def fallback(body: Any) -> str:
    """The fixed neutral line of a refusal or failure, built from the request numbers."""
    if body.period == "day":
        return f"Dia fechado. {body.numbers.kcal} de {body.numbers.ceiling_kcal} kcal."
    recorded = [d for d in body.numbers.days if d.recorded]
    if not recorded:
        return "Semana fechada. Nenhum dia com registro."
    total = sum(d.kcal for d in recorded)
    return f"Semana fechada. {total} kcal em {len(recorded)} dias com registro."
