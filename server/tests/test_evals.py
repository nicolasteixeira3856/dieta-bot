"""Tests for the Chat evaluator (S10): checks, 2-of-3 rule, cases and the run with a fake transport."""

from __future__ import annotations

import json
import os
import unittest
from typing import Any

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"

import httpx2

from evals import run
from evals.checks import FAIL, KNOWN, NA, PASS, case_status, evaluate, repetition_status
from main import ChatIn
from tests.test_api import FAKE_KEY, _envelope, _is_moderation, _moderation

V1_LOG = {
    "reply": "Café com 2 ovos e pão.",
    "estimate": {
        "kcal": 430,
        "p": 24,
        "c": 38,
        "g": 20,
        "confidence": "high",
        "question": None,
        "items": [{"name": "ovo", "g": 100, "kcal": 150}],
        "suggested_slot": "1",
    },
    "digest": None,
    "model": "gpt-6-luna",
}
V1_QUESTION = {"reply": "Oi! Tudo certo.", "estimate": None, "digest": None, "model": "gpt-6-luna"}


def _v2(**extra: Any) -> dict[str, Any]:
    out = json.loads(json.dumps(V1_LOG))
    out["intent"] = extra.pop("intent", "log")
    out["estimate"]["meal_text"] = extra.pop("meal_text", "2 ovos mexidos, 1 pão francês, 200 ml de leite")
    out["memory_updates"] = extra.pop("memory_updates", [])
    out["memory_used"] = extra.pop("memory_used", [])
    out.update(extra)
    return out


def _status(expect: dict[str, Any], output: dict[str, Any]) -> dict[str, str]:
    return {k: v["status"] for k, v in evaluate(expect, output).items()}


class CheckTests(unittest.TestCase):
    def test_passing_case(self) -> None:
        expect = {
            "intent": "log",
            "estimate": "present",
            "suggested_slot": "1",
            "kcal_range": [380, 520],
            "question": "absent",
            "question_not": ["leite"],
            "meal_text_has": ["ovo", "PAO", "leite"],
            "meal_text_not": ["Sempre uso"],
            "memory_updates_has": [{"op": "add", "kind": "permanent", "key": "leite"}],
            "memory_updates_not": [{"op": "remove"}],
            "memory_used_has": ["P1"],
            "reply_has": ["café"],
            "reply_not": ["registrei"],
        }
        output = _v2(
            memory_updates=[{"op": "add", "kind": "permanent", "key": "Leite", "id": None}],
            memory_used=["P1"],
        )
        results = _status(expect, output)
        self.assertEqual(set(results.values()), {PASS}, results)
        self.assertEqual(repetition_status(evaluate(expect, output)), PASS)

    def test_each_expectation_can_fail(self) -> None:
        output = _v2(
            intent="plan",
            meal_text="Sempre uso leite semidesnatado",
            memory_updates=[{"op": "remove", "id": "P2"}],
            memory_used=[],
            reply="Registrei o café.",
        )
        output["estimate"]["question"] = "Qual leite?"
        output["question"] = "Qual leite?"
        output["record"] = "ask"
        output["skip_slot"] = None
        output["digest"] = "Pergunta em aberto: qual leite?"
        failing = {
            "intent": "log",
            "estimate": "absent",
            "suggested_slot": "2",
            "kcal_range": [100, 200],
            "question": "absent",
            "question_not": ["qual"],
            "top_question": "absent",
            "top_question_not": ["leite"],
            "meal_text_has": ["ovo"],
            "meal_text_not": ["sempre uso"],
            "memory_updates_has": [{"op": "add", "key": "leite"}],
            "memory_updates_not": [{"op": "remove"}],
            "memory_used_has": ["P1"],
            "reply_has": ["ovo"],
            "reply_not": ["registrei"],
            "refusal": "out_of_scope",
            "record": "auto",
            "skip_slot": "1",
            "digest": "absent",
            "digest_has": ["arroz"],
            "digest_not": ["leite"],
        }
        self.assertEqual(set(failing), set(KNOWN))
        results = _status(failing, output)
        self.assertEqual(results, {key: FAIL for key in KNOWN})
        self.assertEqual(repetition_status(evaluate(failing, output)), FAIL)

    def test_estimate_checks_fail_without_estimate(self) -> None:
        expect = {"suggested_slot": "1", "kcal_range": [1, 2], "meal_text_has": ["ovo"], "question": "present"}
        self.assertEqual(set(_status(expect, V1_QUESTION).values()), {FAIL})

    def test_v2_fields_are_na_on_v1_output(self) -> None:
        expect = {
            "meal_text_has": ["ovo"],
            "meal_text_not": ["x"],
            "memory_updates_has": [{"op": "add"}],
            "memory_updates_not": [{"op": "add"}],
            "memory_used_has": [],
        }
        results = _status(expect, V1_LOG)
        self.assertEqual(set(results.values()), {NA})
        self.assertEqual(repetition_status(evaluate(expect, V1_LOG)), NA)
        # One applicable check decides the repetition.
        self.assertEqual(repetition_status(evaluate({**expect, "estimate": "present"}, V1_LOG)), PASS)

    def test_top_question_checks(self) -> None:
        """S13: top-level question of a v3 output; NA on an output without the field."""
        asked = {**V1_QUESTION, "intent": "log", "question": "Qual o tamanho do bife? E a batata, frita?"}
        released = {**_v2(), "question": None}
        self.assertEqual(_status({"top_question": "present"}, asked), {"top_question": PASS})
        self.assertEqual(_status({"top_question": ["bife", "batata"]}, asked), {"top_question": PASS})
        self.assertEqual(_status({"top_question": ["bife", "arroz"]}, asked), {"top_question": FAIL})
        self.assertEqual(_status({"top_question": ["bife"]}, released), {"top_question": FAIL})
        self.assertEqual(_status({"top_question": "absent"}, released), {"top_question": PASS})
        self.assertEqual(_status({"top_question_not": ["leite"]}, asked), {"top_question_not": PASS})
        self.assertEqual(_status({"top_question_not": ["BATATA"]}, asked), {"top_question_not": FAIL})
        self.assertEqual(
            _status({"top_question": "absent", "top_question_not": ["x"]}, _v2()),
            {"top_question": NA, "top_question_not": NA},
        )

    def test_record_checks(self) -> None:
        """S14: record and skip_slot of a v4 output; NA on an output without the fields."""
        skipped = {**V1_QUESTION, "intent": "skip", "record": "auto", "skip_slot": "1"}
        self.assertEqual(
            _status({"intent": "skip", "record": "auto", "skip_slot": "1"}, skipped),
            {"intent": PASS, "record": PASS, "skip_slot": PASS},
        )
        logged = {**_v2(), "question": None, "record": "ask", "skip_slot": None}
        self.assertEqual(
            _status({"record": "auto", "skip_slot": None}, logged),
            {"record": FAIL, "skip_slot": PASS},
        )
        self.assertEqual(_status({"record": ["auto", "ask"]}, logged), {"record": PASS})
        self.assertEqual(
            _status({"record": "none", "skip_slot": None}, _v2()),
            {"record": NA, "skip_slot": NA},
        )

    def test_v1_intent_is_deduced_from_estimate(self) -> None:
        self.assertEqual(_status({"intent": "log"}, V1_LOG), {"intent": PASS})
        self.assertEqual(_status({"intent": "plan"}, V1_LOG), {"intent": FAIL})
        self.assertEqual(_status({"intent": "question"}, V1_QUESTION), {"intent": PASS})

    def test_unknown_expectation_is_an_error(self) -> None:
        with self.assertRaises(ValueError):
            evaluate({"kcal": 1}, V1_LOG)

    def test_required_expectations_fail_on_na(self) -> None:
        expect = {"record": "auto", "estimate": "present"}
        self.assertEqual(evaluate(expect, V1_LOG)["record"]["status"], NA)
        checks = evaluate(expect, V1_LOG, required=["record"])
        self.assertEqual(checks["record"]["status"], FAIL)
        self.assertEqual(repetition_status(checks), FAIL)
        with self.assertRaises(ValueError):
            evaluate(expect, V1_LOG, required=["digest"])

    def test_digest_checks_require_content_even_for_negative_terms(self) -> None:
        expect = {"digest": "present", "digest_has": ["PAO"], "digest_not": ["registrad"]}
        self.assertEqual(set(_status(expect, {"digest": "Pão com queijo"}).values()), {PASS})
        self.assertEqual(set(_status(expect, {}).values()), {NA})
        for digest in (None, "", "   "):
            self.assertEqual(set(_status(expect, {"digest": digest}).values()), {FAIL})
        self.assertEqual(_status({"digest": "absent"}, {"digest": None}), {"digest": PASS})

    def test_refusal_check(self) -> None:
        from shaping import REFUSAL_EATING, REFUSAL_OUT_OF_SCOPE

        refused = {**_v2(), "reply": REFUSAL_OUT_OF_SCOPE, "estimate": None, "memory_updates": []}
        self.assertEqual(_status({"refusal": "out_of_scope"}, refused), {"refusal": PASS})
        self.assertEqual(_status({"refusal": "none"}, refused), {"refusal": FAIL})
        self.assertEqual(_status({"refusal": ["none", "out_of_scope"]}, refused), {"refusal": PASS})
        support = {**refused, "reply": REFUSAL_EATING}
        self.assertEqual(_status({"refusal": "safety_support"}, support), {"refusal": PASS})
        self.assertEqual(_status({"refusal": "none"}, _v2()), {"refusal": PASS})

    def test_strict_case_needs_every_repetition(self) -> None:
        self.assertEqual(case_status([PASS, PASS, FAIL], strict=True), FAIL)
        self.assertEqual(case_status([PASS, PASS, PASS], strict=True), PASS)

    def test_two_of_three(self) -> None:
        self.assertEqual(case_status([PASS, PASS, FAIL]), PASS)
        self.assertEqual(case_status([PASS, FAIL, FAIL]), FAIL)
        self.assertEqual(case_status([PASS, PASS, PASS]), PASS)
        self.assertEqual(case_status([PASS]), PASS)
        self.assertEqual(case_status([FAIL]), FAIL)
        self.assertEqual(case_status([NA, NA, NA]), NA)


class CaseFileTests(unittest.TestCase):
    def test_every_case_is_valid(self) -> None:
        cases = run.load_cases()
        self.assertGreaterEqual(len(cases), 20)
        ids = [c["id"] for c in cases]
        self.assertEqual(len(ids), len(set(ids)))
        for case in cases:
            with self.subTest(case=case["_file"]):
                self.assertEqual(case["_file"], case["id"] + ".json")
                self.assertIn(case["since"], ("v1", "v2", "v3", "v4", "v5", "cp2"))
                self.assertTrue(set(case.get("required", [])) <= set(case["expect"]))
                if case["since"] == "v5":
                    self.assertTrue(case["request"]["temp_facts"])
                    self.assertTrue(case["request"]["auto_record"])
                    self.assertIn("clarify_rounds", case["request"])
                if case.get("image"):
                    self.assertTrue((run.MEDIA_DIR / case["image"]).is_file())
                self.assertTrue(case["tags"])
                self.assertTrue(set(case["expect"]) <= set(KNOWN))
                body = ChatIn.model_validate(case["request"])
                slot_ids = {s.id for s in body.profile.slots}
                slot = case["expect"].get("suggested_slot")
                if slot is not None:
                    self.assertIn(slot, slot_ids)

    def test_select_by_id_and_tag(self) -> None:
        cases = run.load_cases()
        self.assertEqual([c["id"] for c in run.select_cases(cases, ["saudacao"], None)], ["saudacao"])
        self.assertTrue(all("memory" in c["tags"] for c in run.select_cases(cases, None, ["memory"])))
        with self.assertRaises(SystemExit):
            run.select_cases(cases, ["nao-existe"], None)


def _usage_envelope(payload: dict[str, Any]) -> dict[str, Any]:
    body = _envelope(json.dumps(payload, ensure_ascii=False))
    body["usage"] = {
        "input_tokens": 1000,
        "input_tokens_details": {"cached_tokens": 400},
        "output_tokens": 200,
        "output_tokens_details": {"reasoning_tokens": 50},
        "total_tokens": 1200,
    }
    return body


class RunTests(unittest.TestCase):
    def _case(self) -> dict[str, Any]:
        return next(c for c in run.load_cases() if c["id"] == "comi-pizza-pao-sirio")

    def test_run_effort_with_fake_transport(self) -> None:
        payload = {
            "reply": "Jantar com pizza de pão sírio.",
            "estimate": {
                **V1_LOG["estimate"],
                "kcal": 520,
                "suggested_slot": "5",
                "meal_text": "1 pão sírio, frango desfiado 120 g, muçarela 40 g",
            },
            "digest": None,
        }
        seen: list[dict[str, Any]] = []
        moderated: list[dict[str, Any]] = []

        def handler(request: httpx2.Request) -> httpx2.Response:
            if _is_moderation(request):
                moderated.append(json.loads(request.content))
                return httpx2.Response(200, json=_moderation())
            seen.append(json.loads(request.content))
            return httpx2.Response(200, json=_usage_envelope(payload))

        report = run.run_effort(
            "low", [self._case()], 3, FAKE_KEY, transport=httpx2.MockTransport(handler)
        )
        self.assertEqual(len(seen), 3)
        # Full orchestration (CP2): input and output moderation around each generation.
        self.assertEqual(len(moderated), 6)
        self.assertEqual({s["reasoning"]["effort"] for s in seen}, {"low"})
        self.assertEqual(report["pass_rate"], {"passed": 1, "failed": 0, "na": 0, "rate": 100.0})
        case = report["cases"][0]
        self.assertEqual((case["status"], case["passes"]), (PASS, 3))
        self.assertEqual(case["na_checks"], [])
        self.assertEqual(report["tokens"], {"input": 3000, "cached_input": 1200, "output": 600, "reasoning": 150})
        expected = (1800 * run.PRICE_INPUT + 1200 * run.PRICE_CACHED_INPUT + 600 * run.PRICE_OUTPUT) / 1e6
        self.assertAlmostEqual(report["cost_usd"], round(expected, 4))
        self.assertIsNotNone(report["latency_ms"]["p95"])

    def test_error_fails_the_repetition_without_leaking_the_key(self) -> None:
        def handler(request: httpx2.Request) -> httpx2.Response:
            raise httpx2.ConnectError("boom " + FAKE_KEY)

        report = run.run_effort("none", [self._case()], 1, FAKE_KEY, transport=httpx2.MockTransport(handler))
        case = report["cases"][0]
        self.assertEqual(case["status"], FAIL)
        self.assertIn("error", case["failed_checks"])
        self.assertNotIn(FAKE_KEY, json.dumps(report))

    def test_default_effort_of_the_route_stays_none(self) -> None:
        seen: list[dict[str, Any]] = []

        def handler(request: httpx2.Request) -> httpx2.Response:
            seen.append(json.loads(request.content))
            return httpx2.Response(200, json=_usage_envelope(V1_QUESTION))

        llm = run.LlmClient(api_key=FAKE_KEY, transport=httpx2.MockTransport(handler))
        try:
            llm.chat_json(user_text="oi", image_b64=None, slot_ids=["1"])
        finally:
            llm.close()
        self.assertEqual(seen[0]["reasoning"], {"effort": "none"})

    def test_compact_eval_uses_digest_schema_and_moderation_failure_path(self) -> None:
        case = next(c for c in run.load_cases() if c["id"] == "digest-pergunta-aberta")
        for mode in ("success", "flagged", "error"):
            seen = []
            moderated = []

            def handler(request: httpx2.Request) -> httpx2.Response:
                if _is_moderation(request):
                    moderated.append(json.loads(request.content))
                    return httpx2.Response(200, json=_moderation({"harassment": True} if mode == "flagged" else None))
                seen.append(json.loads(request.content))
                if mode == "error":
                    raise httpx2.ConnectError("down")
                return httpx2.Response(200, json=_usage_envelope({"digest": "Pergunta em aberto: qual o peso? (cheesecake)"}))

            with self.subTest(mode=mode):
                report = run.run_effort("none", [case], 1, FAKE_KEY, transport=httpx2.MockTransport(handler))
                result = report["cases"][0]
                self.assertEqual(result["status"], PASS if mode == "success" else FAIL)
                self.assertEqual(seen[0]["text"]["format"]["schema"]["required"], ["digest"])
                self.assertNotIn("PROFILE:", json.dumps(seen))
                self.assertEqual(len(moderated), 0 if mode == "error" else 1)
                if mode != "success":
                    self.assertIn("digest", result["failed_checks"])


if __name__ == "__main__":
    unittest.main()
