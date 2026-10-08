"""S37 (ADR-051): plan options with ids, digest keeps them, discovery, equipment and liked facts."""

from __future__ import annotations

import json
import unittest
from typing import Any

from chat_instructions import assemble
from evals.checks import PASS, evaluate
from evals.run import CleanModerator
from llm import chat_format
from main import ChatIn, _chat_text, chat_reply
from moderation import Deadline
from shaping import shape_chat, shape_options

SLOTS = [{"id": "a", "name": "Almoço", "time": "12:30"}, {"id": "j", "name": "Jantar", "time": "20:00"}]


def _body(**extra: Any) -> ChatIn:
    data: dict[str, Any] = {
        "local_time": "2026-10-15T18:00:00-03:00",
        "profile": {"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65, "eat_back": "zero",
                    "slots": SLOTS},
        "facts": [],
        "day": {"date": "2026-10-15", "remaining_kcal": 1300, "eaten_kcal": 700, "eaten_p": 30,
                "slots": [{"id": "a", "status": "eaten", "kcal": 700, "p": 30, "c": 80, "g": 20, "text": "prato"},
                          {"id": "j", "status": "empty"}]},
        "text": "o que eu janto?",
        "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "actions": True,
    }
    data.update(extra)
    return ChatIn.model_validate(data)


def _estimate(kcal: int, p: int, text: str) -> dict[str, Any]:
    return {"kcal": kcal, "p": p, "c": 40, "g": 12, "confidence": "medium", "question": None,
            "items": [{"name": text, "g": 300, "kcal": kcal}], "suggested_slot": "j", "meal_text": text}


def _options_payload() -> dict[str, Any]:
    first = _estimate(480, 12, "omelete de legumes")
    return {"reply": "**Opção 1: Omelete** ...\n**Opção 2: Parmegiana** ...", "scope": "in_scope", "memory_updates": [],
            "memory_used": [], "digest": None, "actions": [{
                "id": "a1", "type": "plan", "slot": "j", "estimate": first, "record_intent": "unsure",
                "meal_day": "today", "meal_change": None, "workout": None, "recipe_id": None,
                "plan_budget": {"reserved": [], "choice": None},
                "options": [{"id": "o1", "name": "Omelete", "estimate": first},
                            {"id": "o2", "name": "Parmegiana", "estimate": _estimate(780, 45, "frango à parmegiana")},
                            {"id": "o2", "name": "Repetida", "estimate": _estimate(100, 5, "x")}]}]}


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


class OptionTests(unittest.TestCase):
    def test_schema_options_on_the_action(self) -> None:
        action = chat_format(["a", "j"], meal_changes=True)["schema"]["properties"]["actions"]["items"]
        option = action["properties"]["options"]["anyOf"][0]["items"]
        self.assertEqual(option["properties"]["id"]["enum"], ["o1", "o2"])
        self.assertEqual(option["required"], ["id", "name", "estimate"])

    def test_shape_options_ids_unique_with_energy(self) -> None:
        raw = _options_payload()["actions"][0]["options"]
        out = shape_options(raw, ["a", "j"])
        self.assertEqual([(o["id"], o["name"], o["estimate"]["kcal"]) for o in out],
                         [("o1", "Omelete", 480), ("o2", "Parmegiana", 780)])
        self.assertIsNone(shape_options([{"id": "o3", "name": "x", "estimate": _estimate(1, 1, "x")}], ["j"]))
        self.assertIsNone(shape_options("x", ["j"]))

    def test_open_request_returns_options_and_gets_no_boost(self) -> None:
        out = _turn(_options_payload())
        action = out["actions"][0]
        self.assertEqual([o["id"] for o in action["options"]], ["o1", "o2"])
        # Option 1 has 12 g of protein, far below the floor, but an open request is never boosted (ADR-055).
        self.assertEqual(action["estimate"]["kcal"], 480)
        self.assertNotIn("Para a proteína", out["reply"])
        self.assertIsNone(out["_record"]["protein_boost"])
        self.assertNotIn("options", out)  # only inside the action

    def test_option_check(self) -> None:
        out = _turn(_options_payload())
        self.assertEqual(evaluate({"actions": [{"type": "plan", "options": 2}]}, out)["actions"]["status"], PASS)

    def test_digest_rule_keeps_the_options(self) -> None:
        self.assertIn("keep for the last such turn each option as Opção {n}: {name}, {kcal} kcal", assemble("compact"))

    def test_plan_rule_names_options_and_later_references(self) -> None:
        for branch in ("legacy", "meal_changes"):
            text = assemble(branch)
            self.assertIn("**Opção 1: {name}** and **Opção 2: {name}**", text)
            self.assertIn("LATER REFERENCE:", text)
            self.assertIn("a reference to an option of an earlier plan", text)


class DiscoveryAndMemoryTests(unittest.TestCase):
    def test_discovery_line_and_rule(self) -> None:
        text = _chat_text(_body(discovery=True))
        self.assertIn("DISCOVERY: first_open\nCURRENT_USER_MESSAGE:", text)
        self.assertNotIn("DISCOVERY", _chat_text(_body()))
        self.assertIn("DISCOVERY: an input line DISCOVERY: first_open", assemble("meal_changes_duro"))

    def test_new_categories_in_request_and_schema(self) -> None:
        body = _body(facts=[{"id": "P1", "kind": "permanent", "category": "equipment", "key": "air fryer",
                             "text": "Tem air fryer"},
                            {"id": "D1", "kind": "dynamic", "category": "liked", "key": "parmegiana",
                             "text": "Gostou da parmegiana", "slot": "j"}])
        self.assertIn("P1 equipment air fryer: Tem air fryer", _chat_text(body))
        update = chat_format(["j"], ["P1"])["schema"]["properties"]["memory_updates"]["items"]
        self.assertIn("equipment", update["properties"]["category"]["enum"])
        self.assertIn("declared", update["required"])

    def test_shaping_declared_equipment_and_liked(self) -> None:
        payload = {"reply": "ok", "intent": "question", "estimate": None, "scope": "in_scope", "memory_updates": [
            {"op": "add", "id": None, "kind": "dynamic", "category": "routine", "key": "almoco", "slot": "a",
             "text": "arroz, feijão e frango", "kcal": 600, "p": 40, "c": 70, "g": 15, "declared": True},
            {"op": "add", "id": None, "kind": "permanent", "category": "equipment", "key": "air fryer", "slot": "j",
             "text": "air fryer", "kcal": None, "p": None, "c": None, "g": None, "declared": True},
            {"op": "add", "id": None, "kind": "dynamic", "category": "liked", "key": "parmegiana", "slot": "j",
             "text": "parmegiana de frango", "kcal": 780, "p": 45, "c": 50, "g": 40, "declared": False},
            {"op": "add", "id": None, "kind": "temp", "category": "equipment", "key": "x", "slot": None,
             "text": "x", "kcal": None, "p": None, "c": None, "g": None, "declared": False}]}
        out = shape_chat(payload, valid_slot_ids=["a", "j"], fact_ids=[], temp_facts=True)["memory_updates"]
        self.assertEqual(len(out), 3)
        self.assertTrue(out[0]["declared"])
        self.assertEqual((out[1]["category"], out[1]["slot"]), ("equipment", None))
        self.assertNotIn("declared", out[1])
        self.assertEqual((out[2]["slot"], out[2]["kcal"]), ("j", 780))


if __name__ == "__main__":
    unittest.main()
