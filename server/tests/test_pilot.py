"""Model pilot (evals.pilot): case sets, request validity and the comparison report."""

from __future__ import annotations

from evals.checks import evaluate
from evals.pilot import run as pilot
from main import ChatIn


def test_pilot_sets_are_complete() -> None:
    cases = pilot.load_pilot_cases()
    by_set: dict[str, list[str]] = {}
    for case in cases:
        by_set.setdefault(case["set"], []).append(case["id"])
    assert by_set["fail"] == list(pilot.FAIL_SET)
    assert len(pilot.FAIL_SET) <= 20
    assert 1 <= len(by_set["creative"]) <= 10
    assert len(by_set["consistency"]) == 1
    assert len(set(c["id"] for c in cases)) == len(cases)


def test_pilot_cases_are_valid_requests_with_known_expectations() -> None:
    for case in pilot.load_pilot_cases(("creative", "consistency")):
        body = ChatIn.model_validate(pilot.base.case_request(case))
        assert body.text.strip()
        assert case.get("note"), case["id"]
        # Unknown expectation keys raise; an empty output must only fail, never crash.
        results = evaluate(case["expect"], {}, required=case.get("required"))
        assert set(results) == set(case["expect"])


def test_consistency_case_repeats_more_than_the_run() -> None:
    (case,) = pilot.load_pilot_cases(("consistency",))
    assert case["repeat"] >= 6


def test_render_markdown_side_by_side() -> None:
    run = {
        "status": "pass", "latency_ms": 1200, "error": None, "failed": {},
        "answer": {
            "reply": "Omelete: 300 kcal", "intent": "plan", "record": "none", "question": None, "digest": None,
            "estimate": {"kcal": 300, "p": 20, "c": 4, "g": 22, "confidence": "medium",
                         "suggested_slot": "5", "meal_text": "omelete de 3 ovos", "question": None},
            "items": [{"name": "ovo", "g": 150, "kcal": 300}],
        },
    }
    case = {
        "id": "c1", "set": "creative", "status": "pass", "passes": 1, "repeat": 1, "tags": ["pilot"],
        "note": "n", "question": {"text": "o que como?", "local_time": "t", "remaining_kcal": 500,
                                   "compact": False, "image": None, "history": [], "facts": []},
        "runs": [run],
    }
    rate = {"passed": 1, "failed": 0, "na": 0, "rate": 100.0}
    report = {
        "provider": "luna", "model": "gpt-6-luna", "effort": "none", "pass_rate": rate,
        "by_set": {"fail": rate, "creative": rate, "consistency": {"passed": 0, "failed": 0, "na": 0, "rate": None}},
        "latency_ms": {"p50": 1200, "p95": 1200},
        "tokens_per_call": {"input": 7000, "cached_input": 0, "output": 200, "reasoning": 0},
        "cost_usd": 0.001, "price_per_1m": pilot.PROVIDERS["luna"]["price"], "cases": [case],
    }
    text = pilot.render_markdown({"luna": report})
    assert "Pendente: grok" in text
    assert "o que como?" in text and "Omelete: 300 kcal" in text and "300 kcal · 20P 4C 22G" in text
    assert pilot._spread([400, 440, 500]) == "400–500 kcal (mediana 440, spread 23%)"
