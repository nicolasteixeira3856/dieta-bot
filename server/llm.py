"""Model client. The photo enters the call and is not retained."""

from __future__ import annotations

import json
from typing import Any

import httpx2
from openai import OpenAI

from config import MODEL, TIMEOUT_SECONDS

_ESTIMATE_INSTRUCTIONS = (
    "Estimate the meal. Reply with one JSON object only, keys "
    "kcal, p, c, g, confidence, question, items. "
    "kcal, p, c and g are numbers. p is protein grams, c carbohydrate, g fat. "
    "confidence is high, medium or low. "
    "If confidence is high, question is null. Otherwise one short question. "
    "items is a list of objects {name, g, kcal}. "
    "Estimate, not advice."
)

_FIT_INSTRUCTIONS = (
    "Build a plate inside budget_kcal. p is a protein target, not a ceiling. "
    "Reply with one JSON object only, keys dish, question and options. "
    "dish has name, portions (list of {name, quantity}), kcal and p. "
    "question is a single string. "
    "If mode is surprise, options is a list of exactly 2 dishes in that shape. "
    "Otherwise options is null. "
    "Not advice."
)


class LlmClient:
    def __init__(self, api_key: str, transport: httpx2.BaseTransport | None = None) -> None:
        self._http: httpx2.Client | None = None
        self._openai: OpenAI | None = None
        if not api_key:
            return
        self._http = httpx2.Client(
            transport=transport,
            timeout=httpx2.Timeout(TIMEOUT_SECONDS),
        )
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

    def estimate_json(self, *, user_text: str, image_b64: str | None) -> dict[str, Any]:
        return self._complete(_ESTIMATE_INSTRUCTIONS, user_text, image_b64)

    def fit_json(self, *, user_text: str, image_b64: str | None) -> dict[str, Any]:
        return self._complete(_FIT_INSTRUCTIONS, user_text, image_b64)

    def _complete(self, instructions: str, user_text: str, image_b64: str | None) -> dict[str, Any]:
        content: list[dict[str, str]] = [{"type": "input_text", "text": user_text}]
        if image_b64:
            content.append(
                {
                    "type": "input_image",
                    "image_url": "data:image/jpeg;base64," + image_b64,
                }
            )
        image_b64 = None
        if self._openai is None:
            raise RuntimeError("llm unavailable")
        try:
            response = self._openai.responses.create(
                model=MODEL,
                reasoning={"effort": "none"},
                instructions=instructions,
                input=[{"role": "user", "content": content}],
                timeout=TIMEOUT_SECONDS,
                store=False,
            )
        finally:
            for part in content:
                part.clear()
            content.clear()
        text = response.output_text
        if not text.strip():
            raise ValueError("empty response")
        return _parse_json_object(text)


def _parse_json_object(text: str) -> dict[str, Any]:
    raw = text.strip()
    if raw.startswith("```"):
        raw = raw.strip("`")
        if raw.lower().startswith("json"):
            raw = raw[4:]
        raw = raw.strip()
    start = raw.find("{")
    end = raw.rfind("}")
    if start < 0 or end <= start:
        raise ValueError("no json")
    data = json.loads(raw[start : end + 1])
    if not isinstance(data, dict):
        raise ValueError("json not an object")
    return data
