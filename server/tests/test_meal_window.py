"""Meal window (ADR-043, S24): pure arithmetic, the DAY/BUDGET/WINDOWS lines and the route."""
import json
import os
import tempfile
import unittest
from datetime import date
from types import SimpleNamespace

import httpx2

import main
import meal_window as mw
from meal_window import Slot
from tests.test_api import INVITE, _client, _mock
from tests.test_chat import _base_chat_payload
from tests.test_plan_budget import model_input, plan, sequence

SLOTS = [
    Slot("1", "Café", "eaten", 400, 20),
    Slot("2", "Almoço", "eaten", 600, 40),
    Slot("3", "Lanche", "skipped"),
    Slot("4", "Jantar", "empty"),
    Slot("5", "Ceia", "empty"),
]


def meal(day, slot, kcal):
    return SimpleNamespace(date=date(2026, 10, day), slot_id=slot, kcal=kcal)


class ExpectedTests(unittest.TestCase):
    def test_no_recent_is_an_equal_share_of_the_ceiling(self):
        self.assertEqual(mw.expected_kcal("5", [], 2030, 6), 338)

    def test_one_day_of_recent_is_not_history(self):
        self.assertEqual(mw.expected_kcal("5", [meal(1, "5", 150)], 2000, 5), 400)

    def test_two_days_average_per_day(self):
        recent = [meal(1, "5", 100), meal(1, "5", 100), meal(2, "5", 301), meal(2, "4", 900)]
        self.assertEqual(mw.expected_kcal("5", recent, 2000, 5), 251)  # (200 + 301) / 2 = 250.5, ties up


class WindowTests(unittest.TestCase):
    expected = {s.id: 300 for s in SLOTS}

    def test_target_and_eaten_or_skipped_slots_are_not_reserved(self):
        w = mw.window(SLOTS, self.expected, 824, "4")
        self.assertEqual((w.window_kcal, w.reserved_upcoming, w.reserved), (524, 300, [{"label": "Ceia", "kcal": 300}]))

    def test_stated_reservation_replaces_its_slot_only_when_larger_and_an_unknown_label_is_added(self):
        stated = [{"label": "ceia", "kcal": 120}, {"label": "fatia de bolo", "kcal": 250}]
        w = mw.window(SLOTS, self.expected, 824, "4", stated)
        # 120 is below the computed 300 (the model's own closing suggestion, not a statement): the computed stays.
        self.assertEqual(w.reserved, [{"label": "Ceia", "kcal": 300}, {"label": "fatia de bolo", "kcal": 250}])
        self.assertEqual((w.window_kcal, w.reserved_upcoming), (274, 550))
        larger = mw.window(SLOTS, self.expected, 824, "4", [{"label": "ceia", "kcal": 450}])
        self.assertEqual(larger.reserved, [{"label": "Ceia", "kcal": 450}])

    def test_a_slot_reservation_counts_only_when_the_user_typed_its_number(self):
        guess = mw.window(SLOTS, self.expected, 824, "4", [{"label": "ceia", "kcal": 450}], typed={200})
        self.assertEqual(guess.reserved, [{"label": "Ceia", "kcal": 300}])
        said = mw.window(SLOTS, self.expected, 824, "4", [{"label": "ceia", "kcal": 450}], typed={450})
        self.assertEqual(said.reserved, [{"label": "Ceia", "kcal": 450}])
        extra = mw.window(SLOTS, self.expected, 824, "4", [{"label": "bolo", "kcal": 250}], typed=set())
        self.assertEqual(extra.reserved[-1], {"label": "bolo", "kcal": 250})  # an extra needs no typed number
        self.assertEqual(mw.typed_numbers(["guarda 1.200 pra ceia e 250kcal de bolo", None]), {1200, 250})

    def test_nearest_upcoming_slot(self):
        timed = [Slot("1", "Café", "eaten", time="07:30"), Slot("2", "Jantar", "empty", time="18:00"), Slot("3", "Ceia", "empty", time="20:30")]
        self.assertEqual(mw.nearest_upcoming(timed, "18:20"), "2")  # dinner of 18:00 is in progress
        self.assertEqual(mw.nearest_upcoming(timed, "19:45"), "3")  # 105 minutes past dinner: the supper
        self.assertEqual(mw.nearest_upcoming(timed, "17:00"), "2")
        self.assertEqual(mw.nearest_upcoming(timed, "23:00"), None)
        self.assertEqual(mw.nearest_upcoming(timed, None), "2")

    def test_a_meal_name_is_never_an_extra_reservation(self):
        stated = [{"label": "Jantar", "kcal": 400}, {"label": "Almoço", "kcal": 600}, {"label": "bolo", "kcal": 250}]
        w = mw.window(SLOTS, self.expected, 824, "4", stated, typed={400, 600, 250})
        self.assertEqual(w.reserved, [{"label": "Ceia", "kcal": 300}, {"label": "bolo", "kcal": 250}])
        self.assertEqual(mw._slot_key("Para a ceia"), mw._key("Ceia"))
        self.assertEqual(mw._slot_key("na Ceia"), mw._key("Ceia"))

    def test_a_stated_reservation_that_echoes_a_served_number_is_ignored(self):
        # 412 is the capped share the server itself wrote for the supper when 824 are left and both expect 300... no:
        # with 824 left the cap does not bind (share 412 > 300), so the served figures are 300 (expected) and 412.
        self.assertIn(412, mw.served_numbers(SLOTS, self.expected, 824, None))
        w = mw.window(SLOTS, self.expected, 824, "4", [{"label": "ceia", "kcal": 412}])
        self.assertEqual(w.reserved, [{"label": "Ceia", "kcal": 300}])

    def test_accents_and_case_name_the_same_slot(self):
        w = mw.window(SLOTS, self.expected, 824, "5", [{"label": "JANTAR", "kcal": 500}])
        self.assertEqual(w.reserved, [{"label": "Jantar", "kcal": 500}])

    def test_a_tight_remainder_is_shared_so_the_window_never_reaches_zero(self):
        # 200 left, target and supper both expect 300: the supper reservation is capped at its share (100).
        w = mw.window(SLOTS, self.expected, 200, "4")
        self.assertEqual((w.window_kcal, w.reserved), (100, [{"label": "Ceia", "kcal": 100}]))
        self.assertEqual(mw.window(SLOTS, self.expected, 0, "4").window_kcal, 0)

    def test_a_stated_reservation_is_not_capped(self):
        w = mw.window(SLOTS, self.expected, 200, "4", [{"label": "ceia", "kcal": 250}])
        self.assertEqual((w.window_kcal, w.reserved_upcoming), (0, 250))

    def test_an_empty_slot_whose_time_has_passed_reserves_nothing(self):
        timed = [
            Slot("1", "Café", "eaten", 400, 20, "07:30"), Slot("2", "Lanche", "empty", time="10:00"),
            Slot("3", "Jantar", "empty", time="18:00"), Slot("4", "Ceia", "empty", time="20:30"),
        ]
        expected = {s.id: 300 for s in timed}
        at_night = mw.window(timed, expected, 824, "3", now="18:20")
        self.assertEqual(at_night.reserved, [{"label": "Ceia", "kcal": 300}])
        in_the_morning = mw.window(timed, expected, 824, "3", now="09:00")
        self.assertEqual([r["label"] for r in in_the_morning.reserved], ["Lanche", "Ceia"])
        self.assertEqual([s.id for s in mw.upcoming(timed, "20:30")], ["4"])
        self.assertEqual([s.id for s in mw.upcoming(timed, None)], ["2", "3", "4"])
        self.assertEqual([s.id for s in mw.upcoming([Slot("x", "Sem hora", "empty")], "23:00")], ["x"])

    def test_without_a_target_only_stated_reservations_count(self):
        w = mw.window(SLOTS, self.expected, 824, None, [{"label": "bolo", "kcal": 100}])
        self.assertEqual((w.window_kcal, w.reserved), (724, [{"label": "bolo", "kcal": 100}]))


class ClosingFallbackTests(unittest.TestCase):
    def test_missing_closing_lines_are_added_with_the_usual_food_or_a_definir(self):
        slots = [Slot("1", "Café", "eaten", 400, 20, "07:30"), Slot("2", "Almoço", "empty", time="12:30"),
                 Slot("3", "Jantar", "empty", time="19:30"), Slot("4", "Ceia", "empty", time="22:00")]
        expected = {s.id: 400 for s in slots}
        out = mw.close_reply("Assumi 30 g de alface.", slots, expected, 1200, 90, "2", 400, 30, now="12:40",
                             usual={"3": "frango com arroz"}, complete=True)
        self.assertEqual(out.split(chr(10)), ["Assumi 30 g de alface.", "Jantar: frango com arroz ~400 kcal · P 30", "Ceia: a definir ~400 kcal · P 30"])


class ClosingWithoutFoodTests(unittest.TestCase):
    def test_a_closing_without_a_food_is_rebuilt_once(self):
        slots = [Slot("1", "Café", "eaten", 400, 20, "07:30"), Slot("2", "Almoço", "empty", time="12:30"),
                 Slot("3", "Jantar", "empty", time="19:30"), Slot("4", "Ceia", "empty", time="22:00")]
        expected = {s.id: 400 for s in slots}
        reply = "Almoço ~477 kcal. Jantar: ~435 kcal · P 33 Ceia: ~435 kcal · P 33"
        out = mw.close_reply(reply, slots, expected, 1200, 90, "2", 400, 30, now="12:40", usual={"3": "frango"}, complete=True)
        self.assertEqual(out.split(chr(10)), ["Almoço ~477 kcal.", "Jantar: frango ~400 kcal · P 30", "Ceia: a definir ~400 kcal · P 30"])


class LinesTests(unittest.TestCase):
    expected = {"1": 400, "2": 600, "3": 300, "4": 600, "5": 200}

    def test_windows_scale_to_the_remainder_with_protein_in_the_same_shares(self):
        self.assertEqual(mw.windows(mw.empty(SLOTS), self.expected, 824, 80), [("Jantar", 618, 60), ("Ceia", 206, 20)])
        self.assertEqual(
            mw.windows_line(SLOTS, self.expected, 824, 80),
            "WINDOWS: Jantar ~618 kcal P 60 · Ceia ~206 kcal P 20 | faltam 80 g P",
        )

    def test_shares_sum_exactly_and_nothing_is_negative(self):
        shares = mw._shares(1000, [1, 1, 1])
        self.assertEqual((sum(shares), shares), (1000, [334, 333, 333]))
        self.assertEqual(mw.windows(mw.empty(SLOTS), self.expected, -50, 30), [("Jantar", 0, 0), ("Ceia", 0, 0)])

    def test_windows_need_two_empty_slots(self):
        one = [s if s.id != "5" else Slot("5", "Ceia", "eaten", 100, 5) for s in SLOTS]
        self.assertIsNone(mw.windows_line(one, self.expected, 824, 80))

    def test_budget_line_lists_each_empty_meal_and_any_other(self):
        self.assertEqual(
            mw.budget_line(SLOTS, self.expected, 824, 80),
            "BUDGET: protein_floor=24; window_kcal by meal: Jantar=624 (reserved_upcoming=200: Ceia); "
            "Ceia=224 (reserved_upcoming=600: Jantar); any other meal=24 (reserved_upcoming=800: Jantar, Ceia)",
        )
        self.assertIsNone(mw.budget_line([Slot("1", "Café", "eaten")], self.expected, 824, 80))
        self.assertTrue(mw.budget_line(SLOTS, self.expected, 824, -5).startswith("BUDGET: protein_floor=0;"))


class CloseReplyTests(unittest.TestCase):
    expected = {"1": 400, "2": 600, "3": 300, "4": 600, "5": 200}
    slots = [Slot("1", "Café", "eaten", 400, 20), Slot("2", "Almoço", "empty"), Slot("4", "Jantar", "empty"),
             Slot("5", "Ceia", "empty")]

    def test_a_log_rewrites_the_numbers_of_the_other_empty_slots(self):
        reply = "Almoço: 500 kcal.\nJantar: omelete ~999 kcal · P 1\nCeia: iogurte ~1 kcal · P 99"
        out = mw.close_reply(reply, self.slots, self.expected, 1600, 120, "2", 500, 30)
        # 1100 kcal and 90 g P left after the lunch, shared 600:200 between dinner and supper.
        self.assertEqual(out, "Almoço: 500 kcal.\nJantar: omelete ~825 kcal · P 68\nCeia: iogurte ~275 kcal · P 22")

    def test_closing_lines_for_the_answered_or_an_eaten_meal_are_dropped(self):
        reply = "Prato.\nAlmoço: arroz ~10 kcal · P 1\nCafé: pão ~10 kcal · P 1\nJantar: sopa ~10 kcal · P 1"
        out = mw.close_reply(reply, self.slots, self.expected, 1600, 120, "2", 500, 30)
        self.assertEqual(out.split("\n")[0], "Prato.")
        self.assertEqual([line.split(":")[0] for line in out.split("\n")[1:]], ["Jantar"])

    def test_an_addition_to_an_eaten_meal_counts_only_the_change(self):
        reply = "Total.\nAlmoço: frango ~1 kcal · P 1"
        out = mw.close_reply(reply, self.slots, self.expected, 1600, 120, "1", 500, 30)
        # The breakfast had 400 kcal / 20 P: the change is +100 kcal / +10 P, 1500 / 110 left for three slots.
        self.assertEqual(out, "Total.\nAlmoço: frango ~643 kcal · P 47")

    def test_trailing_punctuation_and_grams_unit_are_accepted_and_normalised(self):
        reply = "Prato.\nJantar: sopa ~10 kcal · P 1 g.\nCeia: chá ~10 kcal · P 1;"
        out = mw.close_reply(reply, self.slots, self.expected, 1600, 120, "2", 500, 30)
        self.assertEqual(out, "Prato.\nJantar: sopa ~825 kcal · P 68\nCeia: chá ~275 kcal · P 22")

    def test_other_lines_stay(self):
        reply = "Linha sem formato: nada ~ kcal\nOutra: coisa ~10 kcal · P 1"
        self.assertEqual(mw.close_reply(reply, self.slots, self.expected, 1600, 120, "2", 500, 30), reply)


def request(**overrides):
    req = _base_chat_payload(
        text="nao sei o que jantar", clarify_rounds=0, auto_record=True, facts=[], meal_changes=True,
        local_time="2026-10-06T16:50:00-03:00",
    )
    req["profile"].update(ceiling_kcal=2000, p_target=165, slots=[
        {"id": "1", "name": "Café", "time": "07:30"}, {"id": "2", "name": "Almoço", "time": "12:00"},
        {"id": "3", "name": "Jantar", "time": "19:00"}, {"id": "4", "name": "Ceia", "time": "21:00"},
    ])
    req["day"].update(date="2026-10-06", eaten_kcal=1000, eaten_p=85, remaining_kcal=1000, slots=[
        {"id": "1", "status": "eaten", "text": "pão", "kcal": 400, "p": 25, "c": 50, "g": 10},
        {"id": "2", "status": "eaten", "text": "arroz", "kcal": 600, "p": 60, "c": 70, "g": 15},
        {"id": "3", "status": "empty"}, {"id": "4", "status": "empty"},
    ])
    req.update(overrides)
    return req


def dinner_plan(kcal=500, **overrides):
    out = plan(kcal, **overrides)
    out["estimate"]["suggested_slot"] = "3"
    out["meal_change"] = None
    return out


class RouteTests(unittest.IsolatedAsyncioTestCase):
    async def post(self, req, payloads):
        captured: list[httpx2.Request] = []
        saved = os.environ.get("CONVERSATION_LOG_PATH")
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "conversations.jsonl")
            os.environ["CONVERSATION_LOG_PATH"] = path
            app = main.create_app(transport=_mock(sequence(payloads, captured)))
            try:
                async with _client(app) as client:
                    response = await client.post("/v1/chat", headers={"X-Invite": INVITE}, json=req)
            finally:
                app.state.llm.close()
                app.state.moderator.close()
                app.state.conversation_log.close()
                if saved is None:
                    os.environ.pop("CONVERSATION_LOG_PATH", None)
                else:
                    os.environ["CONVERSATION_LOG_PATH"] = saved
            with open(path, encoding="utf-8") as fh:
                records = [json.loads(line) for line in fh if line.strip()]
        self.assertEqual(response.status_code, 200, response.text)
        return response.json(), captured, records

    async def test_day_budget_and_windows_reach_the_model(self):
        _, captured, _ = await self.post(request(), [dinner_plan()])
        text = model_input(captured[0])
        self.assertIn("remaining_kcal=1000, remaining_p=80, remaining_c=", text)
        self.assertIn("WINDOWS: Jantar ~500 kcal P 40 · Ceia ~500 kcal P 40 | faltam 80 g P", text)
        self.assertIn("BUDGET: protein_floor=24; window_kcal by meal: Jantar=500 (reserved_upcoming=500: Ceia); "
                      "Ceia=500", text)
        self.assertLess(text.index("BUDGET:"), text.index("CURRENT_USER_MESSAGE:"))

    async def test_legacy_shape_without_remaining_has_no_window_lines(self):
        req = request()
        del req["day"]["remaining_kcal"]
        _, captured, _ = await self.post(req, [dinner_plan()])
        text = model_input(captured[0])
        for marker in ("remaining_p=", "WINDOWS:", "BUDGET:"):
            self.assertNotIn(marker, text)

    async def test_plan_budget_limit_is_the_window_with_the_computed_reservation(self):
        out, captured, records = await self.post(request(plan_budget=True), [dinner_plan(450)])
        self.assertEqual(out["plan_budget"], {"limit_kcal": 500, "over_kcal": 0,
                                              "reserved": [{"label": "Ceia", "kcal": 500}], "choice": None})
        self.assertEqual(len(captured), 1)
        self.assertEqual(records[-1]["meal_window"],
                         {"window_kcal": 500, "reserved_upcoming": 500, "reserved": [{"label": "Ceia", "kcal": 500}]})

    async def test_meal_window_is_logged_without_the_capability_and_null_for_a_log(self):
        _, _, records = await self.post(request(), [dinner_plan()])
        self.assertEqual(records[-1]["meal_window"]["window_kcal"], 500)
        log = dinner_plan(intent="log", record_intent="clear")
        log["meal_change"] = {"operation": "new", "base_slot": None, "addition": None}
        _, _, records = await self.post(request(), [log])
        self.assertIsNone(records[-1]["meal_window"])

    async def test_closing_line_gets_the_server_numbers(self):
        answer = dinner_plan(600)
        answer["reply"] = "Prato: 100 g de base.\nCeia: iogurte ~123 kcal · P 9"
        out, _, _ = await self.post(request(), [answer])
        # 1000 - 600 = 400 kcal and 80 - 30 = 50 g P left for the supper.
        self.assertEqual(out["reply"], "Prato: 100 g de base.\nCeia: iogurte ~400 kcal · P 50")

    async def test_another_day_keeps_the_reply(self):
        answer = dinner_plan(600, meal_day="other")
        answer["reply"] = "Prato.\nCeia: iogurte ~123 kcal · P 9"
        out, _, _ = await self.post(request(), [answer])
        self.assertEqual(out["reply"], "Prato.\nCeia: iogurte ~123 kcal · P 9")
