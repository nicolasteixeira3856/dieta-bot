"""Export the judge's work as files, so an agent on a subscription (Codex) can judge instead of the API.

    python benchmark/judge_export.py benchmark/out/results-<stamp>-rescored.jsonl

Writes benchmark/out/judge-jobs/<id>.md, one per answered row: the full rubric (judge/judge_prompt.md) and
exactly the input judge.py would send (judge_input), plus <id>.jpg beside it when the case has a photo
(the same ≤ 2048 px JPEG the model saw). <id> is opaque; the map id → (arm, case, rep) is written OUTSIDE
the folder (benchmark/out/judge-jobs-map.json), so the judge stays blind to the arm and the effort.
The agent answers with <id>.json in the schema of judge/judge_schema.json; judge_import.py merges them.
"""
from __future__ import annotations

import base64
import hashlib
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")  # Windows consoles default to cp1252

from judge import CASES, INSTRUCTIONS, judge_input  # noqa: E402
from run import load_image  # noqa: E402

SCHEMA = json.loads((HERE / "judge" / "judge_schema.json").read_text(encoding="utf-8"))["schema"]


def job_id(row: dict) -> str:
    return hashlib.sha1(f"{row['arm']}|{row['case_id']}|{row['rep']}".encode()).hexdigest()[:10]


def main(argv: list[str]) -> int:
    src = Path(argv[0])
    rows = [json.loads(line) for line in src.read_text(encoding="utf-8").splitlines() if line.strip()]
    rows = [r for r in rows if r.get("view")]
    out_dir = HERE / "out" / "judge-jobs"
    out_dir.mkdir(parents=True, exist_ok=True)
    mapping: dict[str, dict] = {}
    images: dict[str, str | None] = {}
    photos = 0
    for row in rows:
        jid = job_id(row)
        mapping[jid] = {k: row[k] for k in ("arm", "case_id", "rep", "family", "origin", "tone")}
        case = CASES[row["case_id"]]
        photo_line = ""
        if case.get("image"):
            if row["case_id"] not in images:
                img = load_image(case)
                images[row["case_id"]] = None if img == "__missing__" else img
            if images[row["case_id"]]:
                (out_dir / f"{jid}.jpg").write_bytes(base64.b64decode(images[row["case_id"]]))
                photo_line = f"\n\nA foto do usuário está em `{jid}.jpg`, nesta pasta: abra e avalie com ela.\n"
                photos += 1
        text = judge_input(row)
        body = (
            f"# Julgamento {jid}\n\n"
            f"Escreva o veredito em `{jid}.json` nesta pasta, um objeto JSON no formato de `_schema.json` "
            f"(todas as chaves, inteiros 1–5, booleanos nas flags, `creativity` null fora de plano, `note` em pt-BR).\n\n"
            f"## Instruções do juiz\n\n```text\n{INSTRUCTIONS}\n```\n\n"
            f"## Entrada\n\n```text\n{text}\n```{photo_line}"
        )
        (out_dir / f"{jid}.md").write_text(body, encoding="utf-8")
    (out_dir / "_schema.json").write_text(json.dumps(SCHEMA, ensure_ascii=False, indent=1), encoding="utf-8")
    (HERE / "out" / "judge-jobs-map.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(rows)} jobs written to {out_dir} ({photos} with photo); map: benchmark/out/judge-jobs-map.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
