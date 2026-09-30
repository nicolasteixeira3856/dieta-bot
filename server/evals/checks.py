"""Expectations of an eval case against one shaped /v1/chat output.

Each check returns PASS, FAIL or NA. NA = the output contract does not carry the field yet
(v2 fields on a v1 output). A case repetition passes when every applicable check passes.
"""

from __future__ import annotations

import math
import unicodedata
from typing import Any

PASS = "pass"
FAIL = "fail"
NA = "n/a"

# Expectation keys this module knows. Anything else in a case is a typo.
KNOWN = (
    "intent",
    "estimate",
    "suggested_slot",
    "kcal_range",
    "meal_text_has",
    "meal_text_not",
    "question",
    "memory_updates_has",
    "memory_updates_not",
    "memory_used_has",
    "reply_has",
    "reply_not",
)

INTENTS = ("log", "plan", "question")


def normalize(text: str) -> str:
    """Case- and accent-insensitive, so "Pão" matches "pao"."""
    decomposed = unicodedata.normalize("NFKD", text.casefold())
    return "".join(ch for ch in decomposed if not unicodedata.combining(ch))


def output_intent(output: dict[str, Any]) -> tuple[str, bool]:
    """(intent, deduced). v1 has no intent: estimate present = log, else question."""
    intent = output.get("intent")
    if intent in INTENTS:
        return intent, False
    return ("log" if output.get("estimate") else "question"), True


def evaluate(expect: dict[str, Any], output: dict[str, Any]) -> dict[str, dict[str, Any]]:
    """{check name: {"status": PASS|FAIL|NA, "detail": str}} for every expectation present."""
    results: dict[str, dict[str, Any]] = {}
    estimate = output.get("estimate") if isinstance(output.get("estimate"), dict) else None

    for key, want in expect.items():
        if key not in KNOWN:
            raise ValueError(f"unknown expectation: {key}")
        results[key] = _check(key, want, output, estimate)
    return results


def _check(
    key: str, want: Any, output: dict[str, Any], estimate: dict[str, Any] | None
) -> dict[str, Any]:
    if key == "intent":
        got, deduced = output_intent(output)
        return _result(got == want, f"got {got}" + (" (deduced)" if deduced else ""))

    if key == "estimate":
        got = "present" if estimate else "absent"
        return _result(got == want, f"got {got}")

    if key == "question":
        question = estimate.get("question") if estimate else None
        got = "present" if isinstance(question, str) and question.strip() else "absent"
        return _result(got == want, f"got {got}" + (f": {question}" if got == "present" else ""))

    if key == "suggested_slot":
        if estimate is None:
            return _result(False, "no estimate")
        got = estimate.get("suggested_slot")
        return _result(got == want, f"got {got}")

    if key == "kcal_range":
        if estimate is None:
            return _result(False, "no estimate")
        low, high = want
        got = estimate.get("kcal")
        ok = isinstance(got, (int, float)) and low <= got <= high
        return _result(ok, f"got {got}")

    if key in ("meal_text_has", "meal_text_not"):
        if estimate is None:
            return _result(False, "no estimate")
        if "meal_text" not in estimate:
            return _na("no meal_text in output")
        return _terms(key.endswith("_has"), want, str(estimate.get("meal_text") or ""))

    if key in ("reply_has", "reply_not"):
        return _terms(key.endswith("_has"), want, str(output.get("reply") or ""))

    if key in ("memory_updates_has", "memory_updates_not"):
        if "memory_updates" not in output:
            return _na("no memory_updates in output")
        updates = [u for u in output.get("memory_updates") or [] if isinstance(u, dict)]
        missing = [w for w in want if not any(_matches(u, w) for u in updates)]
        if key == "memory_updates_has":
            return _result(not missing, f"missing {missing}; got {updates}" if missing else "ok")
        found = [w for w in want if w not in missing]
        return _result(not found, f"unwanted {found}; got {updates}" if found else "ok")

    if key == "memory_used_has":
        if "memory_used" not in output:
            return _na("no memory_used in output")
        used = output.get("memory_used") or []
        missing = [w for w in want if w not in used]
        return _result(not missing, f"missing {missing}; got {used}" if missing else "ok")

    raise ValueError(f"unknown expectation: {key}")


def _terms(must_have: bool, terms: list[str], text: str) -> dict[str, Any]:
    haystack = normalize(text)
    hits = [t for t in terms if normalize(t) in haystack]
    if must_have:
        missing = [t for t in terms if t not in hits]
        return _result(not missing, f"missing {missing} in {text!r}" if missing else "ok")
    return _result(not hits, f"found {hits} in {text!r}" if hits else "ok")


def _matches(update: dict[str, Any], want: dict[str, Any]) -> bool:
    for field, value in want.items():
        got = update.get(field)
        if isinstance(value, str) and isinstance(got, str):
            if normalize(got) != normalize(value):
                return False
        elif got != value:
            return False
    return True


def _result(ok: bool, detail: str) -> dict[str, Any]:
    return {"status": PASS if ok else FAIL, "detail": detail}


def _na(detail: str) -> dict[str, Any]:
    return {"status": NA, "detail": detail}


def repetition_status(results: dict[str, dict[str, Any]]) -> str:
    """PASS when every applicable check passes; NA when none applies."""
    statuses = [r["status"] for r in results.values()]
    if FAIL in statuses:
        return FAIL
    if PASS in statuses:
        return PASS
    return NA


def needed_passes(repeat: int) -> int:
    """2 of 3 (S10). Generalised: at least two thirds, at least one."""
    return max(1, math.ceil(repeat * 2 / 3))


def case_status(repetitions: list[str]) -> str:
    """Case verdict from its repetition verdicts."""
    applicable = [s for s in repetitions if s != NA]
    if not applicable:
        return NA
    passes = sum(1 for s in repetitions if s == PASS)
    return PASS if passes >= needed_passes(len(repetitions)) else FAIL
