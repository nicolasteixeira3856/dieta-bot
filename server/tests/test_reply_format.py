"""S30 part B (ADR-045): the reply formatting subset. All data is synthetic."""

from __future__ import annotations

import json
import os
import time
import unittest
from typing import Any

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"

import httpx2

import main
from evals.checks import FAIL, PASS, evaluate, markers
from reply_format import plain, shape
from shaping import REFUSAL_OUT_OF_SCOPE
from tests.test_api import INVITE, _client, _explodes, _mock, _responds
from tests.test_chat import _estimate, _v2_payload
from tests.test_record import _model


class SubsetTests(unittest.TestCase):
    def test_the_subset_passes_unchanged(self) -> None:
        reply = (
            "**Omelete de espinafre** com **320 kcal** · P 24 · C 4 · G 23.\n"
            "- Opção leve: 2 ovos\n- Opção cheia: 3 ovos\n\n"
            "| Item | Gramas |\n| --- | --- |\n| ovo | 100 |\n| espinafre | 50 |\n\n"
            "1. Bata os ovos.\n2. Cozinhe por 5 min."
        )
        self.assertEqual(shape(reply), (reply, 0))

    def test_every_other_marker_is_removed_keeping_its_text(self) -> None:
        cases = {
            "## Jantar\nArroz.": "Jantar\nArroz.",
            "Veja [a tabela](http://x.y) aqui.": "Veja a tabela aqui.",
            "![prato](http://x.y/p.png) Arroz.": "prato Arroz.",
            "```\narroz 100 g\n```": "arroz 100 g",
            "Use `azeite` pouco.": "Use azeite pouco.",
            "Um _pouco_ de sal e __muito__ alho.": "Um pouco de sal e muito alho.",
            "Um *pouco* de sal.": "Um pouco de sal.",
            "<b>Arroz</b> e feijão": "Arroz e feijão",
            "> Dica: arroz.": "Dica: arroz.",
            "Arroz 🍚 e feijão": "Arroz  e feijão",
            "Arroz\n---\nFeijão": "Arroz\n\nFeijão",
        }
        for raw, want in cases.items():
            with self.subTest(raw=raw):
                got, removed = shape(raw)
                self.assertEqual(got, want)
                self.assertGreater(removed, 0)

    def test_nested_and_other_bullets_become_top_level_dashes(self) -> None:
        self.assertEqual(shape("* arroz\n  - feijão\n+ ovo\n1) Bata."), ("- arroz\n- feijão\n- ovo\n1. Bata.", 4))

    def test_a_table_beyond_six_rows_becomes_bullets(self) -> None:
        rows = "\n".join(f"| item{i} | {i * 10} |" for i in range(1, 8))
        got, removed = shape(f"| Item | Gramas |\n| --- | --- |\n{rows}")
        self.assertEqual(got.split("\n")[0], "- item1: 10")
        self.assertEqual(len(got.split("\n")), 7)
        self.assertEqual(removed, 1)

    def test_a_second_or_three_column_table_becomes_bullets(self) -> None:
        table = "| Item | Gramas |\n| --- | --- |\n| ovo | 100 |"
        got, _ = shape(f"{table}\n\n{table}")
        self.assertEqual(got, f"{table}\n\n- ovo: 100")
        got, _ = shape("| Item | g | kcal |\n|---|---|---|\n| ovo | 100 | 146 |")
        self.assertEqual(got, "- ovo: 100 · 146")

    def test_bold_bounds(self) -> None:
        self.assertEqual(shape("**Esta frase inteira está em negrito.**"), ("Esta frase inteira está em negrito.", 2))
        self.assertEqual(shape("**Frango grelhado**")[0], "**Frango grelhado**")
        self.assertEqual(shape("Total **520 kcal e P 30"), ("Total 520 kcal e P 30", 1))
        self.assertEqual(shape("- **Opção leve:** frango")[0], "- **Opção leve:** frango")

    def test_blank_runs_collapse(self) -> None:
        self.assertEqual(shape("Arroz.\n\n\n\nFeijão.\n\n")[0], "Arroz.\n\nFeijão.")

    def test_plain_drops_the_markers(self) -> None:
        self.assertEqual(plain("- **Jantar**: frango ~400 kcal · P 30"), "Jantar: frango ~400 kcal · P 30")

    def test_a_long_reply_is_fast(self) -> None:
        reply = ("- **Frango** 120 g, arroz 100 g, feijão 80 g e salada à vontade.\n" * 40)[:2000]
        started = time.perf_counter()
        shape(reply)
        self.assertLess(time.perf_counter() - started, 0.05)


class CheckTests(unittest.TestCase):
    def test_reply_format_and_markers(self) -> None:
        good = {"reply": "**Frango** 120 g · **400 kcal**.\n- arroz\n- feijão"}
        self.assertEqual(evaluate({"reply_format": True}, good)["reply_format"]["status"], PASS)
        loud = {"reply": "**a** **b** **c** **d** 1"}
        self.assertEqual(evaluate({"reply_format": True}, loud)["reply_format"]["status"], FAIL)
        heading = {"reply": "## Jantar\n400 kcal"}
        self.assertEqual(evaluate({"reply_format": True}, heading)["reply_format"]["status"], FAIL)
        self.assertEqual(markers(good["reply"]), {"bold": 2, "bullet": 2, "step": 0, "table": 0})
        want = {"has": ["bold", "bullet"], "not": ["table"], "bold_max": 2}
        self.assertEqual(evaluate({"reply_markers": want}, good)["reply_markers"]["status"], PASS)
        self.assertEqual(evaluate({"reply_markers": {"has": ["step"]}}, good)["reply_markers"]["status"], FAIL)


def _request(**kw: Any) -> dict[str, Any]:
    payload = _v2_payload(**{"facts": [], "clarify_rounds": 0, "auto_record": True, "meal_changes": True,
                             "text": "No cafe comi 2 ovos e 1 pao.", **kw})
    payload["day"]["remaining_kcal"] = 1500
    return payload


NEW = {"operation": "new", "base_slot": None, "addition": None}


class RouteTests(unittest.IsolatedAsyncioTestCase):
    async def _post(self, payload: dict[str, Any], handler) -> dict[str, Any]:
        app = main.create_app(transport=_mock(handler))
        try:
            async with _client(app) as client:
                response = await client.post("/v1/chat", headers={"X-Invite": INVITE}, json=payload)
        finally:
            app.state.llm.close()
        self.assertEqual(response.status_code, 200)
        return response.json()

    async def test_heading_and_link_are_removed_and_the_rewritten_total_keeps_its_bold(self) -> None:
        estimate = _estimate(confidence="high", question=None, suggested_slot="1", kcal=430)
        model = _model(estimate=estimate, meal_change=NEW,
                       reply="## Café\nOvos e pão: **430 kcal**. Veja [a tabela](http://x.y).")
        body = await self._post(_request(), _responds(model, []))
        self.assertTrue(body["reply"].startswith("Café\nOvos e pão: **440 kcal**. Veja a tabela."), body["reply"])

    async def test_refusal_and_fallback_never_pass_through_it(self) -> None:
        refused = _model(estimate=None, reply="## fora", scope="out_of_scope", intent="question")
        body = await self._post(_request(), _responds(refused, []))
        self.assertEqual(body["reply"], REFUSAL_OUT_OF_SCOPE)
        body = await self._post(_request(), _explodes(httpx2.ConnectError("down")))
        self.assertEqual(body["reply"], "nao deu pra estimar")

    async def test_compact_digest_is_untouched(self) -> None:
        digest = "Café: **2 ovos** e pão."
        payload = _request(compact=True, meal_changes=False, messages=[{"role": "user", "text": "comi 2 ovos"}])
        body = await self._post(payload, _responds({"digest": digest}, []))
        self.assertEqual(body["digest"], digest)

    async def test_a_bolded_closing_line_is_rewritten_by_the_server(self) -> None:
        estimate = _estimate(confidence="high", question=None, suggested_slot="1")
        payload = _request()
        payload["profile"]["slots"] = [*payload["profile"]["slots"], {"id": "5", "name": "Jantar", "time": "20:00"}]
        payload["day"]["slots"] = [*payload["day"]["slots"], {"id": "5", "status": "empty"}]
        payload["local_time"] = "2026-09-25T08:30:00-03:00"
        model = _model(estimate=estimate, meal_change=NEW,
                       reply="Ovos e pão: **440 kcal**.\n- **Almoço**: arroz ~1 kcal · P 1\n- **Jantar**: sopa ~2 kcal · P 2")
        body = await self._post(payload, _responds(model, []))
        lines = body["reply"].split("\n")
        self.assertEqual(lines[0], "Ovos e pão: **440 kcal**.")
        self.assertTrue(lines[1].startswith("Almoco: arroz ~"), lines)
        self.assertNotIn("~1 kcal", body["reply"])
        self.assertTrue(lines[2].startswith("Jantar: sopa ~"), lines)


if __name__ == "__main__":
    unittest.main()
