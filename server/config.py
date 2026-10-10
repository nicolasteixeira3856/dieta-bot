"""Settings from the repo-root .env. The key is never printed."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

MODEL = "gpt-6-luna"
# One deadline per request, shared by moderation and generation (CP2).
TIMEOUT_SECONDS = 60.0
# Free OpenAI moderation (CP2, ADR-024). Text and image, input and output.
MODERATION_MODEL = "omni-moderation-latest"
PHOTO_MAX_BYTES = 16 * 1024 * 1024
PHOTO_MAX_B64_CHARS = 22_400_000
# Covers PHOTO_MAX_B64_CHARS + JSON. The photo cap stays the 413 photo_too_large (S8).
MAX_BODY_BYTES = 24 * 1024 * 1024
RATE_LIMIT_ESTIMATE = "30/minute"
RATE_LIMIT_FIT = "30/minute"
RATE_LIMIT_CHAT = "30/minute"
# reasoning.effort of estimate, fit, compaction and closures (ADR-023 decision 6, S11 evaluator).
REASONING_EFFORT = "none"
# reasoning.effort of the Chat generation (ADR-054, S33). Measured p95 16 s; the 60 s deadline covers it.
CHAT_EFFORT = "low"
# reasoning.effort of the onboarding profile build (ADR-057, S41): one call per user, like the Chat.
PROFILE_EFFORT = "low"
FALLBACK_QUESTION = "descreve em 1 linha"
CHAT_FALLBACK_QUESTION = "Alguma porção foi diferente do que considerei?"
# Digest <= 400 tokens (spec v1-chat rule 7). ~4 chars per token.
DIGEST_MAX_CHARS = 1600
# Chat v2 (ADR-023). Memory facts and recent meals come from the app; the server is stateless.
# ADR-057 (S41): the permanent cap is 50 (was 30); the request carries every kind at its cap.
FACTS_MAX = 95
MEMORY_PERMANENT_MAX = 50
MEMORY_DYNAMIC_MAX = 40
MEMORY_TEMP_MAX = 5
FACT_KEY_MAX = 40
FACT_TEXT_MAX = 160
RECENT_MAX = 42
# S33: seven-day totals and routine numbers (optional request fields).
RECENT_DAYS_MAX = 7
FACT_KCAL_MAX = 5000
FACT_GRAMS_MAX = 1000
# S38 (ADR-052): the saved-recipe index and the one full recipe a message names.
RECIPES_MAX = 30
RECIPE_NAME_MAX = 60
RECIPE_FOOD_MAX = 40
RECIPE_KEY_FOODS_MAX = 3
RECIPE_INGREDIENTS_MAX = 30
RECIPE_STEPS_MAX = 10
RECIPE_STEP_MAX = 300
RECENT_TEXT_MAX = 240
MEAL_TEXT_MAX = 500
COMPOSED_MEAL_TEXT_MAX = 2000
MEMORY_UPDATES_MAX = 5
MEMORY_USED_MAX = 10
# Questions before the estimate (ADR-026, S13). Release gate in shaping.clarify_gate.
CLARIFY_MAX_ROUNDS = 3
CLARIFY_REPEAT_JACCARD = 0.6
# Record mark (ADR-028, S14): model enums of chat_turn. Gate in shaping.record_gate.
RECORD_INTENTS = ("clear", "unsure")
MEAL_DAYS = ("today", "other")
# Dev conversation log (ADR-015): on only when CONVERSATION_LOG_PATH is set.
CONVERSATION_LOG_MAX_BYTES = 20 * 1024 * 1024
CONVERSATION_LOG_BACKUPS = 5
# Safety identifier namespace (CP3, ADR-025) when SERVER_ENV is unset. The closed test is dev only.
DEFAULT_SERVER_ENV = "dev"

_ROOT_ENV = Path(__file__).resolve().parent.parent / ".env"


def load_settings() -> "Settings":
    if _ROOT_ENV.is_file():
        load_dotenv(_ROOT_ENV, override=False)
    return Settings(
        invite_code=os.environ.get("INVITE_CODE", ""),
        api_key=os.environ.get("OPENAI_API_KEY", ""),
        conversation_log_path=os.environ.get("CONVERSATION_LOG_PATH", "").strip(),
        safety_id_secret=os.environ.get("SAFETY_ID_SECRET", "").strip(),
        server_env=os.environ.get("SERVER_ENV", "").strip() or DEFAULT_SERVER_ENV,
    )


@dataclass(frozen=True)
class Settings:
    invite_code: str
    api_key: str
    conversation_log_path: str = ""
    # Empty secret: the server starts and sends no safety_identifier (/health safety_id off).
    safety_id_secret: str = ""
    server_env: str = DEFAULT_SERVER_ENV
