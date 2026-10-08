"""LLM judge over a results file (separate budget from the 1,400 generations). NOT RUN YET.

    python benchmark/judge.py benchmark/out/results-<stamp>.jsonl [--sample 300] [--model gpt-6-astra] [--effort medium]

Writes benchmark/out/judge-<stamp>.jsonl, one verdict per answered row. The judge never sees the arm or effort.
Prices: JUDGE_PRICE_INPUT / JUDGE_PRICE_OUTPUT (USD per 1M) from the environment, default gpt-6-luna's.
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
RUBRIC = (HERE / "judge" / "judge_prompt.md").read_text(encoding="utf-8")
INSTRUCTIONS = re.search(r"```text\n(.*?)\n```", RUBRIC, re.S).group(1)
SCHEMA = json.loads((HERE / "judge" / "judge_schema.json").read_text(encoding="utf-8"))
CASES = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in (HERE / "cases").glob("*.json")}


def judge_input(row: dict[str, Any]) -> str:
    case = CASES[row["case_id"]]
    r = case["request"]
    prof = r["profile"]
    slots = ", ".join(f"{s['name']} {s['time']}" for s in prof["slots"])
    facts = "; ".join(f"{f['id']} {f['text']}" for f in r["facts"][:12]) or "(vazia)"
    hist = "\n".join(f"{m['role']}: {m['text']}" for m in r["messages"][-6:]) or "(nenhum)"
    view = row.get("view") or {}
    actions = json.dumps(view.get("actions"), ensure_ascii=False)
    return (
        f"PROFILE: teto {prof['ceiling_kcal']} kcal, P {prof['p_target']} C {prof['c_target']} G {prof['g_target']}, tom {prof.get('tone', 'seco')}, "
        f"refeições: {slots}; dia: comeu {r['day']['eaten_kcal']} kcal, restam {r['day']['remaining_kcal']} kcal, treino {r['day']['workout_kcal']}\n"
        f"MEMORY: {facts}\nEXPECTATION: {case['expect']['summary']}\nUSER: {r['text']}\nHISTORY:\n{hist}\n"
        f"REPLY: {view.get('reply', '')}\nACTIONS: {actions}"
    )


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("results")
    ap.add_argument("--sample", type=int, default=0, help="judge only N rows (stratified by arm, seeded)")
    ap.add_argument("--model", default=os.environ.get("JUDGE_MODEL", "gpt-6-astra"))
    ap.add_argument("--effort", default=os.environ.get("JUDGE_EFFORT", "medium"))
    ap.add_argument("--workers", type=int, default=2)
    args = ap.parse_args(argv)
    price_in = float(os.environ.get("JUDGE_PRICE_INPUT", "0.10"))
    price_out = float(os.environ.get("JUDGE_PRICE_OUTPUT", "0.50"))

    rows = [json.loads(l) for l in Path(args.results).read_text(encoding="utf-8").splitlines() if l.strip()]
    rows = [r for r in rows if r.get("view")]
    if args.sample:
        random.seed(7)
        by_arm: dict[str, list[dict[str, Any]]] = {}
        for r in rows:
            by_arm.setdefault(r["arm"], []).append(r)
        per = max(1, args.sample // max(1, len(by_arm)))
        rows = [r for arm_rows in by_arm.values() for r in random.sample(arm_rows, min(per, len(arm_rows)))]
    print(f"judging {len(rows)} answers with {args.model} effort={args.effort}")
    from openai import OpenAI

    client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])
    out = Path(args.results).with_name(Path(args.results).name.replace("results-", "judge-"))
    cost = 0.0

    def work(row: dict[str, Any]) -> dict[str, Any]:
        for attempt in range(4):
            try:
                resp = client.responses.create(model=args.model, reasoning={"effort": args.effort}, instructions=INSTRUCTIONS,
                                               input=[{"role": "user", "content": [{"type": "input_text", "text": judge_input(row)}]}],
                                               text={"format": SCHEMA}, timeout=90, store=False)
                u = resp.usage
                c = ((u.input_tokens or 0) * price_in + (u.output_tokens or 0) * price_out) / 1_000_000
                return {"arm": row["arm"], "case_id": row["case_id"], "rep": row["rep"], "family": row["family"], "origin": row["origin"],
                        "verdict": json.loads(resp.output_text), "cost_usd": round(c, 6), "error": None}
            except Exception as exc:
                err = f"{type(exc).__name__}: {str(exc)[:200]}"
                if "RateLimit" in type(exc).__name__ or "429" in str(exc):
                    time.sleep(5 * (attempt + 1))
                    continue
                return {"arm": row["arm"], "case_id": row["case_id"], "rep": row["rep"], "family": row["family"], "origin": row["origin"],
                        "verdict": None, "cost_usd": 0.0, "error": err}
        return {"arm": row["arm"], "case_id": row["case_id"], "rep": row["rep"], "family": row["family"], "origin": row["origin"],
                "verdict": None, "cost_usd": 0.0, "error": "rate limit"}

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
