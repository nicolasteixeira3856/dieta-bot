"""Configuração lida do .env na raiz do repo. A chave não é impressa."""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

MODEL = "gpt-6-luna"
TIMEOUT_SECONDS = 20.0
PERGUNTA_FALHA = "descreve em 1 linha"

_ROOT_ENV = Path(__file__).resolve().parent.parent / ".env"


def load_settings() -> "Settings":
    if _ROOT_ENV.is_file():
        load_dotenv(_ROOT_ENV, override=False)
    return Settings(
        invite_code=os.environ.get("INVITE_CODE", ""),
        api_key=os.environ.get("OPENAI_API_KEY", ""),
    )


@dataclass(frozen=True)
class Settings:
    invite_code: str
    api_key: str
