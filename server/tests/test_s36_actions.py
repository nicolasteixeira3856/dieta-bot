"""S36 (ADR-050): typed actions per message, shaping per action, the legacy view and the evaluator checks."""

from __future__ import annotations

import json
import unittest
from typing import Any

from pydantic import ValidationError

import actions
from evals.checks import FAIL, PASS, evaluate, migrate_expect, repetition_status
from evals.run import CleanModerator
from main import ChatIn, chat_reply
from moderation import Deadline

SLOTS = [{"id": "c", "name": "Café", "time": "07:00"}, {"id": "a", "name": "Almoço", "time": "12:30"},
         {"id": "l", "name": "Lanche", "time": "16:00"}, {"id": "j", "name": "Jantar", "time": "20:00"}]


def _body(**extra: Any) -> ChatIn:
    data: dict[str, Any] = {
        "local_time": "2026-10-15T15:00:00-03:00",
        "profile": {"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65, "eat_back": "zero",
                    "slots": SLOTS},
        "facts": [],
        "day": {"date": "2026-10-15", "remaining_kcal": 2000, "slots": [{"id": s["id"], "status": "empty"}
                                                                       for s in SLOTS]},
        "text": "x",
        "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "skip_slots": True, "actions": True,
    }
    data.update(extra)
    return ChatIn.model_validate(data)


def _estimate(slot: str, kcal: int, p: int = 20, question: Any = None, text: str = "comida") -> dict[str, Any]:
    return {"kcal": kcal, "p": p, "c": 30, "g": 10, "confidence": "high" if question is None else "medium",
            "question": question, "items": [{"name": text, "g": 200, "kcal": kcal}], "suggested_slot": slot,
            "meal_text": text}


def _action(kind: str, slot: Any = None, estimate: Any = None, **extra: Any) -> dict[str, Any]:
    base = {"id": "x", "type": kind, "slot": slot, "estimate": estimate,
            "record_intent": "clear" if kind == "log" else "unsure", "meal_day": "today",
            "meal_change": {"operation": "new", "base_slot": None, "addition": None} if kind == "log" else None,
            "workout": None, "recipe_id": None, "options": None,
            "plan_budget": {"reserved": [], "choice": None} if kind == "plan" else None}
    base.update(extra)
    return base


def _payload(*listed: dict[str, Any], reply: str = "Ok.") -> dict[str, Any]:
    return {"reply": reply, "scope": "in_scope", "actions": list(listed), "memory_updates": [], "memory_used": [],
            "digest": None}


class SequenceLlm:
    def __init__(self, *payloads: dict[str, Any]) -> None:
        self.payloads = list(payloads)
        self.calls = 0

    def chat_json(self, **kwargs: Any) -> dict[str, Any]:
        self.calls += 1
        return json.loads(json.dumps(self.payloads[min(self.calls, len(self.payloads)) - 1]))


def _turn(llm: SequenceLlm, body: ChatIn | None = None) -> dict[str, Any]:
    record: dict[str, Any] = {}
    out = chat_reply(llm, CleanModerator(), body or _body(), None, record, Deadline())
    out["_record"] = record
    return out


class NormalizeTests(unittest.TestCase):
    def test_order_skips_last_in_profile_order_and_eaten_wins(self) -> None:
        listed = actions.normalize(_payload(_action("skip", "j"), _action("log", "a", _estimate("a", 500)),
                                            _action("skip", "c"), _action("skip", "a"), _action("plan", "l",
                                                                                                _estimate("l", 200))),
                                   ["c", "a", "l", "j"])
        self.assertEqual([(a["id"], a["type"], a.get("slot")) for a in listed],
                         [("a1", "log", "a"), ("a2", "plan", "l"), ("a3", "skip", "c"), ("a4", "skip", "j")])

    def test_cap_six_count_dropped_and_empty_is_a_question(self) -> None:
        many = _payload(*[_action("log", s, _estimate(s, 100)) for s in "cajlcaj"])
        self.assertEqual(len(actions.normalize(many, ["c", "a", "l", "j"])), 6)
        self.assertEqual(actions.dropped(many), 1)
        self.assertEqual([a["type"] for a in actions.normalize(_payload(), ["c"])], ["question"])
        self.assertEqual([a["type"] for a in actions.normalize({"actions": "x"}, ["c"])], ["question"])

    def test_legacy_payload_becomes_actions(self) -> None:
        legacy = {"intent": "log", "estimate": _estimate("a", 500), "skip_slots": ["c", "a"],
                  "workout": {"kcal": 200, "mode": "add"}, "record_intent": "clear", "meal_day": "today"}
        self.assertEqual([(a["type"], a.get("slot")) for a in actions.normalize(legacy, ["c", "a", "l", "j"])],
                         [("log", "a"), ("workout", None), ("skip", "c")])

    def test_question_with_others(self) -> None:
        self.assertTrue(actions.question_with_others([{"type": "question"}, {"type": "log"}]))
        self.assertFalse(actions.question_with_others([{"type": "question"}]))


class RouteTests(unittest.TestCase):
    def test_capability_needs_meal_changes(self) -> None:
        with self.assertRaises(ValidationError):
            _body(meal_changes=False, skip_slots=False)

    def test_whole_day_gives_one_action_per_meal(self) -> None:
        payload = _payload(_action("log", "c", _estimate("c", 350)), _action("log", "a", _estimate("a", 650)),
                           _action("log", "l", _estimate("l", 200)), reply="Café 350, almoço 650, lanche 200.")
        out = _turn(SequenceLlm(payload))
        self.assertEqual([(a["id"], a["type"], a["slot"], a["estimate"]["kcal"], a["record"]) for a in out["actions"]],
                         [("a1", "log", "c", 350, "auto"), ("a2", "log", "a", 650, "auto"), ("a3", "log", "l", 200, "auto")])
        # The legacy view is the first log.
        self.assertEqual(out["estimate"]["suggested_slot"], "c")
        self.assertEqual(out["record"], "auto")
        self.assertEqual(out["_record"]["actions"][2], {"type": "log", "record": "auto", "clarify": "released_confident"})

    def test_skip_and_meal_legacy_skip_slots(self) -> None:
        payload = _payload(_action("skip", "c"), _action("log", "a", _estimate("a", 600)))
        for capable in (True, False):
            out = _turn(SequenceLlm(payload), _body(actions=capable))
            self.assertEqual(out["skip_slots"], ["c"])
            self.assertEqual(out["estimate"]["suggested_slot"], "a")
            self.assertEqual("actions" in out, capable)
        out = _turn(SequenceLlm(payload))
        self.assertEqual([(a["type"], a["slot"], a["record"]) for a in out["actions"]],
                         [("log", "a", "auto"), ("skip", "c", "auto")])

    def test_plan_after_log_sees_the_log_eaten(self) -> None:
        plan = _action("plan", "j", _estimate("j", 700, p=60))
        alone = _turn(SequenceLlm(_payload(plan)), _body(plan_budget=True))
        after = _turn(SequenceLlm(_payload(_action("log", "a", _estimate("a", 400)), plan)), _body(plan_budget=True))
        alone_limit = alone["actions"][0]["plan_budget"]["limit_kcal"]
        after_limit = after["actions"][1]["plan_budget"]["limit_kcal"]
        self.assertLess(after_limit, alone_limit)
        self.assertIsNone(after["actions"][0]["plan_budget"])

    def test_question_next_to_an_action_is_regenerated_once_then_dropped(self) -> None:
        bad = _payload(_action("question"), _action("log", "a", _estimate("a", 600)))
        good = _payload(_action("log", "a", _estimate("a", 600)))
        llm = SequenceLlm(bad, good)
        out = _turn(llm)
        self.assertEqual(llm.calls, 2)
        self.assertEqual([a["type"] for a in out["actions"]], ["log"])
        self.assertTrue(out["_record"]["actions_retry"])
        stubborn = SequenceLlm(bad, bad)
        out = _turn(stubborn)
        self.assertEqual(stubborn.calls, 2)
        self.assertEqual([a["type"] for a in out["actions"]], ["log"])

    def test_clarification_on_one_of_two_meals_keeps_the_other_and_the_reply(self) -> None:
        reply = "Almoço anotado. No lanche: qual o tamanho do bolo?"
        payload = _payload(_action("log", "a", _estimate("a", 600)),
                           _action("log", "l", _estimate("l", 300, question="Qual o tamanho do bolo?")), reply=reply)
        out = _turn(SequenceLlm(payload))
        held = out["actions"][1]
        self.assertIsNone(held["estimate"])
        self.assertEqual(held["question"], "Qual o tamanho do bolo?")
        self.assertEqual(out["actions"][0]["record"], "auto")
        self.assertTrue(out["reply"].startswith("Almoço anotado."))

    def test_refusal_is_one_question_action(self) -> None:
        payload = _payload(_action("log", "a", _estimate("a", 600)))
        payload["scope"] = "out_of_scope"
        out = _turn(SequenceLlm(payload))
        self.assertEqual([(a["type"], a["estimate"]) for a in out["actions"]], [("question", None)])

    def test_workout_and_meal(self) -> None:
        payload = _payload(_action("workout", workout={"kcal": 300, "mode": "replace"}),
                           _action("log", "a", _estimate("a", 600)))
        out = _turn(SequenceLlm(payload), _body(workout=True))
        self.assertEqual(out["workout"], {"kcal": 300, "mode": "replace"})
        self.assertEqual(out["estimate"]["suggested_slot"], "a")
        self.assertEqual([(a["type"], a["workout"]) for a in out["actions"]],
                         [("workout", {"kcal": 300, "mode": "replace"}), ("log", None)])
        self.assertNotIn("workout", _turn(SequenceLlm(payload), _body(workout=False)))

    def test_closing_lines_once_after_the_last_log(self) -> None:
        payload = _payload(_action("log", "c", _estimate("c", 350)), _action("log", "a", _estimate("a", 650)),
                           reply="Café e almoço.")
        out = _turn(SequenceLlm(payload), _body(local_time="2026-10-15T13:00:00-03:00"))
        lines = out["reply"].split("\n")
        self.assertEqual(sum(1 for line in lines if line.startswith("Lanche:")), 1)
        self.assertEqual(sum(1 for line in lines if line.startswith("Jantar:")), 1)


class CheckTests(unittest.TestCase):
    OUT = {"reply": "ok", "actions": [
        {"id": "a1", "type": "log", "slot": "c", "estimate": _estimate("c", 350), "question": None, "record": "auto"},
        {"id": "a2", "type": "log", "slot": "a", "estimate": _estimate("a", 650), "question": None, "record": "auto"},
        {"id": "a3", "type": "skip", "slot": "j", "estimate": None, "question": None, "record": "auto"},
    ]}

    def test_greedy_one_to_one_and_order(self) -> None:
        want = {"actions": [{"type": "log", "slot": "a", "kcal_range": [600, 700]},
                            {"type": "log", "kcal_range": [300, 400], "record": "auto"}, {"type": "skip", "slot": "j"}],
                "actions_order": ["log", "log", "skip"]}
        self.assertEqual(repetition_status(evaluate(want, self.OUT)), PASS)
        twice = {"actions": [{"type": "log", "slot": "a"}, {"type": "log", "slot": "a"}]}
        self.assertEqual(evaluate(twice, self.OUT)["actions"]["status"], FAIL)
        self.assertEqual(evaluate({"actions_order": ["skip", "log", "log"]}, self.OUT)["actions_order"]["status"], FAIL)
        with self.assertRaises(ValueError):
            evaluate({"actions": [{"typo": 1}]}, self.OUT)

    def test_migrated_expectation_gives_the_legacy_verdict(self) -> None:
        legacy_out = {"reply": "Café 350.", "intent": "log", "estimate": _estimate("c", 350), "question": None,
                      "record": "auto", "skip_slots": ["j"], "meal_change": None, "memory_updates": []}
        as_actions = {"reply": legacy_out["reply"], "memory_updates": [], "actions": [
            {"id": "a1", "type": "log", "slot": "c", "estimate": legacy_out["estimate"], "question": None,
             "record": "auto", "meal_change": None},
            {"id": "a2", "type": "skip", "slot": "j", "estimate": None, "question": None, "record": "auto"}]}
        expectations = [
            {"intent": "log", "suggested_slot": "c", "kcal_range": [300, 400], "record": "auto", "skip_slots": ["j"],
             "reply_has": ["350"], "top_question": "absent"},
            {"intent": "log", "kcal_range": [500, 600]},
            {"intent": "plan"},
            {"skip_slots": ["c"]},
            {"intent": "log", "meal_change": None, "memory_updates_not": [{"op": "add"}]},
        ]
        for expect in expectations:
            with self.subTest(expect=expect):
                self.assertEqual(repetition_status(evaluate(expect, legacy_out)),
                                 repetition_status(evaluate(migrate_expect(expect), as_actions)))

    def test_migration_shape(self) -> None:
        self.assertEqual(migrate_expect({"intent": "skip", "skip_slot": "j", "refusal": "none"}),
                         {"refusal": "none", "actions": [{"type": "skip", "slot": "j"}]})
        self.assertEqual(migrate_expect({"intent": "question", "top_question": "present"}),
                         {"actions": [{"type": "question", "top_question": "present"}]})


if __name__ == "__main__":
    unittest.main()


class HeldClosingTests(unittest.TestCase):
    def test_held_meal_gets_no_closing_line(self) -> None:
        reply = ("Almoço: **493 kcal**.\nLanche: qual o tamanho do bolo?\n"
                 "Lanche: refeição com proteína ~950 kcal · P 65.\nJantar: refeição com proteína ~950 kcal · P 65.")
        payload = _payload(_action("log", "a", _estimate("a", 493)),
                           _action("log", "l", _estimate("l", 300, question="Qual o tamanho do bolo?")), reply=reply)
        out = _turn(SequenceLlm(payload), _body(local_time="2026-10-15T13:00:00-03:00"))
        lines = out["reply"].split("\n")
        self.assertFalse(any(line.startswith("Lanche:") and "kcal" in line for line in lines), lines)
        self.assertEqual(sum(1 for line in lines if line.startswith("Jantar:")), 1)
        self.assertIn("Lanche: qual o tamanho do bolo?", lines)
