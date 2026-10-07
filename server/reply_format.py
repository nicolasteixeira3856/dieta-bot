"""The reply formatting subset (ADR-045, S30 part B). Pure functions, no I/O.

The subset: `**bold**`, `- ` bullets, `1. ` numbered steps and one two-column table of at most six rows. Any
other markup is removed, keeping its text; a table beyond the subset becomes bullets. The app renders exactly
this subset and shows anything else as literal text.
"""

from __future__ import annotations

import re

TABLE_MAX_ROWS = 6

_FENCE = re.compile(r"^\s*(```|~~~).*$")
_HEADING = re.compile(r"^\s{0,3}#{1,6}\s+")
_IMAGE = re.compile(r"!\[([^\]]*)\]\([^)]*\)")
_LINK = re.compile(r"\[([^\]]+)\]\([^)]*\)")
_HTML = re.compile(r"</?[A-Za-z][^<>]*>")
_CODE = re.compile(r"`+([^`]*)`+")
_UNDERSCORE = re.compile(r"(?<![\w_])(__?)(?=\S)(.+?)(?<=\S)\1(?![\w_])")
_ITALIC = re.compile(r"(?<![*\w])\*(?=\S)([^*\n]+?)(?<=\S)\*(?![*\w])")
_BULLET = re.compile(r"^(\s*)([-*+•])\s+(.*)$")
_STEP = re.compile(r"^(\s*)(\d{1,2})[.)]\s+(.*)$")
_QUOTE = re.compile(r"^\s*>\s?")
_RULE = re.compile(r"^\s*([-*_])(\s*\1){2,}\s*$")
_TABLE_ROW = re.compile(r"^\s*\|.*\|\s*$")
_TABLE_SEPARATOR = re.compile(r"^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$")
_EMOJI = re.compile(
    "[\U0001F000-\U0001FAFF\U00002600-\U000027BF\U0001F1E6-\U0001F1FF\U00002B00-\U00002BFF️‍]"
)


def shape(reply: str) -> tuple[str, int]:
    """(reply inside the subset, number of markers removed or rewritten)."""
    removed = 0
    lines: list[str] = []
    for line in reply.split("\n"):
        line, n = _inline(line)
        removed += n
        line, n = _block(line)
        removed += n
        lines.append(line)
    lines, n = _tables(lines)
    removed += n
    lines, n = _bold(lines)
    removed += n
    return _blank_runs(lines), removed


def _inline(line: str) -> tuple[str, int]:
    removed = 0
    for pattern, repl in ((_IMAGE, r"\1"), (_LINK, r"\1"), (_HTML, ""), (_CODE, r"\1"), (_EMOJI, "")):
        line, n = pattern.subn(repl, line)
        removed += n
    line, n = _UNDERSCORE.subn(r"\2", line)
    removed += n
    line, n = _ITALIC.subn(r"\1", line)
    removed += n
    return line, removed


def _block(line: str) -> tuple[str, int]:
    if _FENCE.match(line) or _RULE.match(line):
        return "", 1
    removed = 0
    for pattern in (_HEADING, _QUOTE):
        if pattern.match(line):
            line = pattern.sub("", line, count=1)
            removed += 1
    bullet = _BULLET.match(line)
    if bullet:
        indent, marker, text = bullet.groups()
        if indent or marker != "-":
            removed += 1  # a nested or non-dash bullet becomes a top-level "- "
        return f"- {text.strip()}", removed
    step = _STEP.match(line)
    if step:
        indent, number, text = step.groups()
        if indent or not line.lstrip().startswith(f"{number}. "):
            removed += 1
        return f"{number}. {text.strip()}", removed
    return line.rstrip(), removed


def _cells(row: str) -> list[str]:
    return [cell.strip() for cell in row.strip().strip("|").split("|")]


def _tables(lines: list[str]) -> tuple[list[str], int]:
    """Keep the first two-column table of at most six rows; any other table becomes bullets."""
    out: list[str] = []
    removed = 0
    kept = False
    i = 0
    while i < len(lines):
        if not _TABLE_ROW.match(lines[i]):
            out.append(lines[i])
            i += 1
            continue
        block = []
        while i < len(lines) and _TABLE_ROW.match(lines[i]):
            block.append(lines[i])
            i += 1
        header, *rest = block
        has_separator = bool(rest) and bool(_TABLE_SEPARATOR.match(rest[0]))
        rows = [_cells(r) for r in (rest[1:] if has_separator else rest) if not _TABLE_SEPARATOR.match(r)]
        head = _cells(header)
        two_columns = len(head) == 2 and all(len(r) == 2 for r in rows)
        if not kept and has_separator and two_columns and 1 <= len(rows) <= TABLE_MAX_ROWS:
            kept = True
            out.append(f"| {head[0]} | {head[1]} |")
            out.append("| --- | --- |")
            out.extend(f"| {a} | {b} |" for a, b in rows)
            continue
        removed += 1
        for row in rows if has_separator else [head, *rows]:
            cells = [c for c in row if c]
            if cells:
                out.append("- " + (f"{cells[0]}: {' · '.join(cells[1:])}" if len(cells) > 1 else cells[0]))
    return out, removed


def _bold(lines: list[str]) -> tuple[list[str], int]:
    """Balanced bold stays; an unbalanced marker or a line that is bold from end to end loses it."""
    out = []
    removed = 0
    for line in lines:
        prefix = ""
        m = re.match(r"^(- |\d{1,2}\. )", line)
        if m:
            prefix, line = m.group(1), line[m.end():]
        count = line.count("**")
        if count % 2:
            line = line.replace("**", "")
            removed += count
        elif count == 2 and line.startswith("**") and line.endswith("**") and _sentence(line[2:-2]):
            line = line[2:-2]
            removed += 2
        else:
            # Empty bold (`****`) carries nothing.
            line, n = re.subn(r"\*\*\s*\*\*", "", line)
            removed += 2 * n
        out.append(prefix + line)
    return out, removed


def _sentence(text: str) -> bool:
    """A whole sentence, not a dish name or a one-word verdict: it ends like one or runs long."""
    text = text.strip()
    return text.endswith((".", "!", "?", ":")) or len(text.split()) > 6


def _blank_runs(lines: list[str]) -> str:
    """At most one blank line in a row; no blank line at either end."""
    out: list[str] = []
    for line in lines:
        if not line.strip():
            if out and out[-1] == "":
                continue
            out.append("")
            continue
        out.append(line)
    return "\n".join(out).strip("\n")


def plain(text: str) -> str:
    """Text without the subset markers: the closing lines and the receipts never carry them."""
    text = text.replace("**", "")
    text = re.sub(r"^(?:- |\d{1,2}\. )", "", text, flags=re.MULTILINE)
    return text
