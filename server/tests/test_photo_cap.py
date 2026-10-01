"""Over-cap photo is 413 before the model. Under-cap still goes out, then is dropped."""

from __future__ import annotations

import logging
import os
import unittest

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"
os.environ["LLM_MODEL"] = "gpt-nao-usar"

import httpx2

import main
from config import PHOTO_MAX_B64_CHARS, PHOTO_MAX_BYTES, TIMEOUT_SECONDS
from tests.test_api import (
    FAKE_KEY,
    INVITE,
    MEAL,
    _Collector,
    _assert_not_on_disk,
    _client,
    _responds,
    _mock,
)


class PhotoCapTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self._apps = []

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()

    def test_cap_and_timeout_match_the_plan(self) -> None:
        self.assertEqual(TIMEOUT_SECONDS, 60.0)
        self.assertEqual(PHOTO_MAX_BYTES, 16 * 1024 * 1024)
        self.assertEqual(PHOTO_MAX_B64_CHARS, 22_400_000)

    def test_reject_photo_is_too_large_only_over_the_char_cap(self) -> None:
        self.assertIsNone(main.reject_photo(None))
        self.assertIsNone(main.reject_photo(""))
        self.assertIsNone(main.reject_photo("abc"))
        self.assertIsNone(main.reject_photo("x" * PHOTO_MAX_B64_CHARS))
        self.assertEqual(main.reject_photo("x" * (PHOTO_MAX_B64_CHARS + 1)), "too_large")

    async def test_over_cap_photo_is_413_and_skips_the_model(self) -> None:
        too_big = "x" * (PHOTO_MAX_B64_CHARS + 1)
        self.assertGreater(len(too_big), PHOTO_MAX_B64_CHARS)
        routes = (
            ("/v1/estimate", {"text": MEAL, "image_b64": too_big}),
            (
                "/v1/fit",
                {
                    "mode": "want",
                    "text": MEAL,
                    "available_items": [],
                    "budget": {"kcal": 455, "p": 49},
                    "image_b64": too_big,
                },
            ),
        )
        collector = _Collector()
        logging.getLogger().addHandler(collector)
        try:
            for path, payload in routes:
                with self.subTest(path=path):
                    captured: list[httpx2.Request] = []
                    app = self._app(_responds({"kcal": 1, "confidence": "high"}, captured))
                    async with _client(app) as client:
                        response = await client.post(
                            path,
                            headers={"X-Invite": INVITE},
                            json=payload,
                        )
                    self.assertEqual(response.status_code, 413)
                    self.assertIn(response.json()["detail"], ("photo_too_large", "payload_too_large"))
                    self.assertEqual(captured, [])
                    self.assertNotIn(too_big, response.text)
                    self.assertNotIn(FAKE_KEY, response.text)
        finally:
            logging.getLogger().removeHandler(collector)
        for line in collector.lines:
            self.assertNotIn(too_big, line)
            self.assertNotIn(FAKE_KEY, line)
        _assert_not_on_disk(too_big)

    async def test_under_cap_photo_enters_the_model_then_is_dropped(self) -> None:
        sentinel = "IMAGEM-SENTINELA-CAP-OK-3c91"
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
        self.assertEqual(len(captured), 1)
        self.assertIn(sentinel, captured[0].content.decode())
        self.assertNotIn(sentinel, response.text)
        self.assertNotIn(FAKE_KEY, response.text)
        for line in collector.lines:
            self.assertNotIn(sentinel, line)
            self.assertNotIn(FAKE_KEY, line)
        _assert_not_on_disk(sentinel)

    async def test_estimate_two_breads_still_returns_kcal(self) -> None:
        payload = {
            "kcal": 270,
            "p": 9,
            "c": 50,
            "g": 3,
            "confidence": "high",
            "question": None,
            "items": [{"name": "pao", "g": 100, "kcal": 270}],
        }
        captured: list[httpx2.Request] = []
        app = self._app(_responds(payload, captured))
        async with _client(app) as client:
            response = await client.post(
                "/v1/estimate",
                headers={"X-Invite": INVITE},
                json={"text": "2 paes"},
            )
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["kcal"], 270)
        self.assertEqual(body["model"], "gpt-6-luna")
        self.assertEqual(len(captured), 1)
        self.assertIn("2 paes", captured[0].content.decode())
        timeout = captured[0].extensions["timeout"]
        for part in ("connect", "read", "write", "pool"):
            # CP2: what is left of the shared 60 s deadline, after moderation.
            self.assertLessEqual(timeout[part], 60, msg=str(timeout))
            self.assertGreater(timeout[part], 55, msg=str(timeout))

    def _app(self, handler) -> object:
        app = main.create_app(transport=_mock(handler))
        self._apps.append(app)
        return app


if __name__ == "__main__":
    unittest.main()
