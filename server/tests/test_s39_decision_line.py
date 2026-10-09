"""S39 (ADR-056): decision line of a plan with options, chosen option, same-message skip, over_kcal per option."""

from __future__ import annotations

import json
import unittest
from typing import Any

import meal_window
import plan_budget as budgets
from chat_instructions import CUES, assemble
from evals.checks import FAIL, PASS, evaluate
from evals.preview import bubble, split_reply
from evals.run import CleanModerator
from main import ChatIn, _chat_text, _usual_foods, chat_reply
from moderation import Deadline

SLOTS = [
    {"id": "c", "name": "Café", "time": "07:00"},
    {"id": "a", "name": "Almoço", "time": "12:30"},
    {"id": "l", "name": "Lanche", "time": "16:00"},
    {"id": "j", "name": "Jantar", "time": "19:30"},
    {"id": "n", "name": "Ceia", "time": "22:00"},
]
TEXT = "hoje vou pular o lanche; à noite quero um hambúrguer: dois de patinho ou um com batata, qual dos dois?"


def _recent() -> list[dict[str, Any]]:
    out = []
    for day in ("2026-10-12", "2026-10-13", "2026-10-14"):
        out.append({"date": day, "slot_id": "n", "slot_name": "Ceia", "text": "iogurte com granola",
                    "kcal": 220, "p": 25, "c": 20, "g": 5})
        out.append({"date": day, "slot_id": "l", "slot_name": "Lanche", "text": "fruta e castanhas",
                    "kcal": 250, "p": 8, "c": 30, "g": 12})
    return out


def _request(**extra: Any) -> dict[str, Any]:
    data: dict[str, Any] = {
        "local_time": "2026-10-15T16:40:00-03:00",
        "profile": {"ceiling_kcal": 2200, "p_target": 150, "c_target": 240, "g_target": 70, "eat_back": "zero",
                    "slots": SLOTS, "tone": "duro"},
        "facts": [],
        "recent": _recent(),
        "day": {"date": "2026-10-15", "remaining_kcal": 950, "eaten_kcal": 1250, "eaten_p": 85, "eaten_c": 150,
                "eaten_g": 40,
                "slots": [{"id": "c", "status": "eaten", "kcal": 450, "p": 30, "c": 50, "g": 15, "text": "pão e ovo"},
                          {"id": "a", "status": "eaten", "kcal": 800, "p": 55, "c": 100, "g": 25, "text": "prato"},
                          {"id": "l", "status": "empty"}, {"id": "j", "status": "empty"},
                          {"id": "n", "status": "empty"}]},
        "text": TEXT,
        "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "skip_slots": True, "plan_budget": True,
        "actions": True,
    }
    data.update(extra)
    return data


def _estimate(kcal: int, p: int, text: str) -> dict[str, Any]:
    return {"kcal": kcal, "p": p, "c": 40, "g": 20, "confidence": "medium", "question": None,
            "items": [{"name": text, "g": 300, "kcal": kcal}], "suggested_slot": "j", "meal_text": text}


DOUBLE = "Dois hambúrgueres de patinho"
POTATO = "Hambúrguer com batata rústica"


def _payload(first_line: str, *, skip: bool = True) -> dict[str, Any]:
    reply = "\n".join([
        first_line,
        f"**Opção 1: {DOUBLE}**", "- 240 g de patinho", "**780 kcal** · P 70 · C 40 · G 20",
        f"**Opção 2: {POTATO}**", "- 120 g de patinho", "- 100 g de batata", "**690 kcal** · P 40 · C 40 · G 20",
        "Jantar passou o teto na sexta e no sábado.",
        "Ceia: iogurte com granola ~220 kcal · P 25",
    ])
    double, potato = _estimate(780, 70, "dois hambúrgueres de patinho"), _estimate(690, 40, "hambúrguer com batata")
    plan = {"id": "a1", "type": "plan", "slot": "j", "estimate": double, "record_intent": "unsure",
            "meal_day": "today", "meal_change": None, "workout": None, "recipe_id": None, "recipe": None,
            "plan_budget": {"reserved": [], "choice": None},
            "options": [{"id": "o1", "name": DOUBLE, "estimate": double}, {"id": "o2", "name": POTATO, "estimate": potato}]}
    actions = [plan] + ([{"id": "a2", "type": "skip", "slot": "l", "estimate": None, "record_intent": "clear",
                          "meal_day": "today", "meal_change": None, "workout": None, "recipe_id": None,
                          "recipe": None, "plan_budget": None, "options": None}] if skip else [])
    return {"reply": reply, "scope": "in_scope", "memory_updates": [], "memory_used": [], "digest": None,
            "actions": actions}


class FakeLlm:
    def __init__(self, payload: dict[str, Any]) -> None:
        self.payload = payload

    def chat_json(self, **kwargs: Any) -> dict[str, Any]:
        return json.loads(json.dumps(self.payload))


def _turn(payload: dict[str, Any], **extra: Any) -> dict[str, Any]:
    record: dict[str, Any] = {}
    out = chat_reply(FakeLlm(payload), CleanModerator(), ChatIn.model_validate(_request(**extra)), None, record,
                     Deadline())
    out["_record"] = record
    return out


def _plan(out: dict[str, Any]) -> dict[str, Any]:
    return next(a for a in out["actions"] if a["type"] == "plan")


OPTIONS = [{"id": "o1", "name": DOUBLE, "estimate": {"kcal": 780, "p": 70}},
           {"id": "o2", "name": POTATO, "estimate": {"kcal": 690, "p": 40}}]


class DecisionLineTests(unittest.TestCase):
    def test_option_named_by_name_number_and_words(self) -> None:
        self.assertEqual(budgets.named_option(POTATO, OPTIONS), "o2")
        self.assertEqual(budgets.named_option("hamburguer com batata", OPTIONS), "o2")
        self.assertEqual(budgets.named_option("Opção 1", OPTIONS), "o1")
        self.assertEqual(budgets.named_option("os dois de patinho", OPTIONS), "o1")
        self.assertIsNone(budgets.named_option("salada", OPTIONS))

    def test_rewrite_on_form_moves_to_top_with_server_numbers(self) -> None:
        reply = "Bom.\n**Vai de Dois hambúrgueres de patinho: ~600 kcal · P 50 g, cabe na janela do Jantar.** X."
        out, status = budgets.rewrite_decision(reply, OPTIONS, 730, "Jantar")
        self.assertEqual(status, budgets.DECISION_REWRITTEN)
        self.assertEqual(out.split("\n")[0], f"Vai de {DOUBLE}: ~780 kcal · P 70 g, passa ~50 kcal da janela do "
                                             f"Jantar. {POTATO} cabe.")
        self.assertEqual(out.split("\n")[1], "Bom.")

    def test_rewrite_verdicts(self) -> None:
        both_fit, _ = budgets.rewrite_decision(f"Vai de {POTATO}: ~1 kcal · P 1 g, x", OPTIONS, 800, "Jantar")
        self.assertEqual(both_fit, f"Vai de {POTATO}: ~690 kcal · P 40 g, cabe na janela do Jantar. {DOUBLE} também cabe.")
        other_over, _ = budgets.rewrite_decision(f"Vai de {POTATO}: ~1 kcal · P 1 g, x", OPTIONS, 730, "Jantar")
        self.assertEqual(other_over, f"Vai de {POTATO}: ~690 kcal · P 40 g, cabe na janela do Jantar. {DOUBLE} passa ~50 kcal.")
        both_over, _ = budgets.rewrite_decision(f"Vai de {POTATO}: ~1 kcal · P 1 g, x", OPTIONS, 480, "Jantar")
        self.assertEqual(both_over, f"Vai de {POTATO}: ~690 kcal · P 40 g, passa ~210 kcal da janela do Jantar; "
                                    f"{DOUBLE} passa ~300.")

    def test_off_form_left_as_written(self) -> None:
        for reply in ("Prefira a opção 2.\nOpção 1: x", "Vai de salada: ~300 kcal · P 10 g, cabe."):
            out, status = budgets.rewrite_decision(reply, OPTIONS, 730, "Jantar")
            self.assertEqual((out, status), (reply, budgets.DECISION_OFF_FORM))


class ChosenOptionTests(unittest.TestCase):
    def test_chosen_by_name_drives_estimate_budget_overs_and_closing(self) -> None:
        out = _turn(_payload(f"Vai de {POTATO}: ~700 kcal · P 30 g, cabe na janela do Jantar. {DOUBLE} passa ~80 kcal."))
        plan = _plan(out)
        self.assertEqual(plan["estimate"]["kcal"], 690)
        # The Lanche the same message skips reserves nothing: 950 - Ceia 220.
        self.assertEqual(plan["plan_budget"]["limit_kcal"], 730)
        self.assertEqual(plan["plan_budget"]["over_kcal"], 0)
        self.assertEqual({o["id"]: o["over_kcal"] for o in plan["options"]}, {"o1": 50, "o2": 0})
        lines = out["reply"].split("\n")
        self.assertEqual(lines[0], f"Vai de {POTATO}: ~690 kcal · P 40 g, cabe na janela do Jantar. {DOUBLE} passa ~50 kcal.")
        self.assertIn("Ceia: iogurte com granola ~260 kcal · P 25", lines)
        self.assertFalse(any(line.startswith("Lanche:") for line in lines))
        self.assertEqual((out["_record"]["chosen"], out["_record"]["decision_line"]), ("o2", "rewritten"))
        self.assertEqual(evaluate({"decision_line": {"chosen": "o2", "verdict": "other_over"},
                                   "option_over": {"chosen": 0}}, out),
                         {"decision_line": {"status": PASS, "detail": "o2 other_over"},
                          "option_over": {"status": PASS, "detail": "overs {'o1': 50, 'o2': 0}, limit 730"}})

    def test_without_decision_line_option_one_stays(self) -> None:
        out = _turn(_payload("Duas boas opções."))
        self.assertEqual(_plan(out)["estimate"]["kcal"], 780)
        self.assertEqual((out["_record"]["chosen"], out["_record"]["decision_line"]), ("o1", "off_form"))
        self.assertEqual(out["reply"].split("\n")[0], "Duas boas opções.")
        self.assertEqual(evaluate({"decision_line": True}, out)["decision_line"]["status"], FAIL)

    def test_without_skip_action_the_lanche_is_reserved(self) -> None:
        out = _turn(_payload(f"Vai de {POTATO}: ~700 kcal · P 30 g, x", skip=False), text="à noite um hambúrguer?")
        self.assertEqual(_plan(out)["plan_budget"]["limit_kcal"], 480)

    def test_no_over_kcal_without_the_capability(self) -> None:
        out = _turn(_payload(f"Vai de {POTATO}: ~700 kcal · P 30 g, x"), plan_budget=False)
        self.assertTrue(all("over_kcal" not in o for o in _plan(out)["options"]))
        self.assertEqual(out["_record"]["decision_line"], "rewritten")


class ServedSkipTests(unittest.TestCase):
    def test_skip_cue_leaves_the_meal_out_of_budget_and_windows(self) -> None:
        text = _chat_text(ChatIn.model_validate(_request()))
        budget = next(line for line in text.split("\n") if line.startswith("BUDGET:"))
        self.assertIn("Jantar=730 (reserved_upcoming=220: Ceia)", budget)
        windows = next(line for line in text.split("\n") if line.startswith("WINDOWS:"))
        self.assertNotIn("Lanche", windows)
        plain = _chat_text(ChatIn.model_validate(_request(text="à noite um hambúrguer?")))
        self.assertIn("Jantar=480", plain)

    def test_cued_skips_need_a_firm_marker_near_the_meal(self) -> None:
        slots = [meal_window.Slot("l", "Lanche", "empty"), meal_window.Slot("j", "Jantar", "empty"),
                 meal_window.Slot("a", "Almoço", "eaten")]
        firm = CUES["skip-firm"].markers
        hold = CUES["skip-pending"].markers + CUES["skip-hedge"].markers
        self.assertEqual(meal_window.cued_skips("Vou pular o lanche, e o jantar?", slots, firm, hold), ["l"])
        self.assertEqual(meal_window.cued_skips("acho que vou pular o lanche", slots, firm, hold), [])
        self.assertEqual(meal_window.cued_skips("pulei o almoço", slots, firm, hold), [])
        self.assertEqual(meal_window.cued_skips("o lanche foi bom, vou pular sobremesa", slots, firm, hold), [])


class UsualFoodTests(unittest.TestCase):
    def test_shortened_at_a_word_boundary(self) -> None:
        text = "pão integral com queijo minas frescal, peito de peru defumado e tomate cereja"
        body = ChatIn.model_validate(_request(recent=[{"date": "2026-10-14", "slot_id": "n", "slot_name": "Ceia",
                                                        "text": text, "kcal": 300, "p": 20, "c": 30, "g": 10}]))
        food = _usual_foods(body)["n"]
        self.assertTrue(food.endswith("..."))
        self.assertLessEqual(len(food), 60)
        self.assertTrue(text.startswith(food[:-3]))
        self.assertIn(text[len(food) - 3], " ,")


class PromptAndCheckTests(unittest.TestCase):
    def test_rules_and_cue_block_assembled(self) -> None:
        for branch in ("legacy", "meal_changes_duro"):
            text = assemble(branch)
            for marker in ("DECISION LINE:", "COMPARISON:", "COOKING METHOD:", "ASSUMPTIONS:",
                           "Vai de {name}: ~{kcal} kcal · P {protein} g, cabe na janela do {meal}.",
                           "qual dos dois"):
                self.assertIn(marker, text)
        self.assertIn("not when an assistant turn of today in HISTORY already names that pattern",
                      assemble("meal_changes_duro"))
        self.assertNotIn("already names that pattern", assemble("meal_changes"))

    def test_reply_count(self) -> None:
        out = {"reply": "Assumi pão. Assumi queijo."}
        self.assertEqual(evaluate({"reply_count": {"assumi": 1}}, out)["reply_count"]["status"], FAIL)
        self.assertEqual(evaluate({"reply_count": {"assumi": 2}}, out)["reply_count"]["status"], PASS)

    def test_option_over_arithmetic(self) -> None:
        out = _turn(_payload(f"Vai de {POTATO}: ~700 kcal · P 30 g, x"))
        _plan(out)["options"][0]["over_kcal"] = 0
        self.assertEqual(evaluate({"option_over": {}}, out)["option_over"]["status"], FAIL)


class PreviewTests(unittest.TestCase):
    def test_bubble_of_a_comparison(self) -> None:
        out = _turn(_payload(f"Vai de {POTATO}: ~700 kcal · P 30 g, x"))
        text = bubble(_request(), out)
        lines = text.split("\n")
        self.assertTrue(lines[0].startswith(f"Vai de {POTATO}"))
        self.assertIn(f"  │ Opção 2: {POTATO}", lines)
        self.assertIn("  │ Passa 50 kcal da janela do Jantar", lines)
        self.assertIn("  │ Cabe na janela do Jantar", lines)
        self.assertIn("  │ Dia: ~1940 de 2200 kcal · P 125 de 150", lines)
        self.assertIn("Jantar passou o teto na sexta e no sábado.", lines)
        self.assertNotIn("**Opção 1", text)
        self.assertNotIn("do que sobra", text)

    def test_split_keeps_trailing_after_the_last_option(self) -> None:
        lead, trailing = split_reply("Vai de x\n**Opção 1: a**\n- 1\n**Opção 2: b**\n- 2\n**9 kcal** · P 1\nFim")
        self.assertEqual((lead, trailing), (["Vai de x"], ["Fim"]))
        self.assertEqual(bubble({}, {"reply": "só texto"}), "só texto")


if __name__ == "__main__":
    unittest.main()
