"""Render the full prompt texts to Markdown so the owner and reviewers can read exactly what is sent.

    python benchmark/prompts/render.py

Writes new_prompt_{seco,duro}.md (target) and baseline_prompt_{seco,duro}.md (the live server prefix,
meal_changes branch). No model call.
"""
from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT / "server"))
sys.path.insert(0, str(HERE))

from chat_instructions import assemble, chat_branch  # noqa: E402
from new_instructions import assemble_new  # noqa: E402


def main() -> None:
    for tone in ("seco", "duro"):
        new = assemble_new(tone)
        base = assemble(chat_branch(meal_changes=True, tone=tone))
        (HERE / f"new_prompt_{tone}.md").write_text(
            f"# Novo prompt (alvo) — tone {tone}\n\n{len(new)} caracteres. Gerado por render.py de "
            f"new_instructions.py; não editar à mão.\n\n```text\n{new}\n```\n", encoding="utf-8")
        (HERE / f"baseline_prompt_{tone}.md").write_text(
            f"# Prompt atual do server (baseline) — branch meal_changes, tone {tone}\n\n{len(base)} caracteres. "
            f"Gerado por render.py de server/chat_instructions.py; não editar à mão.\n\n```text\n{base}\n```\n",
            encoding="utf-8")
        print(tone, "new", len(new), "baseline", len(base))


if __name__ == "__main__":
    main()
