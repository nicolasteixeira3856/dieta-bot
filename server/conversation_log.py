"""Dev conversation log (ADR-015). One JSON line per LLM call. Off when the path is empty.

The photo never reaches this module. Invite and API key are redacted before writing.
A failure here never changes the HTTP response.
"""

from __future__ import annotations

import itertools
import json
import logging
from collections.abc import Iterable
from datetime import datetime, timedelta, timezone
from logging.handlers import RotatingFileHandler
from typing import Any

from config import CONVERSATION_LOG_BACKUPS, CONVERSATION_LOG_MAX_BYTES

_LOG = logging.getLogger("nutri")
_IDS = itertools.count()
# America/Sao_Paulo has no DST since 2019. Fixed offset: no tzdata needed on Windows or slim images.
_SAO_PAULO = timezone(timedelta(hours=-3), "America/Sao_Paulo")


def now_iso() -> str:
    return datetime.now(_SAO_PAULO).isoformat(timespec="milliseconds")


class _Handler(RotatingFileHandler):
    def handleError(self, record: logging.LogRecord) -> None:
        _LOG.warning("conversation log write failed")


class ConversationLog:
    def __init__(self, path: str, redact: Iterable[str] = ()) -> None:
        self._secrets = [value for value in redact if value]
        self._logger: logging.Logger | None = None
        if not path:
            return
        logger = logging.getLogger(f"nutri.conversation.{next(_IDS)}")
        logger.propagate = False
        logger.setLevel(logging.INFO)
        handler = _Handler(
            path,
            maxBytes=CONVERSATION_LOG_MAX_BYTES,
            backupCount=CONVERSATION_LOG_BACKUPS,
            encoding="utf-8",
            delay=True,
        )
        handler.setFormatter(logging.Formatter("%(message)s"))
        logger.addHandler(handler)
        self._logger = logger

    @property
    def enabled(self) -> bool:
        return self._logger is not None

    def write(self, record: dict[str, Any]) -> None:
        if self._logger is None:
            return
        try:
            line = json.dumps(record, ensure_ascii=False, default=str)
            for secret in self._secrets:
                line = line.replace(secret, "[redacted]")
            self._logger.info(line)
        except Exception as exc:
            _LOG.warning("conversation log failed: %s", type(exc).__name__)

    def close(self) -> None:
        logger = self._logger
        self._logger = None
        if logger is None:
            return
        for handler in list(logger.handlers):
            logger.removeHandler(handler)
            handler.close()
