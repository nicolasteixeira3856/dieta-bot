"""Named-day record resolved in code (S33): "o mesmo almoço de ontem" finds its RECENT row before the model runs.

An equality word, a day word and a slot named (or implied by the eating verb) in one clause of the message pick
the RECENT rows of that slot on that date. One row is a COPY_SOURCE block the model copies; several rows are an
`ambiguous` block that lists them; none is no block (the model asks what was eaten). One block per clause that
names a day. Habitual words ("de sempre") never produce a block: they follow branches A/B of ADR-023.
Pure functions, no I/O.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Any, Sequence

WEEKDAYS = ("segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo")
_EQUALITY = r"(mesm[oa]s?|igual|repeti|o de|a de|os de|as de)"
_DAYWORD = r"(ontem|anteontem|" + "|".join(WEEKDAYS) + r"|semana passada)"
_VERBS = {"almocei": "almoco", "jantei": "jantar", "lanchei": "lanche", "ceei": "ceia", "tomei cafe": "cafe"}
_CLAUSE_SPLIT = re.compile(r"[,;.\n]| e (?=[a-z])")
_NAMED = re.compile(_EQUALITY + r"\s+(?:de |do |da |na |no |ao de |a de )?" + _DAYWORD)
_BARE = re.compile(r"\b(?:de|da|ao de|a de) " + _DAYWORD + r"\b")


@dataclass(frozen=True)
class Block:
    """One resolved clause: the rows of `slot_id` on `day`; `ambiguous` when more than one."""

    day: date
    slot_id: str
    rows: tuple[Any, ...]

    @property
    def ambiguous(self) -> bool:
        return len(self.rows) > 1


def normalize(text: str) -> str:
    decomposed = unicodedata.normalize("NFKD", text.casefold())
    return "".join(ch for ch in decomposed if not unicodedata.combining(ch))


def _slot_for(clause: str, slots: Sequence[Any]) -> str | None:
    """The PROFILE slot named in the clause (longest distinct name wins), else the one its eating verb implies."""
    hits = [s for s in slots if normalize(s.name) in clause]
    if len(hits) == 1:
        return hits[0].id
    if len(hits) > 1:
        hits.sort(key=lambda s: -len(s.name))
        return hits[0].id if normalize(hits[0].name) != normalize(hits[1].name) else None
    for verb, key in _VERBS.items():
        if verb in clause:
            found = [s for s in slots if key in normalize(s.name) or (key == "jantar" and "janta" in normalize(s.name))]
            return found[0].id if len(found) == 1 else None
    # "café de ontem" with a slot named "Café da manhã": the first word of one slot name, as a word.
    words = set(re.findall(r"[a-z]+", clause))
    if "janta" in words:
        words.add("jantar")
    found = [s for s in slots if (normalize(s.name).split() or [""])[0] in words]
    return found[0].id if len(found) == 1 else None


def _target(word: str, today: date) -> date:
    if word == "ontem":
        return today - timedelta(days=1)
    if word == "anteontem":
        return today - timedelta(days=2)
    if word == "semana passada":
        return today - timedelta(days=7)
    return today - timedelta(days=(today.weekday() - WEEKDAYS.index(word)) % 7 or 7)


def _day_word(clause: str) -> str | None:
    match = _NAMED.search(clause)
    if match:
        return match.group(2)
    bare = _BARE.search(clause)
    return bare.group(1) if bare and re.search(_EQUALITY, clause) else None


def resolve(text: str, today: date, slots: Sequence[Any], recent: Sequence[Any]) -> list[Block]:
    """The COPY_SOURCE blocks of a message. slots: objects with id and name; recent: objects with date and slot_id."""
    whole = normalize(text)
    blocks: list[Block] = []
    for clause in _CLAUSE_SPLIT.split(whole):
        if not clause.strip():
            continue
        word = _day_word(clause)
        if word is None:
            continue
        slot_id = _slot_for(clause, slots)
        if slot_id is None and len(re.findall(_DAYWORD, whole)) == 1:
            # "almoço, o mesmo de ontem": the meal word sits in another clause; used only with one day word.
            slot_id = _slot_for(whole, slots)
        if slot_id is None:
            continue
        day = _target(word, today)
        rows = tuple(r for r in recent if r.date == day and r.slot_id == slot_id)
        if rows and not any(b.day == day and b.slot_id == slot_id for b in blocks):
            blocks.append(Block(day, slot_id, rows))
    return blocks
