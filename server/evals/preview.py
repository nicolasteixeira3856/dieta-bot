"""App preview of a Chat answer for the evaluator's `--show` (S39). Print only: the app's own rendering is A70's.

Mirrors the plan bubble of the product Chat spec, rule 25 as ADR-056 § 8 changes it: the reply's lead text, one
block per option (`Opção {n}: {name}`, its items, `~{kcal} kcal · {p}P · {c}C · {g}G`, the fit line from
`over_kcal`, the day with that option, the actions), the trailing text and the budget note of the chosen option.
An answer without options is the reply as it comes.
"""

from __future__ import annotations

import re
from typing import Any

import meal_window
import reply_format

_TITLE = re.compile(r"^Opção \d+\b")
_TOTAL = re.compile(r"\d\s*kcal\s*[·|]")


def _num(value: Any) -> int:
    return round(float(value or 0))


def _option_plan(response: dict[str, Any]) -> dict[str, Any] | None:
    return next((a for a in response.get("actions") or [] if isinstance(a, dict) and a.get("type") == "plan"
                 and isinstance(a.get("options"), list) and a["options"]), None)


def _in_option(line: str) -> bool:
    """A line of an option paragraph after its title: a list or table line, or the option's total."""
    text = reply_format.plain(line).strip()
    if meal_window.is_closing(text):
        return False
    return line.startswith(("- ", "|")) or bool(_TOTAL.search(text))


def split_reply(reply: str) -> tuple[list[str], list[str]]:
    """(lead, trailing): the lines before the first `Opção {n}` paragraph and after the last one."""
    lines = reply.split("\n")
    titles = [i for i, line in enumerate(lines) if _TITLE.match(reply_format.plain(line).strip())]
    if not titles:
        return lines, []
    end = titles[-1] + 1
    while end < len(lines) and _in_option(lines[end]):
        end += 1
    return lines[: titles[0]], lines[end:]


def _item(item: dict[str, Any]) -> str:
    name, grams = str(item.get("name") or ""), _num(item.get("g"))
    return f"{name} ({grams} g)" if name[:1].isdigit() else f"{grams} g de {name}"


def bubble(request: dict[str, Any], response: dict[str, Any]) -> str:
    """The plan bubble as text."""
    reply = str(response.get("reply") or "")
    plan = _option_plan(response)
    if plan is None:
        return reply
    profile, day = request.get("profile") or {}, request.get("day") or {}
    names = {s.get("id"): s.get("name") for s in profile.get("slots") or []}
    meal = names.get(plan.get("slot")) or ""
    remaining = day.get("remaining_kcal")
    eaten_kcal, eaten_p = _num(day.get("eaten_kcal")), _num(day.get("eaten_p"))
    ceiling = eaten_kcal + _num(remaining) if remaining is not None else _num(profile.get("ceiling_kcal"))
    skipped = {a.get("slot") for a in response.get("actions") or [] if isinstance(a, dict) and a.get("type") == "skip"}
    states = {s.get("id"): s.get("status") for s in day.get("slots") or []}
    reserve = plan.get("slot") is not None and states.get(plan["slot"], "empty") == "empty" and plan["slot"] not in skipped
    lead, trailing = split_reply(reply)
    out = [line for line in lead if line.strip()]
    for n, option in enumerate(plan["options"], start=1):
        estimate = option.get("estimate") or {}
        block = [f"Opção {n}: {option.get('name')}"]
        block += [_item(i) for i in estimate.get("items") or [] if isinstance(i, dict)]
        block.append(f"~{_num(estimate.get('kcal'))} kcal · {_num(estimate.get('p'))}P · "
                     f"{_num(estimate.get('c'))}C · {_num(estimate.get('g'))}G")
        over = option.get("over_kcal")
        if isinstance(over, int):
            block.append(f"Cabe na janela do {meal}" if over == 0 else f"Passa {over} kcal da janela do {meal}")
        block.append(f"Dia: ~{eaten_kcal + _num(estimate.get('kcal'))} de {ceiling} kcal · "
                     f"P {eaten_p + _num(estimate.get('p'))} de {_num(profile.get('p_target'))}")
        block.append("[Registrar] [Reservar]" if reserve else "[Registrar]")
        out.append("")
        out += ["  │ " + line for line in block]
    rest = [line for line in trailing if line.strip()]
    if rest:
        out.append("")
        out += rest
    budget = plan.get("plan_budget")
    if isinstance(budget, dict) and budget.get("over_kcal"):
        out += ["", f"Passa {budget['over_kcal']} kcal do que sobra."]
    return "\n".join(out)
