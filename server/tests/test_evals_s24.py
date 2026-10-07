"""Evaluator checks added by S24 (ADR-043): protein minimum, two options, closing lines, any term."""
from __future__ import annotations

import unittest

from evals.checks import FAIL, PASS, evaluate

PLAN = {
    "reply": "Opção 1: 150 g de base. Total: 300 kcal · 40 g P\n"
             "Opção 2: 200 g de outra base. Total: 500 kcal · 30 g P\n"
             "Ceia: fruta ~200 kcal · P 10",
    "estimate": {"kcal": 300, "p": 40},
}


def status(expect: dict, output: dict = PLAN) -> dict[str, str]:
    return {key: result["status"] for key, result in evaluate(expect, output).items()}


class S24CheckTests(unittest.TestCase):
    def test_estimate_min(self) -> None:
        self.assertEqual(status({"estimate_min": {"p": 40}}), {"estimate_min": PASS})
        self.assertEqual(status({"estimate_min": {"p": 41}}), {"estimate_min": FAIL})
        self.assertEqual(status({"estimate_min": {"p": 1}}, {"reply": "x"}), {"estimate_min": FAIL})

    def test_reply_options_counts_dish_totals_and_not_closing_lines(self) -> None:
        self.assertEqual(status({"reply_options": 2}), {"reply_options": PASS})
        self.assertEqual(status({"reply_options": 3}), {"reply_options": FAIL})
        one = {"reply": "Total: 300 kcal · 40 g P\nCeia: fruta ~200 kcal · P 10\nLanche: pão ~100 kcal · P 5"}
        self.assertEqual(status({"reply_options": 2}, one), {"reply_options": FAIL})

    def test_closing_lines_are_exactly_the_named_slots(self) -> None:
        self.assertEqual(status({"closing_lines": ["ceia"]}), {"closing_lines": PASS})
        self.assertEqual(status({"closing_lines": ["Ceia", "Lanche"]}), {"closing_lines": FAIL})
        self.assertEqual(status({"closing_lines": []}), {"closing_lines": FAIL})
        self.assertEqual(status({"closing_lines": []}, {"reply": "Total: 300 kcal · 40 g P"}), {"closing_lines": PASS})

    def test_reply_any(self) -> None:
        self.assertEqual(status({"reply_any": ["colher", "opção"]}), {"reply_any": PASS})
        self.assertEqual(status({"reply_any": ["colher", "fatia"]}), {"reply_any": FAIL})


if __name__ == "__main__":
    unittest.main()
