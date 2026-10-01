"""Settings from the repo-root .env. The key is never printed."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

MODEL = "gpt-6-luna"
TIMEOUT_SECONDS = 60.0
PHOTO_MAX_BYTES = 16 * 1024 * 1024
PHOTO_MAX_B64_CHARS = 22_400_000
# Covers PHOTO_MAX_B64_CHARS + JSON. The photo cap stays the 413 photo_too_large (S8).
MAX_BODY_BYTES = 24 * 1024 * 1024
RATE_LIMIT_ESTIMATE = "30/minute"
RATE_LIMIT_FIT = "30/minute"
RATE_LIMIT_CHAT = "30/minute"
# reasoning.effort of every call (ADR-023 decision 6, chosen by the S11 evaluator).
REASONING_EFFORT = "none"
FALLBACK_QUESTION = "descreve em 1 linha"
CHAT_FALLBACK_QUESTION = "Alguma porção foi diferente do que considerei?"
# Digest <= 400 tokens (spec v1-chat rule 7). ~4 chars per token.
DIGEST_MAX_CHARS = 1600
# Chat v2 (ADR-023). Memory facts and recent meals come from the app; the server is stateless.
FACTS_MAX = 70
MEMORY_PERMANENT_MAX = 30
MEMORY_DYNAMIC_MAX = 40
FACT_KEY_MAX = 40
FACT_TEXT_MAX = 160
RECENT_MAX = 42
RECENT_TEXT_MAX = 240
MEAL_TEXT_MAX = 160
MEMORY_UPDATES_MAX = 5
MEMORY_USED_MAX = 10
# Questions before the estimate (ADR-026, S13). Release gate in shaping.clarify_gate.
CLARIFY_MAX_ROUNDS = 3
CLARIFY_REPEAT_JACCARD = 0.6
# Dev conversation log (ADR-015): on only when CONVERSATION_LOG_PATH is set.
CONVERSATION_LOG_MAX_BYTES = 20 * 1024 * 1024
CONVERSATION_LOG_BACKUPS = 5

_ROOT_ENV = Path(__file__).resolve().parent.parent / ".env"


def load_settings() -> "Settings":
    if _ROOT_ENV.is_file():
        load_dotenv(_ROOT_ENV, override=False)
    return Settings(
        invite_code=os.environ.get("INVITE_CODE", ""),
        api_key=os.environ.get("OPENAI_API_KEY", ""),
        conversation_log_path=os.environ.get("CONVERSATION_LOG_PATH", "").strip(),
    )


@dataclass(frozen=True)
class Settings:
    invite_code: str
    api_key: str
    conversation_log_path: str = ""
