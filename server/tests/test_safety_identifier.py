"""CP3 safety identifier (ADR-025): optional X-Client-Instance-Id → HMAC safety_identifier.

Fake transport only. The serialized request is the installed SDK's real request body.
"""

from __future__ import annotations

import hashlib
import hmac
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
from identity import InvalidInstanceId, SafetyIds
from tests.test_api import FAKE_KEY, INVITE, _client, _envelope, _is_moderation, _moderation
from tests.test_chat import _base_chat_payload
from tests.test_content_policy import FOOD_TURN

SECRET = "safety-secret-sentinel-0123456789"
INSTANCE = "3f1c2b7a-9d4e-4c1a-8b2f-6e5d4c3b2a10"
OTHER = "a0b1c2d3-e4f5-4a6b-9c8d-0e1f2a3b4c5d"
# ASGITransport's client address. Must never reach the provider.
CLIENT_IP = "127.0.0.1"

ESTIMATE = {
    "kcal": 450,
    "p": 22,
    "c": 48,
    "g": 18,
    "confidence": "high",
    "question": None,
    "items": [{"name": "pao", "g": 100, "kcal": 270}],
}
FIT = {
    "question": "",
    "dish": {"name": "omelete", "portions": [{"name": "ovo", "quantity": "2"}], "kcal": 300, "p": 20},
    "options": None,
}
DIGEST = {"digest": "Café com 2 pães e 2 ovos, 450 kcal."}
PAYLOADS = {"estimate": ESTIMATE, "fit": FIT, "chat_turn": FOOD_TURN, "digest": DIGEST}


def _expected(instance: str, env: str = "dev", secret: str = SECRET) -> str:
    message = f"{env}:installation:{instance}".encode()
    return "v1_" + hmac.new(secret.encode(), message, hashlib.sha256).hexdigest()


class Recorder:
    """Keeps every serialized request. Answers by the structured-output name."""

    def __init__(self) -> None:
        self.generations: list[bytes] = []
        self.moderations: list[bytes] = []

    def __call__(self, request: httpx2.Request) -> httpx2.Response:
        raw = bytes(request.content)
        if _is_moderation(request):
            self.moderations.append(raw)
            return httpx2.Response(200, json=_moderation())
        self.generations.append(raw)
        name = json.loads(raw)["text"]["format"]["name"]
        return httpx2.Response(200, json=_envelope(json.dumps(PAYLOADS[name], ensure_ascii=False)))

    def bodies(self) -> list[dict[str, Any]]:
        return [json.loads(raw) for raw in self.generations]


class SafetyIdsTests(unittest.TestCase):
    def test_hmac_formula_and_version_prefix(self) -> None:
        ids = SafetyIds(SECRET, "dev")
        self.assertEqual(ids.derive(INSTANCE), _expected(INSTANCE))
        self.assertTrue(ids.derive(INSTANCE).startswith("v1_"))
        self.assertNotIn(INSTANCE, ids.derive(INSTANCE))

    def test_same_id_same_identifier_different_ids_differ(self) -> None:
        ids = SafetyIds(SECRET, "dev")
        self.assertEqual(ids.derive(INSTANCE), SafetyIds(SECRET, "dev").derive(INSTANCE))
        self.assertNotEqual(ids.derive(INSTANCE), ids.derive(OTHER))

    def test_environment_and_secret_separate_identifiers(self) -> None:
        dev = SafetyIds(SECRET, "dev").derive(INSTANCE)
        self.assertNotEqual(dev, SafetyIds(SECRET, "prod").derive(INSTANCE))
        self.assertNotEqual(dev, SafetyIds("another-secret", "dev").derive(INSTANCE))

    def test_missing_header_has_no_identifier(self) -> None:
        self.assertIsNone(SafetyIds(SECRET, "dev").derive(None))

    def test_missing_secret_is_off_but_still_validates(self) -> None:
        ids = SafetyIds("", "dev")
        self.assertFalse(ids.enabled)
        self.assertIsNone(ids.derive(INSTANCE))
        with self.assertRaises(InvalidInstanceId):
            ids.derive("not-a-uuid")

    def test_only_canonical_uuid_v4_is_accepted(self) -> None:
        ids = SafetyIds(SECRET, "dev")
        invalid = [
            "",
            " ",
            INSTANCE.upper(),
            "{" + INSTANCE + "}",
            INSTANCE + "\n",
            " " + INSTANCE,
            INSTANCE.replace("-", ""),
            INSTANCE + "0",
            "3f1c2b7a-9d4e-1c1a-8b2f-6e5d4c3b2a10",  # version 1
            "3f1c2b7a-9d4e-4c1a-7b2f-6e5d4c3b2a10",  # wrong variant
            "00000000-0000-4000-8000-00000000000g",
            "urn:uuid:" + INSTANCE,
            "3f1c2b7a-9d4e-4c1a-8b2f-6e5d4c3b2a1０",  # full-width digit
        ]
        for value in invalid:
            with self.subTest(value=value):
                with self.assertRaises(InvalidInstanceId) as raised:
                    ids.derive(value)
                self.assertNotIn(value.strip() or "x", str(raised.exception))


class _Routes(unittest.IsolatedAsyncioTestCase):
    secret = SECRET
    server_env: str | None = None

    async def asyncSetUp(self) -> None:
        self._apps: list[Any] = []
        self._tmp = tempfile.TemporaryDirectory()
        self.log_path = Path(self._tmp.name) / "conversations.jsonl"
        self._saved = {
            name: os.environ.get(name)
            for name in ("CONVERSATION_LOG_PATH", "SAFETY_ID_SECRET", "SERVER_ENV")
        }
        os.environ["CONVERSATION_LOG_PATH"] = str(self.log_path)
        os.environ["SAFETY_ID_SECRET"] = self.secret
        if self.server_env is None:
            os.environ["SERVER_ENV"] = ""
        else:
            os.environ["SERVER_ENV"] = self.server_env

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()
            app.state.moderator.close()
            app.state.conversation_log.close()
        for name, value in self._saved.items():
            if value is None:
                os.environ.pop(name, None)
            else:
                os.environ[name] = value
        self._tmp.cleanup()

    async def _all_routes(self, fake: Recorder, instance: str | None) -> list[Any]:
        app = main.create_app(transport=httpx2.MockTransport(fake))
        self._apps.append(app)
        headers = {"X-Invite": INVITE}
        if instance is not None:
            headers["X-Client-Instance-Id"] = instance
        messages = [{"role": "user", "text": "2 paes e 2 ovos"}, {"role": "assistant", "text": "450 kcal"}]
        async with _client(app) as client:
            return [
                await client.post("/v1/chat", headers=headers, json=_base_chat_payload(facts=[])),
                await client.post(
                    "/v1/chat",
                    headers=headers,
                    json=_base_chat_payload(compact=True, text="", messages=messages),
                ),
                await client.post("/v1/estimate", headers=headers, json={"text": "2 paes e 2 ovos"}),
                await client.post(
                    "/v1/fit",
                    headers=headers,
                    json={"mode": "plate", "text": "ovos", "budget": {"kcal": 500, "p": 30}},
                ),
            ]

    def _lines(self) -> list[dict[str, Any]]:
        if not self.log_path.exists():
            return []
        return [json.loads(line) for line in self.log_path.read_text(encoding="utf-8").splitlines()]

    def _assert_no_sentinels(self, fake: Recorder, *extra: str) -> None:
        sentinels = [INSTANCE, OTHER, INVITE, FAKE_KEY, SECRET, CLIENT_IP, *extra]
        texts = [raw.decode("utf-8") for raw in fake.generations + fake.moderations]
        if self.log_path.exists():
            texts.append(self.log_path.read_text(encoding="utf-8"))
        for text in texts:
            for sentinel in sentinels:
                self.assertNotIn(sentinel, text)


class RouteTests(_Routes):
    async def test_every_responses_call_carries_the_same_identifier(self) -> None:
        fake = Recorder()
        responses = await self._all_routes(fake, INSTANCE)
        self.assertEqual([r.status_code for r in responses], [200, 200, 200, 200])
        bodies = fake.bodies()
        self.assertEqual(
            [b["text"]["format"]["name"] for b in bodies], ["chat_turn", "digest", "estimate", "fit"]
        )
        expected = _expected(INSTANCE)
        for body in bodies:
            self.assertEqual(body["safety_identifier"], expected)
            # Request field only: never in the prompt.
            prompt = json.dumps({k: v for k, v in body.items() if k != "safety_identifier"})
            self.assertNotIn(expected, prompt)
        for response in responses:
            self.assertNotIn(expected, response.text)
        self._assert_no_sentinels(fake)

    async def test_moderation_request_has_no_identifier(self) -> None:
        fake = Recorder()
        await self._all_routes(fake, INSTANCE)
        self.assertGreaterEqual(len(fake.moderations), 4)
        expected = _expected(INSTANCE)
        for raw in fake.moderations:
            self.assertNotIn("safety_identifier", json.loads(raw))
            self.assertNotIn(expected, raw.decode("utf-8"))

    async def test_log_carries_the_derived_identifier_only(self) -> None:
        fake = Recorder()
        await self._all_routes(fake, INSTANCE)
        lines = self._lines()
        self.assertEqual([line["route"] for line in lines], ["chat", "compact", "estimate", "fit"])
        for line in lines:
            self.assertEqual(line["safety_identifier"], _expected(INSTANCE))
        self._assert_no_sentinels(fake)

    async def test_different_installations_differ(self) -> None:
        first, second = Recorder(), Recorder()
        await self._all_routes(first, INSTANCE)
        await self._all_routes(second, OTHER)
        ids_first = {b["safety_identifier"] for b in first.bodies()}
        ids_second = {b["safety_identifier"] for b in second.bodies()}
        self.assertEqual(ids_first, {_expected(INSTANCE)})
        self.assertEqual(ids_second, {_expected(OTHER)})

    async def test_missing_header_sends_no_identifier(self) -> None:
        fake = Recorder()
        responses = await self._all_routes(fake, None)
        self.assertEqual([r.status_code for r in responses], [200, 200, 200, 200])
        for body in fake.bodies():
            self.assertNotIn("safety_identifier", body)
        for line in self._lines():
            self.assertIsNone(line["safety_identifier"])
        self._assert_no_sentinels(fake)

    async def test_invalid_header_is_400_and_never_reflected_or_logged(self) -> None:
        invalid = "spoof-INSTANCE-sentinel-not-a-uuid"
        fake = Recorder()
        responses = await self._all_routes(fake, invalid)
        for response in responses:
            self.assertEqual(response.status_code, 400)
            self.assertEqual(response.json(), {"detail": "invalid_client_instance_id"})
            self.assertNotIn(invalid, response.text)
            self.assertNotIn(invalid, json.dumps(dict(response.headers)))
        self.assertEqual(fake.generations, [])
        self.assertEqual(fake.moderations, [])
        self.assertEqual(self._lines(), [])
        self._assert_no_sentinels(fake, invalid)

    async def test_invite_is_checked_before_the_header(self) -> None:
        app = main.create_app(transport=httpx2.MockTransport(Recorder()))
        self._apps.append(app)
        async with _client(app) as client:
            response = await client.post(
                "/v1/estimate",
                headers={"X-Invite": "wrong", "X-Client-Instance-Id": "bad"},
                json={"text": "ovo"},
            )
        self.assertEqual(response.status_code, 401)

    async def test_health_reports_on(self) -> None:
        app = main.create_app(transport=httpx2.MockTransport(Recorder()))
        self._apps.append(app)
        async with _client(app) as client:
            response = await client.get("/health")
        self.assertEqual(response.json(), {"ok": True, "model": "gpt-6-luna", "safety_id": "on"})
        self.assertNotIn(SECRET, response.text)


class EnvironmentTests(_Routes):
    server_env = "prod"

    async def test_server_env_namespaces_the_identifier(self) -> None:
        fake = Recorder()
        await self._all_routes(fake, INSTANCE)
        ids = {b["safety_identifier"] for b in fake.bodies()}
        self.assertEqual(ids, {_expected(INSTANCE, env="prod")})
        self.assertNotEqual(_expected(INSTANCE, env="prod"), _expected(INSTANCE))


class MissingSecretTests(_Routes):
    secret = ""

    async def test_starts_without_identifier_and_health_reports_off(self) -> None:
        fake = Recorder()
        responses = await self._all_routes(fake, INSTANCE)
        self.assertEqual([r.status_code for r in responses], [200, 200, 200, 200])
        for body in fake.bodies():
            self.assertNotIn("safety_identifier", body)
        for line in self._lines():
            self.assertIsNone(line["safety_identifier"])
        self._assert_no_sentinels(fake)
        app = self._apps[-1]
        async with _client(app) as client:
            health = await client.get("/health")
        self.assertEqual(health.json()["safety_id"], "off")

    async def test_invalid_header_is_still_400(self) -> None:
        fake = Recorder()
        responses = await self._all_routes(fake, "not-a-uuid")
        self.assertEqual({r.status_code for r in responses}, {400})
        self.assertEqual(fake.generations, [])


if __name__ == "__main__":
    unittest.main()
