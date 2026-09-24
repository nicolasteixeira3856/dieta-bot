"""Memória curta por papel. Um arquivo, um teto, esquece o que quase não volta.

O número do teto mora só aqui.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import date, datetime
from pathlib import Path

BUDGET_BYTES = 6000
MAX_LESSONS = 24
MAX_TEXT = 320
MAX_PINS = 3
ROOT = Path(__file__).resolve().parents[1]
MEMORY = ROOT / ".grok" / "memory" / "roles"
ROLES = (
    "harper",
    "iris",
    "benjamin",
    "lucas",
    "pesquisador",
    "qa",
    "revisor",
)


def _path(role: str) -> Path:
    if role not in ROLES:
        raise SystemExit(f"papel desconhecido: {role}")
    return MEMORY / f"{role}.json"


def _empty(role: str) -> dict:
    return {"role": role, "budget_bytes": BUDGET_BYTES, "lessons": []}


def _load(role: str) -> dict:
    path = _path(role)
    if not path.is_file():
        return _empty(role)
    data = json.loads(path.read_text(encoding="utf-8"))
    data.setdefault("lessons", [])
    data["budget_bytes"] = BUDGET_BYTES
    return data


def _dump(data: dict) -> str:
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def _save(role: str, data: dict) -> None:
    path = _path(role)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(_dump(data), encoding="utf-8")


def _today() -> date:
    return date.today()


def _parse_day(value: str) -> date:
    return datetime.strptime(value, "%Y-%m-%d").date()


def _norm(text: str) -> str:
    return re.sub(r"\s+", " ", text.strip().lower())


def _score(lesson: dict, today: date) -> float:
    if lesson.get("pin"):
        return 10_000 + int(lesson.get("triggers", 1))
    days = max(0, (today - _parse_day(lesson["last"])).days)
    recency = 1.0 if days <= 14 else max(0.15, 14 / days)
    return int(lesson.get("triggers", 1)) * recency


def _size(data: dict) -> int:
    return len(_dump(data).encode("utf-8"))


def prune(data: dict, today: date | None = None) -> dict:
    today = today or _today()
    lessons = list(data.get("lessons", []))
    pins = [item for item in lessons if item.get("pin")]
    if len(pins) > MAX_PINS:
        pins.sort(key=lambda item: _score(item, today), reverse=True)
        keep_ids = {item["id"] for item in pins[:MAX_PINS]}
        for item in lessons:
            if item.get("pin") and item["id"] not in keep_ids:
                item["pin"] = False
    lessons.sort(key=lambda item: _score(item, today), reverse=True)
    data["lessons"] = lessons[:MAX_LESSONS]
    data["budget_bytes"] = BUDGET_BYTES
    while data["lessons"] and _size(data) > BUDGET_BYTES:
        unpinned = [item for item in data["lessons"] if not item.get("pin")]
        drop = unpinned[-1] if unpinned else data["lessons"][-1]
        data["lessons"].remove(drop)
    return data


def _slug(text: str) -> str:
    base = re.sub(r"[^a-z0-9]+", "-", _norm(text))[:40].strip("-")
    return base or "licao"


def add(role: str, text: str, lesson_id: str | None, pin: bool) -> str:
    cleaned = " ".join(text.split())
    if len(cleaned) > MAX_TEXT:
        cleaned = cleaned[: MAX_TEXT - 1].rstrip() + "…"
    data = _load(role)
    key = _norm(cleaned)[:80]
    found = None
    for lesson in data["lessons"]:
        if lesson_id and lesson["id"] == lesson_id:
            found = lesson
            break
        if _norm(lesson["text"])[:80] == key:
            found = lesson
            break
    if found:
        found["triggers"] = int(found.get("triggers", 1)) + 1
        found["last"] = _today().isoformat()
        if pin:
            found["pin"] = True
        if lesson_id:
            found["id"] = lesson_id
        action = "somou"
    else:
        data["lessons"].append(
            {
                "id": lesson_id or _slug(cleaned),
                "triggers": 1,
                "last": _today().isoformat(),
                "pin": bool(pin),
                "text": cleaned,
            }
        )
        action = "nova"
    prune(data)
    _save(role, data)
    return f"{action} {role} {_size(data)}B {len(data['lessons'])} lições"


def touch(role: str, lesson_id: str) -> str:
    data = _load(role)
    for lesson in data["lessons"]:
        if lesson["id"] == lesson_id:
            lesson["triggers"] = int(lesson.get("triggers", 1)) + 1
            lesson["last"] = _today().isoformat()
            prune(data)
            _save(role, data)
            return f"toque {role} {lesson_id} {lesson['triggers']}"
    raise SystemExit(f"lição não encontrada: {lesson_id}")


def forget(role: str, lesson_id: str) -> str:
    data = _load(role)
    before = len(data["lessons"])
    data["lessons"] = [item for item in data["lessons"] if item["id"] != lesson_id]
    if len(data["lessons"]) == before:
        raise SystemExit(f"lição não encontrada: {lesson_id}")
    prune(data)
    _save(role, data)
    return f"esqueceu {role} {lesson_id}"


def show(role: str) -> str:
    data = prune(_load(role))
    if not data["lessons"]:
        return f"{role}: sem lições"
    lines = [f"{role} ({_size(data)}B / {BUDGET_BYTES}B)"]
    ranked = sorted(data["lessons"], key=lambda item: _score(item, _today()), reverse=True)
    for lesson in ranked:
        mark = " pin" if lesson.get("pin") else ""
        lines.append(
            f"- [{lesson['triggers']}{mark}] {lesson['text']}"
        )
    return "\n".join(lines)


def selftest() -> None:
    role = "revisor"
    path = _path(role)
    backup = path.read_text(encoding="utf-8") if path.is_file() else None
    try:
        _save(role, _empty(role))
        add(role, "Lições fixadas ficam enquanto couberem no teto.", "fixa", True)
        add(role, "O teste do celular usa o serial físico e não o emulador.", "serial", False)
        for _ in range(8):
            touch(role, "serial")
        for index in range(30):
            add(role, f"Detalhe frio número {index} que quase nunca volta.", None, False)
        data = _load(role)
        assert _size(data) <= BUDGET_BYTES, _size(data)
        assert len(data["lessons"]) <= MAX_LESSONS
        ids = {item["id"] for item in data["lessons"]}
        assert "serial" in ids
        assert "fixa" in ids
        cold = [item for item in data["lessons"] if item["text"].startswith("Detalhe frio")]
        assert len(cold) < 30
        print(f"selftest ok {_size(data)}B {len(data['lessons'])} lições")
    finally:
        if backup is None:
            path.unlink(missing_ok=True)
        else:
            path.write_text(backup, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "action",
        choices=("add", "touch", "forget", "show", "prune", "selftest"),
    )
    parser.add_argument("--role")
    parser.add_argument("--text")
    parser.add_argument("--id")
    parser.add_argument("--pin", action="store_true")
    args = parser.parse_args()
    if args.action == "selftest":
        selftest()
        return
    if not args.role:
        raise SystemExit("falta --role")
    if args.action == "add":
        if not args.text:
            raise SystemExit("falta --text")
        print(add(args.role, args.text, args.id, args.pin))
    elif args.action == "touch":
        if not args.id:
            raise SystemExit("falta --id")
        print(touch(args.role, args.id))
    elif args.action == "forget":
        if not args.id:
            raise SystemExit("falta --id")
        print(forget(args.role, args.id))
    elif args.action == "prune":
        data = prune(_load(args.role))
        _save(args.role, data)
        print(f"poda {args.role} {_size(data)}B")
    else:
        print(show(args.role))


if __name__ == "__main__":
    main()
