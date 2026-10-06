"""S13 (ADR-026): questions before the estimate and the 3-round release gate."""

from __future__ import annotations

import json
import os
import tempfile
import unittest
from pathlib import Path
from typing import Any

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"
os.environ["LLM_MODEL"] = "gpt-nao-usar"

import httpx2

import main
from config import CHAT_FALLBACK_QUESTION, CLARIFY_MAX_ROUNDS
from shaping import (
    CLARIFY_ASKED,
    CLARIFY_NONE,
    CLARIFY_RELEASED_CAP,
    CLARIFY_RELEASED_CONFIDENT,
    CLARIFY_RELEASED_FORCE,
    CLARIFY_RELEASED_REPEAT,
    clarify_gate,
    repeats_question,
    shape_chat,
)
from tests.test_api import INVITE, _client, _explodes, _responds, _mock, _is_moderation, _moderation
from tests.test_chat import _estimate, _fact, _v2_payload

QUESTION = "Quanto de macarrão? E o molho era com creme de leite ou requeijão?"
MEAL = "macarrão com frango ao molho branco"


def _model(**estimate: Any) -> dict[str, Any]:
    return {
        "reply": "Jantar de macarrão com frango.",
        "intent": estimate.pop("intent", "log"),
        "estimate": _estimate(
            **{"question": QUESTION, "meal_text": MEAL, "suggested_slot": "3", **estimate}
        ),
        "memory_updates": [],
        "memory_used": [],
        "digest": None,
    }


def _gate(
    payload: dict[str, Any],
    *,
    rounds: int = 0,
    force: bool = False,
    history: list[tuple[str, str]] | None = None,
) -> tuple[dict[str, Any], str]:
    result = shape_chat(payload, valid_slot_ids=["1", "3"], fact_ids=["P1"])
    return clarify_gate(
        result, payload, clarify_rounds=rounds, force_estimate=force, history=history or []
    )


class GateTableTests(unittest.TestCase):
    """One test per row of the S13 § 4 table, in its order."""

    def test_row1_force_releases_even_with_doubt_and_no_rounds(self) -> None:
        out, clarify = _gate(_model(), force=True)
        self.assertEqual(clarify, CLARIFY_RELEASED_FORCE)
        self.assertIsNotNone(out["estimate"])
        self.assertIsNone(out["estimate"]["question"])
        self.assertEqual(out["estimate"]["confidence"], "medium")
        self.assertIsNone(out["question"])

    def test_row2_high_confidence_releases(self) -> None:
        out, clarify = _gate(_model(confidence="high"))
        self.assertEqual(clarify, CLARIFY_RELEASED_CONFIDENT)
        self.assertIsNone(out["estimate"]["question"])
        self.assertIsNone(out["question"])

    def test_row2_empty_question_releases_without_fallback_question(self) -> None:
        for empty in (None, "", "   "):
            with self.subTest(question=empty):
                out, clarify = _gate(_model(question=empty, confidence="low"))
                self.assertEqual(clarify, CLARIFY_RELEASED_CONFIDENT)
                self.assertIsNone(out["estimate"]["question"])
                self.assertEqual(out["estimate"]["confidence"], "low")
                self.assertNotIn(CHAT_FALLBACK_QUESTION, json.dumps(out, ensure_ascii=False))

    def test_row3_cap_releases_at_three_rounds(self) -> None:
        out, clarify = _gate(_model(), rounds=CLARIFY_MAX_ROUNDS)
        self.assertEqual(clarify, CLARIFY_RELEASED_CAP)
        self.assertEqual(out["estimate"]["kcal"], 440)
        self.assertIsNone(out["estimate"]["question"])
        self.assertIsNone(out["question"])

    def test_row4_repeated_question_releases(self) -> None:
        history = [("user", "jantei " + MEAL), ("assistant", f"Entendi: {MEAL}.\n{QUESTION}")]
        out, clarify = _gate(_model(), rounds=1, history=history)
        self.assertEqual(clarify, CLARIFY_RELEASED_REPEAT)
        self.assertIsNotNone(out["estimate"])
        self.assertIsNone(out["question"])

    def test_row4_only_assistant_turns_count(self) -> None:
        out, clarify = _gate(_model(), rounds=1, history=[("user", QUESTION)])
        self.assertEqual(clarify, CLARIFY_ASKED)

    def test_row5_asks_with_draft_meal_and_question(self) -> None:
        payload = _model()
        payload["memory_updates"] = [
            {"op": "reinforce", "id": "P1", "kind": "permanent", "category": "preference",
             "key": "leite", "text": "Leite semidesnatado", "slot": None}
        ]
        payload["memory_used"] = ["P1"]
        out, clarify = _gate(payload, rounds=2)
        self.assertEqual(clarify, CLARIFY_ASKED)
        self.assertEqual(out["intent"], "log")
        self.assertIsNone(out["estimate"])
        self.assertEqual(out["question"], QUESTION)
        self.assertEqual(out["reply"], f"Entendi: {MEAL}.\n{QUESTION}")
        self.assertEqual(out["memory_used"], ["P1"])
        self.assertEqual(len(out["memory_updates"]), 1)

    def test_ask_without_meal_text_is_the_question_only(self) -> None:
        out, clarify = _gate(_model(meal_text="", items=[]))
        self.assertEqual(clarify, CLARIFY_ASKED)
        self.assertEqual(out["reply"], QUESTION)

    def test_ask_does_not_double_the_final_period(self) -> None:
        out, _ = _gate(_model(meal_text=MEAL + "."))
        self.assertEqual(out["reply"], f"Entendi: {MEAL}.\n{QUESTION}")

    def test_plan_question_and_null_estimate_are_untouched(self) -> None:
        plan, clarify = _gate(_model(intent="plan"))
        self.assertEqual(clarify, CLARIFY_NONE)
        self.assertIsNotNone(plan["estimate"])
        self.assertIsNone(plan["estimate"]["question"])
        self.assertIsNone(plan["question"])

        chat = {"reply": "Oi.", "intent": "question", "estimate": None}
        out, clarify = _gate(chat)
        self.assertEqual(clarify, CLARIFY_NONE)
        self.assertEqual(out["reply"], "Oi.")
        self.assertIsNone(out["estimate"])
        self.assertIsNone(out["question"])

        cannot = {"reply": "Não consegui estimar, o que foi?", "intent": "log", "estimate": None}
        out, clarify = _gate(cannot)
        self.assertEqual(clarify, CLARIFY_NONE)
        self.assertEqual(out["reply"], "Não consegui estimar, o que foi?")
        self.assertIsNone(out["question"])


class RepeatTests(unittest.TestCase):
    def test_same_question_with_accents_case_and_punctuation(self) -> None:
        self.assertTrue(
            repeats_question("o LEITE era integral ou desnatado", "Entendi: café.\nO leite era íntegral ou desnatado?")
        )

    def test_stop_words_are_ignored(self) -> None:
        self.assertTrue(
            repeats_question("Qual era o tipo do leite?", "Entendi: café.\nE o tipo de leite, qual foi?")
        )

    def test_subset_of_an_earlier_question_is_a_repeat(self) -> None:
        self.assertTrue(repeats_question("Quanto de macarrão?", f"Entendi: {MEAL}.\n{QUESTION}"))

    def test_paraphrase_below_threshold_is_not_a_repeat(self) -> None:
        self.assertFalse(
            repeats_question(
                "Quantas colheres de arroz e qual carne?",
                "Entendi: arroz e carne.\nFoi um prato raso ou fundo? A carne era grelhada?",
            )
        )

    def test_only_question_sentences_of_the_turn_count(self) -> None:
        # The meal line mentions leite; only "Qual o tamanho do pão?" was asked.
        turn = "Entendi: pão, 200 ml de leite integral.\nQual o tamanho do pão?"
        self.assertFalse(repeats_question("O leite era integral?", turn))
        self.assertFalse(repeats_question("Qual o leite?", "Café com leite, cerca de 430 kcal."))

    def test_empty_texts_never_repeat(self) -> None:
        self.assertFalse(repeats_question("", "Qual o leite?"))
        self.assertFalse(repeats_question("Qual o leite?", ""))
        self.assertFalse(repeats_question("o que?", "e a?"))


# Pre-S13 response of a legacy client for _model(): kept byte for byte.
LEGACY_SNAPSHOT = {
    "reply": "Jantar de macarrão com frango.",
    "intent": "log",
    "estimate": {
        "kcal": 440,
        "p": 25,
        "c": 38,
        "g": 22,
        "confidence": "medium",
        "question": QUESTION,
        "items": [
            {"name": "ovo mexido", "g": 100, "kcal": 150},
            {"name": "pao frances", "g": 50.5, "kcal": 290},
        ],
        "suggested_slot": "3",
        "meal_text": MEAL,
    },
    "memory_updates": [],
    "memory_used": [],
    "digest": None,
    "model": "gpt-6-luna",
}


class ClarifyRouteTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self._apps: list[Any] = []
        self._tmp = tempfile.TemporaryDirectory()
        self.log_path = Path(self._tmp.name) / "conversations.jsonl"
        self._saved = os.environ.get("CONVERSATION_LOG_PATH")
        os.environ["CONVERSATION_LOG_PATH"] = str(self.log_path)

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()
            app.state.conversation_log.close()
        if self._saved is None:
            os.environ.pop("CONVERSATION_LOG_PATH", None)
        else:
            os.environ["CONVERSATION_LOG_PATH"] = self._saved
        self._tmp.cleanup()

    async def _post(self, payload: dict[str, Any], handler) -> Any:
        app = main.create_app(transport=_mock(handler))
        self._apps.append(app)
        async with _client(app) as client:
            return await client.post("/v1/chat", headers={"X-Invite": INVITE}, json=payload)

    def _log(self) -> dict[str, Any]:
        [line] = [json.loads(l) for l in self.log_path.read_text(encoding="utf-8").splitlines()]
        return line

    async def test_clarify_rounds_bounds(self) -> None:
        for rounds, status in ((-1, 422), (0, 200), (3, 200), (4, 422)):
            with self.subTest(rounds=rounds):
                response = await self._post(
                    _v2_payload(facts=[], clarify_rounds=rounds), _responds(_model(), [])
                )
                self.assertEqual(response.status_code, status)

    async def test_legacy_request_keeps_the_pre_s13_response(self) -> None:
        for extra in ({}, {"force_estimate": True}):
            with self.subTest(extra=extra):
                response = await self._post(
                    _v2_payload(facts=[_fact()], **extra), _responds(_model(), [])
                )
                self.assertEqual(response.status_code, 200)
                self.assertEqual(response.content, json.dumps(
                    LEGACY_SNAPSHOT, ensure_ascii=False, separators=(",", ":")
                ).encode("utf-8"))

    async def test_legacy_request_keeps_the_fallback_question(self) -> None:
        response = await self._post(
            _v2_payload(facts=[]), _responds(_model(question=None, confidence="low"), [])
        )
        self.assertEqual(response.json()["estimate"]["question"], CHAT_FALLBACK_QUESTION)
        self.assertNotIn("question", {k for k in response.json() if k != "estimate"})

    async def test_v3_question_only_turn_and_log(self) -> None:
        response = await self._post(
            _v2_payload(facts=[], clarify_rounds=0, text="jantei " + MEAL), _responds(_model(), [])
        )
        body = response.json()
        self.assertEqual(body["intent"], "log")
        self.assertIsNone(body["estimate"])
        self.assertEqual(body["question"], QUESTION)
        self.assertEqual(body["reply"], f"Entendi: {MEAL}.\n{QUESTION}")
        line = self._log()
        self.assertEqual(line["clarify"], CLARIFY_ASKED)
        self.assertEqual(line["clarify_rounds"], 0)
        self.assertEqual(line["response"], body)

    async def test_v3_force_releases_the_estimate(self) -> None:
        response = await self._post(
            _v2_payload(facts=[], clarify_rounds=1, force_estimate=True, text="Pode estimar assim."),
            _responds(_model(), []),
        )
        body = response.json()
        self.assertEqual(body["estimate"]["kcal"], 440)
        self.assertIsNone(body["estimate"]["question"])
        self.assertIsNone(body["question"])
        self.assertEqual(self._log()["clarify"], CLARIFY_RELEASED_FORCE)

    async def test_held_slot_is_v5_only_and_sanitized(self) -> None:
        for slot in ("1", None, "unknown"):
            for version in (3, 4, 5):
                with self.subTest(slot=slot, version=version):
                    response = await self._post(
                        _v2_payload(facts=[], clarify_rounds=0, auto_record=version >= 4,
                                    temp_facts=version == 5, text="comi " + MEAL),
                        _responds(_model(suggested_slot=slot), []),
                    )
                    body = response.json()
                    self.assertIsNone(body["estimate"])
                    self.assertEqual(body["question"], QUESTION)
                    if version == 5:
                        self.assertEqual(body["question_slot"], "1" if slot == "1" else None)
                    else:
                        self.assertNotIn("question_slot", body)

    async def test_v5_released_fallback_and_refusal_have_no_held_slot(self) -> None:
        for handler in (_responds(_model(), []), _explodes(httpx2.ConnectError("down")),
                        _responds({"scope": "out_of_scope"}, [])):
            response = await self._post(
                _v2_payload(facts=[], temp_facts=True, clarify_rounds=3, auto_record=True), handler,
            )
            self.assertNotIn("question_slot", response.json())

    async def test_output_refusal_clears_held_slot_and_temp_updates(self) -> None:
        moderation_calls = []
        model = _model()
        model["memory_updates"] = [{"op": "add", "id": None, "kind": "temp", "category": "portion",
                                    "key": "reference", "text": "reference sentinel", "slot": None}]

        def handler(request: httpx2.Request) -> httpx2.Response:
            if _is_moderation(request):
                moderation_calls.append(1)
                return httpx2.Response(200, json=_moderation({"harassment": True} if len(moderation_calls) > 1 else None))
            return _responds(model, [])(request)

        app = main.create_app(transport=httpx2.MockTransport(handler))
        self._apps.append(app)
        async with _client(app) as client:
            response = await client.post("/v1/chat", headers={"X-Invite": INVITE},
                                         json=_v2_payload(facts=[], temp_facts=True, clarify_rounds=0, auto_record=True))
        body = response.json()
        self.assertEqual(len(moderation_calls), 2)
        self.assertEqual(body["memory_updates"], [])
        self.assertNotIn("question_slot", body)
        self.assertEqual(body["record"], "none")
        self.assertIsNone(self._log()["question_slot"])

    async def test_v3_fallback_has_null_question_and_no_generic_question(self) -> None:
        response = await self._post(
            _v2_payload(facts=[], clarify_rounds=2), _explodes(httpx2.ConnectError("down"))
        )
        body = response.json()
        self.assertIsNone(body["estimate"])
        self.assertIn("question", body)
        self.assertIsNone(body["question"])
        line = self._log()
        self.assertEqual(line["clarify"], CLARIFY_NONE)
        self.assertEqual(line["clarify_rounds"], 2)

    async def test_legacy_log_line_has_clarify_none(self) -> None:
        await self._post(_v2_payload(facts=[]), _responds(_model(), []))
        line = self._log()
        self.assertEqual(line["clarify"], CLARIFY_NONE)
        self.assertIsNone(line["clarify_rounds"])

    async def test_instructions_ask_everything_at_once_and_never_repeat(self) -> None:
        captured: list[httpx2.Request] = []
        await self._post(_v2_payload(facts=[]), _responds(_model(), captured))
        instructions = json.loads(captured[0].content)["instructions"]
        self.assertIn("every open doubt of the meal", instructions)
        self.assertIn("at most 3 short questions", instructions)
        self.assertIn("Never repeat a question already asked in HISTORY", instructions)
        self.assertIn("reply states in one short line what was assumed", instructions)
        self.assertNotIn("One question per meal", instructions)

    async def test_unavailable_details_release_with_honest_confidence_on_v4_and_v5(self) -> None:
        instructions = []
        for temp_facts in (False, True):
            with self.subTest(temp_facts=temp_facts):
                captured = []
                model = _model(question=None, confidence="low")
                model.update(record_intent="clear", meal_day="today")
                response = await self._post(
                    _v2_payload(facts=[], clarify_rounds=0, force_estimate=False,
                                auto_record=True, temp_facts=temp_facts,
                                text="Jantei macarrão com frango. Não sei peso nem tamanho."),
                    _responds(model, captured),
                )
                self.assertEqual(response.status_code, 200)
                body = response.json()
                self.assertEqual(body["estimate"]["confidence"], "low")
                self.assertEqual(body["estimate"]["meal_text"], MEAL)
                self.assertIsNone(body["estimate"]["question"])
                self.assertIsNone(body["question"])
                self.assertEqual(body["record"], "auto")
                self.assertNotIn("question_slot", body)
                instructions.append(json.loads(captured[0].content)["instructions"])
        self.assertEqual(instructions[0], instructions[1])
        for phrase in ("known, omitted and explicitly unavailable", "EACH food and attribute",
                       "another unit", "supplied alternative", "medium/low confidence",
                       "measurement or correction overrides earlier unavailability",
                       "never becomes a memory fact"):
            self.assertIn(phrase, instructions[0])


if __name__ == "__main__":
    unittest.main()
