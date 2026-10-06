"""Model pilot (evals.pilot): case sets, request validity and the comparison report."""

from __future__ import annotations

from evals.checks import CASE_LEVEL, FAIL, PASS, evaluate, spread_check
from evals.pilot import run as pilot
from main import ChatIn


def test_pilot_sets_are_complete() -> None:
    cases = pilot.load_pilot_cases()
    by_set: dict[str, list[str]] = {}
    for case in cases:
        by_set.setdefault(case["set"], []).append(case["id"])
    assert by_set["fail"] == list(pilot.FAIL_SET)
    assert len(pilot.FAIL_SET) <= 20
    assert 10 <= len(by_set["creative"]) <= 12
    assert len(by_set["consistency"]) == 1
    assert len(by_set["rule"]) == 2
    assert len(set(c["id"] for c in cases)) == len(cases)


def test_pilot_cases_are_valid_requests_with_known_expectations() -> None:
    for case in pilot.load_pilot_cases(("creative", "consistency", "rule")):
        body = ChatIn.model_validate(pilot.base.case_request(case))
        assert body.text.strip()
        assert case.get("note"), case["id"]
        # Unknown expectation keys raise; an empty output must only fail, never crash.
        results = evaluate(case["expect"], {}, required=case.get("required"))
        assert set(results) == set(case["expect"]) - set(CASE_LEVEL)


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
        "by_set": {"fail": rate, "creative": rate, "consistency": {"passed": 0, "failed": 0, "na": 0, "rate": None}, "rule": rate},
        "latency_ms": {"p50": 1200, "p95": 1200},
        "tokens_per_call": {"input": 7000, "cached_input": 0, "output": 200, "reasoning": 0},
        "cost_usd": 0.001, "price_per_1m": pilot.PROVIDERS["luna"]["price"], "cases": [case],
    }
    text = pilot.render_markdown({"luna": report})
    assert "Pendente: grok" in text
    assert "o que como?" in text and "Omelete: 300 kcal" in text and "300 kcal · 20P 4C 22G" in text
    assert pilot._spread([400, 440, 500]) == "400–500 kcal (mediana 440, spread 23%)"


def _out(kcal):
    return {"estimate": {"kcal": kcal}} if kcal is not None else {"estimate": None}


def test_spread_check_judges_the_band_across_repetitions() -> None:
    ok = spread_check(10, [_out(k) for k in (400, 420, 430, 410)])
    assert ok["status"] == PASS and "spread 7%" in ok["detail"]
    wide = spread_check(10, [_out(k) for k in (390, 456, 430, 600)])
    assert wide["status"] == FAIL and "390-600 kcal" in wide["detail"]
    assert spread_check(10, [_out(400), _out(None)])["status"] == FAIL
    assert spread_check(10, [_out(400), _out(0)])["status"] == FAIL
    assert spread_check(10, [])["status"] == FAIL


def test_case_level_expectation_is_skipped_per_repetition_and_applied_in_summary() -> None:
    assert evaluate({"kcal_spread": 10, "intent": "log"}, {"intent": "log"}).keys() == {"intent"}
    case = {"id": "c", "tags": [], "expect": {"kcal_spread": 10, "intent": "log"}}
    reps = [{"status": PASS, "checks": {"intent": {"status": PASS, "detail": "ok"}}, "output": {"intent": "log", **_out(k)},
             "raw_output": None, "policy": None, "error": None, "latency_ms": 1,
             "usage": {"input": 0, "cached_input": 0, "output": 0, "reasoning": 0}} for k in (400, 500, 450)]
    report = pilot.base.summarize("none", [case], {"c": reps}, 3)
    assert report["cases"][0]["status"] == FAIL
    assert "kcal_spread" in report["cases"][0]["failed_checks"]
    assert report["cases"][0]["repeat"] == 3
