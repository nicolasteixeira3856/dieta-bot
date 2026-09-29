"""Dev conversation log (ADR-015 / S6): one JSON line per LLM call, off by default."""

from __future__ import annotations

import json
import os
import tempfile
import unittest
import uuid
from pathlib import Path
from typing import Any

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"
os.environ["LLM_MODEL"] = "gpt-nao-usar"

import httpx2

import main
from tests.test_api import FAKE_KEY, INVITE, _client, _envelope, _explodes, _responds
from tests.test_chat import _base_chat_payload

PHOTO_SENTINEL = "UEhPVE9fU0VOVElORUxfTkFPX1BPREVfVkFaQVI" * 40


def _raw(text: str):
    def handler(request: httpx2.Request) -> httpx2.Response:
        return httpx2.Response(200, json=_envelope(text))

    return handler


class ConversationLogTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self._apps: list[Any] = []
        self._tmp = tempfile.TemporaryDirectory()
        self.path = Path(self._tmp.name) / "conversations.jsonl"
        self._saved = os.environ.get("CONVERSATION_LOG_PATH")

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()
            app.state.conversation_log.close()
        if self._saved is None:
            os.environ.pop("CONVERSATION_LOG_PATH", None)
        else:
            os.environ["CONVERSATION_LOG_PATH"] = self._saved
        self._tmp.cleanup()

    def _app(self, handler, log_path: str | None) -> Any:
        if log_path is None:
            os.environ.pop("CONVERSATION_LOG_PATH", None)
        else:
            os.environ["CONVERSATION_LOG_PATH"] = log_path
        app = main.create_app(transport=httpx2.MockTransport(handler))
        self._apps.append(app)
        return app

    def _lines(self) -> list[dict[str, Any]]:
        if not self.path.exists():
            return []
        return [json.loads(line) for line in self.path.read_text(encoding="utf-8").splitlines()]

    async def _chat(self, app: Any, headers: dict[str, str] | None = None, **payload: Any):
        async with _client(app) as client:
            return await client.post(
                "/v1/chat",
                headers={"X-Invite": INVITE, **(headers or {})},
                json=_base_chat_payload(**payload),
            )

    async def test_off_by_default_writes_nothing_and_keeps_the_response(self) -> None:
        model = {"reply": "ok", "estimate": None, "digest": None}
        app = self._app(_responds(model, []), None)
        response = await self._chat(app)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["reply"], "ok")
        self.assertFalse(app.state.conversation_log.enabled)
        self.assertEqual(list(Path(self._tmp.name).iterdir()), [])

    async def test_success_logs_input_raw_output_and_response(self) -> None:
        model = {"reply": "Registrei", "estimate": None, "digest": None}
        app = self._app(_responds(model, []), str(self.path))
        response = await self._chat(
            app, headers={"X-App-Version": "1.0-dev", "X-App-Env": "dev"}, text="o que cabe hoje?"
        )
        self.assertEqual(response.status_code, 200)
        [line] = self._lines()
        self.assertEqual(line["route"], "chat")
        self.assertEqual(line["prompt"], "chat")
        self.assertIn("o que cabe hoje?", line["input_text"])
        self.assertEqual(json.loads(line["raw_output"]), model)
        self.assertEqual(line["response"], response.json())
        self.assertIs(line["fallback"], False)
        self.assertIsNone(line["error"])
        self.assertEqual(line["app_version"], "1.0-dev")
        self.assertEqual(line["app_env"], "dev")
        self.assertIsInstance(line["latency_ms"], int)
        self.assertTrue(line["ts"].endswith("-03:00"))

    async def test_output_without_json_becomes_the_reply_and_marks_text_only(self) -> None:
        app = self._app(_raw("Desculpe, nao consigo ajudar com isso."), str(self.path))
        response = await self._chat(app)
        self.assertEqual(response.json()["reply"], "Desculpe, nao consigo ajudar com isso.")
        self.assertIsNone(response.json()["estimate"])
        [line] = self._lines()
        self.assertEqual(line["raw_output"], "Desculpe, nao consigo ajudar com isso.")
        self.assertIsNone(line["error"])
        self.assertEqual(line["fallback"], "text_only")
        self.assertEqual(line["response"], response.json())

    async def test_malformed_json_is_an_error_fallback(self) -> None:
        app = self._app(_raw('{"reply": "quebrado"'), str(self.path))
        response = await self._chat(app)
        self.assertEqual(response.json()["reply"], "nao deu pra estimar")
        [line] = self._lines()
        self.assertIsNotNone(line["error"])
        self.assertEqual(line["fallback"], "error")

    async def test_empty_reply_is_a_fallback_without_error(self) -> None:
        model = {"reply": "", "estimate": None, "digest": None}
        app = self._app(_responds(model, []), str(self.path))
        response = await self._chat(app)
        self.assertEqual(response.json()["reply"], "nao deu pra estimar")
        [line] = self._lines()
        self.assertIsNone(line["error"])
        self.assertEqual(line["fallback"], "error")

    async def test_transport_error_logs_the_error_and_no_raw_output(self) -> None:
        app = self._app(_explodes(httpx2.ConnectError("boom")), str(self.path))
        response = await self._chat(app)
        self.assertEqual(response.json()["reply"], "nao deu pra estimar")
        [line] = self._lines()
        self.assertIsNone(line["raw_output"])
        self.assertIsNotNone(line["error"])
        self.assertEqual(line["fallback"], "error")

    async def test_photo_is_counted_but_never_written(self) -> None:
        model = {"reply": "ok", "estimate": None, "digest": None}
        app = self._app(_responds(model, []), str(self.path))
        await self._chat(app, image_b64=PHOTO_SENTINEL)
        [line] = self._lines()
        self.assertTrue(line["has_photo"])
        self.assertEqual(line["photo_b64_chars"], len(PHOTO_SENTINEL))
        self.assertNotIn(PHOTO_SENTINEL[:40], self.path.read_text(encoding="utf-8"))

    async def test_request_id_is_reused_or_generated_and_returned(self) -> None:
        model = {"reply": "ok", "estimate": None, "digest": None}
        app = self._app(_responds(model, []), str(self.path))
        sent = await self._chat(app, headers={"X-Request-Id": "app-123-abc"})
        invalid = await self._chat(app, headers={"X-Request-Id": "tem espaco!"})
        missing = await self._chat(app)
        async with _client(app) as client:
            health = await client.get("/health")
        self.assertEqual(sent.headers["X-Request-Id"], "app-123-abc")
        for response in (invalid, missing, health):
            uuid.UUID(response.headers["X-Request-Id"])
        ids = [line["request_id"] for line in self._lines()]
        self.assertEqual(
            ids,
            ["app-123-abc", invalid.headers["X-Request-Id"], missing.headers["X-Request-Id"]],
        )

    async def test_invite_and_key_are_redacted(self) -> None:
        app = self._app(_explodes(httpx2.ConnectError(f"auth {FAKE_KEY} failed")), str(self.path))
        await self._chat(app, text=f"meu convite e {INVITE}")
        content = self.path.read_text(encoding="utf-8")
        self.assertNotIn(FAKE_KEY, content)
        self.assertNotIn(INVITE, content)
        self.assertIn("[redacted]", content)

    async def test_estimate_fit_and_compact_are_logged_too(self) -> None:
        estimate = {"kcal": 300, "p": 20, "c": 30, "g": 10, "confidence": "high", "items": []}
        app = self._app(_responds(estimate, []), str(self.path))
        async with _client(app) as client:
            await client.post("/v1/estimate", headers={"X-Invite": INVITE}, json={"text": "2 ovos"})
            await client.post(
                "/v1/fit",
                headers={"X-Invite": INVITE},
                json={"mode": "text", "text": "jantar", "budget": {"kcal": 500, "p": 30}},
            )
            await client.post(
                "/v1/chat",
                headers={"X-Invite": INVITE},
                json=_base_chat_payload(
                    compact=True, messages=[{"role": "user", "text": "comi 2 ovos"}]
                ),
            )
        routes = [(line["route"], line["prompt"]) for line in self._lines()]
        self.assertEqual(routes, [("estimate", "estimate"), ("fit", "fit"), ("compact", "digest")])

    async def test_unwritable_path_does_not_break_the_route(self) -> None:
        bad = Path(self._tmp.name) / "nao-existe" / "conversations.jsonl"
        model = {"reply": "ok", "estimate": None, "digest": None}
        app = self._app(_responds(model, []), str(bad))
        with self.assertLogs("nutri", level="WARNING") as captured:
            response = await self._chat(app)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["reply"], "ok")
        self.assertTrue(any("conversation log" in message for message in captured.output))


if __name__ == "__main__":
    unittest.main()
