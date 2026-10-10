"""The day a message names, resolved in code before the model (ADR-058, S42). Pure functions, no I/O.

Cues: `ontem`, `anteontem`, a weekday name (the most recent past one; today's weekday is last week), `dia {n}`
(this month when n is before today; a later n has two candidates, last month and a future day of this month, so it
is a question unless the message names the month), `dia {n} de {mês}` and `{n} dias atrás`. Every cue of the
message must land on the same date; two dates are ambiguous. A named day older than WINDOW_DAYS days, or before the
client's first day, is too old. An equality phrase (`o mesmo de ontem`) names a source to copy, not the day the meal
was eaten (copy_source, S33): it yields nothing here.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Literal

from copy_source import WEEKDAYS, normalize

WINDOW_DAYS = 30
TOO_OLD_LINE = "Só registro os últimos 30 dias."
MONTHS = ("janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro",
          "novembro", "dezembro")
_WEEKDAY = r"(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:-feira)?"
_EQUALITY = re.compile(r"\b(mesm[oa]s?|igual|repeti)\b")
_ONTEM = re.compile(r"\b(anteontem|ontem)\b")
# A weekday needs a preposition, `-feira` or `passado`: `a segunda` alone is an ordinal (`fiz a segunda opção`).
_NOT_A_DAY = r"(?! (?:opcao|vez|porcao|parte|colher|fatia|refeicao|dose))"
_WEEKDAY_CUE = re.compile(
    r"\b(?:(?:na|no|nessa|nesta|nesse|neste|ultima|ultimo) " + _WEEKDAY + r"\b" + _NOT_A_DAY
    + r"|(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:-feira| passad[oa])\b)")
_DIA = re.compile(r"\bdia (\d{1,2})(?: de (" + "|".join(MONTHS) + r")| do mes passado)?\b")
_AGO = re.compile(r"\b(\d{1,2}|um|uma|dois|duas|tres|quatro|cinco|seis|sete) dias? atras\b")
_WORD_NUMBERS = {"um": 1, "uma": 1, "dois": 2, "duas": 2, "tres": 3, "quatro": 4, "cinco": 5, "seis": 6, "sete": 7}


@dataclass(frozen=True)
class DayRef:
    """`bound` (one past day), `ambiguous` (candidates), `future`, `too_old`; `words` are the cue labels."""

    kind: Literal["bound", "ambiguous", "future", "too_old"]
    day: date | None = None
    candidates: tuple[date, ...] = ()
    words: tuple[str, ...] = ()

    def line(self, slots: str | None = None) -> str:
        """The DAY_REF line of the model input."""
        if self.kind == "bound":
            label = ", ".join(dict.fromkeys((*self.words, WEEKDAYS[self.day.weekday()])))
            return f"DAY_REF: {self.day.isoformat()} ({label})" + (f" slots=[{slots}]" if slots else "")
        if self.kind == "ambiguous":
            # The pt-BR date next to each candidate, so the question names it as the user would.
            return "DAY_REF: ambiguous (" + " ou ".join(f"{d.isoformat()} = {spoken(d)}" for d in self.candidates) + ")"
        return f"DAY_REF: {self.kind}"


def spoken(day: date) -> str:
    """The pt-BR date of a candidate, `3 de outubro`."""
    month = MONTHS[day.month - 1].replace("marco", "março")
    return f"{day.day} de {month}"


def _month_day(year: int, month: int, day: int) -> date | None:
    try:
        return date(year, month, day)
    except ValueError:
        return None


def _previous_month(today: date, day: int) -> date | None:
    year, month = (today.year, today.month - 1) if today.month > 1 else (today.year - 1, 12)
    return _month_day(year, month, day)


def _dia(match: re.Match[str], today: date) -> tuple[str, list[date], bool]:
    """(label, candidates, future): the dates a `dia {n}` cue can mean."""
    n = int(match.group(1))
    label = match.group(0)
    if match.group(2):
        month = MONTHS.index(match.group(2)) + 1
        year = today.year if month <= today.month else today.year - 1
        found = _month_day(year, month, n)
        return label, [found] if found else [], bool(found and found > today)
    if "mes passado" in label:
        found = _previous_month(today, n)
        return label, [found] if found else [], False
    if n < today.day:
        return label, [date(today.year, today.month, n)], False
    if n == today.day:
        return label, [today], False
    this_month = _month_day(today.year, today.month, n)
    last_month = _previous_month(today, n)
    candidates = [d for d in (last_month, this_month) if d is not None]
    return label, candidates, False


def resolve(text: str, today: date, first_day: date | None = None) -> DayRef | None:
    """The day the message names, or None when it names no day (or only a copy source)."""
    clean = normalize(text)
    if _EQUALITY.search(clean):
        return None
    dates: list[date] = []
    words: list[str] = []
    ambiguous: list[date] = []
    future = False
    for match in _ONTEM.finditer(clean):
        dates.append(today - timedelta(days=2 if match.group(1) == "anteontem" else 1))
        words.append(match.group(1))
    for match in _WEEKDAY_CUE.finditer(clean):
        index = WEEKDAYS.index(match.group(1) or match.group(2))
        dates.append(today - timedelta(days=(today.weekday() - index) % 7 or 7))
        words.append(WEEKDAYS[index])
    for match in _DIA.finditer(clean):
        label, candidates, is_future = _dia(match, today)
        future = future or is_future
        if len(candidates) == 1:
            dates.append(candidates[0])
            words.append(label)
        else:
            ambiguous.extend(candidates)
    for match in _AGO.finditer(clean):
        raw = match.group(1)
        n = int(raw) if raw.isdigit() else _WORD_NUMBERS[raw]
        dates.append(today - timedelta(days=n))
        words.append(match.group(0))
    if future:
        return DayRef("future")
    distinct = sorted(set(dates))
    if ambiguous:
        if len(distinct) == 1 and distinct[0] in ambiguous:
            ambiguous = []
        else:
            return DayRef("ambiguous", candidates=tuple(sorted(set(ambiguous + distinct))))
    if not distinct:
        return None
    if len(distinct) > 1:
        return DayRef("ambiguous", candidates=tuple(distinct))
    day = distinct[0]
    if day >= today:
        # `dia {n}` of today, or a cue that lands on today: the meal is today's.
        return None
    if (today - day).days > WINDOW_DAYS or (first_day is not None and day < first_day):
        return DayRef("too_old", day=day, words=tuple(dict.fromkeys(words)))
    return DayRef("bound", day=day, words=tuple(dict.fromkeys(words)))
