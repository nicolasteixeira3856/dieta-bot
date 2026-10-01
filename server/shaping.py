"""Build the contract JSON from the model payload. No second nutrition pass."""

from __future__ import annotations

import re
import unicodedata
from collections.abc import Iterable
from typing import Any

from config import (
    CHAT_FALLBACK_QUESTION,
    CLARIFY_MAX_ROUNDS,
    CLARIFY_REPEAT_JACCARD,
    DIGEST_MAX_CHARS,
    FACT_KEY_MAX,
    FACT_TEXT_MAX,
    FALLBACK_QUESTION,
    MEAL_TEXT_MAX,
    MEMORY_UPDATES_MAX,
    MEMORY_USED_MAX,
    MODEL,
)
from moderation import (
    IN_SCOPE,
    OUT_OF_SCOPE,
    POLICY_BLOCKED,
    SAFETY_SUPPORT,
    SCOPES,
    SUPPORT_VIOLENCE,
)

CHAT_FALLBACK_REPLY = "nao deu pra estimar"

# Fixed copy (docs/content-policy/specifications/refusal-copy.pt-BR.md, CP2). Never model text.
REFUSAL_OUT_OF_SCOPE = "Posso ajudar com refeições, porções e o orçamento alimentar do dia."
REFUSAL_BLOCKED = (
    "Não posso analisar esse conteúdo. Envie uma descrição de refeição ou uma foto de alimentos."
)
REFUSAL_PHOTO = (
    "Não identifiquei alimentos ou informações de um rótulo nessa imagem. "
    "Envie outra foto ou descreva a refeição."
)
REFUSAL_EATING = (
    "Não posso orientar práticas alimentares que possam causar dano. "
    "Se quiser conversar com alguém agora, o CVV atende 24 horas pelo 188. "
    "Procure também um profissional de saúde."
)
REFUSAL_VIOLENCE = (
    "Não consigo atender essa situação pelo app. Se houver risco imediato, ligue 190 ou 192."
)
REFUSAL_REPLIES = (
    REFUSAL_OUT_OF_SCOPE,
    REFUSAL_BLOCKED,
    REFUSAL_PHOTO,
    REFUSAL_EATING,
    REFUSAL_VIOLENCE,
)


def payload_scope(payload: dict[str, Any]) -> str:
    """The model's scope field. Missing or unknown is out_of_scope: the server fails closed."""
    scope = payload.get("scope")
    return scope if scope in SCOPES else OUT_OF_SCOPE


def refusal_reply(code: str, *, support: str | None = None, photo_only: bool = False) -> str:
    """Fixed copy for a non-in_scope code. photo_only: a photo with no text."""
    if code == POLICY_BLOCKED:
        return REFUSAL_BLOCKED
    if code == SAFETY_SUPPORT:
        return REFUSAL_VIOLENCE if support == SUPPORT_VIOLENCE else REFUSAL_EATING
    if code == IN_SCOPE:
        raise ValueError("in_scope is not a refusal")
    return REFUSAL_PHOTO if photo_only else REFUSAL_OUT_OF_SCOPE


def refuse_chat(reply: str) -> dict[str, Any]:
    """Chat refusal: the existing shape, intent question, nothing to record or remember."""
    return {**fail_chat(), "reply": reply}


def chat_output_texts(result: dict[str, Any]) -> list[str]:
    """Generated text of a chat response, for output moderation (one batched call)."""
    texts = [result.get("reply"), result.get("question"), result.get("digest")]
    estimate = result.get("estimate")
    if isinstance(estimate, dict):
        texts.append(estimate.get("question"))
        texts.append(
            "\n".join(
                [str(estimate.get("meal_text") or "")]
                + [str(item.get("name") or "") for item in estimate.get("items") or []]
            )
        )
    texts.append(
        "\n".join(
            f"{update.get('key')}: {update.get('text')}" for update in result.get("memory_updates") or []
        )
    )
    return _texts(texts)


def estimate_output_texts(result: dict[str, Any]) -> list[str]:
    names = "\n".join(str(item.get("name") or "") for item in result.get("items") or [])
    return _texts([result.get("question"), names])


def fit_output_texts(result: dict[str, Any]) -> list[str]:
    dishes = [result.get("dish"), *(result.get("options") or [])]
    lines: list[str] = []
    for dish in dishes:
        if not isinstance(dish, dict):
            continue
        lines.append(str(dish.get("name") or ""))
        lines.extend(f"{p.get('name')} {p.get('quantity')}" for p in dish.get("portions") or [])
    return _texts([result.get("question"), "\n".join(lines)])


def digest_output_texts(result: dict[str, Any]) -> list[str]:
    return _texts([result.get("digest")])


def _texts(values: list[Any]) -> list[str]:
    return [value for value in values if isinstance(value, str) and value.strip()]
CHAT_INTENTS = ("log", "plan", "question")
MEMORY_OPS = ("add", "reinforce", "replace", "remove")
FACT_KINDS = ("permanent", "dynamic")
FACT_CATEGORIES = ("preference", "portion", "routine")


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
    fact_ids: list[str] | None = None,
) -> dict[str, Any]:
    """fact_ids None = legacy client (no facts in the request): no memory fields, plan without card."""
    valid_ids = set(valid_slot_ids) if valid_slot_ids else set()
    legacy = fact_ids is None
    known_facts = set(fact_ids or [])

    reply = payload.get("reply")
    if not isinstance(reply, str) or not reply.strip():
        reply = CHAT_FALLBACK_REPLY
    else:
        reply = reply.strip()

    raw_estimate = payload.get("estimate")
    intent = payload.get("intent")
    if intent not in CHAT_INTENTS:
        intent = "log" if isinstance(raw_estimate, dict) else "question"

    estimate: dict[str, Any] | None = None
    if isinstance(raw_estimate, dict) and intent != "question":
        estimate = _chat_estimate(raw_estimate, valid_ids, plan=intent == "plan")
    if legacy and intent == "plan":
        # APK <= 0.0.3 shows a card for any estimate: grams and total stay in the reply.
        estimate = None

    raw_digest = payload.get("digest")
    digest = (
        raw_digest.strip()
        if isinstance(raw_digest, str) and raw_digest.strip()
        else None
    )

    return {
        "reply": reply,
        "intent": intent,
        "estimate": estimate,
        "memory_updates": [] if legacy else _memory_updates(payload.get("memory_updates"), known_facts, valid_ids),
        "memory_used": [] if legacy else _memory_used(payload.get("memory_used"), known_facts),
        "digest": digest,
        "model": MODEL,
    }


def _chat_estimate(raw: dict[str, Any], valid_ids: set[str], *, plan: bool) -> dict[str, Any]:
    try:
        confidence = _confidence(raw.get("confidence"))
    except (TypeError, ValueError):
        confidence = "medium"

    items = [_item(it) for it in raw.get("items") or [] if isinstance(it, dict)] if isinstance(
        raw.get("items"), list
    ) else []

    question = raw.get("question")
    if confidence == "high" or plan:
        # A plan never asks (ADR-023 decision 3).
        clean_question = None
    else:
        clean_question = (
            question.strip()
            if isinstance(question, str) and question.strip()
            else CHAT_FALLBACK_QUESTION
        )

    return {
        "kcal": _number_or_zero(raw.get("kcal")),
        "p": _number_or_zero(raw.get("p")),
        "c": _number_or_zero(raw.get("c")),
        "g": _number_or_zero(raw.get("g")),
        "confidence": confidence,
        "question": clean_question,
        "items": items,
        "suggested_slot": _slot_id(raw.get("suggested_slot"), valid_ids),
        "meal_text": _meal_text(raw.get("meal_text"), items),
    }


def _meal_text(value: Any, items: list[dict[str, Any]]) -> str:
    text = value.strip() if isinstance(value, str) else ""
    if not text:
        text = ", ".join(f"{it['name']} {_grams(it['g'])} g" for it in items if it["name"])
    return _cut_at_comma(text, MEAL_TEXT_MAX)


def _grams(value: int | float) -> str:
    return str(int(value)) if float(value).is_integer() else str(value)


def _cut_at_comma(text: str, limit: int) -> str:
    """Over the limit: cut at the last comma before it; no comma, a plain cut."""
    if len(text) <= limit:
        return text
    head = text[:limit]
    comma = head.rfind(",")
    return (head[:comma] if comma > 0 else head).strip()


def _memory_updates(raw: Any, known_facts: set[str], valid_slots: set[str]) -> list[dict[str, Any]]:
    """The app applies them; drop anything it could not apply safely (S11 § 5)."""
    if not isinstance(raw, list):
        return []
    out: list[dict[str, Any]] = []
    for update in raw:
        if len(out) == MEMORY_UPDATES_MAX:
            break
        if not isinstance(update, dict):
            continue
        op = update.get("op")
        kind = update.get("kind")
        category = update.get("category")
        if op not in MEMORY_OPS or kind not in FACT_KINDS or category not in FACT_CATEGORIES:
            continue
        fact_id = update.get("id")
        if op == "add":
            if fact_id is not None:
                continue
        elif fact_id not in known_facts:
            continue
        slot = _slot_id(update.get("slot"), valid_slots)
        if category == "routine" and slot is None:
            continue
        key = update.get("key").strip() if isinstance(update.get("key"), str) else ""
        text = update.get("text").strip() if isinstance(update.get("text"), str) else ""
        if not key or not text:
            continue
        out.append(
            {
                "op": op,
                "id": fact_id,
                "kind": kind,
                "category": category,
                "key": key[:FACT_KEY_MAX],
                "text": text[:FACT_TEXT_MAX],
                "slot": slot if category == "routine" else None,
            }
        )
    return out


def _memory_used(raw: Any, known_facts: set[str]) -> list[str]:
    if not isinstance(raw, list):
        return []
    used = [fid for fid in dict.fromkeys(v for v in raw if isinstance(v, str)) if fid in known_facts]
    return used[:MEMORY_USED_MAX]


# Log values of the release gate (ADR-026, S13). "none" = the gate did not apply.
CLARIFY_NONE = "none"
CLARIFY_ASKED = "asked"
CLARIFY_RELEASED_FORCE = "released_force"
CLARIFY_RELEASED_CONFIDENT = "released_confident"
CLARIFY_RELEASED_CAP = "released_cap"
CLARIFY_RELEASED_REPEAT = "released_repeat"

# Dropped before comparing questions. Short and fixed on purpose (S13 § 4).
_STOP_WORDS = frozenset(
    "a o as os um uma uns umas de do da dos das e ou em no na nos nas ao aos com sem por "
    "para pra pro que qual quais quanto quanta quantos quantas como foi era eram tinha tem "
    "voce vc voces se me te seu sua seus suas isso esse essa este esta mais ja la".split()
)
_TOKEN = re.compile(r"[a-z0-9]+")
_QUESTION_SENTENCE = re.compile(r"[^.?!\n]*\?")


def clarify_gate(
    result: dict[str, Any],
    payload: dict[str, Any],
    *,
    clarify_rounds: int,
    force_estimate: bool,
    history: Iterable[tuple[str, str]],
) -> tuple[dict[str, Any], str]:
    """v3 client (clarify_rounds present): a log with a material doubt becomes a question-only turn.

    result is the shape_chat output; payload the raw model JSON (its question, never the fallback).
    history is (role, text) of the request messages. Returns (response, clarify log value).
    """
    out = {**result, "question": None}
    estimate = out.get("estimate")
    if out.get("intent") != "log" or not isinstance(estimate, dict):
        if isinstance(estimate, dict):
            out["estimate"] = {**estimate, "question": None}
        return out, CLARIFY_NONE

    raw_estimate = payload.get("estimate") if isinstance(payload.get("estimate"), dict) else {}
    raw_question = raw_estimate.get("question")
    question = raw_question.strip() if isinstance(raw_question, str) else ""

    if force_estimate:
        clarify = CLARIFY_RELEASED_FORCE
    elif estimate.get("confidence") == "high" or not question:
        clarify = CLARIFY_RELEASED_CONFIDENT
    elif clarify_rounds >= CLARIFY_MAX_ROUNDS:
        clarify = CLARIFY_RELEASED_CAP
    elif any(role == "assistant" and repeats_question(question, text) for role, text in history):
        clarify = CLARIFY_RELEASED_REPEAT
    else:
        meal_text = str(estimate.get("meal_text") or "").strip().rstrip(".")
        out["reply"] = f"Entendi: {meal_text}.\n{question}" if meal_text else question
        out["estimate"] = None
        out["question"] = question
        return out, CLARIFY_ASKED

    out["estimate"] = {**estimate, "question": None}
    return out, clarify


def repeats_question(question: str, assistant_text: str) -> bool:
    """True when question repeats one already asked in an assistant turn.

    Only the question sentences of the turn (ending in ?) count. Token Jaccard at or above
    CLARIFY_REPEAT_JACCARD, or one token set containing the other, is a repeat.
    """
    new = _question_tokens(question)
    asked = _question_tokens(" ".join(_QUESTION_SENTENCE.findall(assistant_text)))
    if not new or not asked:
        return False
    if new <= asked or asked <= new:
        return True
    return len(new & asked) / len(new | asked) >= CLARIFY_REPEAT_JACCARD


def _question_tokens(text: str) -> set[str]:
    decomposed = unicodedata.normalize("NFKD", text.casefold())
    plain = "".join(ch for ch in decomposed if not unicodedata.combining(ch))
    return {token for token in _TOKEN.findall(plain) if token not in _STOP_WORDS}


def text_only_chat(text: str) -> dict[str, Any]:
    """The model answered without JSON: fixed scope refusal. The raw text never returns (CP2)."""
    return refuse_chat(REFUSAL_OUT_OF_SCOPE)


def fail_chat() -> dict[str, Any]:
    return {
        "reply": CHAT_FALLBACK_REPLY,
        "intent": "question",
        "estimate": None,
        "memory_updates": [],
        "memory_used": [],
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


def _slot_id(value: Any, valid_ids: set[str]) -> str | None:
    """Accepts "1", 1 and {"id": 1}. Returns the id only when it is a profile slot."""
    if isinstance(value, dict):
        value = value.get("id")
    if isinstance(value, bool):
        return None
    if isinstance(value, float) and value.is_integer():
        value = int(value)
    if not isinstance(value, (str, int)):
        return None
    slot = str(value).strip()
    return slot if slot in valid_ids else None


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
