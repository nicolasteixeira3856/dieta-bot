"""S35 (ADR-049, CP10): workout energy through the Chat, the scope and the skip boundary."""

from __future__ import annotations

import json
import unittest
from typing import Any

from pydantic import ValidationError

from chat_instructions import assemble
from evals.run import CleanModerator
from llm import chat_format
from main import ChatIn, _chat_text, chat_reply
from moderation import Deadline
from shaping import shape_workout

SLOTS = [{"id": "a", "name": "Almoço", "time": "12:00"}, {"id": "j", "name": "Jantar", "time": "19:30"}]


def _body(**extra: Any) -> ChatIn:
    data: dict[str, Any] = {
        "local_time": "2026-10-15T18:00:00-03:00",
        "profile": {"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65, "eat_back": "50",
                    "slots": SLOTS},
        "facts": [],
        "day": {"date": "2026-10-15", "remaining_kcal": 1200, "workout_kcal": None,
                "slots": [{"id": "a", "status": "eaten", "kcal": 800, "p": 40, "c": 90, "g": 25,
                           "text": "prato feito"}, {"id": "j", "status": "empty"}]},
        "text": "treino de hoje, 450 kcal",
        "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "skip_slots": True, "workout": True,
    }
    data.update(extra)
    return ChatIn.model_validate(data)


class FakeLlm:
    def __init__(self, payload: dict[str, Any]) -> None:
        self.payload = payload

    def chat_json(self, **kwargs: Any) -> dict[str, Any]:
        return json.loads(json.dumps(self.payload))


def _turn(payload: dict[str, Any], body: ChatIn | None = None) -> dict[str, Any]:
    record: dict[str, Any] = {}
    out = chat_reply(FakeLlm(payload), CleanModerator(), body or _body(), None, record, Deadline())
    out["_record"] = record
    return out


QUESTION = {"reply": "Treino de 450 kcal; o app recalcula o crédito.", "intent": "question", "estimate": None,
            "record_intent": "unsure", "meal_day": "today", "skip_slots": [], "memory_updates": [],
            "memory_used": [], "digest": None, "plan_budget": None, "meal_change": None, "scope": "in_scope"}


class ShapeTests(unittest.TestCase):
    def test_bounds_and_modes(self) -> None:
        ok = {"scope": "in_scope"}
        self.assertEqual(shape_workout({**ok, "workout": {"kcal": 449.6, "mode": "replace"}}),
                         {"kcal": 450, "mode": "replace"})
        self.assertEqual(shape_workout({**ok, "workout": {"kcal": 200, "mode": "add"}}), {"kcal": 200, "mode": "add"})
        for bad in ({"kcal": 0, "mode": "add"}, {"kcal": 5001, "mode": "add"}, {"kcal": 300, "mode": "sum"},
                    {"kcal": True, "mode": "add"}, {"kcal": "300", "mode": "add"}, None, []):
            self.assertIsNone(shape_workout({**ok, "workout": bad}), bad)
        self.assertIsNone(shape_workout({"scope": "out_of_scope", "workout": {"kcal": 300, "mode": "add"}}))


class RouteTests(unittest.TestCase):
    def test_capability_needs_the_record_mark(self) -> None:
        with self.assertRaises(ValidationError):
            _body(clarify_rounds=None, meal_changes=False, skip_slots=False)
        with self.assertRaises(ValidationError):
            _body(workout="yes")
        self.assertFalse(ChatIn.model_validate({**_body().model_dump(mode="json"), "compact": True,
                                                "messages": [{"role": "user", "text": "oi"}]}).workout)

    def test_workout_only_turn_is_recorded(self) -> None:
        out = _turn({**QUESTION, "workout": {"kcal": 450, "mode": "replace"}})
        self.assertEqual(out["workout"], {"kcal": 450, "mode": "replace"})
        self.assertEqual(out["record"], "auto")
        self.assertIsNone(out["estimate"])
        self.assertEqual(out["_record"]["record"], "auto_workout")
        self.assertEqual(out["_record"]["workout"], {"kcal": 450, "mode": "replace"})

    def test_workout_without_capability_is_absent(self) -> None:
        body = _body(workout=False)
        out = _turn({**QUESTION, "workout": {"kcal": 450, "mode": "replace"}}, body)
        self.assertNotIn("workout", out)
        self.assertEqual(out["record"], "none")

    def test_no_number_and_refusal_give_null(self) -> None:
        self.assertIsNone(_turn({**QUESTION, "workout": None})["workout"])
        refused = _turn({**QUESTION, "scope": "out_of_scope", "workout": {"kcal": 450, "mode": "add"}})
        self.assertIsNone(refused["workout"])
        self.assertEqual(refused["record"], "none")

    def test_workout_next_to_a_meal_keeps_both(self) -> None:
        meal = {**QUESTION, "intent": "log", "record_intent": "clear", "reply": "Jantar: 520 kcal.",
                "estimate": {"kcal": 520, "p": 38, "c": 50, "g": 18, "confidence": "high", "question": None,
                             "items": [{"name": "omelete", "g": 200, "kcal": 320},
                                       {"name": "pão", "g": 70, "kcal": 200}],
                             "suggested_slot": "j", "meal_text": "omelete e pão"},
                "meal_change": {"operation": "new", "base_slot": None, "addition": None},
                "workout": {"kcal": 300, "mode": "add"}}
        out = _turn(meal)
        self.assertEqual(out["workout"], {"kcal": 300, "mode": "add"})
        self.assertEqual(out["estimate"]["kcal"], 520)
        self.assertEqual(out["record"], "auto")
        self.assertEqual(out["_record"]["record"], "auto_log")


class PromptTests(unittest.TestCase):
    def test_scope_skip_boundary_and_workout_rule_in_every_chat_prefix(self) -> None:
        for branch in ("legacy", "legacy_duro", "meal_changes", "meal_changes_duro"):
            text = assemble(branch)
            self.assertIn("a report of a workout done today with or without its energy in kcal", text)
            self.assertIn("training plans, exercise prescriptions or physiology", text)
            self.assertIn("Skipping one meal of the day or saying so", text)
            self.assertIn("WORKOUT: workout is {kcal, mode}", text)

    def test_schema_and_guard(self) -> None:
        schema = chat_format(["a"])["schema"]
        self.assertIn("workout", schema["required"])
        self.assertEqual(schema["properties"]["workout"]["anyOf"][0]["properties"]["mode"]["enum"], ["replace", "add"])
        self.assertIn("treino em kcal", _chat_text(_body()))


if __name__ == "__main__":
    unittest.main()
