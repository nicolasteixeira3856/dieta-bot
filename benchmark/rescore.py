"""Re-score a results file with the current scoring.py and cases (no model call) and drop superseded rows.

    python benchmark/rescore.py benchmark/out/results-<stamp>.jsonl

Writes benchmark/out/results-<stamp>-rescored.jsonl: one row per (arm, case, rep), the last non-error row of
a resumed run winning over its earlier error row; `checks`, `pass` and `pass_supported` recomputed from `view`
against the current `cases/*.json`. Use it after a scoring or expectation fix, so a run is never repeated for
a checker defect. The model answers (`output`, `view`, usage, latency) are never changed.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")  # Windows consoles default to cp1252
from scoring import evaluate, passed, passed_supported  # noqa: E402

CASES = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in (HERE / "cases").glob("*.json")}


def main(argv: list[str]) -> int:
    src = Path(argv[0])
    rows = [json.loads(line) for line in src.read_text(encoding="utf-8").splitlines() if line.strip()]
    best: dict[tuple[str, str, int], dict] = {}
    for r in rows:
        key = (r["arm"], r["case_id"], r["rep"])
        if key not in best or not r.get("error") or best[key].get("error"):
            best[key] = r
    flips = 0
    for r in best.values():
        if not r.get("view"):
            continue
        before = r["pass"]
        r["checks"] = evaluate(CASES[r["case_id"]]["expect"], r["view"], baseline=r["arm"] == "baseline-none")
        r["pass"], r["pass_supported"] = passed(r["checks"]), passed_supported(r["checks"])
        flips += r["pass"] != before
    out = src.with_name(src.stem + "-rescored.jsonl")
    out.write_text("".join(json.dumps(r, ensure_ascii=False) + "\n" for r in best.values()), encoding="utf-8")
    print(f"{len(rows)} rows in, {len(best)} kept, {flips} pass flips -> {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
