"""Model pilot: the same /v1/chat turn against gpt-6-luna and against Grok (xAI).

Inside server/, with the .venv:

    python -m evals.pilot.run --provider luna                 # baseline, OPENAI_API_KEY
    python -m evals.pilot.run --provider grok                 # XAI_API_KEY in the repo-root .env
    python -m evals.pilot.run --provider luna grok --repeat 3 # both, then the comparison report
    python -m evals.pilot.run --report-only                   # rebuild the report from the newest runs

Case sets: "fail" = cases of the main suite where gpt-6-luna failed or flaked in the recorded
runs (FAIL_SET below); "creative" = open questions in evals/pilot/cases (no gold answer, manual
review); "consistency" = one meal text repeated, the kcal spread is the measure.
Same orchestration as the route (main.chat_reply / compact_reply), no HTTP, no moderation call
(local clean verdict: the provider's moderation cap stays with the dev server).
Keys are read from the repo-root .env by name and never printed.
Reports: logs/evals/pilot/<stamp>-<provider>.json and logs/evals/pilot/<stamp>-report.md (outside git).
"""

from __future__ import annotations

import argparse
import json
import os
import statistics
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path
from typing import Any

from dotenv import load_dotenv

from config import load_settings
from evals import run as base
from evals.checks import FAIL, NA, PASS, case_status
from llm import LlmClient

PILOT_CASES_DIR = Path(__file__).resolve().parent / "cases"
REPORT_DIR = base.REPORT_DIR / "pilot"

# Cases of the main suite that failed or flaked for gpt-6-luna in logs/evals (2026-10-02..05).
FAIL_SET = (
    "ceia-completa-suco",
    "memoria-cheia",
    "de-sempre-recent-igual",
    "de-sempre-quantidade-diferente",
    "registro-noturno-ontem",
    "hard-resposta-curta",
    "dia-pedi-estimar-ontem",
    "digest-pergunta-aberta",
    "digest-sem-dia-suposto",
    "registro-forcado",
    "hard-acrescimo-dois-slots",
    "pergunta-resposta-libera",
    "memoria-reforco-iogurte",
    "temp-rotulo-texto",
    "hard-ainda-nao-almocei",
    "registro-pulei-sem-slot",
    "hard-madrugada-pao",
    "correcao-slot-sugerido",
    "hard-foto-pergunta",
    "registro-ontem",
)

# USD per 1M tokens. Luna: evals.run (30/09/2026). Grok: docs.x.ai models page (06/10/2026),
# context under 200k; the cached-input price is not published there, so it is billed as input.
PROVIDERS: dict[str, dict[str, Any]] = {
    "luna": {
        "model": "gpt-6-luna",
        "base_url": None,
        "effort": "none",
        "key_env": "OPENAI_API_KEY",
        "price": {"input": base.PRICE_INPUT, "cached_input": base.PRICE_CACHED_INPUT, "output": base.PRICE_OUTPUT},
    },
    "grok": {
        "model": "grok-4.7",
        "base_url": "https://api.x.ai/v1",
        # Reasoning cannot be disabled on grok-4.7 (low | medium | high | xhigh). low is the closest to none.
        "effort": "low",
        "key_env": "XAI_API_KEY",
        "price": {"input": 2.00, "cached_input": 2.00, "output": 6.00},
    },
}
SETS = ("fail", "creative", "consistency")


def load_pilot_cases(sets: tuple[str, ...] = SETS) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    if "fail" in sets:
        main_cases = {c["id"]: c for c in base.load_cases()}
        for case_id in FAIL_SET:
            case = dict(main_cases[case_id])
            case["set"] = "fail"
            cases.append(case)
    pilot = base.load_cases(PILOT_CASES_DIR)
    for case in pilot:
        case["set"] = "consistency" if "consistency" in case.get("tags", []) else "creative"
        if case["set"] in sets:
            cases.append(case)
    return cases


def run_provider(
    name: str,
    cases: list[dict[str, Any]],
    repeat: int,
    api_key: str,
    workers: int,
    model: str | None = None,
    effort: str | None = None,
) -> dict[str, Any]:
    spec = PROVIDERS[name]
    model = model or spec["model"]
    effort = effort or spec["effort"]
    usage = base.UsageTransport()
    llm = LlmClient(api_key=api_key, transport=usage, effort=effort, model=model, base_url=spec["base_url"])
    clean = base.CleanModerator()
    jobs = [(case, i) for case in cases for i in range(int(case.get("repeat") or repeat))]
    started = time.monotonic()
    try:
        with ThreadPoolExecutor(max_workers=max(1, min(workers, base.MAX_WORKERS))) as pool:
            runs = list(pool.map(lambda job: base.run_once(llm, usage, job[0], api_key, clean), jobs))
    finally:
        llm.close()
    per_case: dict[str, list[dict[str, Any]]] = {case["id"]: [] for case in cases}
    for (case, _), result in zip(jobs, runs):
        per_case[case["id"]].append(result)
    report = base.summarize(effort, cases, per_case, repeat)
    report["provider"] = name
    report["model"] = model
    report["wall_seconds"] = round(time.monotonic() - started)
    report["cost_usd"] = round(_cost(report["tokens"], spec["price"]), 4)
    report["price_per_1m"] = spec["price"]
    for case_report, case in zip(report["cases"], cases):
        reps = per_case[case["id"]]
        case_report["set"] = case["set"]
        case_report["repeat"] = len(reps)
        case_report["status"] = case_status([r["status"] for r in reps], strict=bool(case.get("strict")))
        case_report["note"] = case.get("note")
        case_report["question"] = _question(case)
        case_report["runs"] = [
            {
                "status": r["status"],
                "latency_ms": r["latency_ms"],
                "error": r["error"],
                "failed": {n: c["detail"] for n, c in r["checks"].items() if c["status"] == FAIL},
                "answer": _answer(r["output"]),
            }
            for r in reps
        ]
        del case_report["outputs"], case_report["policy"]
    report["by_set"] = {s: base._rate([c for c in report["cases"] if c["set"] == s]) for s in SETS}
    return report


def _cost(tokens: dict[str, int], price: dict[str, float]) -> float:
    return (
        (tokens["input"] - tokens["cached_input"]) * price["input"]
        + tokens["cached_input"] * price["cached_input"]
        + tokens["output"] * price["output"]
    ) / 1_000_000


def _question(case: dict[str, Any]) -> dict[str, Any]:
    """What the tester typed, with the context a human needs to judge the answer."""
    req = case["request"]
    day = req.get("day") or {}
    return {
        "text": req.get("text", ""),
        "local_time": req.get("local_time"),
        "remaining_kcal": day.get("remaining_kcal"),
        "compact": bool(req.get("compact")),
        "image": case.get("image"),
        "history": [f"{m['role']}: {m['text']}" for m in req.get("messages") or []],
        "facts": [f"{f['id']}: {f['text']}" for f in req.get("facts") or []],
    }


def _answer(output: dict[str, Any]) -> dict[str, Any]:
    estimate = output.get("estimate") if isinstance(output.get("estimate"), dict) else None
    answer: dict[str, Any] = {
        "reply": output.get("reply"),
        "intent": output.get("intent"),
        "record": output.get("record"),
        "question": output.get("question"),
        "digest": output.get("digest"),
    }
    if estimate:
        answer["estimate"] = {
            k: estimate.get(k)
            for k in ("kcal", "p", "c", "g", "confidence", "suggested_slot", "meal_text", "question")
        }
        answer["items"] = estimate.get("items")
    if output.get("memory_updates"):
        answer["memory_updates"] = output["memory_updates"]
    return answer


# ---- report -------------------------------------------------------------------------------


def write_json(report: dict[str, Any], stamp: str) -> Path:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    path = REPORT_DIR / f"{stamp}-{report['provider']}-{report['effort']}.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def latest_reports() -> dict[str, dict[str, Any]]:
    found: dict[str, dict[str, Any]] = {}
    for name in PROVIDERS:
        paths = sorted(REPORT_DIR.glob(f"*-{name}*.json"), key=lambda p: p.stat().st_mtime)
        if paths:
            found[name] = json.loads(paths[-1].read_text(encoding="utf-8"))
            found[name]["_file"] = paths[-1].name
    return found


def _kcals(case_report: dict[str, Any]) -> list[float]:
    return [
        r["answer"]["estimate"]["kcal"]
        for r in case_report["runs"]
        if r["answer"].get("estimate") and isinstance(r["answer"]["estimate"].get("kcal"), (int, float))
    ]


def _spread(values: list[float]) -> str:
    if not values:
        return "sem estimativa"
    lo, hi, med = min(values), max(values), statistics.median(values)
    pct = round(100 * (hi - lo) / med) if med else 0
    return f"{lo:g}–{hi:g} kcal (mediana {med:g}, spread {pct}%)"


def _fmt_answer(run: dict[str, Any]) -> list[str]:
    lines: list[str] = []
    a = run["answer"]
    mark = {PASS: "ok", FAIL: "FALHA", NA: "n/a"}[run["status"]]
    head = f"{mark} · {run['latency_ms']} ms"
    if a.get("intent"):
        head += f" · intent={a['intent']}"
    if a.get("record") is not None:
        head += f" · record={a['record']}"
    lines.append(f"  - **{head}**")
    if run["error"]:
        lines.append(f"    - erro: `{run['error']}`")
    if a.get("digest"):
        lines.append(f"    - digest: {a['digest']}")
    elif a.get("reply") is not None:
        lines.append(f"    - resposta: {a['reply']}")
    est = a.get("estimate")
    if est:
        lines.append(
            f"    - estimativa: {est.get('kcal'):g} kcal · {est.get('p'):g}P {est.get('c'):g}C {est.get('g'):g}G"
            f" · confiança {est.get('confidence')} · slot {est.get('suggested_slot')} · \"{est.get('meal_text')}\""
        )
        if a.get("items"):
            items = ", ".join(f"{i.get('name')} {i.get('g'):g} g {i.get('kcal'):g} kcal" for i in a["items"])
            lines.append(f"    - itens: {items}")
        if est.get("question"):
            lines.append(f"    - pergunta da estimativa: {est['question']}")
    if a.get("question") and not est:
        lines.append(f"    - pergunta: {a['question']}")
    if a.get("memory_updates"):
        ups = "; ".join(f"{u.get('op')} {u.get('kind')} {u.get('key')}: {u.get('text')}" for u in a["memory_updates"])
        lines.append(f"    - memória: {ups}")
    if run["failed"]:
        lines.append("    - checks: " + "; ".join(f"{k} → {v}" for k, v in run["failed"].items()))
    return lines


def render_markdown(reports: dict[str, dict[str, Any]]) -> str:
    names = [n for n in PROVIDERS if n in reports]
    out: list[str] = []
    out.append("# Piloto de modelo — gpt-6-luna × Grok\n")
    out.append(f"Gerado em {datetime.now(base.TZ).isoformat(timespec='minutes')}. "
               "Um turno de `/v1/chat` por repetição, mesmo prompt, mesmo schema, sem moderação. "
               "Avaliação automática = checks do avaliador S10; os casos criativos têm checks mínimos e pedem leitura manual.\n")
    missing = [n for n in PROVIDERS if n not in reports]
    if missing:
        out.append(f"Pendente: {', '.join(missing)} (sem execução registrada em logs/evals/pilot).\n")

    out.append("## Resumo\n")
    out.append("| | " + " | ".join(f"{n} ({reports[n]['model']}, effort {reports[n]['effort']})" for n in names) + " |")
    out.append("|---|" + "---|" * len(names))
    rows = [
        ("Aprovação total", lambda r: _pct(r["pass_rate"])),
        ("Conjunto `fail` (falhas conhecidas do Luna)", lambda r: _pct(r["by_set"]["fail"])),
        ("Conjunto `creative`", lambda r: _pct(r["by_set"]["creative"])),
        ("Conjunto `consistency`", lambda r: _pct(r["by_set"]["consistency"])),
        ("Latência p50 / p95", lambda r: f"{r['latency_ms']['p50']} / {r['latency_ms']['p95']} ms"),
        ("Tokens por chamada (entrada / saída / raciocínio)",
         lambda r: f"{r['tokens_per_call']['input']} / {r['tokens_per_call']['output']} / {r['tokens_per_call']['reasoning']}"),
        ("Custo da rodada", lambda r: f"US$ {r['cost_usd']}"),
        ("Custo por 1000 turnos (projeção)", lambda r: f"US$ {_per_1000(r)}"),
        ("Preço (US$/1M entrada · saída)", lambda r: f"{r['price_per_1m']['input']} · {r['price_per_1m']['output']}"),
        ("Arquivo", lambda r: f"`{r['_file']}`" if "_file" in r else "—"),
    ]
    for label, fn in rows:
        out.append(f"| {label} | " + " | ".join(fn(reports[n]) for n in names) + " |")
    out.append("")

    out.append("## Consistência (mesma mensagem repetida)\n")
    for n in names:
        for c in reports[n]["cases"]:
            if c["set"] == "consistency":
                out.append(f"- **{n}** · {c['id']} · {len(c['runs'])} repetições: {_spread(_kcals(c))} · "
                           f"valores: {', '.join(f'{k:g}' for k in _kcals(c))}")
    out.append("")

    out.append("## Casos lado a lado\n")
    first = reports[names[0]]
    for idx, c in enumerate(first["cases"]):
        out.append(f"### {idx + 1}. `{c['id']}` · conjunto {c['set']}\n")
        q = c["question"]
        if c.get("note"):
            out.append(f"_{c['note']}_\n")
        ctx = f"{q['local_time']}"
        if q["remaining_kcal"] is not None:
            ctx += f" · restam {q['remaining_kcal']} kcal"
        if q["image"]:
            ctx += f" · foto `{q['image']}`"
        if q["facts"]:
            ctx += " · memória: " + "; ".join(q["facts"])
        out.append(f"Contexto: {ctx}  ")
        if q["history"]:
            out.append("Histórico:  ")
            out.extend(f"> {h}  " for h in q["history"])
        if q["compact"]:
            out.append("**Pedido:** resumir a conversa acima (compact)  ")
        else:
            out.append(f"**Pergunta:** {q['text']}\n")
        for n in names:
            cr = next((x for x in reports[n]["cases"] if x["id"] == c["id"]), None)
            if cr is None:
                out.append(f"**{n}** — não executado nesta rodada")
                out.append("")
                continue
            mark = {PASS: "ok", FAIL: "FALHA", NA: "n/a"}[cr["status"]]
            out.append(f"**{n}** — {mark} ({cr['passes']}/{cr['repeat']})\n")
            for r in cr["runs"]:
                out.extend(_fmt_answer(r))
            out.append("")
    return "\n".join(out) + "\n"


def _pct(rate: dict[str, Any]) -> str:
    judged = rate["passed"] + rate["failed"]
    return f"{rate['passed']}/{judged} ({rate['rate']}%)" if judged else "—"


def _per_1000(report: dict[str, Any]) -> float:
    calls = sum(len(c["runs"]) for c in report["cases"]) or 1
    return round(1000 * report["cost_usd"] / calls, 2)


def write_markdown(reports: dict[str, dict[str, Any]], stamp: str) -> Path:
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    path = REPORT_DIR / f"{stamp}-report.md"
    path.write_text(render_markdown(reports), encoding="utf-8")
    return path


def _load_ancestor_env(explicit: str | None) -> None:
    """Keys from --env, or from the first .env found walking up from the repo root (a worktree has none)."""
    if explicit:
        load_dotenv(explicit, override=False)
        return
    for folder in [base.SERVER.parent, *base.SERVER.parent.parents]:
        candidate = folder / ".env"
        if candidate.is_file():
            load_dotenv(candidate, override=False)
            return


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="python -m evals.pilot.run")
    parser.add_argument("--provider", nargs="+", default=["luna"], choices=list(PROVIDERS))
    parser.add_argument("--repeat", type=int, default=3)
    parser.add_argument("--set", nargs="+", default=list(SETS), choices=SETS)
    parser.add_argument("--only", help="comma-separated case ids")
    parser.add_argument("--workers", type=int, default=base.DEFAULT_WORKERS)
    parser.add_argument("--grok-model", default=None, help=f"default {PROVIDERS['grok']['model']}")
    parser.add_argument("--grok-effort", default=None, choices=("low", "medium", "high"))
    parser.add_argument("--luna-effort", default=None, choices=("none", "minimal", "low", "medium", "high"))
    parser.add_argument("--report-only", action="store_true", help="markdown from the newest run of each provider")
    parser.add_argument("--env", default=None, help="an extra .env to read keys from (default: the nearest .env up the tree, so a worktree finds the main checkout)")
    args = parser.parse_args(argv)
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    stamp = datetime.now(base.TZ).strftime("%Y-%m-%d-%H%M%S")
    if args.report_only:
        reports = latest_reports()
        if not reports:
            print("no pilot run in", REPORT_DIR, file=sys.stderr)
            return 2
        print(f"report: {write_markdown(reports, stamp)}")
        return 0
    if args.repeat < 1:
        parser.error("--repeat must be >= 1")
    load_settings()  # loads the repo-root .env into the environment
    _load_ancestor_env(args.env)
    cases = load_pilot_cases(tuple(args.set))
    if args.only:
        only = {i.strip() for i in args.only.split(",") if i.strip()}
        cases = [c for c in cases if c["id"] in only]
    if not cases:
        print("no case selected", file=sys.stderr)
        return 2
    for name in args.provider:
        key = os.environ.get(PROVIDERS[name]["key_env"], "").strip()
        if not key:
            print(f"{PROVIDERS[name]['key_env']} missing in the repo-root .env", file=sys.stderr)
            return 2
        model = args.grok_model if name == "grok" else None
        effort = args.grok_effort if name == "grok" else args.luna_effort
        calls = sum(int(c.get("repeat") or args.repeat) for c in cases)
        print(f"[{name}] {len(cases)} cases, {calls} calls, model {model or PROVIDERS[name]['model']}...", flush=True)
        report = run_provider(name, cases, args.repeat, key, args.workers, model=model, effort=effort)
        base.print_report(report)
        print(f"json: {write_json(report, stamp)}")
    print(f"report: {write_markdown(latest_reports(), stamp)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
