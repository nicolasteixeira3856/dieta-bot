"""Real routes via ASGI. The stub only swaps the model's HTTP transport."""

from __future__ import annotations

import json
import logging
import os
import unittest
from pathlib import Path

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"
os.environ["LLM_MODEL"] = "gpt-nao-usar"

import httpx
import httpx2

import main
from config import FALLBACK_QUESTION

INVITE = "convite-teste"
FAKE_KEY = "sk-test-sentinel-not-a-real-key"
MEAL = "2 paes, ovo, cafe com leite"


class ApiTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self._apps = []

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()

    async def test_health_repeats_ok_and_model_without_key(self) -> None:
        app = self._app(_refuses)
        async with _client(app) as client:
            first_resp = await client.get("/health")
            second_resp = await client.get("/health")
        first = first_resp.json()
        second = second_resp.json()
        self.assertEqual(first_resp.text, '{"ok": true, "model": "gpt-6-luna"}')
        self.assertEqual(second_resp.text, first_resp.text)
        self.assertEqual(first, {"ok": True, "model": "gpt-6-luna"})
        self.assertEqual(second, first)
        self.assertNotIn(FAKE_KEY, first_resp.text)

    async def test_estimate_returns_the_model_payload(self) -> None:
        payload = {
            "kcal": 517,
            "p": 19,
            "c": 63,
            "g": 14,
            "confidence": "medium",
            "question": "os paes eram franceses?",
            "items": [{"name": "pao", "g": 100, "kcal": 270}],
        }
        captured: list[httpx2.Request] = []
        app = self._app(_responds(payload, captured))
        async with _client(app) as client:
            response = await client.post(
                "/v1/estimate",
                headers={"X-Invite": INVITE},
                json={"text": MEAL, "image_b64": None},
            )
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["kcal"], payload["kcal"])
        self.assertEqual(body["p"], payload["p"])
        self.assertEqual(body["c"], payload["c"])
        self.assertEqual(body["g"], payload["g"])
        self.assertEqual(body["confidence"], payload["confidence"])
        self.assertEqual(body["question"], payload["question"])
        self.assertEqual(body["items"], payload["items"])
        self.assertEqual(body["model"], "gpt-6-luna")
        self.assertEqual(len(captured), 1)
        request = json.loads(captured[0].content)
        self.assertEqual(request["model"], "gpt-6-luna")
        self.assertNotEqual(request["model"], "gpt-nao-usar")
        self.assertEqual(request["reasoning"]["effort"], "none")
        self.assertIn(MEAL, captured[0].content.decode())
        timeout = captured[0].extensions["timeout"]
        for part in ("connect", "read", "write", "pool"):
            self.assertEqual(timeout[part], 20, msg=str(timeout))

    async def test_question_only_when_confidence_is_not_high(self) -> None:
        high = {
            "kcal": 410,
            "p": 18,
            "c": 44,
            "g": 11,
            "confidence": "high",
            "question": "nao deve sair",
            "items": [],
        }
        app = self._app(_responds(high, []))
        async with _client(app) as client:
            body = (
                await client.post(
                    "/v1/estimate",
                    headers={"X-Invite": INVITE},
                    json={"text": MEAL},
                )
            ).json()
        self.assertEqual(body["confidence"], "high")
        self.assertNotIn("question", body)

    async def test_timeout_and_transport_error_become_fixed_question(self) -> None:
        for err in (
            httpx2.TimeoutException("timeout"),
            httpx2.ConnectError("conexao"),
        ):
            with self.subTest(err=type(err).__name__):
                app = self._app(_explodes(err))
                async with _client(app) as client:
                    response = await client.post(
                        "/v1/estimate",
                        headers={"X-Invite": INVITE},
                        json={"text": MEAL},
                    )
                body = response.json()
                self.assertEqual(response.status_code, 200)
                self.assertEqual(body["confidence"], "low")
                self.assertEqual(body["question"], FALLBACK_QUESTION)
                self.assertEqual(body["question"], "descreve em 1 linha")

    async def test_photo_enters_the_call_and_does_not_stay_on_disk_or_log(self) -> None:
        sentinel = "IMAGEM-SENTINELA-NAO-PERSISTIR-7f3a"
        payload = {
            "kcal": 220,
            "p": 8,
            "c": 20,
            "g": 9,
            "confidence": "medium",
            "question": "era uma foto de pao?",
            "items": [{"name": "pao", "g": 50, "kcal": 130}],
        }
        captured: list[httpx2.Request] = []
        collector = _Collector()
        logging.getLogger().addHandler(collector)
        try:
            app = self._app(_responds(payload, captured))
            async with _client(app) as client:
                response = await client.post(
                    "/v1/estimate",
                    headers={"X-Invite": INVITE},
                    json={"text": MEAL, "image_b64": sentinel},
                )
        finally:
            logging.getLogger().removeHandler(collector)
        self.assertEqual(response.status_code, 200)
        self.assertIn(sentinel, captured[0].content.decode())
        self.assertNotIn(sentinel, response.text)
        self.assertNotIn(FAKE_KEY, response.text)
        for line in collector.lines:
            self.assertNotIn(sentinel, line)
            self.assertNotIn(FAKE_KEY, line)
        _assert_not_on_disk(sentinel)

    async def test_missing_or_wrong_invite_rejects_without_calling_the_model(self) -> None:
        for headers in ({}, {"X-Invite": "errado"}):
            with self.subTest(headers=headers):
                captured: list[httpx2.Request] = []
                app = self._app(_responds({"kcal": 1}, captured))
                async with _client(app) as client:
                    response = await client.post(
                        "/v1/estimate",
                        headers=headers,
                        json={"text": MEAL},
                    )
                self.assertEqual(response.status_code, 401)
                self.assertEqual(captured, [])

    async def test_fit_surprise_has_two_options_and_one_question(self) -> None:
        payload = {
            "question": "qual dos dois?",
            "options": [
                {
                    "name": "pao com ovo",
                    "portions": [{"name": "pao", "quantity": "1 unidade"}],
                    "kcal": 280,
                    "p": 14,
                },
                {
                    "name": "iogurte",
                    "portions": [{"name": "iogurte", "quantity": "1 pote"}],
                    "kcal": 150,
                    "p": 12,
                },
            ],
        }
        app = self._app(_responds(payload, []))
        async with _client(app) as client:
            body = (
                await client.post(
                    "/v1/fit",
                    headers={"X-Invite": INVITE},
                    json={
                        "mode": "surprise",
                        "text": "algo rapido",
                        "available_items": ["ovo", "pao"],
                        "budget": {"kcal": 455, "p": 49},
                        "image_b64": None,
                    },
                )
            ).json()
        self.assertEqual(len(body["options"]), 2)
        self.assertEqual(body["question"], payload["question"])
        self.assertIsInstance(body["question"], str)
        self.assertNotIn("perguntas", body)
        self.assertEqual(body["options"][0]["portions"], payload["options"][0]["portions"])
        self.assertEqual(body["options"][1]["portions"], payload["options"][1]["portions"])
        self.assertTrue(body["fits"])
        self.assertIn("name", body["dish"])
        self.assertIn("portions", body["dish"])

    async def test_dish_above_budget_does_not_fit(self) -> None:
        payload = {
            "question": "aceita um prato menor?",
            "dish": {
                "name": "hamburguer",
                "portions": [{"name": "pao", "quantity": "1 unidade"}],
                "kcal": 900,
                "p": 35,
            },
        }
        app = self._app(_responds(payload, []))
        async with _client(app) as client:
            body = (
                await client.post(
                    "/v1/fit",
                    headers={"X-Invite": INVITE},
                    json={
                        "mode": "want",
                        "text": "quero hamburguer",
                        "available_items": [],
                        "budget": {"kcal": 100, "p": 20},
                    },
                )
            ).json()
        self.assertEqual(body["dish"]["kcal"], 900)
        self.assertFalse(body["fits"])
        self.assertEqual(body["question"], payload["question"])
        self.assertNotIn("options", body)

    async def test_surprise_option_above_budget_does_not_fit(self) -> None:
        payload = {
            "question": "fico com o menor?",
            "options": [
                {
                    "name": "grande",
                    "portions": [{"name": "pao", "quantity": "2 unidades"}],
                    "kcal": 800,
                    "p": 30,
                },
                {
                    "name": "pequeno",
                    "portions": [{"name": "ovo", "quantity": "1 unidade"}],
                    "kcal": 90,
                    "p": 7,
                },
            ],
        }
        app = self._app(_responds(payload, []))
        async with _client(app) as client:
            body = (
                await client.post(
                    "/v1/fit",
                    headers={"X-Invite": INVITE},
                    json={
                        "mode": "surprise",
                        "text": "algo leve",
                        "available_items": [],
                        "budget": {"kcal": 455, "p": 49},
                    },
                )
            ).json()
        self.assertEqual(len(body["options"]), 2)
        self.assertEqual(body["question"], payload["question"])
        over = [option for option in body["options"] if option["kcal"] > 455]
        inside = [option for option in body["options"] if option["kcal"] <= 455]
        self.assertEqual(len(over), 1)
        self.assertFalse(over[0]["fits"])
        self.assertTrue(inside[0]["fits"])
        self.assertLessEqual(body["dish"]["kcal"], 455)
        self.assertTrue(body["fits"])

    def _app(self, handler) -> object:
        app = main.create_app(transport=httpx2.MockTransport(handler))
        self._apps.append(app)
        return app


def _client(app: object) -> httpx.AsyncClient:
    return httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app),
        base_url="http://test",
    )


def _envelope(text: str) -> dict:
    return {
        "id": "resp_test",
        "object": "response",
        "created_at": 0,
        "model": "gpt-6-luna",
        "parallel_tool_calls": True,
        "tool_choice": "none",
        "tools": [],
        "output": [
            {
                "id": "msg_test",
                "type": "message",
                "role": "assistant",
                "status": "completed",
                "content": [
                    {"type": "output_text", "text": text, "annotations": []},
                ],
            }
        ],
    }


def _responds(payload: dict, captured: list[httpx2.Request]):
    def handler(request: httpx2.Request) -> httpx2.Response:
        captured.append(request)
        return httpx2.Response(
            200,
            json=_envelope(json.dumps(payload, ensure_ascii=False)),
        )

    return handler


def _explodes(err: Exception):
    def handler(request: httpx2.Request) -> httpx2.Response:
        raise err

    return handler


def _refuses(request: httpx2.Request) -> httpx2.Response:
    raise AssertionError("health does not call the model")


class _Collector(logging.Handler):
    def __init__(self) -> None:
        super().__init__()
        self.lines: list[str] = []

    def emit(self, record: logging.LogRecord) -> None:
        self.lines.append(record.getMessage())


def _assert_not_on_disk(sentinel: str) -> None:
    blob = sentinel.encode()
    root = Path(__file__).resolve().parents[1]
    for path in _server_files(root):
        if blob in path.read_bytes():
            raise AssertionError(f"photo persisted in {path.name}")


def _server_files(directory: Path):
    skip = {"tests", ".venv", "__pycache__"}
    for path in directory.iterdir():
        if path.name in skip:
            continue
        if path.is_dir():
            yield from _server_files(path)
        elif path.is_file():
            yield path


if __name__ == "__main__":
    unittest.main()
