"""S14 (ADR-028): record mark (auto | ask | none) and skip by text for a v4 client."""

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
from llm import chat_format
from shaping import (
    REFUSAL_OUT_OF_SCOPE,
    RECORD_ASK_NO_SLOT,
    RECORD_ASK_UNSURE,
    RECORD_AUTO_LOG,
    RECORD_AUTO_SKIP,
    RECORD_NONE_INTENT,
    RECORD_NONE_OTHER_DAY,
    RECORD_NONE_POLICY,
    RECORD_NONE_SKIP_SLOT,
    clarify_gate,
    record_gate,
    shape_chat,
)
from tests.test_api import INVITE, _client, _explodes, _is_moderation, _moderation, _responds, _mock
from tests.test_chat import _estimate, _v2_payload

SLOTS = ["1", "3"]
USER_TEXT = "Na janta comi arroz, feijao e 150 g de frango grelhado"


def _model(intent: str = "log", **fields: Any) -> dict[str, Any]:
    """Model payload of a released log of slot 1, clear and of today, unless overridden."""
    estimate = fields.pop("estimate", _estimate(confidence="high", question=None))
    return {
        "reply": "Jantar de arroz, feijao e frango.",
        "intent": intent,
        "estimate": None if intent in ("question", "skip") else estimate,
        "record_intent": "clear",
        "meal_day": "today",
        "skip_slot": None,
        "memory_updates": [],
        "memory_used": [],
        "digest": None,
        "scope": "in_scope",
        **fields,
    }


def _gate(
    payload: dict[str, Any],
    *,
    rounds: int = 0,
    force: bool = False,
    photo_only: bool = False,
) -> tuple[dict[str, Any], str]:
    result = shape_chat(payload, valid_slot_ids=SLOTS, fact_ids=[], skip=True)
    result, _ = clarify_gate(
        result, payload, clarify_rounds=rounds, force_estimate=force, history=[]
    )
    return record_gate(
        result, payload, valid_slot_ids=SLOTS, force_estimate=force, photo_only=photo_only
    )


class RecordGateTests(unittest.TestCase):
    """One test per row of the S14 § 4 table, in its order."""

    def test_row1_not_in_scope_is_none_policy(self) -> None:
        for scope in ("out_of_scope", "policy_blocked", "safety_support", None):
            with self.subTest(scope=scope):
                out, log = _gate(_model(scope=scope))
                self.assertEqual((out["record"], log), ("none", RECORD_NONE_POLICY))
                self.assertIsNone(out["skip_slot"])

    def test_row2_question_and_plan_are_none_intent(self) -> None:
        for intent in ("question", "plan"):
            with self.subTest(intent=intent):
                out, log = _gate(_model(intent))
                self.assertEqual((out["record"], log), ("none", RECORD_NONE_INTENT))

    def test_row2_question_only_turn_is_none_intent(self) -> None:
        payload = _model(estimate=_estimate(confidence="medium", question="Quanto de arroz?"))
        out, log = _gate(payload)
        self.assertIsNone(out["estimate"])
        self.assertEqual(out["question"], "Quanto de arroz?")
        self.assertEqual((out["record"], log), ("none", RECORD_NONE_INTENT))

    def test_row2_log_without_estimate_is_none_intent(self) -> None:
        out, log = _gate(_model(estimate=None))
        self.assertEqual((out["record"], log), ("none", RECORD_NONE_INTENT))

    def test_row3_other_day_is_none_for_log_and_skip(self) -> None:
        for payload in (_model(meal_day="other"), _model("skip", meal_day="other", skip_slot="1")):
            with self.subTest(intent=payload["intent"]):
                out, log = _gate(payload)
                self.assertEqual((out["record"], log), ("none", RECORD_NONE_OTHER_DAY))
                self.assertIsNone(out["skip_slot"])

    def test_row4_skip_with_profile_slot_is_auto(self) -> None:
        out, log = _gate(_model("skip", skip_slot="1", reply="Ok."))
        self.assertEqual((out["record"], log), ("auto", RECORD_AUTO_SKIP))
        self.assertEqual(out["intent"], "skip")
        self.assertEqual(out["skip_slot"], "1")
        self.assertIsNone(out["estimate"])
        self.assertEqual(out["reply"], "Ok.")

    def test_row5_skip_without_valid_slot_becomes_question(self) -> None:
        for slot in (None, "9", ""):
            with self.subTest(slot=slot):
                out, log = _gate(_model("skip", skip_slot=slot))
                self.assertEqual((out["record"], log), ("none", RECORD_NONE_SKIP_SLOT))
                self.assertEqual(out["intent"], "question")
                self.assertIsNone(out["skip_slot"])

    def test_row6_released_log_without_slot_is_ask(self) -> None:
        payload = _model(estimate=_estimate(confidence="high", question=None, suggested_slot="9"))
        out, log = _gate(payload)
        self.assertIsNone(out["estimate"]["suggested_slot"])
        self.assertEqual((out["record"], log), ("ask", RECORD_ASK_NO_SLOT))

    def test_row6_wins_over_clear_force_and_photo(self) -> None:
        payload = _model(estimate=_estimate(confidence="high", question=None, suggested_slot=None))
        out, log = _gate(payload, force=True, photo_only=True)
        self.assertEqual((out["record"], log), ("ask", RECORD_ASK_NO_SLOT))

    def test_row7_clear_released_log_is_auto(self) -> None:
        out, log = _gate(_model())
        self.assertEqual((out["record"], log), ("auto", RECORD_AUTO_LOG))
        self.assertIsNone(out["skip_slot"])
        self.assertEqual(out["estimate"]["suggested_slot"], "1")

    def test_row8_unsure_released_log_is_ask(self) -> None:
        for intent in ("unsure", None, "maybe"):
            with self.subTest(record_intent=intent):
                out, log = _gate(_model(record_intent=intent))
                self.assertEqual((out["record"], log), ("ask", RECORD_ASK_UNSURE))

    def test_force_estimate_counts_as_clear(self) -> None:
        payload = _model(
            record_intent="unsure",
            estimate=_estimate(confidence="medium", question="Quanto de arroz?"),
        )
        out, log = _gate(payload, rounds=1, force=True)
        self.assertIsNotNone(out["estimate"])
        self.assertEqual((out["record"], log), ("auto", RECORD_AUTO_LOG))

    def test_photo_without_text_counts_as_clear(self) -> None:
        out, log = _gate(_model(record_intent="unsure"), photo_only=True)
        self.assertEqual((out["record"], log), ("auto", RECORD_AUTO_LOG))

    def test_meal_day_missing_is_today(self) -> None:
        payload = _model()
        del payload["meal_day"]
        self.assertEqual(_gate(payload)[1], RECORD_AUTO_LOG)


class SkipShapingTests(unittest.TestCase):
    def test_skip_before_v4_is_a_question_with_the_reply(self) -> None:
        payload = _model("skip", skip_slot="1", reply="Ok, sem cafe hoje.")
        for fact_ids in (None, []):
            with self.subTest(legacy=fact_ids is None):
                out = shape_chat(payload, valid_slot_ids=SLOTS, fact_ids=fact_ids)
                self.assertEqual(out["intent"], "question")
                self.assertEqual(out["reply"], "Ok, sem cafe hoje.")
                self.assertIsNone(out["estimate"])
                for key in ("skip_slot", "record", "record_intent", "meal_day"):
                    self.assertNotIn(key, out)

    def test_skip_never_carries_an_estimate(self) -> None:
        payload = {**_model("skip", skip_slot="1"), "estimate": _estimate()}
        self.assertIsNone(shape_chat(payload, valid_slot_ids=SLOTS, fact_ids=[], skip=True)["estimate"])


class SchemaTests(unittest.TestCase):
    def test_strict_schema_requires_the_new_fields(self) -> None:
        schema = chat_format(SLOTS)["schema"]
        props = schema["properties"]
        self.assertEqual(props["intent"]["enum"], ["log", "plan", "question", "skip"])
        self.assertEqual(props["record_intent"], {"type": "string", "enum": ["clear", "unsure"]})
        self.assertEqual(props["meal_day"], {"type": "string", "enum": ["today", "other"]})
        self.assertEqual(props["skip_slot"], {"type": ["string", "null"], "enum": ["1", "3", None]})
        self.assertEqual(set(schema["required"]), set(props))
        self.assertEqual(schema["required"][-1], "scope")
        self.assertFalse(schema["additionalProperties"])

    def test_profile_without_slots_allows_only_null_skip_slot(self) -> None:
        props = chat_format([])["schema"]["properties"]
        self.assertEqual(props["skip_slot"]["enum"], [None])


# Responses of master before S14 for these model payloads (new fields included): byte for byte.
_ITEMS = [{"name": "ovo mexido", "g": 100, "kcal": 150}, {"name": "pao frances", "g": 50.5, "kcal": 140}]
V3_LOG_SNAPSHOT = {
    "reply": "Jantar de arroz, feijao e frango.",
    "intent": "log",
    "estimate": {
        "kcal": 440, "p": 25, "c": 38, "g": 22, "confidence": "high", "question": None,
        "items": _ITEMS, "suggested_slot": "1", "meal_text": "2 ovos mexidos, 1 pao frances",
    },
    "memory_updates": [],
    "memory_used": [],
    "digest": None,
    "model": "gpt-6-luna",
    "question": None,
}
LEGACY_SKIP_SNAPSHOT = {
    "reply": "Ok, cafe pulado.",
    "intent": "question",
    "estimate": None,
    "memory_updates": [],
    "memory_used": [],
    "digest": None,
    "model": "gpt-6-luna",
}


def _bytes(body: dict[str, Any]) -> bytes:
    return json.dumps(body, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


class RecordRouteTests(unittest.IsolatedAsyncioTestCase):
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

    @staticmethod
    def _v4(**kw: Any) -> dict[str, Any]:
        return _v2_payload(**{"facts": [], "clarify_rounds": 0, "auto_record": True, "text": USER_TEXT, **kw})

    async def test_v4_clear_log_is_auto_and_logged(self) -> None:
        response = await self._post(self._v4(), _responds(_model(), []))
        body = response.json()
        self.assertEqual(body["record"], "auto")
        self.assertIsNone(body["skip_slot"])
        self.assertIsNone(body["question"])
        self.assertEqual(body["estimate"]["suggested_slot"], "1")
        line = self._log()
        self.assertEqual(line["record"], RECORD_AUTO_LOG)
        self.assertEqual(line["record_intent"], "clear")
        self.assertEqual(line["meal_day"], "today")
        self.assertEqual(line["response"], body)

    async def test_v4_skip_is_auto_with_slot(self) -> None:
        response = await self._post(
            self._v4(text="pulei o cafe hoje"), _responds(_model("skip", skip_slot="1"), [])
        )
        body = response.json()
        self.assertEqual((body["intent"], body["record"], body["skip_slot"]), ("skip", "auto", "1"))
        self.assertIsNone(body["estimate"])
        self.assertEqual(self._log()["record"], RECORD_AUTO_SKIP)

    async def test_v4_force_estimate_is_auto(self) -> None:
        payload = _model(record_intent="unsure", estimate=_estimate(question="Quanto de arroz?"))
        response = await self._post(
            self._v4(text="Pode estimar assim.", force_estimate=True), _responds(payload, [])
        )
        self.assertEqual(response.json()["record"], "auto")

    async def test_v4_photo_without_text_is_auto(self) -> None:
        response = await self._post(
            self._v4(text="", image_b64="aGVsbG8="),
            _responds(_model(record_intent="unsure"), []),
        )
        self.assertEqual(response.json()["record"], "auto")

    async def test_v4_scope_refusal_is_none_policy(self) -> None:
        response = await self._post(self._v4(), _responds(_model(scope="out_of_scope"), []))
        body = response.json()
        self.assertEqual(body["reply"], REFUSAL_OUT_OF_SCOPE)
        self.assertEqual((body["record"], body["skip_slot"], body["question"]), ("none", None, None))
        self.assertEqual(self._log()["record"], RECORD_NONE_POLICY)

    async def test_v4_output_moderation_flag_overrides_auto(self) -> None:
        calls: list[int] = []

        def handler(request: httpx2.Request) -> httpx2.Response:
            if _is_moderation(request):
                calls.append(1)
                # Input clean, output flagged.
                flags = {"harassment": True} if len(calls) > 1 else None
                return httpx2.Response(200, json=_moderation(flags))
            return _responds(_model(), [])(request)

        app = main.create_app(transport=httpx2.MockTransport(handler))
        self._apps.append(app)
        async with _client(app) as client:
            response = await client.post("/v1/chat", headers={"X-Invite": INVITE}, json=self._v4())
        body = response.json()
        self.assertEqual(body["record"], "none")
        self.assertIsNone(body["estimate"])
        self.assertEqual(self._log()["record"], RECORD_NONE_POLICY)

    async def test_v4_fallback_is_none_intent(self) -> None:
        response = await self._post(self._v4(), _explodes(httpx2.ConnectError("down")))
        body = response.json()
        self.assertEqual((body["record"], body["skip_slot"]), ("none", None))
        self.assertEqual(self._log()["record"], RECORD_NONE_INTENT)

    async def test_log_fields_carry_no_user_text(self) -> None:
        await self._post(self._v4(), _responds(_model(), []))
        line = self._log()
        for key in ("record", "record_intent", "meal_day"):
            self.assertNotIn("frango", json.dumps(line[key]))

    async def test_v3_and_legacy_carry_no_record_fields(self) -> None:
        cases = {
            "v3": _v2_payload(facts=[], clarify_rounds=0, text=USER_TEXT),
            "v3_auto_false": _v2_payload(facts=[], clarify_rounds=0, auto_record=False, text=USER_TEXT),
            "auto_without_rounds": _v2_payload(facts=[], auto_record=True, text=USER_TEXT),
        }
        for name, payload in cases.items():
            with self.subTest(client=name):
                response = await self._post(payload, _responds(_model(), []))
                body = response.json()
                self.assertNotIn("record", body)
                self.assertNotIn("skip_slot", body)
                line = json.loads(self.log_path.read_text(encoding="utf-8").splitlines()[-1])
                self.assertIsNone(line["record"])
                self.assertEqual(line["record_intent"], "clear")

    async def test_v3_log_is_the_pre_s14_response_byte_for_byte(self) -> None:
        for extra in ({}, {"auto_record": False}):
            with self.subTest(extra=extra):
                response = await self._post(
                    _v2_payload(facts=[], clarify_rounds=0, text=USER_TEXT, **extra),
                    _responds(_model(meal_day="other"), []),
                )
                self.assertEqual(response.content, _bytes(V3_LOG_SNAPSHOT))

    async def test_skip_before_v4_is_the_pre_s14_response_byte_for_byte(self) -> None:
        payload = _model("skip", skip_slot="1", reply="Ok, cafe pulado.")
        for request, snapshot in (
            (_v2_payload(), LEGACY_SKIP_SNAPSHOT),
            (_v2_payload(facts=[], auto_record=True), LEGACY_SKIP_SNAPSHOT),
            (_v2_payload(facts=[], clarify_rounds=0), {**LEGACY_SKIP_SNAPSHOT, "question": None}),
        ):
            with self.subTest(request=sorted(request)):
                response = await self._post(request, _responds(payload, []))
                self.assertEqual(response.content, _bytes(snapshot))

    async def test_instructions_define_the_record_mark(self) -> None:
        captured: list[httpx2.Request] = []
        await self._post(self._v4(), _responds(_model(), captured))
        instructions = json.loads(captured[0].content)["instructions"]
        for rule in (
            "record_intent is clear or unsure",
            "a meal name followed by food",
            "an explicit request to register it",
            "food with no sign of having been eaten",
            "meal_day is today or other",
            "Before 05:00 local time",
            "intent is log, plan, question or skip",
            "O Chat registra apenas refeições de hoje.",
            "If meal_day is today, that sentence is forbidden",
            "Never say in reply that you recorded, registered, noted, saved or skipped a meal",
        ):
            self.assertIn(rule, instructions)
        self.assertNotIn("it will be recorded today", instructions)

    async def test_instructions_define_pending_hedged_skip_and_photo_question(self) -> None:
        """S15: a pending or hedged meal is not a skip; a photo with a question is log, unsure."""
        captured: list[httpx2.Request] = []
        await self._post(self._v4(), _responds(_model(), captured))
        instructions = json.loads(captured[0].content)["instructions"]
        for rule in (
            "firmly says it will not happen today",
            "A meal that has not happened yet is not skip",
            "A hedged skip is not skip",
            "a food photo sent with a nutrition question about it",
            "that photo is log, estimate the plate, record_intent unsure",
        ):
            self.assertIn(rule, instructions)
        self.assertNotIn("a photo of a plate with or without text", instructions)

    async def test_instructions_separate_eating_day_record_slot_and_reference(self) -> None:
        captured: list[httpx2.Request] = []
        await self._post(self._v4(), _responds(_model(), captured))
        instructions = json.loads(captured[0].content)["instructions"]
        for rule in (
            "other means the food was EATEN on another day",
            "asking to register food eaten on an earlier day remains other and never records today",
            "A day statement alone after a pure nutrition question does not become a log",
            "The user's explicit facts now override DIGESTS and earlier assistant assumptions",
            "[refeição sugerida: {slot name}]",
            "never the user's words",
            "Never ask which meal when its slot was named by the user or suggested earlier",
            "two most recent distinct dates",
            "remove brand names, then compare food types, quantities and units",
            "fewer than two distinct days",
            "reply MUST name the weekday supplied on that newest record",
            "including another portion on a later turn",
            "Recording or citing a temp fact never removes it",
            "never promote it or copy it into a habit",
        ):
            self.assertIn(rule, instructions)


if __name__ == "__main__":
    unittest.main()
