"""Plan budget (ADR-039, S21): server arithmetic, the adjustment call, shaping and legacy shapes."""
import json
import os
import tempfile
import unittest

import httpx2
import pytest

import main
import plan_budget
from chat_instructions import assemble
from evals.checks import FAIL, PASS, evaluate
from llm import chat_format
from shaping import chat_output_texts
from tests.test_api import INVITE, _client, _envelope, _mock
from tests.test_chat import _base_chat_payload


def request(**overrides):
    req = _base_chat_payload(text="me passa a receita", clarify_rounds=0, auto_record=True,
                             facts=[], plan_budget=True)
    req["day"]["remaining_kcal"] = 500
    req.update(overrides)
    return req


def plan(kcal=800, *, reserved=(), choice=None, **overrides):
    out = dict(
        reply="Prato: 100 g de base.", intent="plan",
        estimate=dict(kcal=kcal, p=30, c=60, g=20, confidence="medium", question=None,
                      items=[dict(name="base", g=100, kcal=kcal)], suggested_slot="cafe",
                      meal_text="100 g de base"),
        record_intent="unsure", meal_day="today", skip_slots=[], memory_updates=[], memory_used=[],
        digest=None, plan_budget=dict(reserved=list(reserved), choice=choice), scope="in_scope",
    )
    out.update(overrides)
    return out


def sequence(payloads, captured):
    """Model answers in order; an Exception entry is raised instead."""
    queue = list(payloads)

    def handler(req: httpx2.Request) -> httpx2.Response:
        captured.append(req)
        item = queue.pop(0)
        if isinstance(item, Exception):
            raise item
        return httpx2.Response(200, json=_envelope(json.dumps(item, ensure_ascii=False)))

    return handler


def model_input(req: httpx2.Request) -> str:
    return json.loads(req.content)["input"][0]["content"][0]["text"]


@pytest.mark.parametrize("raw,expected", [
    (None, dict(reserved=[], choice=None)),
    ("x", dict(reserved=[], choice=None)),
    (dict(reserved=[dict(label="  bolo  ", kcal=249.6)], choice="fit"),
     dict(reserved=[dict(label="bolo", kcal=250)], choice="fit")),
    (dict(reserved=[dict(label="", kcal=10), dict(label="x" * 41, kcal=10), dict(label="a", kcal=0),
                    dict(label="b", kcal=3001), dict(label="c", kcal=True), dict(label=3, kcal=10),
                    dict(label="d", kcal=float("nan")), "junk", dict(label="ok", kcal=3000)],
          choice="maybe"),
     dict(reserved=[dict(label="ok", kcal=3000)], choice=None)),
    (dict(reserved=[dict(label=str(i), kcal=10) for i in range(5)], choice="over_ok"),
     dict(reserved=[dict(label=str(i), kcal=10) for i in range(3)], choice="over_ok")),
])
def test_model_budget_shaping_limits(raw, expected):
    assert plan_budget.shape_model_budget(raw) == expected


@pytest.mark.parametrize("kcal,remaining,reserved,fit,limit,over", [
    (800, 500, [], None, 500, 300),
    (500, 500, [], None, 500, 0),
    (500.2, 500, [], None, 500, 1),
    (400, 700, [dict(label="bolo", kcal=250)], None, 450, 0),
    (460, 700, [dict(label="bolo", kcal=250)], None, 450, 10),
    (300, 100, [dict(label="bolo", kcal=250)], None, -150, 450),
    (600, 100, [dict(label="bolo", kcal=250)], 550, 550, 50),
    (300, -120, [], None, -120, 420),
])
def test_server_arithmetic(kcal, remaining, reserved, fit, limit, over):
    got = plan_budget.check(kcal, remaining, dict(reserved=reserved, choice=None), fit)
    assert (got["limit_kcal"], got["over_kcal"]) == (limit, over)


@pytest.mark.parametrize("payload", [
    plan(intent="log"), plan(meal_day="other"), plan(estimate=None), plan(kcal=0),
    plan(kcal=float("inf")), plan(kcal=True), "junk",
])
def test_no_check_without_a_plan_of_today(payload):
    assert plan_budget.plan_kcal(payload) is None


def test_adjustment_needs_a_choice_an_excess_and_a_positive_target():
    over = dict(limit_kcal=500, over_kcal=300, reserved=[], choice=None)
    assert not plan_budget.needs_adjustment(over, None)
    assert plan_budget.needs_adjustment({**over, "choice": "fit"}, None)
    assert plan_budget.needs_adjustment(over, 500)
    assert not plan_budget.needs_adjustment({**over, "choice": "over_ok"}, None)
    assert not plan_budget.needs_adjustment({**over, "choice": "fit", "over_kcal": 0}, None)
    assert not plan_budget.needs_adjustment({**over, "choice": "fit", "limit_kcal": 0}, None)


def test_every_schema_carries_the_same_required_plan_budget():
    legacy = chat_format(["a"])["schema"]
    modern = chat_format(["a"], meal_changes=True)["schema"]
    for schema in (legacy, modern):
        assert "plan_budget" in schema["required"]
        assert schema["properties"]["plan_budget"] == plan_budget.SCHEMA
    assert "meal_change" not in legacy["properties"]


def test_plan_rule_is_shared_and_target_is_not_in_fixed_instructions():
    for branch in ("legacy", "meal_changes"):
        text = assemble(branch)
        assert "COOKING:" in text and "plan_budget is {reserved, choice}" in text
        assert "BUDGET_TARGET: only when the input ends" in text
        assert "say by how many kcal it goes over" not in text
    assert "plan_budget" not in assemble("compact")


def test_output_moderation_sees_reservation_labels():
    texts = chat_output_texts(dict(reply="ok", plan_budget=dict(reserved=[dict(label="bolo de fubá", kcal=250)])))
    assert "bolo de fubá" in texts


def test_eval_check_rejects_wrong_or_inconsistent_budget():
    output = dict(estimate=dict(kcal=800), plan_budget=dict(limit_kcal=450, over_kcal=350, choice=None,
                                                         reserved=[dict(label="Bolo", kcal=250)]))
    good = dict(over=True, choice=None, limit_kcal=450, reserved_has=["bolo"], reserved_kcal_range=[250, 250])
    assert evaluate(dict(plan_budget=good), output)["plan_budget"]["status"] == PASS
    for wrong in (dict(over=False), dict(choice="fit"), dict(limit_kcal=500), dict(reserved_has=["suco"]),
                  dict(reserved_kcal_range=[0, 0]), None, "absent"):
        assert evaluate(dict(plan_budget=wrong), output)["plan_budget"]["status"] == FAIL
    lying = dict(estimate=dict(kcal=800), plan_budget=dict(limit_kcal=450, over_kcal=0, choice="fit", reserved=[]))
    assert evaluate(dict(plan_budget=dict(over=False)), lying)["plan_budget"]["status"] == FAIL
    assert evaluate(dict(plan_budget="absent"), dict(estimate=None))["plan_budget"]["status"] == PASS
    assert evaluate(dict(plan_budget=None), dict(plan_budget=None))["plan_budget"]["status"] == PASS
    assert evaluate(dict(plan_budget=None), dict(estimate=None))["plan_budget"]["status"] == FAIL


class RouteTests(unittest.IsolatedAsyncioTestCase):
    async def post(self, req, payloads, log_path=None):
        captured = []
        saved = os.environ.get("CONVERSATION_LOG_PATH")
        if log_path:
            os.environ["CONVERSATION_LOG_PATH"] = log_path
        app = main.create_app(transport=_mock(sequence(payloads, captured)))
        try:
            async with _client(app) as client:
                response = await client.post("/v1/chat", headers={"X-Invite": INVITE}, json=req)
            return response, captured
        finally:
            app.state.llm.close()
            app.state.moderator.close()
            app.state.conversation_log.close()
            if log_path:
                if saved is None:
                    os.environ.pop("CONVERSATION_LOG_PATH", None)
                else:
                    os.environ["CONVERSATION_LOG_PATH"] = saved

    async def test_over_without_choice_reports_and_makes_one_call(self):
        response, captured = await self.post(request(), [plan(800, reserved=[dict(label="bolo", kcal=100)])])
        out = response.json()
        self.assertEqual(out["plan_budget"], dict(limit_kcal=400, over_kcal=400, choice=None,
                                                  reserved=[dict(label="bolo", kcal=100)]))
        self.assertEqual(out["record"], "none")
        self.assertEqual(out["estimate"]["kcal"], 800)
        self.assertEqual(len(captured), 1)
        self.assertNotIn("BUDGET_TARGET", model_input(captured[0]))

    async def test_fits_and_over_ok_never_retry(self):
        for payload, over in ((plan(450), 0), (plan(900, choice="over_ok"), 400)):
            response, captured = await self.post(request(), [payload])
            self.assertEqual(response.json()["plan_budget"]["over_kcal"], over)
            self.assertEqual(len(captured), 1)

    async def test_fit_choice_retries_once_with_the_server_target(self):
        first = plan(800, reserved=[dict(label="bolo", kcal=100)], choice="fit")
        second = plan(390, reserved=[], choice=None, reply="Prato ajustado.")
        response, captured = await self.post(request(), [first, second])
        out = response.json()
        self.assertEqual(len(captured), 2)
        self.assertTrue(model_input(captured[1]).endswith("BUDGET_TARGET: 400 kcal"))
        self.assertEqual(model_input(captured[1])[: -len("\nBUDGET_TARGET: 400 kcal")], model_input(captured[0]))
        self.assertEqual(json.loads(captured[0].content)["instructions"],
                         json.loads(captured[1].content)["instructions"])
        self.assertEqual(out["reply"], "Prato ajustado.")
        self.assertEqual(out["estimate"]["kcal"], 390)
        # Same target on the second check: the reservations and choice of the first call.
        self.assertEqual(out["plan_budget"], dict(limit_kcal=400, over_kcal=0, choice="fit",
                                                  reserved=[dict(label="bolo", kcal=100)]))

    async def test_fit_kcal_is_the_target_and_still_over_is_reported_without_a_third_call(self):
        response, captured = await self.post(request(fit_kcal=450), [plan(800), plan(520)])
        out = response.json()
        self.assertEqual(len(captured), 2)
        self.assertIn("BUDGET_TARGET: 450 kcal", model_input(captured[1]))
        self.assertEqual(out["estimate"]["kcal"], 520)
        self.assertEqual((out["plan_budget"]["limit_kcal"], out["plan_budget"]["over_kcal"]), (450, 70))

    async def test_invalid_or_failed_adjustment_keeps_the_first_plan(self):
        first = plan(800, choice="fit")
        for second in (plan(300, intent="question", estimate=None), plan(300, scope="out_of_scope"),
                       plan(0), RuntimeError("boom")):
            response, captured = await self.post(request(), [first, second])
            out = response.json()
            self.assertEqual(response.status_code, 200)
            self.assertEqual(len(captured), 2)
            self.assertEqual(out["estimate"]["kcal"], 800)
            self.assertEqual(out["plan_budget"]["over_kcal"], 300)

    async def test_no_target_left_means_no_adjustment_call(self):
        first = plan(300, reserved=[dict(label="bolo", kcal=600)], choice="fit")
        response, captured = await self.post(request(), [first])
        self.assertEqual(len(captured), 1)
        self.assertEqual((response.json()["plan_budget"]["limit_kcal"],
                          response.json()["plan_budget"]["over_kcal"]), (0, 300))

    async def test_null_budget_cases(self):
        no_remaining = request()
        del no_remaining["day"]["remaining_kcal"]
        log = plan(300, intent="log", record_intent="clear")
        for req, payload in ((no_remaining, plan(800, choice="fit")), (request(), plan(800, meal_day="other")),
                             (request(), log), (request(), plan(800, scope="out_of_scope")),
                             (request(), plan(800, intent="question", estimate=None))):
            response, captured = await self.post(req, [payload])
            out = response.json()
            self.assertIn("plan_budget", out)
            self.assertIsNone(out["plan_budget"])
            self.assertEqual(len(captured), 1)

    async def test_clients_without_the_capability_keep_their_shape(self):
        payload = plan(800, reserved=[dict(label="bolo", kcal=100)], choice="fit")
        for overrides in (dict(plan_budget=False), dict(plan_budget=False, meal_changes=False, temp_facts=True),
                          dict(plan_budget=False, clarify_rounds=None, auto_record=False),
                          dict(plan_budget=False, facts=None, clarify_rounds=None, auto_record=False)):
            req = request(**overrides)
            response, captured = await self.post(req, [payload])
            out = response.json()
            self.assertEqual(response.status_code, 200)
            self.assertNotIn("plan_budget", out)
            self.assertEqual(len(captured), 1)
            without = dict(payload)
            del without["plan_budget"]
            old_shape = (await self.post(req, [without]))[0].json()
            self.assertEqual(out, old_shape)

    async def test_capability_validation(self):
        bad = [request(clarify_rounds=None), request(auto_record=False), request(plan_budget="true"),
               request(plan_budget=1), request(plan_budget=False, fit_kcal=400), request(fit_kcal=0),
               request(fit_kcal=5001), request(fit_kcal="400"), request(fit_kcal=400.5), request(fit_kcal=True)]
        for req in bad:
            response, captured = await self.post(req, [plan()])
            self.assertEqual(response.status_code, 422, req)
            self.assertEqual(captured, [])
        for fit in (1, 5000):
            self.assertEqual((await self.post(request(fit_kcal=fit), [plan(1), plan(1)]))[0].status_code, 200)

    async def test_compact_ignores_the_capability(self):
        req = request(compact=True, fit_kcal=400, messages=[dict(role="user", text="comi arroz")])
        response, _ = await self.post(req, [dict(digest="Comeu arroz.")])
        self.assertEqual(response.status_code, 200)
        self.assertNotIn("plan_budget", response.json())

    async def test_log_records_budget_and_retry(self):
        with tempfile.TemporaryDirectory() as folder:
            path = os.path.join(folder, "chat.jsonl")
            await self.post(request(fit_kcal=450), [plan(800), plan(430)], log_path=path)
            await self.post(request(), [plan(300)], log_path=path)
            await self.post(request(plan_budget=False), [plan(300)], log_path=path)
            with open(path, encoding="utf-8") as handle:
                lines = [json.loads(line) for line in handle]
        self.assertTrue(lines[0]["adjust_retry"])
        self.assertEqual(lines[0]["plan_budget"]["limit_kcal"], 450)
        self.assertEqual(lines[0]["plan_budget"]["over_kcal"], 0)
        self.assertFalse(lines[1]["adjust_retry"])
        self.assertEqual(lines[1]["plan_budget"]["over_kcal"], 0)
        self.assertFalse(lines[2]["adjust_retry"])
        self.assertIsNone(lines[2]["plan_budget"])
