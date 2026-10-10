"""The days a message names, resolved in code before the model (ADR-058, S42). Pure functions, no I/O.

Cues: `ontem`, `anteontem`, a weekday with a preposition, `-feira` or `passado` (the most recent past one; today's
weekday is last week), `dia {n}` (this month when n is before today), `{n} de {mês}` / `dia {n} de {mês}`,
`dia {n} do mês passado` and `{n} dias atrás`. Each distinct date is one day of the message: two meals on two days
are two records (owner decision, 09/10/2026). A `dia {n}` later than today can only be last month's: the turn asks
to confirm that past date (`confirm`), never offering the future one; the answer to that question is resolved from
the date the question names (`resolve_answer`). A named day older than WINDOW_DAYS days, or before the client's first
day, is too old. An equality phrase (`o mesmo de ontem`) names a source to copy, not the day the meal was eaten
(copy_source, S33): it yields nothing here.
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
_MONTH = "(" + "|".join(MONTHS) + ")"
_WEEKDAY = r"(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:-feira)?"
_EQUALITY = re.compile(r"\b(mesm[oa]s?|igual|repeti)\b")
_ONTEM = re.compile(r"\b(anteontem|ontem)\b")
# A weekday needs a preposition, `-feira` or `passado`: `a segunda` alone is an ordinal (`fiz a segunda opção`).
_NOT_A_DAY = r"(?! (?:opcao|vez|porcao|parte|colher|fatia|refeicao|dose))"
_WEEKDAY_CUE = re.compile(
    r"\b(?:(?:na|no|nessa|nesta|nesse|neste|ultima|ultimo) " + _WEEKDAY + r"\b" + _NOT_A_DAY
    + r"|(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:-feira| passad[oa])\b)")
_DATE = re.compile(r"\b(?:dia )?(\d{1,2}) de " + _MONTH + r"\b")
_DIA = re.compile(r"\bdia (\d{1,2})( do mes passado)?\b(?! de " + _MONTH + r")")
_AGO = re.compile(r"\b(\d{1,2}|um|uma|dois|duas|tres|quatro|cinco|seis|sete) dias? atras\b")
_WORD_NUMBERS = {"um": 1, "uma": 1, "dois": 2, "duas": 2, "tres": 3, "quatro": 4, "cinco": 5, "seis": 6, "sete": 7}


def spoken(day: date) -> str:
    """The pt-BR date, `3 de outubro`."""
    return f"{day.day} de {MONTHS[day.month - 1].replace('marco', 'março')}"


@dataclass(frozen=True)
class DayRef:
    """`bound` (one or more past days, each with its cue words), `confirm` (the one past date a later `dia {n}` can
    mean, to be confirmed), `future`, `too_old`."""

    kind: Literal["bound", "confirm", "future", "too_old"]
    days: tuple[tuple[date, tuple[str, ...]], ...] = ()

    @property
    def dates(self) -> tuple[date, ...]:
        return tuple(day for day, _ in self.days)

    def lines(self, slots: dict[date, str] | None = None) -> list[str]:
        """The DAY_REF lines of the model input: one per bound day, with that day's slots."""
        if self.kind == "bound":
            return [f"DAY_REF: {day.isoformat()} ({_label(day, words)})"
                    + (f" slots=[{slots[day]}]" if slots and slots.get(day) else "") for day, words in self.days]
        if self.kind == "confirm":
            day, words = self.days[0]
            return [f"DAY_REF: confirm {day.isoformat()} ({_label(day, words)})"]
        return [f"DAY_REF: {self.kind}"]


def _label(day: date, words: tuple[str, ...]) -> str:
    return ", ".join(dict.fromkeys((*words, spoken(day), WEEKDAYS[day.weekday()])))


def _month_day(year: int, month: int, day: int) -> date | None:
    try:
        return date(year, month, day)
    except ValueError:
        return None


def _previous_month(today: date, day: int) -> date | None:
    year, month = (today.year, today.month - 1) if today.month > 1 else (today.year - 1, 12)
    return _month_day(year, month, day)


def _named_date(match: re.Match[str], today: date) -> date | None:
    """`{n} de {mês}`: this year, or last year when that month is still ahead."""
    month = MONTHS.index(match.group(2)) + 1
    return _month_day(today.year if month <= today.month else today.year - 1, month, int(match.group(1)))


def _collect(clean: str, today: date) -> tuple[dict[date, list[str]], list[date], bool]:
    """(bound dates with their words, dates to confirm, a future day named)."""
    found: dict[date, list[str]] = {}
    confirm: list[date] = []
    future = False

    def add(day: date, word: str) -> None:
        found.setdefault(day, []).append(word)

    for match in _ONTEM.finditer(clean):
        add(today - timedelta(days=2 if match.group(1) == "anteontem" else 1), match.group(1))
    for match in _WEEKDAY_CUE.finditer(clean):
        index = WEEKDAYS.index(match.group(1) or match.group(2))
        add(today - timedelta(days=(today.weekday() - index) % 7 or 7), WEEKDAYS[index])
    for match in _DATE.finditer(clean):
        day = _named_date(match, today)
        if day is not None and day > today:
            future = True
        elif day is not None:
            add(day, match.group(0))
    for match in _DIA.finditer(clean):
        n = int(match.group(1))
        if match.group(2):
            day = _previous_month(today, n)
            if day is not None:
                add(day, match.group(0))
        elif n <= today.day:
            add(date(today.year, today.month, n), match.group(0))
        elif (past := _previous_month(today, n)) is not None:
            # This month's `n` is still ahead: only last month's can have been eaten. Confirm it.
            confirm.append(past)
        elif _month_day(today.year, today.month, n) is not None:
            future = True
    for match in _AGO.finditer(clean):
        raw = match.group(1)
        add(today - timedelta(days=int(raw) if raw.isdigit() else _WORD_NUMBERS[raw]), match.group(0))
    return found, confirm, future


def _decide(found: dict[date, list[str]], confirm: list[date], future: bool, today: date,
            first_day: date | None) -> DayRef | None:
    past = {day: words for day, words in found.items() if day < today}
    if future and not past and not confirm:
        return DayRef("future")
    if confirm and not past:
        return _bounded(DayRef("confirm", ((confirm[0], ()),)), today, first_day)
    # Next to a certain day, the only past date of a later `dia {n}` is certain too.
    for day in confirm:
        past.setdefault(day, [f"dia {day.day}"])
    if not past:
        return None
    return _bounded(DayRef("bound", tuple((day, tuple(dict.fromkeys(past[day]))) for day in sorted(past))),
                    today, first_day)


def _bounded(ref: DayRef, today: date, first_day: date | None) -> DayRef:
    if any((today - day).days > WINDOW_DAYS or (first_day is not None and day < first_day) for day in ref.dates):
        return DayRef("too_old", ref.days)
    return ref


def resolve(text: str, today: date, first_day: date | None = None) -> DayRef | None:
    """The days the message names, or None when it names no past day (or only a copy source)."""
    clean = normalize(text)
    if _EQUALITY.search(clean):
        return None
    return _decide(*_collect(clean, today), today, first_day)


def resolve_answer(question: str, today: date, first_day: date | None = None) -> DayRef | None:
    """The answer to a confirmation: the one explicit date (`{n} de {mês}`) the previous assistant question names,
    as a bound day; None when the question names none or several."""
    clean = normalize(question)
    if not clean.rstrip().endswith("?"):
        return None
    dates = {d for m in _DATE.finditer(clean) if (d := _named_date(m, today)) is not None}
    if len(dates) != 1:
        return None
    day = dates.pop()
    if day >= today:
        return DayRef("future")
    return _bounded(DayRef("bound", ((day, ("confirmado",)),)), today, first_day)
