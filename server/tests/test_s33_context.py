"""S33: named-day record resolved in code, routine macros, seven-day totals, input order, Chat effort."""

from __future__ import annotations

import json
import unittest
from datetime import date
from types import SimpleNamespace
from typing import Any

import httpx2
from pydantic import ValidationError

import copy_source
from llm import LlmClient, chat_format
from main import ChatIn, _chat_text
from shaping import shape_chat

SLOTS = [
    {"id": "1", "name": "Café da manhã", "time": "07:30"},
    {"id": "3", "name": "Almoço", "time": "12:00"},
    {"id": "4", "name": "Lanche", "time": "16:00"},
    {"id": "5", "name": "Jantar", "time": "19:30"},
]
# 2026-10-08 is a Thursday (quinta).
TODAY = "2026-10-08"


def _recent(day: str, slot: str, text: str, kcal: float) -> dict[str, Any]:
    name = next(s["name"] for s in SLOTS if s["id"] == slot)
    return {"date": day, "slot_id": slot, "slot_name": name, "text": text, "kcal": kcal, "p": 20, "c": 30, "g": 10}


RECENT = [
    _recent("2026-10-07", "1", "2 fatias de cuscuz com ovo", 410),
    _recent("2026-10-07", "3", "arroz, lentilha e sobrecoxa assada", 690),
    _recent("2026-10-06", "3", "macarrão ao sugo com almôndegas", 720),
    _recent("2026-10-05", "5", "sopa de abóbora", 330),
    _recent("2026-10-05", "5", "torrada com requeijão", 180),
]


def _body(text: str, **extra: Any) -> ChatIn:
    data: dict[str, Any] = {
        "local_time": f"{TODAY}T12:40:00-03:00",
        "profile": {"ceiling_kcal": 2000, "p_target": 140, "c_target": 220, "g_target": 65, "eat_back": "zero",
                    "slots": SLOTS},
        "facts": [],
        "recent": RECENT,
        "day": {"date": TODAY, "remaining_kcal": 2000, "slots": [{"id": s["id"], "status": "empty"} for s in SLOTS]},
        "text": text,
    }
    data.update(extra)
    return ChatIn.model_validate(data)


def _slots() -> list[SimpleNamespace]:
    return [SimpleNamespace(**s) for s in SLOTS]


def _rows() -> list[SimpleNamespace]:
    return [SimpleNamespace(**{**r, "date": date.fromisoformat(r["date"])}) for r in RECENT]


def _resolve(text: str) -> list[copy_source.Block]:
    return copy_source.resolve(text, date.fromisoformat(TODAY), _slots(), _rows())


class CopySourceTests(unittest.TestCase):
    def test_one_row_of_yesterday_by_slot_name(self) -> None:
        blocks = _resolve("almoço foi o mesmo de ontem")
        self.assertEqual(len(blocks), 1)
        self.assertEqual(blocks[0].slot_id, "3")
        self.assertEqual([r.text for r in blocks[0].rows], ["arroz, lentilha e sobrecoxa assada"])
        self.assertFalse(blocks[0].ambiguous)

    def test_weekday_and_eating_verb(self) -> None:
        blocks = _resolve("almocei igual ao de terça")
        self.assertEqual([(b.day.isoformat(), b.slot_id) for b in blocks], [("2026-10-06", "3")])

    def test_two_rows_are_ambiguous(self) -> None:
        blocks = _resolve("jantar igual ao de segunda")
        self.assertTrue(blocks[0].ambiguous)
        self.assertEqual(len(blocks[0].rows), 2)

    def test_no_row_gives_no_block(self) -> None:
        self.assertEqual(_resolve("lanche o mesmo de ontem"), [])

    def test_two_days_in_one_message_give_two_blocks(self) -> None:
        blocks = _resolve("café o mesmo de ontem, almoço igual ao de terça")
        self.assertEqual([(b.day.isoformat(), b.slot_id) for b in blocks], [("2026-10-07", "1"), ("2026-10-06", "3")])

    def test_first_word_of_a_slot_name_and_a_meal_in_another_clause(self) -> None:
        self.assertEqual([b.slot_id for b in _resolve("café, o mesmo de ontem")], ["1"])

    def test_habitual_words_and_a_day_without_equality_never_resolve(self) -> None:
        self.assertEqual(_resolve("almoço de sempre"), [])
        self.assertEqual(_resolve("ontem almocei arroz"), [])

    def test_same_weekday_last_week(self) -> None:
        self.assertEqual(_resolve("almoço igual ao da semana passada"), [])


class ChatTextTests(unittest.TestCase):
    def test_copy_source_block_sits_before_day(self) -> None:
        text = _chat_text(_body("almoço foi o mesmo de ontem"))
        self.assertIn('COPY_SOURCE:\n2026-10-07 quarta 3 Almoço: "arroz, lentilha e sobrecoxa assada" 690kcal', text)
        self.assertLess(text.index("COPY_SOURCE:"), text.index("DAY:"))

    def test_ambiguous_block_lists_every_row(self) -> None:
        text = _chat_text(_body("jantar igual ao de segunda"))
        self.assertIn("COPY_SOURCE: ambiguous\n2026-10-05 segunda 5 Jantar", text)
        self.assertIn("sopa de abóbora", text)
        self.assertIn("torrada com requeijão", text)

    def test_no_block_without_named_day(self) -> None:
        self.assertNotIn("COPY_SOURCE", _chat_text(_body("almocei arroz e feijão")))

    def test_routine_macros_follow_the_text(self) -> None:
        facts = [
            {"id": "D1", "kind": "dynamic", "category": "routine", "key": "cafe", "slot": "1",
             "text": "2 fatias de cuscuz com ovo", "days_seen": 4, "kcal": 410, "p": 18.5, "c": 52, "g": 14},
            {"id": "P1", "kind": "permanent", "category": "preference", "key": "leite", "text": "leite desnatado",
             "kcal": 100},
        ]
        text = _chat_text(_body("oi", facts=facts))
        self.assertIn("D1 routine slot=1 cafe: 2 fatias de cuscuz com ovo · 410 kcal · P 18.5 · C 52 · G 14 (seen 4 days)",
                      text)
        # Partial numbers are not printed.
        self.assertIn("P1 preference leite: leite desnatado (seen 0 days)", text)

    def test_recent_days_lines_and_order(self) -> None:
        days = [
            {"date": "2026-10-07", "recorded": True, "kcal": 2310, "p": 120, "c": 250, "g": 80,
             "ceiling_kcal": 2000, "over_slot": "5", "missing_slots": ["4"]},
            {"date": "2026-10-06", "recorded": False, "ceiling_kcal": 2000},
            {"date": "2026-10-05", "recorded": True, "kcal": 1900, "p": 140, "c": 200, "g": 60, "ceiling_kcal": 2150},
        ]
        text = _chat_text(_body("oi", recent_days=days))
        self.assertIn(
            "RECENT_DAYS:\n2026-10-05 segunda: 1900 kcal de 2150 · P 140 · C 200 · G 60 · registrado · sem registro em: —\n"
            "2026-10-06 terca: sem registro\n"
            "2026-10-07 quarta: 2310 kcal de 2000 · P 120 · C 250 · G 80 · registrado · passou em: Jantar · "
            "sem registro em: Lanche",
            text,
        )
        order = [text.index(k) for k in ("PROFILE:", "MEMORY:", "RECENT:", "RECENT_DAYS:", "DAY:", "CURRENT_USER_MESSAGE:")]
        self.assertEqual(order, sorted(order))

    def test_recent_day_slot_must_be_in_profile(self) -> None:
        with self.assertRaises(ValidationError):
            _body("oi", recent_days=[{"date": "2026-10-07", "recorded": True, "ceiling_kcal": 2000, "over_slot": "9"}])
        with self.assertRaises(ValidationError):
            _body("oi", recent_days=[{"date": "2026-10-07", "recorded": "yes", "ceiling_kcal": 2000}])

    def test_at_most_seven_days(self) -> None:
        day = {"date": "2026-10-07", "recorded": False, "ceiling_kcal": 2000}
        with self.assertRaises(ValidationError):
            _body("oi", recent_days=[day] * 8)


class RoutineProposalTests(unittest.TestCase):
    def test_schema_carries_numbers_on_memory_updates(self) -> None:
        update = chat_format(["1"], ["D1"])["schema"]["properties"]["memory_updates"]["items"]
        for key in ("kcal", "p", "c", "g"):
            self.assertIn(key, update["required"])
            self.assertEqual(update["properties"][key], {"type": ["number", "null"]})

    def test_routine_numbers_pass_through_rounded(self) -> None:
        payload = {
            "reply": "ok", "intent": "question", "estimate": None, "scope": "in_scope",
            "memory_updates": [
                {"op": "add", "id": None, "kind": "dynamic", "category": "routine", "key": "cafe", "slot": "1",
                 "text": "cuscuz com ovo", "kcal": 410.4, "p": 18.6, "c": 52, "g": 14},
                {"op": "add", "id": None, "kind": "dynamic", "category": "routine", "key": "lanche", "slot": "1",
                 "text": "fruta", "kcal": None, "p": None, "c": None, "g": None},
                {"op": "add", "id": None, "kind": "permanent", "category": "preference", "key": "leite",
                 "slot": None, "text": "leite desnatado", "kcal": 90, "p": 8, "c": 12, "g": 0},
            ],
        }
        out = shape_chat(payload, valid_slot_ids=["1"], fact_ids=[])["memory_updates"]
        self.assertEqual({k: out[0][k] for k in ("kcal", "p", "c", "g")}, {"kcal": 410, "p": 19, "c": 52, "g": 14})
        self.assertNotIn("kcal", out[1])
        self.assertNotIn("kcal", out[2])


class EffortTests(unittest.TestCase):
    def test_chat_low_digest_none_and_usage_in_trace(self) -> None:
        seen: list[dict[str, Any]] = []

        def handler(request: httpx2.Request) -> httpx2.Response:
            seen.append(json.loads(request.content))
            text = json.dumps({"digest": "x"})
            return httpx2.Response(200, json={
                "id": "r", "object": "response", "created_at": 0, "model": "gpt-6-luna", "status": "completed",
                "output": [{"type": "message", "id": "m", "role": "assistant", "status": "completed",
                            "content": [{"type": "output_text", "text": text, "annotations": []}]}],
                "usage": {"input_tokens": 900, "input_tokens_details": {"cached_tokens": 600}, "output_tokens": 300,
                          "output_tokens_details": {"reasoning_tokens": 240}, "total_tokens": 1200},
                "parallel_tool_calls": False, "tool_choice": "auto", "tools": [],
            })

        llm = LlmClient(api_key="sk-test", transport=httpx2.MockTransport(handler))
        trace: dict[str, Any] = {}
        try:
            llm.chat_json(user_text="oi", image_b64=None, slot_ids=["1"], trace=trace)
            digest_trace: dict[str, Any] = {}
            llm.digest_json(history_text="user: oi", trace=digest_trace)
        finally:
            llm.close()
        self.assertEqual([s["reasoning"]["effort"] for s in seen], ["low", "none"])
        self.assertEqual(trace["effort"], "low")
        self.assertEqual(trace["usage"], {"input": 900, "cached": 600, "output": 300, "reasoning": 240})
        self.assertEqual(digest_trace["effort"], "none")


if __name__ == "__main__":
    unittest.main()
