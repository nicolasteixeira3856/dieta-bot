"""Chat evaluator (S10): runs the /v1/chat turn against the real model.

Inside server/, with the .venv:

    python -m evals.run --effort none low --repeat 3 [--only id,...] [--tag tag] [--moderation cp2|all|none]

Same orchestration as the route (main.chat_reply: moderation, generation, scope, shaping,
output moderation; CP2), no HTTP. The provider's moderation endpoint has a daily request cap per
project, shared with the dev server, so by default only CP2 cases (since cp2 or tag cp2) reach it;
every other case gets a local clean verdict. A case may name a benign synthetic photo in "image".
A "strict" case (CP2 safety sets) passes only when every repetition passes: no leak is excused.
Key: OPENAI_API_KEY from the repo-root .env via config.load_settings(). Never printed.
Report: terminal + logs/evals/<date>-<effort>.json (outside git).
"""

from __future__ import annotations

import argparse
import base64
import json
import statistics
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

import httpx2

from config import MODEL, load_settings
from evals.checks import FAIL, NA, PASS, case_status, evaluate, repetition_status
from llm import LlmClient
from main import ChatIn, chat_reply, compact_reply
from moderation import CLEAN, Deadline, ModerationUnavailable, Moderator
from shaping import fail_chat, fail_digest

SERVER = Path(__file__).resolve().parent.parent
CASES_DIR = Path(__file__).resolve().parent / "cases"
MEDIA_DIR = CASES_DIR / "media"
REPORT_DIR = SERVER.parent / "logs" / "evals"
MAX_WORKERS = 3
DEFAULT_WORKERS = 2
RATE_LIMIT_RETRIES = 2
EFFORTS = ("none", "minimal", "low", "medium", "high")
# USD per 1M tokens, gpt-6-luna standard tier (30/09/2026). Reasoning tokens bill as output.
PRICE_INPUT = 0.10
PRICE_CACHED_INPUT = 0.01
PRICE_OUTPUT = 0.50
# Same as conversation_log: Brazil has no DST since 2019.
TZ = timezone(timedelta(hours=-3), "America/Sao_Paulo")


class UsageTransport(httpx2.BaseTransport):
    """Keeps response.usage of the last model call made by the current thread.

    Moderation responses carry no usage and leave it untouched.
    """

    def __init__(self, inner: httpx2.BaseTransport | None = None) -> None:
        self._inner = inner or httpx2.HTTPTransport()
        self._local = threading.local()

    def handle_request(self, request: httpx2.Request) -> httpx2.Response:
        response = self._inner.handle_request(request)
        response.read()
        try:
            usage = json.loads(response.content).get("usage")
        except (ValueError, AttributeError):
            usage = None
        if isinstance(usage, dict):
            self._local.usage = usage
        return response

    def take_usage(self) -> dict[str, int]:
        usage = getattr(self._local, "usage", None) or {}
        self._local.usage = None
        return {
            "input": _int(usage.get("input_tokens")),
            "cached_input": _int((usage.get("input_tokens_details") or {}).get("cached_tokens")),
            "output": _int(usage.get("output_tokens")),
            "reasoning": _int((usage.get("output_tokens_details") or {}).get("reasoning_tokens")),
        }

    def close(self) -> None:
        self._inner.close()


def _int(value: Any) -> int:
    return value if isinstance(value, int) and not isinstance(value, bool) else 0


def load_cases(directory: Path = CASES_DIR) -> list[dict[str, Any]]:
    cases = []
    for path in sorted(directory.glob("*.json")):
        case = json.loads(path.read_text(encoding="utf-8"))
        case["_file"] = path.name
        cases.append(case)
    return cases


def select_cases(
    cases: list[dict[str, Any]], only: list[str] | None, tags: list[str] | None
) -> list[dict[str, Any]]:
    if only:
        unknown = set(only) - {c["id"] for c in cases}
        if unknown:
            raise SystemExit(f"unknown case id: {', '.join(sorted(unknown))}")
        cases = [c for c in cases if c["id"] in only]
    if tags:
        cases = [c for c in cases if set(tags) & set(c.get("tags", []))]
    return cases


BUDGET_TAG = "over-budget"
MODERATION_MODES = ("cp2", "all", "none")
CP2_TAG = "cp2"


class CleanModerator:
    """Local clean verdict: no request leaves the machine. Benign synthetic fixtures do not need the endpoint."""

    def check(self, **_: Any):
        return CLEAN

    def close(self) -> None:
        return None


def moderated_for_real(case: dict[str, Any], mode: str) -> bool:
    if mode == "all":
        return True
    if mode == "none":
        return False
    return case.get("since") == CP2_TAG or CP2_TAG in case.get("tags", [])


def case_request(case: dict[str, Any]) -> dict[str, Any]:
    """The request as sent: a case tagged over-budget opts in to the plan budget capability (ADR-039)."""
    if BUDGET_TAG in case.get("tags", []):
        return {**case["request"], "plan_budget": True}
    return case["request"]


def run_once(
    llm: LlmClient,
    usage: UsageTransport,
    case: dict[str, Any],
    secret: str = "",
    moderator: "Moderator | CleanModerator | None" = None,
) -> dict[str, Any]:
    """One repetition, handled like the /v1/chat route. Moderation down = an error repetition."""
    body = ChatIn.model_validate(case_request(case))
    image = None if body.compact else _case_image(case)
    trace: dict[str, Any] = {}
    error: str | None = None
    started = time.monotonic()
    usage.take_usage()
    for attempt in range(RATE_LIMIT_RETRIES + 1):
        trace = {"route": "compact" if body.compact else "chat"}
        try:
            if body.compact:
                output = compact_reply(llm, moderator, body, trace, Deadline())
            else:
                output = chat_reply(llm, moderator, body, image, trace, Deadline())
            failure = trace.get("error") or {}
            error = _error_text(failure) if failure else None
        except ModerationUnavailable as exc:
            output = fail_digest() if body.compact else fail_chat()
            error = f"ModerationUnavailable: {exc.reason}"
        except Exception as exc:
            message = str(exc)[:300]
            if secret:
                message = message.replace(secret, "[redacted]")
            output = fail_digest() if body.compact else fail_chat()
            error = f"{type(exc).__name__}: {message}"
        if error and "RateLimitError" in error and attempt < RATE_LIMIT_RETRIES:
            time.sleep(5 * (attempt + 1))
            started = time.monotonic()
            continue
        break
    latency_ms = round((time.monotonic() - started) * 1000)
    checks = evaluate(case["expect"], output, required=case.get("required"))
    return {
        "status": FAIL if error else repetition_status(checks),
        "checks": checks,
        "output": output,
        "raw_output": trace.get("raw_output"),
        "policy": trace.get("policy"),
        "error": error,
        "latency_ms": latency_ms,
        "usage": usage.take_usage(),
    }


def _error_text(failure: dict[str, Any]) -> str:
    status = failure.get("status")
    return failure.get("type", "error") + (f" {status}" if status else "")


def _case_image(case: dict[str, Any]) -> str | None:
    name = case.get("image")
    if not name:
        return None
    return base64.b64encode((MEDIA_DIR / name).read_bytes()).decode("ascii")


def run_effort(
    effort: str,
    cases: list[dict[str, Any]],
    repeat: int,
    api_key: str,
    workers: int = DEFAULT_WORKERS,
    transport: httpx2.BaseTransport | None = None,
    moderation: str = "cp2",
) -> dict[str, Any]:
    if moderation not in MODERATION_MODES:
        raise ValueError(f"moderation must be one of {MODERATION_MODES}")
    usage = UsageTransport(transport)
    llm = LlmClient(api_key=api_key, transport=usage, effort=effort)
    moderator = Moderator(api_key=api_key, transport=usage)
    clean = CleanModerator()
    jobs = [(case, i) for case in cases for i in range(repeat)]
    try:
        with ThreadPoolExecutor(max_workers=max(1, min(workers, MAX_WORKERS))) as pool:
            runs = list(
                pool.map(
                    lambda job: run_once(
                        llm, usage, job[0], api_key,
                        moderator if moderated_for_real(job[0], moderation) else clean,
                    ),
                    jobs,
                )
            )
    finally:
        llm.close()
        moderator.close()
    per_case: dict[str, list[dict[str, Any]]] = {case["id"]: [] for case in cases}
    for (case, _), result in zip(jobs, runs):
        per_case[case["id"]].append(result)
    report = summarize(effort, cases, per_case, repeat)
    report["moderation"] = moderation
    return report


def summarize(
    effort: str,
    cases: list[dict[str, Any]],
    per_case: dict[str, list[dict[str, Any]]],
    repeat: int,
) -> dict[str, Any]:
    case_reports = []
    for case in cases:
        reps = per_case[case["id"]]
        status = case_status([r["status"] for r in reps], strict=bool(case.get("strict")))
        failed: dict[str, list[str]] = {}
        for rep in reps:
            for name, check in rep["checks"].items():
                if check["status"] == FAIL:
                    failed.setdefault(name, []).append(check["detail"])
            if rep["error"]:
                failed.setdefault("error", []).append(rep["error"])
        na = sorted({n for r in reps for n, c in r["checks"].items() if c["status"] == NA})
        case_reports.append(
            {
                "id": case["id"],
                "since": case.get("since"),
                "tags": case.get("tags", []),
                "strict": bool(case.get("strict")),
                "status": status,
                "passes": sum(1 for r in reps if r["status"] == PASS),
                "repeat": repeat,
                "failed_checks": failed,
                "na_checks": na,
                "raw_outputs": [r["raw_output"] for r in reps if r["status"] == FAIL],
                "outputs": [r["output"] for r in reps],
                "policy": [r.get("policy") for r in reps],
            }
        )

    all_reps = [r for reps in per_case.values() for r in reps]
    latencies = sorted(r["latency_ms"] for r in all_reps)
    tokens = {
        key: sum(r["usage"][key] for r in all_reps)
        for key in ("input", "cached_input", "output", "reasoning")
    }
    cost = (
        (tokens["input"] - tokens["cached_input"]) * PRICE_INPUT
        + tokens["cached_input"] * PRICE_CACHED_INPUT
        + tokens["output"] * PRICE_OUTPUT
    ) / 1_000_000
    calls = len(all_reps) or 1
    return {
        "model": MODEL,
        "effort": effort,
        "repeat": repeat,
        "at": datetime.now(TZ).isoformat(timespec="seconds"),
        "pass_rate": _rate(case_reports),
        "by_tag": {
            tag: _rate([c for c in case_reports if tag in c["tags"]])
            for tag in sorted({t for c in case_reports for t in c["tags"]})
        },
        "latency_ms": {"p50": _percentile(latencies, 50), "p95": _percentile(latencies, 95)},
        "tokens": tokens,
        "tokens_per_call": {k: round(v / calls) for k, v in tokens.items()},
        "cost_usd": round(cost, 4),
        "cases": case_reports,
    }


def _rate(case_reports: list[dict[str, Any]]) -> dict[str, Any]:
    passed = sum(1 for c in case_reports if c["status"] == PASS)
    failed = sum(1 for c in case_reports if c["status"] == FAIL)
    na = sum(1 for c in case_reports if c["status"] == NA)
    judged = passed + failed
    return {
        "passed": passed,
        "failed": failed,
        "na": na,
        "rate": round(100 * passed / judged, 1) if judged else None,
    }


def _percentile(values: list[int], pct: int) -> int | None:
    if not values:
        return None
    if len(values) == 1:
        return values[0]
    return round(statistics.quantiles(values, n=100, method="inclusive")[pct - 1])


def print_report(report: dict[str, Any]) -> None:
    rate = report["pass_rate"]
    print(f"\n== {report['model']} effort={report['effort']} repeat={report['repeat']} ==")
    print(f"pass: {rate['passed']}/{rate['passed'] + rate['failed']} ({rate['rate']}%), n/a: {rate['na']}")
    for tag, tag_rate in report["by_tag"].items():
        print(f"  {tag:<12} {tag_rate['passed']}/{tag_rate['passed'] + tag_rate['failed']} ({tag_rate['rate']}%)")
    lat = report["latency_ms"]
    tok = report["tokens_per_call"]
    print(f"latency p50 {lat['p50']} ms, p95 {lat['p95']} ms")
    print(
        f"tokens/call: input {tok['input']} (cached {tok['cached_input']}), "
        f"output {tok['output']} (reasoning {tok['reasoning']})"
    )
    print(f"cost: US$ {report['cost_usd']}")
    for case in report["cases"]:
        mark = {PASS: "ok  ", FAIL: "FAIL", NA: "n/a "}[case["status"]]
        print(f"[{mark}] {case['id']} ({case['passes']}/{case['repeat']})")
        if case["status"] == FAIL:
            for name, details in case["failed_checks"].items():
                print(f"       {name}: {details[0]}")
            if case["raw_outputs"] and case["raw_outputs"][0]:
                print(f"       raw: {case['raw_outputs'][0][:400]}")


def write_report(report: dict[str, Any], directory: Path = REPORT_DIR) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(TZ).strftime("%Y-%m-%d-%H%M%S")
    path = directory / f"{stamp}-{report['effort']}.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="python -m evals.run")
    parser.add_argument("--effort", nargs="+", default=["none"], choices=EFFORTS)
    parser.add_argument("--repeat", type=int, default=3)
    parser.add_argument("--only", help="comma-separated case ids")
    parser.add_argument("--tag", action="append", help="run cases with this tag (repeatable)")
    parser.add_argument("--workers", type=int, default=DEFAULT_WORKERS,
                        help=f"default {DEFAULT_WORKERS}, max {MAX_WORKERS}")
    parser.add_argument("--moderation", choices=MODERATION_MODES, default="cp2",
                        help="which cases call the provider's moderation endpoint (default cp2 cases only)")
    args = parser.parse_args(argv)
    if args.repeat < 1:
        parser.error("--repeat must be >= 1")

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    api_key = load_settings().api_key
    if not api_key:
        print("OPENAI_API_KEY missing in .env", file=sys.stderr)
        return 2
    only = [i.strip() for i in args.only.split(",") if i.strip()] if args.only else None
    cases = select_cases(load_cases(), only, args.tag)
    if not cases:
        print("no case selected", file=sys.stderr)
        return 2

    for effort in args.effort:
        print(f"running {len(cases)} cases x {args.repeat} with effort={effort}...", flush=True)
        report = run_effort(effort, cases, args.repeat, api_key, workers=args.workers,
                            moderation=args.moderation)
        print_report(report)
        print(f"report: {write_report(report)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
