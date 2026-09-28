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
MAX_BODY_BYTES = 20 * 1024 * 1024
RATE_LIMIT_ESTIMATE = "30/minute"
RATE_LIMIT_FIT = "30/minute"
RATE_LIMIT_CHAT = "30/minute"
FALLBACK_QUESTION = "descreve em 1 linha"
# Digest <= 400 tokens (spec v1-chat rule 7). ~4 chars per token.
DIGEST_MAX_CHARS = 1600
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
