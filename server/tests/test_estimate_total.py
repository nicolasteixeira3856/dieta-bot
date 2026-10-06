"""Estimate total as server arithmetic over the items (ADR-042, S23)."""
import json

import httpx2
import pytest

import estimate_total
import main
from moderation import CLEAN, Deadline
from tests.test_api import _envelope
from tests.test_chat import _base_chat_payload


def _items(*kcals, g=100):
    return [{"name": f"item{i}", "g": g, "kcal": k} for i, k in enumerate(kcals)]


def test_total_is_the_sum_of_rounded_items_and_macros_scale():
    est = {"kcal": 589, "p": 27.5, "c": 38.6, "g": 26.5, "items": _items(146, 150, 120, 44.4)}
    change = estimate_total.apply(est)
    assert change == {"model_kcal": 589, "items_kcal": 460}
    assert est["kcal"] == 460 and est["items"][3]["kcal"] == 44
    assert (est["p"], est["c"], est["g"]) == (21, 30, 21)


def test_equal_total_is_untouched():
    est = {"kcal": 416, "p": 25, "c": 39, "g": 20, "items": _items(146, 150, 120)}
    assert estimate_total.apply(est) is None
    assert est["p"] == 25


@pytest.mark.parametrize("estimate", [
    None, "x", {"kcal": 300, "items": []}, {"kcal": 300, "items": None},
    {"kcal": 300, "items": [{"name": "a", "g": 100}]},
    {"kcal": 300, "items": [{"name": "a", "g": 100, "kcal": 0}]},
    {"kcal": 300, "items": [{"name": "a", "g": 0, "kcal": 120}]},
    {"kcal": 300, "items": [{"name": "a", "g": 100, "kcal": float("nan")}, {"name": "b", "g": 50, "kcal": -3}]},
])
def test_without_usable_item_energy_nothing_applies(estimate):
    before = json.dumps(estimate, default=str)
    assert estimate_total.apply(estimate) is None
    assert json.dumps(estimate, default=str) == before


def test_invalid_items_are_skipped_in_the_sum_and_zero_model_kcal_keeps_macros():
    est = {"kcal": 0, "p": 10, "c": 20, "g": 5.5,
           "items": _items(100, 50) + [{"name": "bad", "g": -1, "kcal": 999}, "junk"]}
    assert estimate_total.apply(est) == {"model_kcal": 0, "items_kcal": 150}
    assert est["kcal"] == 150 and (est["p"], est["c"], est["g"]) == (10, 20, 6)


def test_ties_round_upward_once():
    est = {"kcal": 10, "p": 0, "c": 0, "g": 0, "items": _items(0.5, 1.5)}
    assert estimate_total.apply(est) == {"model_kcal": 10, "items_kcal": 3}
    assert [i["kcal"] for i in est["items"]] == [1, 2]


def test_hundred_items():
    est = {"kcal": 1, "p": 1, "c": 1, "g": 1, "items": _items(*([10.4] * 100))}
    assert estimate_total.apply(est)["items_kcal"] == 1000


@pytest.mark.parametrize("reply,expected", [
    ("Total: 589 kcal · 27 g P", "Total: 460 kcal · 27 g P"),
    ("cerca de 589,0 kcal", "cerca de 460 kcal"),
    ("Total do almoço: 1.589 kcal.", "Total do almoço: 1.589 kcal."),
    ("ovos 146 kcal, pão 150 kcal: 589 kcal", "ovos 146 kcal, pão 150 kcal: 460 kcal"),
    ("cerca de 590 kcal", "cerca de 590 kcal"),
    ("1589 kcal", "1589 kcal"),
    ("sem número", "sem número"),
])
def test_reply_rewrites_only_the_quoted_model_total(reply, expected):
    assert estimate_total.rewrite_reply(reply, 589, 460) == expected
    assert estimate_total.rewrite_reply("Total do almoço: 1.589 kcal.", 1589, 1460) == "Total do almoço: 1.460 kcal."


def test_apply_turn_keeps_a_total_copied_from_a_supplied_record():
    payload = {"reply": "Almoço igual: 620 kcal.", "estimate": {"kcal": 620, "p": 52, "c": 70, "g": 12,
                                                            "items": _items(192, 76, 239)}}
    assert estimate_total.apply_turn(payload, {620.0, 430.0}) is None
    assert payload["estimate"]["kcal"] == 620 and payload["reply"] == "Almoço igual: 620 kcal."
    assert estimate_total.apply_turn(payload, {430.0}) == {"model_kcal": 620, "items_kcal": 507}


def test_apply_turn_changes_estimate_and_reply_together():
    payload = {"reply": "Estimei 589 kcal · 27 g P.", "estimate": {"kcal": 589, "p": 27, "c": 38, "g": 26,
                                                                  "items": _items(146, 150, 120, 44)}}
    assert estimate_total.apply_turn(payload) == {"model_kcal": 589, "items_kcal": 460}
    assert payload["reply"] == "Estimei 460 kcal · 27 g P."
    assert estimate_total.apply_turn({"reply": "x", "estimate": None}) is None
    assert estimate_total.apply_turn(None) is None


# ---- routes -------------------------------------------------------------------------------


def _turn(**overrides):
    out = dict(
        reply="Estimei 589 kcal · 27 g P · 38 g C · 26 g G.", intent="log",
        estimate=dict(kcal=589, p=27.5, c=38.6, g=26.5, confidence="medium", question=None,
                      items=_items(146, 150, 120, 44), suggested_slot="cafe",
                      meal_text="2 ovos, 1 pão francês, 200 ml de leite"),
        record_intent="clear", meal_day="today", skip_slot=None, memory_updates=[], memory_used=[],
        digest=None, plan_budget=None, scope="in_scope",
    )
    out.update(overrides)
    return out


def _llm(payloads, captured=None):
    queue = list(payloads)

    def handler(req: httpx2.Request) -> httpx2.Response:
        if captured is not None:
            captured.append(req)
        return httpx2.Response(200, json=_envelope(json.dumps(queue.pop(0), ensure_ascii=False)))

    return main.LlmClient(api_key="k", transport=httpx2.MockTransport(handler))


class _Clean:
    def check(self, **_):
        return CLEAN


def _chat(body_overrides, payloads):
    body = main.ChatIn.model_validate(_base_chat_payload(**body_overrides))
    record: dict = {}
    out = main.chat_reply(_llm(payloads), _Clean(), body, None, record, Deadline())
    return out, record


def test_chat_log_total_is_the_item_sum_and_reply_and_log_follow():
    out, record = _chat(dict(text="2 ovos, 1 pão francês e 200 ml de leite", clarify_rounds=0,
                             auto_record=True, facts=[]), [_turn()])
    est = out["estimate"]
    assert est["kcal"] == 460 and est["p"] == 21 and est["c"] == 30 and est["g"] == 21
    assert out["reply"].startswith("Estimei 460 kcal")
    assert out["record"] == "auto"
    assert record["kcal_resum"] == {"model_kcal": 589, "items_kcal": 460}


def test_chat_without_items_is_untouched_and_logged_null():
    out, record = _chat(dict(text="comi um prato", clarify_rounds=0, auto_record=True, facts=[]),
                        [_turn(estimate=dict(_turn()["estimate"], items=[]))])
    assert out["estimate"]["kcal"] == 589 and record["kcal_resum"] is None


def test_plan_budget_uses_the_server_total():
    body = dict(text="me monta um jantar", clarify_rounds=0, auto_record=True, facts=[], plan_budget=True)
    plan = _turn(intent="plan", record_intent="unsure", plan_budget=dict(reserved=[], choice=None),
                 estimate=dict(_turn()["estimate"], kcal=900, items=_items(146, 150, 120, 44)))
    req = _base_chat_payload(**body)
    req["day"]["remaining_kcal"] = 500
    record: dict = {}
    out = main.chat_reply(_llm([plan]), _Clean(), main.ChatIn.model_validate(req), None, record, Deadline())
    # 460 against 500: fits; without the server total the model's 900 would have triggered an adjustment call.
    assert out["estimate"]["kcal"] == 460
    assert out["plan_budget"] == {"limit_kcal": 500, "over_kcal": 0, "reserved": [], "choice": None}
    assert record.get("adjust_retry") is not True


def test_meal_change_revise_recomputes_instead_of_failing():
    req = _base_chat_payload(text="na verdade o café foi 2 ovos e 1 pão", clarify_rounds=0, auto_record=True,
                             facts=[], meal_changes=True)
    slot = req["profile"]["slots"][0]["id"]
    req["day"]["slots"] = [dict(id=s["id"], status="empty") for s in req["profile"]["slots"]]
    req["day"]["slots"][0] = dict(id=slot, status="eaten", text="1 ovo", kcal=73, p=6, c=1, g=5)
    turn = _turn(estimate=dict(_turn()["estimate"], kcal=400, suggested_slot=slot, items=_items(146, 150)),
                 meal_change=dict(operation="revise", base_slot=slot, addition=None))
    record: dict = {}
    out = main.chat_reply(_llm([turn]), _Clean(), main.ChatIn.model_validate(req), None, record, Deadline())
    assert out["estimate"] is not None and out["estimate"]["kcal"] == 296
    assert out["meal_change"] is not None and out["meal_change"]["operation"] == "revise"
