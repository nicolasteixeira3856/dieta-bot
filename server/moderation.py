"""OpenAI moderation (CP2, ADR-024). Free endpoint, same SDK and account as the model.

Input: current user text and photo, before generation. Output: generated text, after it.
Any error or timeout is ModerationUnavailable: the route fails closed (503). The provider
error text never leaves this module.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Any

import httpx2
from openai import OpenAI

from config import MODERATION_MODEL, TIMEOUT_SECONDS

# Internal codes (content-policy spec). Not public Chat intents.
IN_SCOPE = "in_scope"
OUT_OF_SCOPE = "out_of_scope"
POLICY_BLOCKED = "policy_blocked"
SAFETY_SUPPORT = "safety_support"
SCOPES = (IN_SCOPE, OUT_OF_SCOPE, POLICY_BLOCKED, SAFETY_SUPPORT)

# Which fixed copy a safety_support verdict uses (refusal-copy.pt-BR.md).
SUPPORT_EATING = "eating"
SUPPORT_VIOLENCE = "violence"

# Versioned category → action table. Bump the version on any change; it goes to the log.
POLICY_TABLE_VERSION = "cp2.1"
# Severe: stop the turn, no further content-bearing call, metadata-only log.
SEVERE_CATEGORIES = ("sexual/minors",)
# Checked in this order; the first flagged row decides.
CATEGORY_ACTIONS: tuple[tuple[str, str, str | None], ...] = (
    ("sexual/minors", POLICY_BLOCKED, None),
    ("self-harm", SAFETY_SUPPORT, SUPPORT_EATING),
    ("self-harm/intent", SAFETY_SUPPORT, SUPPORT_EATING),
    ("self-harm/instructions", SAFETY_SUPPORT, SUPPORT_EATING),
    ("sexual", POLICY_BLOCKED, None),
    ("harassment/threatening", POLICY_BLOCKED, None),
    ("hate/threatening", POLICY_BLOCKED, None),
    ("illicit/violent", POLICY_BLOCKED, None),
    ("illicit", POLICY_BLOCKED, None),
    ("violence/graphic", POLICY_BLOCKED, None),
    ("hate", POLICY_BLOCKED, None),
    ("harassment", POLICY_BLOCKED, None),
    ("violence", SAFETY_SUPPORT, SUPPORT_VIOLENCE),
)
CATEGORIES = tuple(name for name, _, _ in CATEGORY_ACTIONS)


class ModerationUnavailable(Exception):
    """Moderation could not give a verdict. Fail closed: 503 content_policy_unavailable."""

    def __init__(self, reason: str) -> None:
        super().__init__(reason)
        self.reason = reason


class DeadlineExceeded(TimeoutError):
    """The shared request deadline ran out before a call could start."""


class Deadline:
    """One budget for every external call of a request (moderation + generation)."""

    def __init__(self, seconds: float = TIMEOUT_SECONDS, clock=time.monotonic) -> None:
        self._clock = clock
        self._end = clock() + seconds

    def remaining(self) -> float:
        left = self._end - self._clock()
        if left <= 0:
            raise DeadlineExceeded("deadline")
        return left


@dataclass(frozen=True)
class Verdict:
    code: str = IN_SCOPE
    support: str | None = None
    severe: bool = False
    categories: dict[str, bool] = field(default_factory=dict)

    @property
    def flagged(self) -> bool:
        return self.code != IN_SCOPE

    def log(self, stage: str) -> dict[str, Any]:
        """Metadata for the conversation log: codes and category booleans, never content."""
        return {
            "stage": stage,
            "code": self.code,
            "severe": self.severe,
            "categories": {name: value for name, value in self.categories.items() if value},
            "table": POLICY_TABLE_VERSION,
        }


CLEAN = Verdict()


def verdict_from_results(results: list[dict[str, Any]]) -> Verdict:
    """OR of every result (one per input string, or one for a multimodal input)."""
    categories = {name: False for name in CATEGORIES}
    flagged = False
    for result in results:
        flagged = flagged or bool(result.get("flagged"))
        raw = result.get("categories") or {}
        for name in CATEGORIES:
            categories[name] = categories[name] or bool(raw.get(name))
    for name, code, support in CATEGORY_ACTIONS:
        if categories[name]:
            severe = any(categories[s] for s in SEVERE_CATEGORIES)
            return Verdict(code=code, support=support, severe=severe, categories=categories)
    if flagged:
        # Flagged with a category this table does not know: block.
        return Verdict(code=POLICY_BLOCKED, categories=categories)
    return Verdict(categories=categories)


class Moderator:
    def __init__(self, api_key: str, transport: httpx2.BaseTransport | None = None) -> None:
        self._http: httpx2.Client | None = None
        self._openai: OpenAI | None = None
        if not api_key:
            return
        self._http = httpx2.Client(transport=transport, timeout=httpx2.Timeout(TIMEOUT_SECONDS))
        self._openai = OpenAI(
            api_key=api_key,
            timeout=TIMEOUT_SECONDS,
            max_retries=0,
            http_client=self._http,
        )

    def close(self) -> None:
        openai = self._openai
        http = self._http
        self._openai = None
        self._http = None
        if openai is not None:
            openai.close()
        elif http is not None:
            http.close()

    def check(
        self, *, texts: list[str], image_b64: str | None = None, deadline: Deadline
    ) -> Verdict:
        """One batched call. Blank texts are skipped; nothing to check is CLEAN without a call."""
        parts: list[dict[str, Any]] = [
            {"type": "text", "text": text} for text in texts if text and text.strip()
        ]
        if image_b64:
            parts.append(
                {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64," + image_b64}}
            )
        image_b64 = None
        if not parts:
            return CLEAN
        try:
            if self._openai is None:
                raise ModerationUnavailable("no_key")
            try:
                timeout = deadline.remaining()
            except DeadlineExceeded:
                raise ModerationUnavailable("deadline") from None
            try:
                response = self._openai.moderations.create(
                    model=MODERATION_MODEL, input=parts, timeout=timeout
                )
                results = [r.model_dump(by_alias=True) for r in response.results]
            except Exception as exc:
                # Type only: the provider message stays here.
                raise ModerationUnavailable(type(exc).__name__) from None
            if not results:
                raise ModerationUnavailable("no_results")
            return verdict_from_results(results)
        finally:
            for part in parts:
                part.clear()
            parts.clear()
