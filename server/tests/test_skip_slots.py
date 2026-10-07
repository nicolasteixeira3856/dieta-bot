"""S29 (ADR-047): skip_slots next to any intent for an opted-in client. All data is synthetic."""

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
from pydantic import ValidationError

import main
from shaping import RECORD_AUTO_LOG, RECORD_AUTO_SKIP, skipped_slots
from tests.test_api import INVITE, _client, _explodes, _mock, _responds
from tests.test_chat import _estimate, _v2_payload
from tests.test_record import _model

# _v2_payload: profile slots "1" (café da manhã) and "3" (almoço), both empty today.
SLOTS = ["1", "3"]
NEW = {"operation": "new", "base_slot": None, "addition": None}


def _request(**kw: Any) -> dict[str, Any]:
    return _v2_payload(
        **{"facts": [], "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "skip_slots": True,
           "text": "Pulei o almoco. No cafe comi 2 ovos e 1 pao.", **kw}
    )


def _log_and_skip(skips: list[Any], **kw: Any) -> dict[str, Any]:
    """A clear log of slot 1 that also lists skips."""
    estimate = _estimate(confidence="high", question=None, suggested_slot="1")
    return _model(estimate=estimate, meal_change=NEW, skip_slots=skips, **kw)


def _shape(payload: dict[str, Any], **request: Any) -> tuple[dict[str, Any], str | None]:
    out, _, log = main.shape_chat_turn(main.ChatIn.model_validate(_request(**request)), payload)
    return out, log


class SkippedSlotsTests(unittest.TestCase):
    def test_profile_order_without_repeats_or_unknown_ids(self) -> None:
        payload = {"intent": "skip", "skip_slots": ["3", "9", "1", "3", None, "", {"id": 1}, True]}
        self.assertEqual(skipped_slots(payload, SLOTS), ["1", "3"])

    def test_the_slot_of_the_logged_meal_is_never_skipped(self) -> None:
        self.assertEqual(skipped_slots(_log_and_skip(["1", "3"]), SLOTS), ["3"])

    def test_a_plan_slot_is_not_dropped(self) -> None:
        payload = _model("plan", estimate=_estimate(suggested_slot="3"), skip_slots=["3"])
        self.assertEqual(skipped_slots(payload, SLOTS), ["3"])

    def test_missing_or_malformed_list_is_empty(self) -> None:
        for raw in (None, "3", 3, {"id": "3"}):
            with self.subTest(raw=raw):
                self.assertEqual(skipped_slots({"intent": "log", "skip_slots": raw}, SLOTS), [])


class ShapeTests(unittest.TestCase):
    def test_incident_shape_records_the_log_and_lists_the_skip(self) -> None:
        out, log = _shape(_log_and_skip(["3"]))
        self.assertEqual((out["intent"], out["record"], log), ("log", "auto", RECORD_AUTO_LOG))
        self.assertEqual(out["skip_slots"], ["3"])
        self.assertIsNone(out["skip_slot"])

    def test_skip_only_turn_keeps_skip_slot_and_lists_every_slot(self) -> None:
        out, log = _shape(_model("skip", skip_slots=["3", "1"]), text="Pulei o cafe e o almoco")
        self.assertEqual((out["intent"], out["record"], out["skip_slot"], log), ("skip", "auto", "1", RECORD_AUTO_SKIP))
        self.assertEqual(out["skip_slots"], ["1", "3"])

    def test_plan_and_question_turns_carry_their_skips(self) -> None:
        plan = _model("plan", estimate=_estimate(question=None, suggested_slot="3"), skip_slots=["1"])
        question = _model("question", skip_slots=["1"])
        for payload in (plan, question):
            with self.subTest(intent=payload["intent"]):
                out, _ = _shape(payload)
                self.assertEqual((out["record"], out["skip_slots"]), ("none", ["1"]))

    def test_refusal_and_other_day_skip_nothing(self) -> None:
        for payload in (_log_and_skip(["3"], scope="out_of_scope"), _model("skip", skip_slots=["3"], meal_day="other")):
            with self.subTest(scope=payload["scope"], day=payload["meal_day"]):
                out, _ = _shape(payload)
                self.assertEqual(out["skip_slots"], [])

    def test_held_turn_keeps_its_skips(self) -> None:
        estimate = _estimate(confidence="low", question="Qual leite?", suggested_slot="1")
        out, _ = _shape(_model(estimate=estimate, meal_change=NEW, skip_slots=["3"], record_intent="unsure"))
        self.assertIsNotNone(out["question"])
        self.assertEqual((out["record"], out["skip_slots"]), ("none", ["3"]))

    def test_without_the_capability_the_list_is_not_returned(self) -> None:
        out, _ = _shape(_log_and_skip(["3"]), skip_slots=False)
        self.assertNotIn("skip_slots", out)
        self.assertIsNone(out["skip_slot"])


class CapabilityTests(unittest.TestCase):
    def test_requires_meal_changes_and_a_strict_bool(self) -> None:
        for kw in ({"meal_changes": False}, {"skip_slots": "true"}, {"skip_slots": 1}):
            with self.subTest(kw=kw), self.assertRaises(ValidationError):
                main.ChatIn.model_validate(_request(**kw))

    def test_compact_drops_it(self) -> None:
        body = main.ChatIn.model_validate(_request(compact=True, meal_changes=False))
        self.assertFalse(body.skip_slots)


class SkipRouteTests(unittest.IsolatedAsyncioTestCase):
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
        return json.loads(self.log_path.read_text(encoding="utf-8").splitlines()[-1])

    async def test_log_and_skip_over_http_and_the_log_counts_skips(self) -> None:
        response = await self._post(_request(), _responds(_log_and_skip(["3"]), []))
        body = response.json()
        self.assertEqual((body["record"], body["skip_slots"]), ("auto", ["3"]))
        self.assertEqual(self._log()["skips"], 1)

    async def test_fallback_has_an_empty_list(self) -> None:
        response = await self._post(_request(), _explodes(httpx2.ConnectError("down")))
        body = response.json()
        self.assertEqual((body["record"], body["skip_slots"]), ("none", []))
        self.assertEqual(self._log()["skips"], 0)

    async def test_missing_meal_changes_is_422(self) -> None:
        response = await self._post(_request(meal_changes=False), _responds(_log_and_skip(["3"]), []))
        self.assertEqual(response.status_code, 422)
