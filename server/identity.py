"""Installation pseudonym (CP3, ADR-025). The raw installation UUID never leaves this module.

safety_identifier = "v1_" + HMAC-SHA256(SAFETY_ID_SECRET, SERVER_ENV + ":installation:" + uuid).
"""

from __future__ import annotations

import hashlib
import hmac
import re

INSTANCE_ID_HEADER = "X-Client-Instance-Id"
# Canonical UUID v4, lowercase, 36 characters (what java.util.UUID.toString() produces).
_UUID_V4 = re.compile(r"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
_VERSION = "v1_"


class InvalidInstanceId(ValueError):
    """The header is present but not a canonical UUID v4. The value is never kept."""

    def __init__(self) -> None:
        super().__init__("invalid_client_instance_id")


class SafetyIds:
    """Derives the per-installation safety_identifier. Off (always None) without a secret."""

    def __init__(self, secret: str, server_env: str) -> None:
        self._key = secret.encode("utf-8") if secret else None
        self._prefix = f"{server_env}:installation:"

    @property
    def enabled(self) -> bool:
        return self._key is not None

    def derive(self, instance_id: str | None) -> str | None:
        """Missing header → None. Invalid header → InvalidInstanceId, even with the secret off."""
        if instance_id is None:
            return None
        if len(instance_id) != 36 or not _UUID_V4.fullmatch(instance_id):
            raise InvalidInstanceId()
        if self._key is None:
            return None
        message = (self._prefix + instance_id).encode("utf-8")
        return _VERSION + hmac.new(self._key, message, hashlib.sha256).hexdigest()
