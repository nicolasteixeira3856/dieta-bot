"""S42 (ADR-058): extras outside the meals and a record in a named past day. All data is synthetic."""

from __future__ import annotations

import unittest
from datetime import date
from typing import Any

from pydantic import ValidationError

import day_ref
import llm
from main import ChatIn, _chat_text
from tests.test_s36_actions import SLOTS, SequenceLlm, _action, _estimate, _payload, _turn

THURSDAY = date(2026, 10, 15)


def _body(**extra: Any) -> ChatIn:
    data: dict[str, Any] = {
        "local_time": "2026-10-15T15:00:00-03:00",
        "profile": {"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65, "eat_back": "zero",
                    "slots": SLOTS},
        "facts": [],
        "day": {"date": "2026-10-15", "remaining_kcal": 1200, "eaten_kcal": 800,
                "slots": [{"id": "c", "status": "eaten", "text": "pão", "kcal": 300, "p": 10, "c": 50, "g": 5},
                          {"id": "a", "status": "eaten", "text": "prato", "kcal": 500, "p": 30, "c": 60, "g": 15},
                          {"id": "l", "status": "empty"}, {"id": "j", "status": "empty"}]},
        "text": "x",
        "clarify_rounds": 0, "auto_record": True, "meal_changes": True, "skip_slots": True, "actions": True,
        "extras": True, "other_day": True,
    }
    data.update(extra)
    return ChatIn.model_validate(data)


def _resolve(text: str, today: date = THURSDAY, first_day: date | None = None) -> str | None:
    found = day_ref.resolve(text, today, first_day)
    return found.line() if found else None


class ResolverTests(unittest.TestCase):
    def test_every_cue(self) -> None:
        table = {
            "ontem jantei pizza": "DAY_REF: 2026-10-14 (ontem, quarta)",
            "anteontem almocei": "DAY_REF: 2026-10-13 (anteontem, terca)",
            "na segunda almocei arroz": "DAY_REF: 2026-10-12 (segunda)",
            "sábado passado comi sushi": "DAY_REF: 2026-10-10 (sabado)",
            "na sexta-feira jantei": "DAY_REF: 2026-10-09 (sexta)",
            "dia 3 jantei fora": "DAY_REF: 2026-10-03 (dia 3, sabado)",
            "dia 20 de setembro": "DAY_REF: 2026-09-20 (dia 20 de setembro, domingo)",
            "dia 20 do mês passado": "DAY_REF: 2026-09-20 (dia 20 do mes passado, domingo)",
            "3 dias atrás comi": "DAY_REF: 2026-10-12 (3 dias atras, segunda)",
            "dois dias atrás": "DAY_REF: 2026-10-13 (dois dias atras, terca)",
        }
        for text, line in table.items():
            with self.subTest(text):
                self.assertEqual(_resolve(text), line)

    def test_today_weekday_is_last_week_and_today_is_no_ref(self) -> None:
        self.assertEqual(_resolve("na quinta jantei pizza"), "DAY_REF: 2026-10-08 (quinta)")
        self.assertIsNone(_resolve("dia 15 comi"))
        self.assertIsNone(_resolve("jantei pizza"))

    def test_month_boundary(self) -> None:
        first = date(2026, 11, 1)
        self.assertEqual(_resolve("ontem jantei", first), "DAY_REF: 2026-10-31 (ontem, sabado)")
        # November has no 31st: the only candidate is October's.
        self.assertEqual(_resolve("dia 31 jantei", first), "DAY_REF: 2026-10-31 (dia 31, sabado)")
        self.assertEqual(_resolve("na segunda", date(2026, 3, 1)), "DAY_REF: 2026-02-23 (segunda)")

    def test_two_candidates_and_future_are_not_bound(self) -> None:
        self.assertEqual(_resolve("dia 3 jantei pizza", date(2026, 10, 2)),
                         "DAY_REF: ambiguous (2026-09-03 = 3 de setembro ou 2026-10-03 = 3 de outubro)")
        self.assertEqual(_resolve("ontem e na segunda"),
                         "DAY_REF: ambiguous (2026-10-12 = 12 de outubro ou 2026-10-14 = 14 de outubro)")
        self.assertEqual(_resolve("dia 20 de outubro jantei"), "DAY_REF: future")
        # September has no 31st: on 30 October the only 31st is still ahead.
        self.assertEqual(_resolve("dia 31 jantei sushi", date(2026, 10, 30)), "DAY_REF: future")

    def test_bound_and_too_old(self) -> None:
        self.assertEqual(_resolve("30 dias atrás"), "DAY_REF: 2026-09-15 (30 dias atras, terca)")
        self.assertEqual(_resolve("40 dias atrás jantei"), "DAY_REF: too_old")
        self.assertEqual(_resolve("ontem", first_day=THURSDAY), "DAY_REF: too_old")
        self.assertEqual(_resolve("ontem", first_day=date(2026, 10, 14)), "DAY_REF: 2026-10-14 (ontem, quarta)")

    def test_copy_source_and_ordinals_name_no_day(self) -> None:
        for text in ("almocei o mesmo de ontem", "igual ao jantar de segunda", "fiz a segunda opção",
                     "na segunda vez", "no quintal"):
            with self.subTest(text):
                self.assertIsNone(_resolve(text))


class RequestTests(unittest.TestCase):
    def test_capabilities_need_meal_changes_and_are_strict(self) -> None:
        with self.assertRaises(ValidationError):
            _body(meal_changes=False, skip_slots=False, actions=False)
        with self.assertRaises(ValidationError):
            _body(actions=False)
        with self.assertRaises(ValidationError):
            _body(extras="sim")
        compact = _body(compact=True, messages=[{"role": "user", "text": "x"}])
        self.assertFalse(compact.extras or compact.other_day)

    def test_slots_by_day_recent_extra_and_first_day(self) -> None:
        weekend = [{"id": "b", "name": "Brunch", "time": "10:30"}, {"id": "j", "name": "Jantar", "time": "20:00"}]
        body = _body(profile={"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65,
                              "eat_back": "zero", "slots": SLOTS,
                              "slots_by_day": [{"weekdays": [6, 7], "slots": weekend}]},
                     recent=[{"date": "2026-10-14", "slot_id": "extra", "time": "15:40", "text": "energético",
                              "kcal": 110, "p": 0, "c": 27, "g": 0}],
                     first_day="2026-09-01", text="sábado passado comi um brunch")
        self.assertEqual([s.id for s in body.slots_on(date(2026, 10, 10))], ["b", "j"])
        self.assertEqual([s.id for s in body.slots_on(date(2026, 10, 12))], ["c", "a", "l", "j"])
        text = _chat_text(body)
        self.assertIn('2026-10-14 quarta Extra 15:40 · "energético" · 110kcal 0P 27C 0G', text)
        self.assertIn("EXTRAS: on", text)
        self.assertIn("DAY_REF: 2026-10-10 (sabado) slots=[b (Brunch at 10:30), j (Jantar at 20:00)]", text)
        for bad in ({"weekdays": [0], "slots": weekend}, {"weekdays": [6, 6], "slots": weekend}):
            with self.subTest(bad), self.assertRaises(ValidationError):
                _body(profile={"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65,
                               "eat_back": "zero", "slots": SLOTS, "slots_by_day": [bad]})
        with self.assertRaises(ValidationError):
            _body(recent=[{"date": "2026-10-14", "slot_id": "extra", "time": "25:00", "text": "x",
                           "kcal": 1, "p": 0, "c": 0, "g": 0}])

    def test_no_lines_without_the_capabilities(self) -> None:
        text = _chat_text(_body(extras=False, other_day=False, text="ontem jantei pizza"))
        self.assertNotIn("EXTRAS", text)
        self.assertNotIn("DAY_REF", text)

    def test_schema(self) -> None:
        fmt = llm.chat_format(["c", "a"], [], meal_changes=True, extras=True, other_slot_ids=["b"])
        action = fmt["schema"]["properties"]["actions"]["items"]
        self.assertEqual(action["properties"]["slot"]["enum"], ["c", "a", "extra", None])
        self.assertIn("time", action["required"])
        estimate = action["properties"]["estimate"]["anyOf"][0]
        self.assertEqual(estimate["properties"]["suggested_slot"]["enum"], ["c", "a", "b", None])
        plain = llm.chat_format(["c", "a"], [], meal_changes=True)["schema"]["properties"]["actions"]["items"]
        self.assertNotIn("time", plain["properties"])
        self.assertEqual(plain["properties"]["slot"]["enum"], ["c", "a", None])


def _extra_log(kcal: int = 110, time: Any = "15:40", **extra: Any) -> dict[str, Any]:
    return _action("log", "extra", _estimate(None, kcal, p=0, text="1 lata de energético"), time=time, **extra)


class ExtraTests(unittest.TestCase):
    def test_extra_is_recorded_in_no_slot_with_its_time(self) -> None:
        payload = _payload(_extra_log(), reply="Energético: **110 kcal**.\nLanche: iogurte ~200 kcal · P 10")
        out = _turn(SequenceLlm(payload), _body(text="tomei um energético entre o almoço e o lanche às 15:40"))
        action = out["actions"][0]
        self.assertEqual((action["type"], action["slot"], action["time"], action["record"], action["day"]),
                         ("log", "extra", "15:40", "auto", None))
        self.assertIsNone(action["estimate"]["suggested_slot"])
        # The extra answers no meal: Lanche and Jantar share what is left after its 110 kcal.
        self.assertIn("Lanche: iogurte ~", out["reply"])
        self.assertIn("Jantar: ", out["reply"])
        self.assertEqual(out["_record"]["record"], "auto_extra")

    def test_bad_time_is_null_and_unsure_extra_asks(self) -> None:
        out = _turn(SequenceLlm(_payload(_extra_log(time="tarde", record_intent="unsure"))), _body())
        self.assertEqual((out["actions"][0]["time"], out["actions"][0]["record"]), (None, "ask"))

    def test_plan_is_never_extra_and_no_extra_without_capability(self) -> None:
        plan = _action("plan", "extra", _estimate("l", 250))
        out = _turn(SequenceLlm(_payload(plan)), _body())
        self.assertEqual(out["actions"][0]["slot"], "l")
        self.assertIsNone(out["actions"][0]["time"])
        legacy = _turn(SequenceLlm(_payload(_extra_log())), _body(extras=False, other_day=False))
        self.assertNotIn("time", legacy["actions"][0])
        self.assertEqual(legacy["actions"][0]["record"], "ask")

    def test_extra_before_a_plan_counts_in_the_day(self) -> None:
        payload = _payload(_extra_log(kcal=300), _action("plan", "j", _estimate("j", 500)))
        out = _turn(SequenceLlm(payload), _body(plan_budget=True))
        plan = out["actions"][1]
        # 1200 left − 300 of the extra = 900, less the Lanche reservation.
        self.assertLess(plan["plan_budget"]["limit_kcal"], 900)
        alone = _turn(SequenceLlm(_payload(_action("plan", "j", _estimate("j", 500)))), _body(plan_budget=True))
        self.assertGreater(alone["actions"][0]["plan_budget"]["limit_kcal"], plan["plan_budget"]["limit_kcal"])


def _past_log(slot: str = "j", kcal: int = 800, **extra: Any) -> dict[str, Any]:
    return _action("log", slot, _estimate(slot, kcal, text="2 fatias de pizza"), meal_day="other", **extra)


class OtherDayTests(unittest.TestCase):
    def test_named_past_day_is_recorded_there(self) -> None:
        payload = _payload(_past_log(), reply="O Chat registra apenas refeições de hoje. Jantar de ontem: **800 kcal**.")
        out = _turn(SequenceLlm(payload), _body(text="ontem jantei 2 fatias de pizza"))
        action = out["actions"][0]
        self.assertEqual((action["meal_day"], action["day"], action["slot"], action["record"]),
                         ("other", "2026-10-14", "j", "auto"))
        self.assertEqual(out["reply"], "Jantar de ontem: **800 kcal**.")
        self.assertEqual(out["_record"]["day_ref"], "bound")

    def test_past_day_slot_from_slots_by_day(self) -> None:
        weekend = [{"id": "b", "name": "Brunch", "time": "10:30"}, {"id": "j", "name": "Jantar", "time": "20:00"}]
        body = _body(text="sábado passado comi um brunch",
                     profile={"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65,
                              "eat_back": "zero", "slots": SLOTS,
                              "slots_by_day": [{"weekdays": [6, 7], "slots": weekend}]})
        out = _turn(SequenceLlm(_payload(_past_log("b", 600))), body)
        self.assertEqual((out["actions"][0]["slot"], out["actions"][0]["day"], out["actions"][0]["record"]),
                         ("b", "2026-10-10", "auto"))

    def test_occupied_today_slot_does_not_block_a_past_record(self) -> None:
        # Almoço is eaten today; a past lunch is a new meal of that past day.
        out = _turn(SequenceLlm(_payload(_past_log("a", 650))), _body(text="na segunda almocei feijoada"))
        self.assertEqual((out["actions"][0]["day"], out["actions"][0]["record"]), ("2026-10-12", "auto"))

    def test_too_old_writes_nothing(self) -> None:
        out = _turn(SequenceLlm(_payload(_past_log())), _body(text="40 dias atrás jantei pizza"))
        self.assertEqual(out["reply"], "Só registro os últimos 30 dias.")
        self.assertEqual([(a["type"], a["record"]) for a in out["actions"]], [("question", "none")])
        self.assertIsNone(out["estimate"])

    def test_ambiguous_or_unbound_is_never_a_record(self) -> None:
        for text in ("dia 20 jantei pizza", "jantei pizza semana passada"):
            with self.subTest(text):
                out = _turn(SequenceLlm(_payload(_past_log())), _body(text=text))
                self.assertEqual((out["actions"][0]["record"], out["actions"][0]["day"]), ("none", None))

    def test_without_capability_other_day_is_unchanged(self) -> None:
        out = _turn(SequenceLlm(_payload(_past_log())), _body(other_day=False, text="ontem jantei pizza"))
        self.assertEqual(out["actions"][0]["record"], "none")
        self.assertNotIn("day", out["actions"][0])


if __name__ == "__main__":
    unittest.main()
