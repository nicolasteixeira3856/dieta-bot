"""Merge the <id>.json verdicts an agent wrote in benchmark/out/judge-jobs/ into a judge-<stamp>.jsonl.

    python benchmark/judge_import.py <stamp>          # writes benchmark/out/judge-<stamp>-codex.jsonl

Each verdict is validated against judge/judge_schema.json (keys, integer 1–5 scores, boolean flags, verdict
enum); invalid or missing files are listed, never guessed. The map written by judge_export.py restores
(arm, case, rep). Then: python benchmark/report.py benchmark/out/results-<stamp>-rescored.jsonl benchmark/out/judge-<stamp>-codex.jsonl
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")  # Windows consoles default to cp1252

SCHEMA = json.loads((HERE / "judge" / "judge_schema.json").read_text(encoding="utf-8"))["schema"]
SCORES = ("correctness", "numbers", "tone", "format", "usefulness")


def problems(v: object) -> list[str]:
    out: list[str] = []
    if not isinstance(v, dict):
        return ["not an object"]
    for key in SCHEMA["required"]:
        if key not in v:
            out.append(f"missing {key}")
    for key in SCORES:
        val = v.get(key)
        if not isinstance(val, int) or isinstance(val, bool) or not 1 <= val <= 5:
            out.append(f"{key} must be an integer 1-5")
    cr = v.get("creativity")
    if cr is not None and (not isinstance(cr, int) or isinstance(cr, bool) or not 1 <= cr <= 5):
        out.append("creativity must be null or an integer 1-5")
    flags = v.get("flags")
    if not isinstance(flags, dict):
        out.append("flags must be an object")
    else:
        for key in SCHEMA["properties"]["flags"]["required"]:
            if not isinstance(flags.get(key), bool):
                out.append(f"flags.{key} must be a boolean")
    if v.get("verdict") not in ("pass", "partial", "fail"):
        out.append("verdict must be pass|partial|fail")
    if not isinstance(v.get("note"), str):
        out.append("note must be a string")
    return out


def main(argv: list[str]) -> int:
    stamp = argv[0]
    jobs = HERE / "out" / "judge-jobs"
    mapping = json.loads((HERE / "out" / "judge-jobs-map.json").read_text(encoding="utf-8"))
    rows, missing, invalid = [], [], []
    for jid, meta in mapping.items():
        path = jobs / f"{jid}.json"
        if not path.exists():
            missing.append(jid)
            continue
        try:
            verdict = json.loads(path.read_text(encoding="utf-8-sig"))
        except Exception as exc:  # noqa: BLE001
            invalid.append(f"{jid}: {exc}")
            continue
        errs = problems(verdict)
        if errs:
            invalid.append(f"{jid}: " + "; ".join(errs))
            continue
        rows.append({**meta, "verdict": verdict, "cost_usd": 0.0, "usage": None, "error": None, "judge": "codex"})
    out = HERE / "out" / f"judge-{stamp}-codex.jsonl"
    out.write_text("".join(json.dumps(r, ensure_ascii=False) + "\n" for r in rows), encoding="utf-8")
    print(f"{len(rows)} verdicts merged -> {out}; missing {len(missing)}; invalid {len(invalid)}")
    for line in invalid[:30]:
        print("  invalid", line)
    if missing:
        print("  missing ids:", " ".join(missing[:30]), "..." if len(missing) > 30 else "")
    return 0 if not invalid else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
