"""LLM judge over a results file (separate budget from the 1,400 generations). NOT RUN YET.

    JUDGE_PRICE_INPUT=<usd per 1M> JUDGE_PRICE_OUTPUT=<usd per 1M> \
    python benchmark/judge.py benchmark/out/results-<stamp>.jsonl [--sample 300] [--model gpt-6-astra] [--effort medium]

Writes benchmark/out/judge-<stamp>.jsonl, one verdict per answered row. The judge never sees the arm or the
effort. It receives the whole context the rubric needs: profile with slot ids, memory, RECENT (last 7 days),
RECENT_DAYS, recipes, COPY_SOURCE when the app resolved one, the WINDOWS/BUDGET lines (window and protein
floor of each meal), digests, history, the photo when the case has
one, the reply, the actions, the memory updates and the facts cited. `--sample N` is paired: it picks N/arms
(case, rep) pairs and judges every arm for each, so arms are compared on the same answers.
The judge's prices must be given explicitly: the astra tariff is not recorded in this repository.
"""
from __future__ import annotations

import argparse
import json
import os
import random
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")  # Windows consoles default to cp1252
RUBRIC = (HERE / "judge" / "judge_prompt.md").read_text(encoding="utf-8")
INSTRUCTIONS = re.search(r"```text\n(.*?)\n```", RUBRIC, re.S).group(1)
SCHEMA = json.loads((HERE / "judge" / "judge_schema.json").read_text(encoding="utf-8"))
CASES = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in (HERE / "cases").glob("*.json")}


def judge_input(row: dict[str, Any]) -> str:
    from run import (ChatIn, _day_slots, _expected, _now, _remaining_macros, legacy_request, load_image,  # noqa: F401
                     meal_window, recent_line, resolve_copy_source)

    case = CASES[row["case_id"]]
    r = case["request"]
    prof = r["profile"]
    slots = ", ".join(f"{s['id']}={s['name']} {s['time']}" for s in prof["slots"])
    facts = "\n".join(f"- {f['id']} {f['category']} [{f.get('slot') or '-'}] {f['text']}"
                      + (f" · {f['kcal']} kcal P{f['p']} C{f['c']} G{f['g']}" if f.get("kcal") is not None else "") for f in r["facts"]) or "(vazia)"
    recent = "\n".join(f"- {recent_line(m)}" for m in r["recent"]) or "(nenhum)"
    days = "\n".join(f"- {d['date']} ({d['weekday']}): " + ("sem registro" if not d.get("recorded") else f"{d['kcal']} kcal de {d['ceiling_kcal']} · P {d['p']}" + (f" · passou em {d['over_slot']}" if d.get("over_slot") else "") + (f" · sem registro em {', '.join(d['gaps'])}" if d.get("gaps") else ""))
                     for d in r.get("recent_days", [])) or "(nenhum)"
    recipes = "\n".join(f"- {x['id']} {x['name']} · {x['kcal']} kcal · P {x['p']} · {', '.join(x['key_foods'])}" for x in r.get("recipes", [])) or "(nenhuma)"
    copy_src = "\n".join(resolve_copy_source(case)) or "(nenhum)"
    day = r["day"]
    day_slots = ", ".join(f"{s['id']}:{s['status']}" + (f" {s['kcal']} kcal {s['text']}" if s["status"] in ("eaten", "planned") else "") for s in day["slots"])
    hist = "\n".join(f"{m['role']}: {m['text']}" for m in r["messages"]) or "(nenhum)"
    digests = "\n".join(f"- {d}" for d in r["digests"]) or "(nenhum)"
    body = ChatIn.model_validate(legacy_request(case))
    slots, left = _day_slots(body), _remaining_macros(body)
    expected = _expected(body, slots)
    budget = "\n".join(x for x in (meal_window.windows_line(slots, expected, body.day.remaining_kcal, left["p"], _now(body)),
                                   meal_window.budget_line(slots, expected, body.day.remaining_kcal, left["p"], _now(body))) if x) or "(nenhuma)"
    view = row.get("view") or {}
    return (
        f"PROFILE: teto {prof['ceiling_kcal']} kcal, metas P {prof['p_target']} C {prof['c_target']} G {prof['g_target']}, eat_back {prof['eat_back']}, tom {prof.get('tone', 'seco')}; "
        f"refeições (id=nome hora): {slots}\n"
        f"DAY: {day['date']} às {r['local_time'][11:16]}; comeu {day['eaten_kcal']} kcal (P {day['eaten_p']}), restam {day['remaining_kcal']} kcal e {left['p']} g de proteína; treino {day['workout_kcal']}; slots: {day_slots}\n"
        f"WINDOWS/BUDGET (server, ADR-043):\n{budget}\n"
        f"MEMORY:\n{facts}\nRECENT (7 dias):\n{recent}\nRECENT_DAYS:\n{days}\nRECIPES:\n{recipes}\n"
        + (f"RECIPE_FULL:\n{r['recipe_full']}\n" if r.get("recipe_full") else "")
        + f"COPY_SOURCE (resolvido pelo app):\n{copy_src}\nDIGESTS:\n{digests}\nHISTORY:\n{hist}\n"
        + ("DISCOVERY: first_open\n" if r.get("discovery") else "")
        + f"EXPECTATION: {case['expect']['summary']}\nUSER: {r['text']}\n"
        + ("USER PHOTO: attached\n" if case.get("image") else "")
        + f"REPLY: {view.get('reply', '')}\nACTIONS: {json.dumps(view.get('actions'), ensure_ascii=False)}\n"
        f"MEMORY_UPDATES: {json.dumps(view.get('memory_updates'), ensure_ascii=False)}\nMEMORY_USED: {json.dumps(view.get('memory_used'), ensure_ascii=False)}"
    )


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("results")
    ap.add_argument("--sample", type=int, default=0, help="judge only ~N rows, paired across arms (seeded)")
    ap.add_argument("--model", default=os.environ.get("JUDGE_MODEL", "gpt-6-astra"))
    ap.add_argument("--effort", default=os.environ.get("JUDGE_EFFORT", "medium"))
    ap.add_argument("--workers", type=int, default=2)
    args = ap.parse_args(argv)
    try:
        price_in = float(os.environ["JUDGE_PRICE_INPUT"])
        price_out = float(os.environ["JUDGE_PRICE_OUTPUT"])
    except KeyError:
        print("set JUDGE_PRICE_INPUT and JUDGE_PRICE_OUTPUT (USD per 1M tokens) for the judge model")
        return 2

    rows = [json.loads(l) for l in Path(args.results).read_text(encoding="utf-8").splitlines() if l.strip()]
    rows = [r for r in rows if r.get("view")]
    arms = sorted({r["arm"] for r in rows})
    if args.sample:
        random.seed(7)
        pairs = sorted({(r["case_id"], r["rep"]) for r in rows})
        keep = set(random.sample(pairs, min(len(pairs), max(1, args.sample // max(1, len(arms))))))
        rows = [r for r in rows if (r["case_id"], r["rep"]) in keep]
    print(f"judging {len(rows)} answers ({len(arms)} arms) with {args.model} effort={args.effort}")
    from openai import OpenAI
    from run import load_image

    client = OpenAI(api_key=os.environ["OPENAI_API_KEY"], max_retries=0)
    out = Path(args.results).with_name(Path(args.results).name.replace("results-", "judge-"))
    cost = 0.0
    images: dict[str, str | None] = {}

    def work(row: dict[str, Any]) -> dict[str, Any]:
        case = CASES[row["case_id"]]
        content: list[dict[str, Any]] = [{"type": "input_text", "text": judge_input(row)}]
        if case.get("image"):
            if row["case_id"] not in images:
                img = load_image(case)
                images[row["case_id"]] = None if img == "__missing__" else img
            if images[row["case_id"]]:
                content.append({"type": "input_image", "image_url": "data:image/jpeg;base64," + images[row["case_id"]]})
        meta = {"arm": row["arm"], "case_id": row["case_id"], "rep": row["rep"], "family": row["family"], "origin": row["origin"], "tone": row.get("tone")}
        for attempt in range(3):
            try:
                resp = client.responses.create(model=args.model, reasoning={"effort": args.effort}, instructions=INSTRUCTIONS,
                                               input=[{"role": "user", "content": content}], text={"format": SCHEMA}, timeout=120, store=False)
                u = resp.usage
                c = ((u.input_tokens or 0) * price_in + (u.output_tokens or 0) * price_out) / 1_000_000
                return {**meta, "verdict": json.loads(resp.output_text), "cost_usd": round(c, 6), "error": None}
            except Exception as exc:
                err = f"{type(exc).__name__}: {str(exc)[:200]}"
                if ("RateLimit" in type(exc).__name__ or "429" in str(exc)) and attempt < 2:
                    time.sleep(5 * (attempt + 1))
                    continue
                return {**meta, "verdict": None, "cost_usd": 0.0, "error": err}
        return {**meta, "verdict": None, "cost_usd": 0.0, "error": "rate limit"}

    with out.open("w", encoding="utf-8") as fh, ThreadPoolExecutor(max_workers=args.workers) as pool:
        for n, res in enumerate(pool.map(work, rows), start=1):
            fh.write(json.dumps(res, ensure_ascii=False) + "\n")
            cost += res["cost_usd"]
            if n % 25 == 0 or n == len(rows):
                print(f"{n}/{len(rows)} · US$ {cost:.4f}", flush=True)
    print(f"judgments: {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
