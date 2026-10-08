"""S38 (ADR-052): saved recipes in the Chat: index and full recipe in the input, recall, log by recipe."""

from __future__ import annotations

import json
import unittest
from typing import Any

from pydantic import ValidationError

from chat_instructions import assemble
from evals.checks import PASS, evaluate
from evals.run import CleanModerator
from llm import chat_format
from main import ChatIn, _chat_text, chat_reply
from moderation import Deadline
from shaping import shape_recipe

SLOTS = [{"id": "a", "name": "Almoço", "time": "12:30"}, {"id": "j", "name": "Jantar", "time": "20:00"}]
RECIPES = [
    {"id": "R1", "name": "Wrap de atum", "kcal": 430, "p": 38, "c": 35, "g": 14,
     "key_foods": ["rap10", "atum", "cream cheese light"]},
    {"id": "R2", "name": "Escondidinho de frango", "kcal": 520, "p": 42, "c": 48, "g": 16, "key_foods": ["mandioca"]},
]
FULL = {**RECIPES[0], "ingredients": [{"name": "rap10", "g": 40, "kcal": 120}, {"name": "atum em lata", "g": 120,
                                                                               "kcal": 140},
                                      {"name": "cream cheese light", "g": 30, "kcal": 70},
                                      {"name": "alface e tomate", "g": 80, "kcal": 100}],
        "steps": ["Misture o atum com o cream cheese.", "Recheie e feche o wrap."]}


def _body(**extra: Any) -> ChatIn:
    data: dict[str, Any] = {
        "local_time": "2026-10-15T20:10:00-03:00",
        "profile": {"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65, "eat_back": "zero",
                    "slots": SLOTS},
        "facts": [],
        "day": {"date": "2026-10-15", "remaining_kcal": 1300,
                "slots": [{"id": "a", "status": "eaten", "kcal": 700, "p": 30, "c": 80, "g": 20, "text": "prato"},
                          {"id": "j", "status": "empty"}]},
        "text": "jantei o wrap de atum",
        "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "actions": True,
        "recipes": RECIPES, "recipe_full": FULL,
    }
    data.update(extra)
    return ChatIn.model_validate(data)


def _log(confidence: str, kcal: int = 455, recipe_id: Any = "R1") -> dict[str, Any]:
    estimate = {"kcal": kcal, "p": 35, "c": 36, "g": 15, "confidence": confidence, "question": None,
                "items": [{"name": "wrap de atum", "g": 270, "kcal": kcal}], "suggested_slot": "j",
                "meal_text": "Wrap de atum: rap10, atum, cream cheese light"}
    return {"reply": "Jantar: wrap de atum.", "scope": "in_scope", "memory_updates": [], "memory_used": [],
            "digest": None, "actions": [{"id": "a1", "type": "log", "slot": "j", "estimate": estimate,
                                         "record_intent": "clear", "meal_day": "today",
                                         "meal_change": {"operation": "new", "base_slot": None, "addition": None},
                                         "workout": None, "recipe_id": recipe_id, "recipe": None, "options": None,
                                         "plan_budget": None}]}


class FakeLlm:
    def __init__(self, payload: dict[str, Any]) -> None:
        self.payload = payload
        self.kwargs: dict[str, Any] = {}

    def chat_json(self, **kwargs: Any) -> dict[str, Any]:
        self.kwargs = kwargs
        return json.loads(json.dumps(self.payload))


def _turn(payload: dict[str, Any], body: ChatIn | None = None) -> dict[str, Any]:
    out = chat_reply(FakeLlm(payload), CleanModerator(), body or _body(), None, {}, Deadline())
    return out


class RequestTests(unittest.TestCase):
    def test_index_and_full_recipe_lines_before_day(self) -> None:
        text = _chat_text(_body())
        self.assertIn("RECIPES:\nR1 Wrap de atum · 430 kcal · P 38 · C 35 · G 14 · rap10, atum, cream cheese light\n"
                      "R2 Escondidinho de frango · 520 kcal · P 42 · C 48 · G 16 · mandioca", text)
        self.assertIn("RECIPE_FULL:\nR1 Wrap de atum · 430 kcal", text)
        self.assertIn("ingredientes: rap10 40 g (120 kcal); atum em lata 120 g (140 kcal)", text)
        self.assertIn("passos: 1. Misture o atum com o cream cheese. 2. Recheie e feche o wrap.", text)
        self.assertLess(text.index("RECIPES:"), text.index("DAY:"))
        self.assertNotIn("RECIPE", _chat_text(_body(recipes=[], recipe_full=None)))

    def test_validation(self) -> None:
        with self.assertRaises(ValidationError):
            _body(recipe_full={**FULL, "id": "R9"})
        with self.assertRaises(ValidationError):
            _body(recipes=RECIPES + [RECIPES[0]])
        with self.assertRaises(ValidationError):
            _body(recipes=[{**RECIPES[0], "id": "X1"}], recipe_full=None)
        with self.assertRaises(ValidationError):
            _body(recipes=[RECIPES[0]] * 31, recipe_full=None)

    def test_schema_enumerates_the_recipe_ids(self) -> None:
        action = chat_format(["a", "j"], meal_changes=True, recipe_ids=["R1", "R2"])["schema"]["properties"]["actions"]["items"]
        self.assertEqual(action["properties"]["recipe_id"]["enum"], ["R1", "R2", None])
        self.assertIn("recipe", action["required"])
        bare = chat_format(["a"], meal_changes=True)["schema"]["properties"]["actions"]["items"]
        self.assertEqual(bare["properties"]["recipe_id"], {"type": "null"})


class ShapingTests(unittest.TestCase):
    def test_log_by_recipe_copies_the_full_recipe(self) -> None:
        out = _turn(_log("high"))
        estimate = out["actions"][0]["estimate"]
        self.assertEqual((estimate["kcal"], estimate["p"], estimate["c"], estimate["g"]), (430, 38, 35, 14))
        self.assertEqual(estimate["items"], [])  # a copied record keeps its totals with no items (S28)
        self.assertTrue(estimate["meal_text"].startswith("Wrap de atum"))
        self.assertEqual(out["actions"][0]["recipe_id"], "R1")
        self.assertEqual(out["estimate"]["kcal"], 430)  # the legacy view records the copy too

    def test_log_with_only_the_index_line_copies_totals_without_items(self) -> None:
        out = _turn(_log("high", recipe_id="R2"), _body(text="jantei o escondidinho", recipe_full=None))
        estimate = out["actions"][0]["estimate"]
        self.assertEqual((estimate["kcal"], estimate["items"]), (520, []))

    def test_log_with_a_change_is_not_copied(self) -> None:
        out = _turn(_log("medium", kcal=520))
        self.assertEqual(out["actions"][0]["estimate"]["kcal"], 520)

    def test_unknown_recipe_id_is_dropped(self) -> None:
        out = _turn(_log("high", recipe_id="R7"))
        self.assertIsNone(out["actions"][0]["recipe_id"])
        self.assertEqual(out["actions"][0]["estimate"]["kcal"], 455)

    def test_recall_and_recipe_structure(self) -> None:
        recall = _log("high")
        recall["actions"][0].update(type="recipe_recall", estimate=None, meal_change=None, record_intent="unsure")
        out = _turn(recall, _body(text="lembra a receita do wrap?"))
        self.assertEqual((out["actions"][0]["type"], out["actions"][0]["recipe_id"]), ("recipe_recall", "R1"))
        self.assertEqual(evaluate({"actions": [{"type": "recipe_recall", "recipe_id": "R1"}]}, out)["actions"]["status"],
                         PASS)
        self.assertNotIn("recipe_id", out)

    def test_shape_recipe(self) -> None:
        raw = {"name": "  Frango   na air fryer ", "steps": ["Tempere.", " ", "Asse 18 min a 200 °C."],
               "ingredients": [{"name": "peito de frango", "g": 150, "kcal": 239.4}, {"name": "", "g": 10, "kcal": 1},
                               {"name": "azeite", "g": 0, "kcal": 40}]}
        self.assertEqual(shape_recipe(raw), {"name": "Frango na air fryer",
                                             "ingredients": [{"name": "peito de frango", "g": 150, "kcal": 239}],
                                             "steps": ["Tempere.", "Asse 18 min a 200 °C."]})
        self.assertIsNone(shape_recipe({"name": "x", "ingredients": [], "steps": []}))
        self.assertIsNone(shape_recipe(None))

    def test_recipes_rule_only_in_the_actions_branch(self) -> None:
        self.assertIn("RECIPES: an input block RECIPES", assemble("meal_changes"))
        self.assertNotIn("RECIPES: an input block RECIPES", assemble("legacy"))
        self.assertIn("recipe_id and recipe follow RECIPES", assemble("meal_changes_duro"))


if __name__ == "__main__":
    unittest.main()
