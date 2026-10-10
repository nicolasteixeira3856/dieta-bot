"""S40: eval personas. Each persona is a valid /v1/chat request, its week is consistent, cases merge it."""

from __future__ import annotations

import contextlib
import io
import json
import os
import re
import tempfile
import unittest
from datetime import date
from pathlib import Path

os.environ.setdefault("OPENAI_API_KEY", "sk-test-sentinel-not-a-real-key")
os.environ.setdefault("INVITE_CODE", "convite-teste")

from pydantic import ValidationError

from evals import run
from evals.checks import FAIL, case_file_errors, needs_persona
from main import ChatIn

PERSONAS = run.load_personas()
README = run.PERSONAS_DIR / "README.md"
EXPECTED = {"nicolas", "ana-deficit", "pedro-vegetariano", "marta-diabetes", "lucas-noturno", "rafael-bulking",
            "clara-nutri"}


def _turn(persona: dict, **extra) -> dict:
    return {**persona["request"], "text": "o que eu janto?", **extra}


class PersonaFilesTest(unittest.TestCase):
    def test_the_seven_personas_exist(self) -> None:
        self.assertEqual(set(PERSONAS), EXPECTED)

    def test_readme_lists_every_persona(self) -> None:
        listed = set(re.findall(r"^\| `([a-z-]+)` \|", README.read_text(encoding="utf-8"), re.M))
        self.assertEqual(listed, set(PERSONAS))

    def test_each_persona_is_a_valid_chat_request(self) -> None:
        for pid, persona in PERSONAS.items():
            with self.subTest(pid):
                self.assertTrue(persona["summary"].strip())
                body = ChatIn.model_validate(_turn(persona))
                self.assertTrue(body.records and body.meal_changes and body.actions)
                self.assertIsNotNone(body.facts)
                self.assertTrue(body.facts and body.recent and len(body.recent_days) == 7)
                self.assertIn(body.profile.tone, ("seco", "duro"))

    def test_an_out_of_limit_field_is_rejected(self) -> None:
        persona = json.loads(json.dumps(PERSONAS["nicolas"]))
        persona["request"]["facts"][0]["text"] = "x" * 161
        with self.assertRaises(ValidationError):
            ChatIn.model_validate(_turn(persona))

    def test_recent_and_recent_days_agree(self) -> None:
        for pid, persona in PERSONAS.items():
            with self.subTest(pid):
                request = persona["request"]
                today = date.fromisoformat(request["day"]["date"])
                days = request["recent_days"]
                dates = [d["date"] for d in days]
                self.assertEqual(dates, sorted(dates, reverse=True), "recent_days newest first")
                self.assertTrue(all(date.fromisoformat(d) < today for d in dates))
                meals: dict[str, list[dict]] = {}
                for meal in request["recent"]:
                    meals.setdefault(meal["date"], []).append(meal)
                recorded = {d["date"] for d in days if d["recorded"]}
                self.assertEqual(set(meals), recorded, "same dates in recent and recorded recent_days")
                for day in days:
                    for key in ("kcal", "p", "c", "g"):
                        eaten = sum(m[key] for m in meals.get(day["date"], []))
                        self.assertGreaterEqual(day[key] + 0.5, eaten, f"{day['date']} {key}")
                self.assertTrue(any(d["recorded"] and d["kcal"] > d["ceiling_kcal"] for d in days), "a day over")
                self.assertTrue(any(not d["recorded"] for d in days), "a day without record")

    def test_today_adds_up(self) -> None:
        for pid, persona in PERSONAS.items():
            with self.subTest(pid):
                day = persona["request"]["day"]
                eaten = [s for s in day["slots"] if s["status"] == "eaten"]
                self.assertEqual(day["eaten_kcal"], sum(s["kcal"] for s in eaten))
                self.assertTrue(any(s["status"] == "empty" for s in day["slots"]), "a meal still open")


class PersonaCasesTest(unittest.TestCase):
    def test_case_fields_win_and_facts_are_added(self) -> None:
        persona = PERSONAS["pedro-vegetariano"]
        first = persona["request"]["facts"][0]
        case = {"id": "x", "persona": "pedro-vegetariano", "request": {
            "text": "jantei pizza", "local_time": "2026-10-15T21:00:00-03:00",
            "facts": [{**first, "text": "Vegano"}, {"id": "T1", "kind": "temp", "category": "preference",
                                                   "key": "hoje", "text": "Sem fome hoje", "slot": None}],
        }}
        merged = run.with_persona(case, persona)["request"]
        self.assertEqual(merged["text"], "jantei pizza")
        self.assertEqual(merged["local_time"], "2026-10-15T21:00:00-03:00")
        self.assertEqual(merged["profile"], persona["request"]["profile"])
        ids = [f["id"] for f in merged["facts"]]
        self.assertEqual(ids, [f["id"] for f in persona["request"]["facts"]] + ["T1"])
        self.assertEqual(merged["facts"][0]["text"], "Vegano")
        ChatIn.model_validate(merged)

    def test_v6_case_needs_a_known_persona(self) -> None:
        names = set(PERSONAS)
        self.assertTrue(needs_persona({"since": "v6"}))
        self.assertTrue(needs_persona({"since": "meal_changes", "tags": ["v7"]}))
        self.assertFalse(needs_persona({"since": "v5", "tags": ["s39"]}))
        self.assertEqual(len(case_file_errors({"id": "a", "since": "v6"}, names)), 1)
        self.assertEqual(len(case_file_errors({"id": "a", "since": "v6", "persona": "ninguem"}, names)), 1)
        self.assertEqual(case_file_errors({"id": "a", "since": "v6", "persona": "nicolas"}, names), [])
        self.assertEqual(case_file_errors({"id": "a", "since": "v4"}, names), [])

    def test_case_files_load_and_persona_cases_validate(self) -> None:
        cases = run.load_cases()
        persona_cases = [c for c in cases if c.get("persona")]
        self.assertEqual({c["persona"] for c in persona_cases}, set(PERSONAS))
        for case in persona_cases:
            with self.subTest(case["id"]):
                if case.get("route") == run.PROFILE_ROUTE:
                    # S41: an onboarding request is its own shape; the persona lends only its summary.
                    self.assertNotIn("facts", case["request"])
                else:
                    ChatIn.model_validate(run.case_request(case))
                self.assertEqual(case["persona_summary"], PERSONAS[case["persona"]]["summary"])

    def test_invalid_case_file_stops_the_load(self) -> None:
        with tempfile.TemporaryDirectory() as tmp, self.assertRaises(SystemExit):
            (Path(tmp) / "a.json").write_text(json.dumps({"id": "a", "since": "v6", "request": {}}), encoding="utf-8")
            run.load_cases(Path(tmp))

    def test_list_and_dry_run_make_no_call(self) -> None:
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            self.assertEqual(run.main(["--list"]), 0)
            self.assertEqual(run.main(["--dry-run", "--tag", "s40"]), 0)
        text = out.getvalue()
        for pid in PERSONAS:
            self.assertIn(f"{pid}:", text)
        self.assertIn("== s40-nicolas-proxima-refeicao (persona nicolas) ==", text)
        self.assertIn('"ceiling_kcal": 2030', text)

    def test_failing_case_prints_the_persona_summary(self) -> None:
        case = run.load_cases()
        case = next(c for c in case if c.get("persona") == "marta-diabetes")
        rep = {"status": FAIL, "checks": {"refusal": {"status": FAIL, "detail": "x"}}, "error": None,
               "raw_output": None, "output": {}, "latency_ms": 1,
               "usage": {"input": 0, "cached_input": 0, "output": 0, "reasoning": 0}}
        report = run.summarize("low", [case], {case["id"]: [rep]}, 1)
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            run.print_report(report)
        self.assertIn("persona marta-diabetes: Marta tem diabetes", out.getvalue())


if __name__ == "__main__":
    unittest.main()
