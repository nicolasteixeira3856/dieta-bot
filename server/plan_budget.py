"""Plan budget (ADR-039): the model reports reservations and the user's choice, the server does the arithmetic.

The decision line of a plan with options (ADR-056): the model chooses, the server writes the numbers.
"""
from __future__ import annotations

import math
import re
import unicodedata
from typing import Any

import reply_format

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
    default = window.limit_kcal if window is not None else remaining_kcal - sum(r["kcal"] for r in reserved)
    limit = fit_kcal if fit_kcal is not None else default
    return {
        "limit_kcal": limit,
        "over_kcal": option_over(kcal, limit),
        "reserved": reserved,
        "choice": model_budget["choice"],
    }


def needs_adjustment(budget: dict[str, Any], fit_kcal: int | None) -> bool:
    adjusting = fit_kcal is not None or budget["choice"] == "fit"
    return adjusting and budget["limit_kcal"] >= 1 and budget["over_kcal"] > 0


def option_over(kcal: float, limit: int) -> int:
    """The kcal an option passes its window by, 0 when it fits: the rounding of over_kcal."""
    return max(0, math.ceil(kcal - limit))


# Dev-log statuses of the decision line (S39).
DECISION_REWRITTEN = "rewritten"
DECISION_OFF_FORM = "off_form"
_DECISION = re.compile(r"^Vai de (?P<name>[^:\n]{1,80}): ~?\s?\d+(?:[.,]\d+)? kcal\b")
_OPTION_NUMBER = re.compile(r"^(?:a )?opcao (\d)\b")


def _words(text: str) -> str:
    """Case- and accent-insensitive words of a name, single-spaced."""
    decomposed = unicodedata.normalize("NFKD", text.casefold())
    bare = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    return " ".join(re.findall(r"\w+", bare))


def named_option(name: str, options: list[dict[str, Any]]) -> str | None:
    """The id of the option a decision line names: `Opção {n}`, the same name, one name inside the other, else
    the option sharing the most words of three letters or more. A tie names none."""
    said = _words(name)
    if not said:
        return None
    number = _OPTION_NUMBER.match(said)
    if number:
        return next((o["id"] for o in options if o.get("id") == f"o{number.group(1)}"), None)
    names = [(o.get("id"), _words(str(o.get("name") or ""))) for o in options]
    for same in (lambda own: own == said, lambda own: bool(own) and (own in said or said in own)):
        hits = [oid for oid, own in names if same(own)]
        if hits:
            return hits[0] if len(hits) == 1 else None
    said_words = {w for w in said.split() if len(w) >= 3}
    scores = sorted(((len(said_words & {w for w in own.split() if len(w) >= 3}), oid) for oid, own in names),
                    key=lambda s: s[0], reverse=True)
    if scores and scores[0][0] > 0 and (len(scores) == 1 or scores[0][0] > scores[1][0]):
        return scores[0][1]
    return None


def decision_option(reply: str, options: list[dict[str, Any]]) -> tuple[int | None, str | None]:
    """(index of the first line in the decision form, the option it names or None). (None, None) without one."""
    for index, line in enumerate(reply.split("\n")):
        m = _DECISION.match(reply_format.plain(line).strip())
        if m:
            return index, named_option(m.group("name"), options)
    return None, None


def decision_text(chosen: dict[str, Any], other: dict[str, Any] | None, limit: int, meal: str) -> str:
    """The decision line with the server's numbers: the chosen option against the window of its meal, then the
    other option's excess or that it fits too (ADR-056 decision 1)."""
    estimate = chosen["estimate"]
    head = f"Vai de {chosen['name']}: ~{round(estimate['kcal'])} kcal · P {round(estimate['p'])} g"
    over = option_over(estimate["kcal"], limit)
    other_over = option_over(other["estimate"]["kcal"], limit) if other is not None else None
    if over == 0:
        line = f"{head}, cabe na janela do {meal}."
        if other_over is None:
            return line
        return f"{line} {other['name']} " + (f"passa ~{other_over} kcal." if other_over else "também cabe.")
    line = f"{head}, passa ~{over} kcal da janela do {meal}"
    if other_over is None:
        return f"{line}."
    return f"{line}; {other['name']} passa ~{other_over}." if other_over else f"{line}. {other['name']} cabe."


def rewrite_decision(reply: str, options: list[dict[str, Any]], limit: int, meal: str) -> tuple[str, str]:
    """(reply, status). The decision line is rewritten with the server's numbers and opens the reply; a reply
    with no line in the form, or a line naming no option, stays as written (off_form)."""
    index, chosen_id = decision_option(reply, options)
    chosen = next((o for o in options if o["id"] == chosen_id), None)
    if index is None or chosen is None:
        return reply, DECISION_OFF_FORM
    other = next((o for o in options if o["id"] != chosen_id), None)
    lines = reply.split("\n")
    del lines[index]
    while lines and not lines[0].strip():
        del lines[0]
    return "\n".join([decision_text(chosen, other, limit, meal), *lines]), DECISION_REWRITTEN
