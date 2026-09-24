"""API HTTP. uvicorn main:app na porta 8080."""

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
from shaping import falha_estimate, falha_fit, montar_estimate, montar_fit

_LOG = logging.getLogger("nutri")
_LOG_READY = False


class AssumptionIn(BaseModel):
    alimento: str
    modificador: str


class EstimateIn(BaseModel):
    text: str
    local_time: str | None = None
    janela: str | None = None
    image_b64: str | None = None
    assumptions: list[AssumptionIn] | None = None


class OrcamentoIn(BaseModel):
    kcal: float
    p: float


class FitIn(BaseModel):
    mode: str
    text: str = ""
    itens_disponiveis: list[Any] = Field(default_factory=list)
    orcamento: OrcamentoIn
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
        _exigir_convite(x_invite, app.state.invite_code)
        image = body.image_b64
        body.image_b64 = None
        try:
            payload = llm.estimate_json(user_text=_texto_estimate(body), image_b64=image)
            return montar_estimate(payload)
        except Exception as exc:
            _LOG.warning("estimate falhou: %s", type(exc).__name__)
            return falha_estimate()
        finally:
            image = None

    @app.post("/v1/fit")
    def fit(
        body: FitIn,
        x_invite: str | None = Header(default=None, alias="X-Invite"),
    ) -> dict[str, Any]:
        _exigir_convite(x_invite, app.state.invite_code)
        image = body.image_b64
        body.image_b64 = None
        try:
            payload = llm.fit_json(user_text=_texto_fit(body), image_b64=image)
            return montar_fit(payload, orcamento_kcal=body.orcamento.kcal, mode=body.mode)
        except Exception as exc:
            _LOG.warning("fit falhou: %s", type(exc).__name__)
            return falha_fit(body.mode)
        finally:
            image = None

    return app


def _exigir_convite(recebido: str | None, esperado: str) -> None:
    if not recebido or not esperado or len(recebido) != len(esperado):
        raise HTTPException(status_code=401, detail="unauthorized")
    if not secrets.compare_digest(recebido, esperado):
        raise HTTPException(status_code=401, detail="unauthorized")


def _texto_estimate(body: EstimateIn) -> str:
    linhas = [f"refeicao: {body.text}"]
    if body.janela:
        linhas.append(f"janela: {body.janela}")
    if body.local_time:
        linhas.append(f"local_time: {body.local_time}")
    if body.assumptions:
        linhas.append(
            "assumptions: "
            + ", ".join(f"{item.alimento}={item.modificador}" for item in body.assumptions)
        )
    return "\n".join(linhas)


def _texto_fit(body: FitIn) -> str:
    itens = ", ".join(str(item) for item in body.itens_disponiveis)
    return "\n".join(
        [
            f"mode: {body.mode}",
            f"text: {body.text}",
            f"itens_disponiveis: {itens}",
            f"orcamento_kcal: {body.orcamento.kcal}",
            f"orcamento_p: {body.orcamento.p}",
        ]
    )


def _configure_logging() -> None:
    global _LOG_READY
    if _LOG_READY:
        return
    _LOG_READY = True

    class _RedigeChave(logging.Filter):
        def filter(self, record: logging.LogRecord) -> bool:
            chave = os.environ.get("OPENAI_API_KEY", "")
            if not chave:
                return True
            try:
                mensagem = record.getMessage()
            except Exception:
                return True
            if chave in mensagem:
                record.msg = mensagem.replace(chave, "[redacted]")
                record.args = ()
            return True

    logging.getLogger().addFilter(_RedigeChave())
    for nome in ("openai", "httpx", "httpx2", "httpcore", "httpcore2"):
        logging.getLogger(nome).setLevel(logging.CRITICAL)


app = create_app()
