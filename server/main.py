"""HTTP API. uvicorn main:app on port 8080."""

from __future__ import annotations

import logging
import os
import secrets
from contextlib import asynccontextmanager
from typing import Any

import httpx2
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

from config import MODEL, load_settings
from llm import LlmClient
from shaping import fail_estimate, fail_fit, shape_estimate, shape_fit

_LOG = logging.getLogger("nutri")
_LOG_READY = False


class AssumptionIn(BaseModel):
    food: str
    modifier: str


class EstimateIn(BaseModel):
    text: str
    local_time: str | None = None
    window: str | None = None
    image_b64: str | None = None
    assumptions: list[AssumptionIn] | None = None


class BudgetIn(BaseModel):
    kcal: float
    p: float


class FitIn(BaseModel):
    mode: str
    text: str = ""
    available_items: list[Any] = Field(default_factory=list)
    budget: BudgetIn
    image_b64: str | None = None


def create_app(transport: httpx2.BaseTransport | None = None) -> FastAPI:
    _configure_logging()
    settings = load_settings()
    llm = LlmClient(api_key=settings.api_key, transport=transport)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        yield
        llm.close()

    app = FastAPI(lifespan=lifespan)
    app.state.llm = llm
    app.state.invite_code = settings.invite_code

    @app.get("/health")
    def health() -> Response:
        body = '{"ok": true, "model": "' + MODEL + '"}'
        return Response(content=body.encode("utf-8"), media_type="application/json")

    @app.post("/v1/estimate")
    def estimate(
        body: EstimateIn,
        x_invite: str | None = Header(default=None, alias="X-Invite"),
    ) -> dict[str, Any]:
        _require_invite(x_invite, app.state.invite_code)
        image = body.image_b64
        body.image_b64 = None
        try:
            payload = llm.estimate_json(user_text=_estimate_text(body), image_b64=image)
            return shape_estimate(payload)
        except Exception as exc:
            _LOG.warning("estimate failed: %s", type(exc).__name__)
            return fail_estimate()
        finally:
            image = None

    @app.post("/v1/fit")
    def fit(
        body: FitIn,
        x_invite: str | None = Header(default=None, alias="X-Invite"),
    ) -> dict[str, Any]:
        _require_invite(x_invite, app.state.invite_code)
        image = body.image_b64
        body.image_b64 = None
        try:
            payload = llm.fit_json(user_text=_fit_text(body), image_b64=image)
            return shape_fit(payload, budget_kcal=body.budget.kcal, mode=body.mode)
        except Exception as exc:
            _LOG.warning("fit failed: %s", type(exc).__name__)
            return fail_fit(body.mode)
        finally:
            image = None

    return app


def _require_invite(received: str | None, expected: str) -> None:
    if not received or not expected or len(received) != len(expected):
        raise HTTPException(status_code=401, detail="unauthorized")
    if not secrets.compare_digest(received, expected):
        raise HTTPException(status_code=401, detail="unauthorized")


def _estimate_text(body: EstimateIn) -> str:
    lines = [f"meal: {body.text}"]
    if body.window:
        lines.append(f"window: {body.window}")
    if body.local_time:
        lines.append(f"local_time: {body.local_time}")
    if body.assumptions:
        lines.append(
            "assumptions: "
            + ", ".join(f"{item.food}={item.modifier}" for item in body.assumptions)
        )
    return "\n".join(lines)


def _fit_text(body: FitIn) -> str:
    items = ", ".join(str(item) for item in body.available_items)
    return "\n".join(
        [
            f"mode: {body.mode}",
            f"text: {body.text}",
            f"available_items: {items}",
            f"budget_kcal: {body.budget.kcal}",
            f"budget_p: {body.budget.p}",
        ]
    )


def _configure_logging() -> None:
    global _LOG_READY
    if _LOG_READY:
        return
    _LOG_READY = True

    class _RedactKey(logging.Filter):
        def filter(self, record: logging.LogRecord) -> bool:
            key = os.environ.get("OPENAI_API_KEY", "")
            if not key:
                return True
            try:
                message = record.getMessage()
            except Exception:
                return True
            if key in message:
                record.msg = message.replace(key, "[redacted]")
                record.args = ()
            return True

    logging.getLogger().addFilter(_RedactKey())
    for name in ("openai", "httpx", "httpx2", "httpcore", "httpcore2"):
        logging.getLogger(name).setLevel(logging.CRITICAL)


app = create_app()
