"""S18: validate a proposed mutation and compose additions from the supplied DAY aggregate."""

from __future__ import annotations

from copy import deepcopy
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
import math
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from config import MEAL_TEXT_MAX, COMPOSED_MEAL_TEXT_MAX
import estimate_total

NUTRIENTS = ("kcal", "p", "c", "g")


class AdditionItemIn(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    name: str = Field(min_length=1, max_length=MEAL_TEXT_MAX)
    g: float = Field(gt=0, strict=True)
    kcal: float = Field(ge=0, strict=True)


class AdditionIn(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    meal_text: str = Field(min_length=1, max_length=MEAL_TEXT_MAX)
    kcal: float = Field(ge=0, strict=True)
    p: float = Field(ge=0, strict=True)
    c: float = Field(ge=0, strict=True)
    g: float = Field(ge=0, strict=True)
    items: list[AdditionItemIn] = Field(min_length=1, max_length=100)


class TotalsIn(AdditionIn):
    """A whole-meal estimate whose items carried no usable grams (S28): totals alone, items empty."""

    items: list[AdditionItemIn] = Field(default_factory=list, max_length=0)


class PendingAdditionIn(BaseModel):
    model_config = ConfigDict(extra="forbid")
    base_slot: str | None
    addition: AdditionIn


def number(value: Any) -> Decimal:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError("invalid nutrient")
    result = Decimal(str(value))
    if not result.is_finite() or result < 0:
        raise ValueError("invalid nutrient")
    return result


def rounded(value: Any) -> int:
    try:
        return int(number(value).quantize(Decimal(1), rounding=ROUND_HALF_UP))
    except InvalidOperation as exc:
        raise ValueError("nutrient cannot be rounded") from exc


def wire(value: Decimal) -> int | float:
    if not math.isfinite(float(value)):
        raise ValueError("nutrient total overflow")
    return int(value) if value == value.to_integral_value() else float(value)


def display(value: int | float) -> str:
    # Avoid :g's default six significant digits: copy must match the wire value.
    return str(wire(number(value)))


def description(value: Any, limit: int) -> str:
    if not isinstance(value, str) or not value.strip() or len(value) > limit:
        raise ValueError("invalid meal description")
    return value


def nutrition(raw: dict[str, Any]) -> dict[str, Any]:
    """Round each supplied amount once; validate energy against the rounded item amounts."""
    parsed = AdditionIn.model_validate(raw).model_dump()
    description(parsed["meal_text"], MEAL_TEXT_MAX)
    out = {"meal_text": parsed["meal_text"], **{k: rounded(parsed[k]) for k in NUTRIENTS}}
    out["items"] = [
        {"name": description(it["name"], MEAL_TEXT_MAX), "g": it["g"], "kcal": rounded(it["kcal"])}
        for it in parsed["items"]
    ]
    if out["kcal"] != sum(it["kcal"] for it in out["items"]):
        raise ValueError("inconsistent item energy")
    if out["kcal"] == 0 and any(number(parsed[k]) > 0 for k in NUTRIENTS):
        raise ValueError("caloric addition rounded to zero")
    # Energy may include alcohol: never force 4P + 4C + 9G to equal kcal.
    return out


def usable_items(items: Any) -> list[dict[str, Any]]:
    """The items that count toward a whole-meal total (ADR-042): finite positive grams, finite nonnegative kcal.

    S28: a copied record often comes back as one item without grams; such items never counted toward the
    total and are dropped before validation instead of failing the turn.
    """
    kept: list[dict[str, Any]] = []
    for item in items if isinstance(items, list) else []:
        if not isinstance(item, dict):
            continue
        grams, kcal = item.get("g"), item.get("kcal")
        if isinstance(grams, bool) or isinstance(kcal, bool):
            continue
        if not isinstance(grams, (int, float)) or not isinstance(kcal, (int, float)):
            continue
        if not math.isfinite(grams) or grams <= 0 or not math.isfinite(kcal) or kcal < 0:
            continue
        kept.append(item)
    return kept


def totals_only(raw: dict[str, Any]) -> dict[str, Any]:
    """Round the supplied totals once with no items (S28): the whole-meal nutrition of a copied record."""
    parsed = TotalsIn.model_validate({**raw, "items": []}).model_dump()
    description(parsed["meal_text"], MEAL_TEXT_MAX)
    out = {"meal_text": parsed["meal_text"], **{k: rounded(parsed[k]) for k in NUTRIENTS}}
    if out["kcal"] == 0 and any(number(parsed[k]) > 0 for k in NUTRIENTS):
        raise ValueError("caloric addition rounded to zero")
    out["items"] = []
    return out


OCCUPIED_TEXT_MAX = 60


def occupied_question(slot: dict[str, Any], state: dict[str, Any]) -> str:
    """S31: name what the eaten slot already holds and ask add or replace. Never raises."""
    text = state.get("text")
    held = ""
    if isinstance(text, str) and text.strip():
        text = " ".join(text.split())
        if len(text) > OCCUPIED_TEXT_MAX:
            text = text[: OCCUPIED_TEXT_MAX - 1].rstrip(" ,;") + "…"
        held = f": {text}"
    kcal = state.get("kcal")
    if not isinstance(kcal, bool) and isinstance(kcal, (int, float)) and math.isfinite(kcal) and kcal >= 0:
        held += f" ({rounded(kcal)} kcal)"
    return f"{slot['name']} de hoje já tem registro{held}. Somo a esse registro ou substituo?"


def prepare_change(
    payload: dict[str, Any], profile: list[dict], day: list[dict], keep: frozenset[float] | set[float] = frozenset(),
) -> dict[str, Any]:
    """Validate before clarification can release a draft; never infer metadata from prose.

    keep: kcal of the supplied RECENT rows and eaten DAY slots (ADR-042). A `new`/`revise` total equal to
    one of them is a copied record: its numbers stand and its items, an unknown breakdown, are dropped.
    """
    out = deepcopy(payload)
    change = out.get("meal_change")
    estimate = out.get("estimate")
    if out.get("intent") != "log":
        out["meal_change"] = None
        return out
    if estimate is None and change is None:
        # An unresolved operation has no candidate record. The outer gate bounds the question.
        return out
    if "meal_change" in out and change is None and isinstance(estimate, dict):
        question = estimate.get("question")
        if isinstance(question, str) and question.strip():
            # An explicit null operation and a question cannot release provisional numbers.
            # Retain the question without interpreting prose to invent an operation.
            out.update(estimate=None, reply=question.strip())
            return out
    if not isinstance(estimate, dict) or not isinstance(change, dict):
        raise ValueError("missing meal change")
    if set(change) != {"operation", "base_slot", "addition"}:
        raise ValueError("invalid meal change fields")
    slots = {s["id"]: s for s in profile}
    states = {s["id"]: s for s in day}
    target = estimate.get("suggested_slot")
    base = change.get("base_slot")
    op = change.get("operation")
    if target is not None and (target not in slots or target not in states):
        raise ValueError("unknown meal target")
    occupied = target is not None and states[target]["status"] == "eaten"
    if base is not None and (base != target or not occupied):
        raise ValueError("invalid meal base")
    if op == "new" and base is None and occupied:
        # S31: a new meal aimed at an eaten slot is add or revise; the server never picks. The outer gate
        # asks (or, with no round left, states) instead of failing the turn.
        out.update(estimate=None, meal_change=None, reply=occupied_question(slots[target], states[target]))
        return out
    if op == "add":
        if occupied != (base is not None):
            raise ValueError("addition must preserve occupied target")
        addition = nutrition(change.get("addition"))
        change["addition"] = addition
        if base is None:
            estimate.update(addition)
        else:
            prior = states[base]
            text = description(prior.get("text"), COMPOSED_MEAL_TEXT_MAX)
            text = description(text + "; " + addition["meal_text"], COMPOSED_MEAL_TEXT_MAX)
            totals = {k: wire(number(prior.get(k)) + number(addition[k])) for k in NUTRIENTS}
            estimate.update(totals, meal_text=text, items=[])
    elif op in ("new", "revise"):
        if change.get("addition") is not None:
            raise ValueError("unexpected addition")
        if op == "new" and (base is not None or occupied):
            raise ValueError("new meal cannot overwrite occupied target")
        if op == "revise" and base is None:
            raise ValueError("revision needs an occupied base")
        raw = {k: estimate.get(k) for k in ("meal_text", *NUTRIENTS)}
        model_kcal = estimate.get("kcal")
        copied = (
            not isinstance(model_kcal, bool) and isinstance(model_kcal, (int, float))
            and any(float(model_kcal) == float(k) for k in keep)
        )
        if copied:
            # S28: a copied record keeps the record's numbers (ADR-042 keep rule); its breakdown is unknown.
            values = totals_only(raw)
        else:
            # ADR-042: the whole-meal total is the sum of the items; a mismatch is recomputed, not refused.
            estimate_total.apply(estimate)
            # S28: items that never counted (no usable grams or energy) are dropped, not refused; with none
            # left, the totals stand alone.
            kept = usable_items(estimate.get("items"))
            raw = {k: estimate.get(k) for k in ("meal_text", *NUTRIENTS)}
            values = nutrition({**raw, "items": kept}) if kept else totals_only(raw)
        estimate.update(values)
        if base is not None:
            for k in NUTRIENTS:
                number(states[base].get(k))
    else:
        raise ValueError("unknown meal operation")
    return out


def explain_change(result: dict[str, Any], payload: dict[str, Any], profile: list[dict], day: list[dict]) -> None:
    """Only released log estimates carry an actionable change; labels use validated numbers."""
    result["meal_change"] = None
    if result.get("record") not in ("auto", "ask") or not result.get("estimate"):
        return
    change = payload["meal_change"]
    estimate = result["estimate"]
    slots = {s["id"]: s for s in profile}
    states = {s["id"]: s for s in day}
    result["meal_change"] = change
    op, base = change["operation"], change["base_slot"]
    if op == "add":
        addition = change["addition"]
        lines = [f"{addition['meal_text']}: +{addition['kcal']} kcal"]
        if base is not None:
            name = slots[base]["name"]
            lines += [
                f"Já registrado no {name}: {display(states[base]['kcal'])} kcal",
                f"Total do {name}: {display(estimate['kcal'])} kcal",
            ]
        elif estimate["suggested_slot"] is None:
            lines.append("Escolha a refeição para este acréscimo.")
        else:
            lines.append(f"Total do {slots[estimate['suggested_slot']]['name']}: {display(estimate['kcal'])} kcal")
        result["reply"] = "\n".join(lines)
    elif op == "revise":
        result["reply"] = (
            f"Atualizar {slots[base]['name']}?\nAntes: {display(states[base]['kcal'])} kcal\n"
            f"Novo total: {display(estimate['kcal'])} kcal"
        )
