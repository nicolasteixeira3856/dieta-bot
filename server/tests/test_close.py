"""S30 part A (ADR-044): profile.tone in Chat and POST /v1/close. All data is synthetic."""

from __future__ import annotations

import json
import os
import tempfile
import unittest
from pathlib import Path
from typing import Any

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"

import httpx2
from pydantic import ValidationError

import closure
import main
from chat_instructions import assemble
from tests.test_api import INVITE, _client, _envelope, _explodes, _is_moderation, _mock, _moderation, _responds
from tests.test_chat import _base_chat_payload


def _day(**kw: Any) -> dict[str, Any]:
    numbers = {
        "date": "2026-10-07", "kcal": 2230, "p": 98, "c": 250, "g": 80, "ceiling_kcal": 2000,
        "workout_kcal": None,
        "slots": [
            {"name": "Café", "status": "eaten", "kcal": 410},
            {"name": "Almoço", "status": "eaten", "kcal": 980},
            {"name": "Lanche", "status": "skipped"},
            {"name": "Jantar", "status": "eaten", "kcal": 840},
            {"name": "Ceia", "status": "empty"},
        ],
    }
    numbers.update(kw)
    return numbers


def _week(**kw: Any) -> dict[str, Any]:
    days = []
    for i, kcal in enumerate((2100, 2400, 0, 1900, 2300, 0, 2600)):
        days.append({"date": f"2026-10-{5 + i:02d}", "kcal": kcal, "p": 110 if kcal else 0, "c": 200 if kcal else 0,
                     "g": 70 if kcal else 0, "ceiling_kcal": 2000, "workout_kcal": None, "recorded": kcal > 0})
    numbers = {"days": days, "over_slot": {"name": "Jantar", "days": 4}}
    numbers.update(kw)
    return numbers


def _request(period: str = "day", **kw: Any) -> dict[str, Any]:
    body = {
        "period": period,
        "tone": "duro",
        "local_time": "2026-10-07T22:00:00-03:00",
        "profile": {
            "ceiling_kcal": 2000, "p_target": 150, "c_target": 220, "g_target": 65,
            "slots": [{"id": "m", "name": "Café", "time": "07:00"}, {"id": "j", "name": "Jantar", "time": "19:30"}],
        },
        "numbers": _day() if period == "day" else _week(),
    }
    body.update(kw)
    return body


def _body(period: str = "day", **kw: Any) -> main.CloseIn:
    return main.CloseIn.model_validate(_request(period, **kw))


class ToneTests(unittest.TestCase):
    def test_absent_or_null_is_seco_and_other_values_are_rejected(self) -> None:
        payload = _base_chat_payload()
        self.assertEqual(main.ChatIn.model_validate(payload).profile.tone, "seco")
        payload["profile"]["tone"] = None
        self.assertEqual(main.ChatIn.model_validate(payload).profile.tone, "seco")
        payload["profile"]["tone"] = "duro"
        self.assertEqual(main.ChatIn.model_validate(payload).profile.tone, "duro")
        for value in ("bravo", "", 1, True):
            payload["profile"]["tone"] = value
            with self.subTest(value=value), self.assertRaises(ValidationError):
                main.ChatIn.model_validate(payload)

    def test_four_chat_prefixes_and_the_tone_rule_is_last(self) -> None:
        prefixes = {b: assemble(b) for b in ("legacy", "legacy_duro", "meal_changes", "meal_changes_duro")}
        self.assertEqual(len(set(prefixes.values())), 4)
        self.assertTrue(prefixes["legacy"].rstrip().endswith("fasting to compensate."))
        self.assertIn("TONE: duro", prefixes["meal_changes_duro"].split("\n\n")[-1])
        self.assertNotIn("TONE", assemble("compact"))


class ToneRouteTests(unittest.IsolatedAsyncioTestCase):
    async def test_duro_reaches_the_model_prefix_and_the_log(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            log_path = Path(tmp) / "log.jsonl"
            os.environ["CONVERSATION_LOG_PATH"] = str(log_path)
            try:
                captured: list[httpx2.Request] = []
                model = {"reply": "Oi.", "intent": "question", "estimate": None, "record_intent": "unsure",
                         "meal_day": "today", "skip_slots": [], "memory_updates": [], "memory_used": [],
                         "digest": None, "plan_budget": None, "scope": "in_scope"}
                app = main.create_app(transport=_mock(_responds(model, captured)))
                payload = _base_chat_payload()
                payload["profile"]["tone"] = "duro"
                async with _client(app) as client:
                    response = await client.post("/v1/chat", headers={"X-Invite": INVITE}, json=payload)
                app.state.llm.close()
                app.state.conversation_log.close()
            finally:
                os.environ.pop("CONVERSATION_LOG_PATH", None)
            self.assertEqual(response.status_code, 200)
            self.assertEqual(json.loads(captured[0].content)["instructions"], assemble("legacy_duro"))
            self.assertEqual(json.loads(log_path.read_text(encoding="utf-8").splitlines()[-1])["tone"], "duro")


class CloseSchemaTests(unittest.TestCase):
    def test_day_and_week_validate(self) -> None:
        self.assertIsInstance(_body("day").numbers, main.CloseDayIn)
        self.assertIsInstance(_body("week").numbers, main.CloseWeekIn)
        self.assertEqual(main.CloseIn.model_validate({**_request(), "tone": "seco"}).tone, "seco")
        body = _request()
        del body["tone"]
        self.assertEqual(main.CloseIn.model_validate(body).tone, "seco")

    def test_rejects_free_text_floats_unknown_keys_and_mismatched_period(self) -> None:
        bad = [
            _request(tone="gentil"),
            _request(period="week", numbers=_day()),
            _request(numbers=_week()),
            _request(numbers=_day(kcal=2230.5)),
            _request(numbers=_day(kcal="2230")),
            _request(numbers=_day(note="texto livre")),
            _request(numbers=_day(slots=[{"name": "x" * 41, "status": "eaten", "kcal": 1}])),
            _request(numbers=_day(slots=[{"name": "Café", "status": "comido", "kcal": 1}])),
            _request(period="week", numbers=_week(days=[])),
            _request(period="week", numbers=_week(over_slot={"name": "Jantar", "days": 8})),
            {**_request(), "text": "me xinga"},
        ]
        for payload in bad:
            with self.subTest(payload=json.dumps(payload)[:120]), self.assertRaises(ValidationError):
                main.CloseIn.model_validate(payload)


class CloseNumbersTests(unittest.TestCase):
    def test_day_numbers_carry_the_differences_and_the_empty_last_meal(self) -> None:
        text = closure.numbers_text(_body())
        self.assertIn("EATEN: kcal=2230", text)
        self.assertIn("over_kcal=230", text)
        self.assertIn("p_missing=52", text)
        self.assertIn('LAST_MEAL_EMPTY: "Ceia"', text)
        self.assertIn('TOP_MEAL: "Almoço" 980 kcal', text)

    def test_week_numbers_count_only_recorded_days(self) -> None:
        text = closure.numbers_text(_body("week"))
        self.assertIn("days_recorded=5, days_without_record=2, total_kcal=11300", text)
        self.assertIn("mean_kcal=2260", text)
        self.assertIn("days_over_ceiling=4", text)
        self.assertIn('OVER_MEAL: "Jantar" over its share on 4 days', text)

    def test_fallback_lines(self) -> None:
        self.assertEqual(closure.fallback(_body()), "Dia fechado. 2230 de 2000 kcal.")
        self.assertEqual(closure.fallback(_body("week")), "Semana fechada. 11300 kcal em 5 dias com registro.")
        empty = _week(days=[{**d, "recorded": False, "kcal": 0} for d in _week()["days"]])
        self.assertEqual(closure.fallback(_body("week", numbers=empty)), "Semana fechada. Nenhum dia com registro.")


class CloseShapeTests(unittest.TestCase):
    allowed = closure.allowed_numbers(closure.numbers_text(_body()))

    def test_a_sentence_with_a_number_not_in_the_request_is_dropped(self) -> None:
        text, dropped = closure.shape_text(
            "Passou 230 kcal do teto. O dia somou 2.500 kcal. Amanhã: 2 ovos no café.", self.allowed)
        self.assertEqual((text, dropped), ("Passou 230 kcal do teto. Amanhã: 2 ovos no café.", 1))

    def test_markers_are_removed_and_lines_capped(self) -> None:
        text, _ = closure.shape_text("**Passou 230 kcal.**\n- Faltou proteína.\n\n# Ajuste\n1. Ceia vazia.\nMais.",
                                     self.allowed)
        self.assertEqual(text, "Passou 230 kcal.\nFaltou proteína.\nAjuste")

    def test_length_cap_keeps_whole_sentences(self) -> None:
        long = " ".join(["Faltou proteína no jantar."] * 30)
        text, _ = closure.shape_text(long, self.allowed)
        self.assertLessEqual(len(text), closure.TEXT_MAX_CHARS)
        self.assertTrue(text.endswith("jantar."))

    def test_nothing_left_is_none(self) -> None:
        self.assertEqual(closure.shape_text("Média de 1.234 kcal.", self.allowed), (None, 1))
        self.assertEqual(closure.shape_text(None, self.allowed), (None, 0))
        self.assertEqual(closure.shape_text("Ficou em 1,5 kg.", self.allowed), (None, 1))


def _close_model(text: str) -> dict[str, Any]:
    return {"text": text}


class CloseRouteTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self._apps: list[Any] = []
        self._tmp = tempfile.TemporaryDirectory()
        self.log_path = Path(self._tmp.name) / "conversations.jsonl"
        os.environ["CONVERSATION_LOG_PATH"] = str(self.log_path)

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()
            app.state.conversation_log.close()
        os.environ.pop("CONVERSATION_LOG_PATH", None)
        self._tmp.cleanup()

    async def _post(self, payload: Any, transport: httpx2.BaseTransport, **headers: str) -> Any:
        app = main.create_app(transport=transport)
        self._apps.append(app)
        async with _client(app) as client:
            if isinstance(payload, bytes):
                return await client.post("/v1/close", headers={"X-Invite": INVITE, "Content-Type": "application/json",
                                                                 **headers}, content=payload)
            return await client.post("/v1/close", headers={"X-Invite": INVITE, **headers}, json=payload)

    def _log(self) -> dict[str, Any]:
        return json.loads(self.log_path.read_text(encoding="utf-8").splitlines()[-1])

    async def test_text_is_shaped_and_logged_with_tone_and_period(self) -> None:
        captured: list[httpx2.Request] = []
        model = _close_model("Passou 230 kcal do teto; o Almoço teve 980 kcal. Comeu 3.000 kcal.\nAmanhã: ovos no café.")
        response = await self._post(_request(), _mock(_responds(model, captured)), **{"X-Request-Id": "close-test-1"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["text"], "Passou 230 kcal do teto; o Almoço teve 980 kcal.\nAmanhã: ovos no café.")
        self.assertEqual(response.json()["model"], "gpt-6-luna")
        call = json.loads(captured[0].content)
        self.assertEqual(call["instructions"], assemble("close_duro"))
        self.assertIn("### CLOSE_NUMBERS_START", call["input"][0]["content"][0]["text"])
        log = self._log()
        self.assertEqual((log["route"], log["tone"], log["period"], log["close_dropped"], log["fallback"]),
                         ("close", "duro", "day", 1, False))
        self.assertNotIn("has_photo", log)

    async def test_failure_returns_the_neutral_line(self) -> None:
        response = await self._post(_request(), _mock(_explodes(httpx2.ConnectError("down"))))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["text"], "Dia fechado. 2230 de 2000 kcal.")
        self.assertEqual(self._log()["fallback"], "error")

    async def test_empty_text_after_shaping_returns_the_neutral_line(self) -> None:
        response = await self._post(_request(), _mock(_responds(_close_model("Comeu 3.000 kcal."), [])))
        self.assertEqual(response.json()["text"], "Dia fechado. 2230 de 2000 kcal.")

    async def test_flagged_output_returns_the_neutral_line_without_content_in_the_log(self) -> None:
        def route(request: httpx2.Request) -> httpx2.Response:
            if _is_moderation(request):
                return httpx2.Response(200, json=_moderation({"harassment": True}))
            return httpx2.Response(200, json=_envelope(json.dumps(_close_model("Texto ofensivo."))))

        response = await self._post(_request(period="week"), httpx2.MockTransport(route))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["text"], "Semana fechada. 11300 kcal em 5 dias com registro.")
        log = self._log()
        self.assertEqual(log["fallback"], "policy")
        self.assertEqual(log["policy"]["stage"], "output")

    async def test_moderation_down_is_not_an_http_error(self) -> None:
        def route(request: httpx2.Request) -> httpx2.Response:
            if _is_moderation(request):
                return httpx2.Response(500, json={"error": "down"})
            return httpx2.Response(200, json=_envelope(json.dumps(_close_model("Passou 230 kcal."))))

        response = await self._post(_request(), httpx2.MockTransport(route))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["text"], "Dia fechado. 2230 de 2000 kcal.")
        log = self._log()
        self.assertEqual((log["fallback"], log["raw_output"], log["policy"]["code"]), ("moderation", None, "unavailable"))

    async def test_auth_validation_and_body_limit(self) -> None:
        app = main.create_app(transport=_mock(_responds(_close_model("x"), [])))
        self._apps.append(app)
        async with _client(app) as client:
            unauthorized = await client.post("/v1/close", json=_request())
            invalid = await client.post("/v1/close", headers={"X-Invite": INVITE}, json=_request(tone="gentil"))
        self.assertEqual((unauthorized.status_code, invalid.status_code), (401, 422))
        big = json.dumps(_request()).encode() + b" " * (main.CLOSE_MAX_BODY_BYTES)
        response = await self._post(big, _mock(_responds(_close_model("x"), [])))
        self.assertEqual(response.status_code, 413)


if __name__ == "__main__":
    unittest.main()
