"""Build the contract JSON from the model payload. No second nutrition pass."""

from __future__ import annotations

from typing import Any

from config import DIGEST_MAX_CHARS, FALLBACK_QUESTION, MODEL

CHAT_FALLBACK_REPLY = "nao deu pra estimar"


def shape_estimate(payload: dict[str, Any]) -> dict[str, Any]:
    confidence = _confidence(payload.get("confidence"))
    body: dict[str, Any] = {
        "kcal": _number(payload.get("kcal")),
        "p": _number(payload.get("p")),
        "c": _number(payload.get("c")),
        "g": _number(payload.get("g")),
        "confidence": confidence,
        "items": [_item(item) for item in _list(payload.get("items"))],
        "model": MODEL,
    }
    if confidence != "high":
        question = payload.get("question")
        body["question"] = question.strip() if isinstance(question, str) and question.strip() else FALLBACK_QUESTION
    return body


def fail_estimate() -> dict[str, Any]:
    return {
        "kcal": 0,
        "p": 0,
        "c": 0,
        "g": 0,
        "confidence": "low",
        "question": FALLBACK_QUESTION,
        "items": [],
        "model": MODEL,
    }


def shape_fit(payload: dict[str, Any], *, budget_kcal: float, mode: str) -> dict[str, Any]:
    question = payload.get("question")
    if not isinstance(question, str) or not question.strip():
        raise ValueError("fit without question")
    if mode == "surprise":
        raw = payload.get("options")
        if not isinstance(raw, list) or len(raw) != 2:
            raise ValueError("surprise needs 2 options")
        options = [_option(item, budget_kcal) for item in raw]
        that_fit = [option for option in options if option["fits"]]
        chosen = that_fit[0] if that_fit else options[0]
        return {
            "dish": _dish(chosen),
            "fits": chosen["fits"],
            "question": question.strip(),
            "options": options,
        }
    dish = _option(payload.get("dish"), budget_kcal)
    return {
        "dish": _dish(dish),
        "fits": dish["fits"],
        "question": question.strip(),
    }


def fail_fit(mode: str) -> dict[str, Any]:
    empty = {"name": "", "portions": [], "kcal": 0, "p": 0}
    body: dict[str, Any] = {
        "dish": dict(empty),
        "fits": False,
        "question": FALLBACK_QUESTION,
    }
    if mode == "surprise":
        body["options"] = [{**empty, "fits": False}, {**empty, "fits": False}]
    return body


def shape_chat(
    payload: dict[str, Any],
    *,
    valid_slot_ids: set[str] | list[str] | None = None,
) -> dict[str, Any]:
    valid_ids = set(valid_slot_ids) if valid_slot_ids else set()

    reply = payload.get("reply")
    if not isinstance(reply, str) or not reply.strip():
        reply = CHAT_FALLBACK_REPLY
    else:
        reply = reply.strip()

    raw_estimate = payload.get("estimate")
    estimate: dict[str, Any] | None = None
    if isinstance(raw_estimate, dict):
        try:
            confidence = _confidence(raw_estimate.get("confidence"))
        except (TypeError, ValueError):
            confidence = "medium"

        raw_slot = raw_estimate.get("suggested_slot")
        suggested_slot = (
            str(raw_slot).strip()
            if isinstance(raw_slot, str) and str(raw_slot).strip() in valid_ids
            else None
        )

        raw_items = raw_estimate.get("items")
        items = []
        if isinstance(raw_items, list):
            for it in raw_items:
                if isinstance(it, dict):
                    items.append(_item(it))

        question = raw_estimate.get("question")
        if confidence != "high":
            clean_question = (
                question.strip()
                if isinstance(question, str) and question.strip()
                else FALLBACK_QUESTION
            )
        else:
            clean_question = None

        estimate = {
            "kcal": _number_or_zero(raw_estimate.get("kcal")),
            "p": _number_or_zero(raw_estimate.get("p")),
            "c": _number_or_zero(raw_estimate.get("c")),
            "g": _number_or_zero(raw_estimate.get("g")),
            "confidence": confidence,
            "question": clean_question,
            "items": items,
            "suggested_slot": suggested_slot,
        }

    raw_digest = payload.get("digest")
    digest = (
        raw_digest.strip()
        if isinstance(raw_digest, str) and raw_digest.strip()
        else None
    )

    return {
        "reply": reply,
        "estimate": estimate,
        "digest": digest,
        "model": MODEL,
    }


def fail_chat() -> dict[str, Any]:
    return {
        "reply": CHAT_FALLBACK_REPLY,
        "estimate": None,
        "digest": None,
        "model": MODEL,
    }


def shape_digest(payload: dict[str, Any]) -> dict[str, Any]:
    """compact=true OUT: digest only. reply "", estimate null."""
    raw = payload.get("digest")
    if not isinstance(raw, str) or not raw.strip():
        raise ValueError("digest missing")
    return {
        "reply": "",
        "estimate": None,
        "digest": raw.strip()[:DIGEST_MAX_CHARS],
        "model": MODEL,
    }


def fail_digest() -> dict[str, Any]:
    """Fail-soft compact: no digest. The client keeps its raw messages."""
    return {
        "reply": "",
        "estimate": None,
        "digest": None,
        "model": MODEL,
    }


def _number_or_zero(value: Any) -> int | float:
    try:
        return _number(value)
    except (TypeError, ValueError):
        return 0


def _confidence(value: Any) -> str:
    if not isinstance(value, str):
        raise TypeError("confidence missing")
    normal = value.strip().lower()
    mapping = {
        "high": "high",
        "alto": "high",
        "alta": "high",
        "medium": "medium",
        "medio": "medium",
        "média": "medium",
        "media": "medium",
        "low": "low",
        "baixa": "low",
        "baixo": "low",
    }
    if normal not in mapping:
        raise ValueError("unknown confidence")
    return mapping[normal]


def _number(value: Any) -> int | float:
    if isinstance(value, bool):
        raise TypeError("number missing")
    if isinstance(value, (int, float)):
        return value
    if isinstance(value, str):
        try:
            number = float(value.strip().replace(",", "."))
        except ValueError:
            raise TypeError("number missing") from None
        if number.is_integer():
            return int(number)
        return number
    raise TypeError("number missing")


def _list(value: Any) -> list[Any]:
    if value is None:
        return []
    if not isinstance(value, list):
        raise TypeError("list missing")
    return value


def _item(raw: Any) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise TypeError("invalid item")
    name = raw.get("name", raw.get("nome"))
    return {"name": str(name or ""), "g": _number_or_zero(raw.get("g")), "kcal": _number_or_zero(raw.get("kcal"))}


def _option(raw: Any, budget_kcal: float) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise TypeError("invalid dish")
    portions = []
    for portion in _list(raw.get("portions", raw.get("porcoes"))):
        if not isinstance(portion, dict):
            raise TypeError("invalid portion")
        portions.append(
            {
                "name": str(portion.get("name", portion.get("nome"))),
                "quantity": str(portion.get("quantity", portion.get("quantidade"))),
            }
        )
    kcal = _number(raw.get("kcal"))
    name = raw.get("name", raw.get("nome") or "")
    return {
        "name": str(name),
        "portions": portions,
        "kcal": kcal,
        "p": _number(raw.get("p")),
        "fits": kcal <= budget_kcal,
    }


def _dish(option: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": option["name"],
        "portions": option["portions"],
        "kcal": option["kcal"],
        "p": option["p"],
    }
