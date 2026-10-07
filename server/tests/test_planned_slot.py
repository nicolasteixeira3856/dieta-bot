"""S30 part C (ADR-046): a planned slot in DAY. All data is synthetic."""

from __future__ import annotations

import os
import unittest
from typing import Any

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"

from pydantic import ValidationError

import main
import meal_window as mw
from meal_window import Slot
from tests.test_chat import _estimate, _v2_payload
from tests.test_record import _model

PLAN = {"id": "3", "status": "planned", "text": "frango grelhado com arroz", "kcal": 600, "p": 45, "c": 60, "g": 15}
NEW = {"operation": "new", "base_slot": None, "addition": None}


def _request(day_slots: list[dict[str, Any]] | None = None, **kw: Any) -> dict[str, Any]:
    payload = _v2_payload(**{"facts": [], "clarify_rounds": 0, "auto_record": True, "meal_changes": True,
                             "text": "jantei frango com arroz", **kw})
    payload["day"]["slots"] = day_slots if day_slots is not None else [{"id": "1", "status": "empty"}, dict(PLAN)]
    return payload


def _shape(payload: dict[str, Any], **kw: Any) -> dict[str, Any]:
    body = main.ChatIn.model_validate(_request(**kw))
    out, _, _ = main.shape_chat_turn(body, payload)
    out["_difference"] = main._plan_difference(body, payload, out)
    return out


class ContractTests(unittest.TestCase):
    def test_planned_needs_text_and_finite_nonnegative_numbers(self) -> None:
        self.assertEqual(main.ChatIn.model_validate(_request()).day.slots[1].status, "planned")
        bad = [{**PLAN, "text": None}, {**PLAN, "text": " "}, {**PLAN, "kcal": None}, {**PLAN, "p": -1},
               {**PLAN, "g": float("inf")}, {**PLAN, "status": "reservado"}]
        for slot in bad:
            with self.subTest(slot=slot), self.assertRaises(ValidationError):
                main.ChatIn.model_validate(_request([{"id": "1", "status": "empty"}, slot]))

    def test_legacy_shapes_are_unchanged(self) -> None:
        body = main.ChatIn.model_validate(_request([{"id": "1", "status": "empty"}, {"id": "3", "status": "empty"}]))
        self.assertEqual([s.status for s in body.day.slots], ["empty", "empty"])

    def test_day_serialization_carries_the_plan(self) -> None:
        text = main._chat_text(main.ChatIn.model_validate(_request()))
        self.assertIn('3:planned (600.0kcal, 45.0P 60.0C 15.0G, text="frango grelhado com arroz")', text)

    def test_a_planned_total_is_not_a_copied_record(self) -> None:
        self.assertNotIn(600.0, main._record_totals(main.ChatIn.model_validate(_request())))


class WindowTests(unittest.TestCase):
    slots = [
        Slot("1", "Café", "eaten", 400, 20, "07:00"),
        Slot("2", "Almoço", "empty", time="12:30"),
        Slot("3", "Lanche", "empty", time="16:00"),
        Slot("4", "Jantar", "planned", 650, 45, "20:00"),
    ]
    expected = {"1": 500, "2": 500, "3": 500, "4": 500}

    def test_a_planned_slot_is_reserved_by_its_own_kcal_without_cap(self) -> None:
        w = mw.window(self.slots, self.expected, 1600, "2", now="11:00")
        self.assertIn({"label": "Jantar", "kcal": 650}, w.reserved)
        # The lunch and the snack share the 950 left by the plan: the snack is capped at its share, 475.
        self.assertEqual(w.reserved, [{"label": "Lanche", "kcal": 475}, {"label": "Jantar", "kcal": 650}])
        self.assertEqual(w.limit_kcal, 1600 - 475 - 650)

    def test_the_plan_target_is_not_reserved(self) -> None:
        w = mw.window(self.slots, self.expected, 1600, "4", now="11:00")
        self.assertNotIn("Jantar", [r["label"] for r in w.reserved])

    def test_a_planned_slot_is_reserved_even_after_its_time(self) -> None:
        w = mw.window(self.slots, self.expected, 1600, None, now="23:00")
        self.assertEqual(w.reserved, [{"label": "Jantar", "kcal": 650}])

    def test_a_stated_reservation_never_overrides_the_plan(self) -> None:
        w = mw.window(self.slots, self.expected, 1600, "2", [{"label": "jantar", "kcal": 900}], now="11:00",
                      typed={900})
        self.assertIn({"label": "Jantar", "kcal": 650}, w.reserved)

    def test_windows_line_lists_the_plan_and_shares_the_rest(self) -> None:
        line = mw.windows_line(self.slots, self.expected, 1600, 100, "11:00")
        self.assertEqual(line, "WINDOWS: Almoço ~475 kcal P 28 · Lanche ~475 kcal P 27 · Jantar: planejado 650 kcal | faltam 100 g P")

    def test_budget_line_counts_the_plan(self) -> None:
        line = mw.budget_line(self.slots, self.expected, 1600, 100, "11:00")
        self.assertIn("Jantar=", line)
        self.assertIn("any other meal=0 (reserved_upcoming=1600: Almoço, Lanche, Jantar)", line)

    def test_closing_lines_skip_the_planned_slot(self) -> None:
        reply = "Almoço registrado.\nLanche: iogurte ~1 kcal · P 1\nJantar: frango ~1 kcal · P 1"
        out = mw.close_reply(reply, self.slots, self.expected, 1600, 100, "2", 500, 30, now="12:40")
        self.assertEqual(out, "Almoço registrado.\nLanche: iogurte ~450 kcal · P 25")


class MealChangeTests(unittest.TestCase):
    def test_a_log_into_a_planned_slot_is_a_new_meal(self) -> None:
        estimate = _estimate(confidence="high", question=None, suggested_slot="3")
        out = _shape(_model(estimate=estimate, meal_change=NEW))
        self.assertEqual((out["record"], out["meal_change"]["operation"], out["meal_change"]["base_slot"]),
                         ("auto", "new", None))

    def test_an_addition_on_a_planned_base_fails_safe(self) -> None:
        estimate = _estimate(confidence="high", question=None, suggested_slot="3")
        add = {"operation": "add", "base_slot": "3", "addition": None}
        # The route turns this into the fallback turn (no record), like any invalid meal change.
        with self.assertRaises(ValueError):
            _shape(_model(estimate=estimate, meal_change=add))


class DifferenceTests(unittest.TestCase):
    def _log(self, kcal: int, reply: str = "Jantar de frango com arroz.", **kw: Any) -> dict[str, Any]:
        items = [{"name": "frango", "g": 150, "kcal": kcal - 200}, {"name": "arroz", "g": 150, "kcal": 200}]
        estimate = _estimate(confidence="high", question=None, suggested_slot="3", kcal=kcal, items=items)
        return _shape(_model(estimate=estimate, meal_change=NEW, reply=reply, **kw))

    def test_over_under_and_equal(self) -> None:
        over = self._log(720)
        self.assertEqual((over["reply"], over["_difference"]), ("Jantar de frango com arroz.\n+120 kcal sobre o plano.", 120))
        self.assertTrue(self._log(520)["reply"].endswith("\n−80 kcal abaixo do plano."))
        self.assertTrue(self._log(600)["reply"].endswith("\nIgual ao plano."))

    def test_a_difference_written_by_the_model_is_replaced(self) -> None:
        out = self._log(720, reply="Jantar de frango. 100 kcal sobre o plano.")
        self.assertEqual(out["reply"], "Jantar de frango.\n+120 kcal sobre o plano.")

    def test_no_clause_for_a_plan_a_held_log_another_day_or_an_empty_slot(self) -> None:
        plan = _shape(_model("plan", estimate=_estimate(question=None, suggested_slot="3")))
        held = _shape(_model(estimate=_estimate(confidence="low", question="Quanto arroz?", suggested_slot="3"),
                             meal_change=NEW, record_intent="unsure"))
        other = _shape(_model(estimate=_estimate(confidence="high", question=None, suggested_slot="3"),
                              meal_change=NEW, meal_day="other"))
        empty = _shape(_model(estimate=_estimate(confidence="high", question=None, suggested_slot="1"), meal_change=NEW))
        for out in (plan, held, other, empty):
            with self.subTest(reply=out["reply"]):
                self.assertNotIn("plano", out["reply"])
                self.assertIsNone(out["_difference"])


if __name__ == "__main__":
    unittest.main()
