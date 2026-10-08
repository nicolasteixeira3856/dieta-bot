"""Aggregate results (+ judgments) into benchmark/out/report-<stamp>.md.

    python benchmark/report.py benchmark/out/results-<stamp>.jsonl [benchmark/out/judge-<stamp>.jsonl]

Scores per arm:
  det_strict      share of case×rep passing every deterministic check
  det_supported   same, ignoring checks the baseline cannot satisfy by construction (shows the capability gap)
  case_majority   share of cases passing in ≥ 2 of 3 repetitions
  judge_*         mean 1–5 per rubric dimension, flag rates, pass/partial/fail shares
  composite       0.5 × det_strict + 0.5 × (judge mean of correctness, tone, usefulness − 1) / 4   (0–1)
Plus latency p50/p95, tokens and USD per call, and the same by family, origin and persona tone.
"""
from __future__ import annotations

import json
import statistics
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

DIMS = ("correctness", "numbers", "tone", "format", "usefulness", "creativity")


def pct(x: float) -> str:
    return f"{100 * x:.1f} %"


def p(values: list[float], q: float) -> float:
    if not values:
        return 0.0
    s = sorted(values)
    k = max(0, min(len(s) - 1, round(q * (len(s) - 1))))
    return s[k]


def main(argv: list[str]) -> int:
    results = [json.loads(l) for l in Path(argv[0]).read_text(encoding="utf-8").splitlines() if l.strip()]
    judg = [json.loads(l) for l in Path(argv[1]).read_text(encoding="utf-8").splitlines() if l.strip()] if len(argv) > 1 else []
    jmap = {(j["arm"], j["case_id"], j["rep"]): j["verdict"] for j in judg if j.get("verdict")}
    arms = sorted({r["arm"] for r in results})
    lines = ["# Benchmark — relatório", "", f"Resultados: `{argv[0]}`" + (f" · juiz: `{argv[1]}`" if judg else " · sem juiz"), ""]

    def block(title: str, rows: list[dict[str, Any]], key) -> None:
        groups: dict[str, dict[str, list[dict[str, Any]]]] = defaultdict(lambda: defaultdict(list))
        for r in rows:
            groups[key(r)][r["arm"]].append(r)
        lines.append(f"## {title}")
        lines.append("")
        lines.append("| Grupo | Braço | n | det_strict | det_supported | juiz pass | correctness | tone | usefulness |")
        lines.append("|---|---|---:|---:|---:|---:|---:|---:|---:|")
        for g in sorted(groups):
            for arm in arms:
                rs = groups[g].get(arm, [])
                if not rs:
                    continue
                det = sum(r["pass"] for r in rs) / len(rs)
                sup = sum(r["pass_supported"] for r in rs) / len(rs)
                vs = [jmap[(r["arm"], r["case_id"], r["rep"])] for r in rs if (r["arm"], r["case_id"], r["rep"]) in jmap]
                jp = pct(sum(v["verdict"] == "pass" for v in vs) / len(vs)) if vs else "—"
                def m(d: str) -> str:
                    vals = [v[d] for v in vs if v.get(d) is not None]
                    return f"{statistics.mean(vals):.2f}" if vals else "—"
                lines.append(f"| {g} | {arm} | {len(rs)} | {pct(det)} | {pct(sup)} | {jp} | {m('correctness')} | {m('tone')} | {m('usefulness')} |")
        lines.append("")

    # --- headline
    lines += ["## Placar por braço", "", "| Braço | chamadas | erros | det_strict | det_supported | casos ≥2/3 | juiz pass/partial/fail | correctness | numbers | tone | format | usefulness | creativity | composite | p50 ms | p95 ms | tokens in (cache) / out (+reasoning) | US$/chamada |",
              "|---|---:|---:|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---:|"]
    for arm in arms:
        rs = [r for r in results if r["arm"] == arm]
        ok = [r for r in rs if not r["error"]]
        det = sum(r["pass"] for r in rs) / len(rs)
        sup = sum(r["pass_supported"] for r in rs) / len(rs)
        bycase: dict[str, list[bool]] = defaultdict(list)
        for r in rs:
            bycase[r["case_id"]].append(r["pass"])
        maj = sum(sum(v) * 2 >= len(v) + (len(v) % 2 == 0) for v in bycase.values()) / len(bycase)
        vs = [jmap[(r["arm"], r["case_id"], r["rep"])] for r in rs if (r["arm"], r["case_id"], r["rep"]) in jmap]
        means = {d: (statistics.mean([v[d] for v in vs if v.get(d) is not None]) if any(v.get(d) is not None for v in vs) else None) for d in DIMS} if vs else {}
        verd = "—"
        if vs:
            verd = " / ".join(pct(sum(v["verdict"] == k for v in vs) / len(vs)) for k in ("pass", "partial", "fail"))
        comp = 0.5 * det + (0.5 * ((statistics.mean([means[d] for d in ("correctness", "tone", "usefulness") if means.get(d)]) - 1) / 4) if vs else 0.0)
        lat = [r["latency_ms"] for r in ok]
        us = [r["usage"] for r in ok if r.get("usage")]
        tin = statistics.mean(u["input"] for u in us) if us else 0
        tca = statistics.mean(u["cached"] for u in us) if us else 0
        tou = statistics.mean(u["output"] for u in us) if us else 0
        tre = statistics.mean(u["reasoning"] for u in us) if us else 0
        cost = statistics.mean(r["cost_usd"] for r in ok) if ok else 0
        fm = lambda d: f"{means[d]:.2f}" if means.get(d) else "—"  # noqa: E731
        lines.append(f"| {arm} | {len(rs)} | {len(rs) - len(ok)} | {pct(det)} | {pct(sup)} | {pct(maj)} | {verd} | {fm('correctness')} | {fm('numbers')} | {fm('tone')} | {fm('format')} | {fm('usefulness')} | {fm('creativity')} | {comp:.3f} | {p(lat, 0.5):.0f} | {p(lat, 0.95):.0f} | {tin:.0f} ({tca:.0f}) / {tou:.0f} (+{tre:.0f}) | {cost:.5f} |")
    lines.append("")
    lines.append("`composite` só é comparável entre braços quando o juiz cobriu os mesmos casos. `det_supported` mostra o que a baseline consegue sem as capacidades novas.")
    lines.append("")

    # --- flags
    if jmap:
        lines += ["## Flags do juiz (taxa por braço)", "", "| Braço | corpo/peso | elogio | ! no duro | diz que gravou | sugere pular | totais do dia na prosa | pergunta o já sabido | markdown fora do subset | não pt-BR |", "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|"]
        for arm in arms:
            vs = [v for (a, _, _), v in jmap.items() if a == arm]
            if not vs:
                continue
            f = lambda k: pct(sum(v["flags"][k] for v in vs) / len(vs))  # noqa: E731
            lines.append(f"| {arm} | {f('mentions_body_weight')} | {f('praise')} | {f('exclamation_duro')} | {f('claims_recorded')} | {f('suggests_skip')} | {f('computes_day_totals_in_reply')} | {f('asks_known_detail')} | {f('markdown_outside_subset')} | {f('language_not_ptbr')} |")
        lines.append("")

    block("Por família", results, lambda r: r["family"])
    block("Por origem do caso", results, lambda r: r["origin"])
    block("Por tom", results, lambda r: r["tone"])

    # --- worst cases per arm
    lines += ["## Casos que mais falharam (por braço)", ""]
    for arm in arms:
        bycase: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for r in results:
            if r["arm"] == arm:
                bycase[r["case_id"]].append(r)
        worst = sorted(bycase.items(), key=lambda kv: sum(x["pass"] for x in kv[1]))[:12]
        lines.append(f"### {arm}")
        for cid, rs in worst:
            fails = [c["name"] + (" (" + c["detail"][:60] + ")" if c["detail"] else "") for r in rs for c in r["checks"] if not c["ok"]]
            top = sorted(set(fails), key=fails.count, reverse=True)[:3]
            lines.append(f"- `{cid}`: {sum(x['pass'] for x in rs)}/{len(rs)} · {'; '.join(top) or (rs[0]['error'] or '')}")
        lines.append("")

    out = Path(argv[0]).with_name(Path(argv[0]).name.replace("results-", "report-").replace(".jsonl", ".md"))
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    print(f"\nwritten: {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
