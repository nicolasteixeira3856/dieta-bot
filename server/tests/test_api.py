"""Rotas reais via ASGI. O stub troca só o transporte HTTP do modelo."""

from __future__ import annotations

import json
import logging
import os
import unittest
from pathlib import Path

os.environ["OPENAI_API_KEY"] = "sk-test-sentinel-not-a-real-key"
os.environ["INVITE_CODE"] = "convite-teste"
os.environ["LLM_MODEL"] = "gpt-nao-usar"

import httpx
import httpx2

import main
from config import PERGUNTA_FALHA

INVITE = "convite-teste"
CHAVE_FALSA = "sk-test-sentinel-not-a-real-key"
CAFE = "2 paes, ovo, cafe com leite"


class ApiTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self._apps = []

    async def asyncTearDown(self) -> None:
        for app in self._apps:
            app.state.llm.close()

    async def test_health_repete_ok_e_modelo_sem_chave(self) -> None:
        app = self._app(_recusa)
        async with _cliente(app) as cliente:
            primeira_resp = await cliente.get("/health")
            segunda_resp = await cliente.get("/health")
        primeira = primeira_resp.json()
        segunda = segunda_resp.json()
        self.assertEqual(primeira_resp.text, '{"ok": true, "model": "gpt-6-luna"}')
        self.assertEqual(segunda_resp.text, primeira_resp.text)
        self.assertEqual(primeira, {"ok": True, "model": "gpt-6-luna"})
        self.assertEqual(segunda, primeira)
        self.assertNotIn(CHAVE_FALSA, primeira_resp.text)

    async def test_estimate_devolve_o_payload_do_modelo(self) -> None:
        payload = {
            "kcal": 517,
            "p": 19,
            "c": 63,
            "g": 14,
            "confianca": "medio",
            "pergunta": "os paes eram franceses?",
            "itens": [{"nome": "pao", "g": 100, "kcal": 270}],
        }
        capturado: list[httpx2.Request] = []
        app = self._app(_responde(payload, capturado))
        async with _cliente(app) as cliente:
            resposta = await cliente.post(
                "/v1/estimate",
                headers={"X-Invite": INVITE},
                json={"text": CAFE, "image_b64": None},
            )
        self.assertEqual(resposta.status_code, 200)
        corpo = resposta.json()
        self.assertEqual(corpo["kcal"], payload["kcal"])
        self.assertEqual(corpo["p"], payload["p"])
        self.assertEqual(corpo["c"], payload["c"])
        self.assertEqual(corpo["g"], payload["g"])
        self.assertEqual(corpo["confianca"], payload["confianca"])
        self.assertEqual(corpo["pergunta"], payload["pergunta"])
        self.assertEqual(corpo["itens"], payload["itens"])
        self.assertEqual(corpo["model"], "gpt-6-luna")
        self.assertEqual(len(capturado), 1)
        pedido = json.loads(capturado[0].content)
        self.assertEqual(pedido["model"], "gpt-6-luna")
        self.assertNotEqual(pedido["model"], "gpt-nao-usar")
        self.assertEqual(pedido["reasoning"]["effort"], "none")
        self.assertIn(CAFE, capturado[0].content.decode())
        timeout = capturado[0].extensions["timeout"]
        for parte in ("connect", "read", "write", "pool"):
            self.assertEqual(timeout[parte], 20, msg=str(timeout))

    async def test_pergunta_so_quando_confianca_nao_e_alto(self) -> None:
        alto = {
            "kcal": 410,
            "p": 18,
            "c": 44,
            "g": 11,
            "confianca": "alto",
            "pergunta": "nao deve sair",
            "itens": [],
        }
        app = self._app(_responde(alto, []))
        async with _cliente(app) as cliente:
            corpo = (
                await cliente.post(
                    "/v1/estimate",
                    headers={"X-Invite": INVITE},
                    json={"text": CAFE},
                )
            ).json()
        self.assertEqual(corpo["confianca"], "alto")
        self.assertNotIn("pergunta", corpo)

    async def test_timeout_e_erro_de_transporte_viram_pergunta_fixa(self) -> None:
        for erro in (
            httpx2.TimeoutException("timeout"),
            httpx2.ConnectError("conexao"),
        ):
            with self.subTest(erro=type(erro).__name__):
                app = self._app(_estoura(erro))
                async with _cliente(app) as cliente:
                    resposta = await cliente.post(
                        "/v1/estimate",
                        headers={"X-Invite": INVITE},
                        json={"text": CAFE},
                    )
                corpo = resposta.json()
                self.assertEqual(resposta.status_code, 200)
                self.assertEqual(corpo["confianca"], "baixa")
                self.assertEqual(corpo["pergunta"], PERGUNTA_FALHA)
                self.assertEqual(corpo["pergunta"], "descreve em 1 linha")

    async def test_foto_entra_na_chamada_e_nao_fica_em_disco_nem_log(self) -> None:
        sentinela = "IMAGEM-SENTINELA-NAO-PERSISTIR-7f3a"
        payload = {
            "kcal": 220,
            "p": 8,
            "c": 20,
            "g": 9,
            "confianca": "medio",
            "pergunta": "era uma foto de pao?",
            "itens": [{"nome": "pao", "g": 50, "kcal": 130}],
        }
        capturado: list[httpx2.Request] = []
        coletor = _Coletor()
        logging.getLogger().addHandler(coletor)
        try:
            app = self._app(_responde(payload, capturado))
            async with _cliente(app) as cliente:
                resposta = await cliente.post(
                    "/v1/estimate",
                    headers={"X-Invite": INVITE},
                    json={"text": CAFE, "image_b64": sentinela},
                )
        finally:
            logging.getLogger().removeHandler(coletor)
        self.assertEqual(resposta.status_code, 200)
        self.assertIn(sentinela, capturado[0].content.decode())
        self.assertNotIn(sentinela, resposta.text)
        self.assertNotIn(CHAVE_FALSA, resposta.text)
        for linha in coletor.linhas:
            self.assertNotIn(sentinela, linha)
            self.assertNotIn(CHAVE_FALSA, linha)
        _assert_fora_do_disco(sentinela)

    async def test_convite_ausente_ou_errado_rejeita_sem_chamar_modelo(self) -> None:
        for cabecalhos in ({}, {"X-Invite": "errado"}):
            with self.subTest(cabecalhos=cabecalhos):
                capturado: list[httpx2.Request] = []
                app = self._app(_responde({"kcal": 1}, capturado))
                async with _cliente(app) as cliente:
                    resposta = await cliente.post(
                        "/v1/estimate",
                        headers=cabecalhos,
                        json={"text": CAFE},
                    )
                self.assertEqual(resposta.status_code, 401)
                self.assertEqual(capturado, [])

    async def test_fit_surprise_tem_duas_opcoes_e_uma_pergunta(self) -> None:
        payload = {
            "pergunta": "qual dos dois?",
            "opcoes": [
                {
                    "nome": "pao com ovo",
                    "porcoes": [{"nome": "pao", "quantidade": "1 unidade"}],
                    "kcal": 280,
                    "p": 14,
                },
                {
                    "nome": "iogurte",
                    "porcoes": [{"nome": "iogurte", "quantidade": "1 pote"}],
                    "kcal": 150,
                    "p": 12,
                },
            ],
        }
        app = self._app(_responde(payload, []))
        async with _cliente(app) as cliente:
            corpo = (
                await cliente.post(
                    "/v1/fit",
                    headers={"X-Invite": INVITE},
                    json={
                        "mode": "surprise",
                        "text": "algo rapido",
                        "itens_disponiveis": ["ovo", "pao"],
                        "orcamento": {"kcal": 455, "p": 49},
                        "image_b64": None,
                    },
                )
            ).json()
        self.assertEqual(len(corpo["opcoes"]), 2)
        self.assertEqual(corpo["pergunta"], payload["pergunta"])
        self.assertIsInstance(corpo["pergunta"], str)
        self.assertNotIn("perguntas", corpo)
        self.assertEqual(corpo["opcoes"][0]["porcoes"], payload["opcoes"][0]["porcoes"])
        self.assertEqual(corpo["opcoes"][1]["porcoes"], payload["opcoes"][1]["porcoes"])
        self.assertTrue(corpo["cabe"])
        self.assertIn("nome", corpo["prato"])
        self.assertIn("porcoes", corpo["prato"])

    async def test_prato_acima_do_orcamento_nao_cabe(self) -> None:
        payload = {
            "pergunta": "aceita um prato menor?",
            "prato": {
                "nome": "hamburguer",
                "porcoes": [{"nome": "pao", "quantidade": "1 unidade"}],
                "kcal": 900,
                "p": 35,
            },
        }
        app = self._app(_responde(payload, []))
        async with _cliente(app) as cliente:
            corpo = (
                await cliente.post(
                    "/v1/fit",
                    headers={"X-Invite": INVITE},
                    json={
                        "mode": "want",
                        "text": "quero hamburguer",
                        "itens_disponiveis": [],
                        "orcamento": {"kcal": 100, "p": 20},
                    },
                )
            ).json()
        self.assertEqual(corpo["prato"]["kcal"], 900)
        self.assertFalse(corpo["cabe"])
        self.assertEqual(corpo["pergunta"], payload["pergunta"])
        self.assertNotIn("opcoes", corpo)

    async def test_opcao_surprise_acima_do_orcamento_nao_cabe(self) -> None:
        payload = {
            "pergunta": "fico com o menor?",
            "opcoes": [
                {
                    "nome": "grande",
                    "porcoes": [{"nome": "pao", "quantidade": "2 unidades"}],
                    "kcal": 800,
                    "p": 30,
                },
                {
                    "nome": "pequeno",
                    "porcoes": [{"nome": "ovo", "quantidade": "1 unidade"}],
                    "kcal": 90,
                    "p": 7,
                },
            ],
        }
        app = self._app(_responde(payload, []))
        async with _cliente(app) as cliente:
            corpo = (
                await cliente.post(
                    "/v1/fit",
                    headers={"X-Invite": INVITE},
                    json={
                        "mode": "surprise",
                        "text": "algo leve",
                        "itens_disponiveis": [],
                        "orcamento": {"kcal": 455, "p": 49},
                    },
                )
            ).json()
        self.assertEqual(len(corpo["opcoes"]), 2)
        self.assertEqual(corpo["pergunta"], payload["pergunta"])
        acima = [opcao for opcao in corpo["opcoes"] if opcao["kcal"] > 455]
        dentro = [opcao for opcao in corpo["opcoes"] if opcao["kcal"] <= 455]
        self.assertEqual(len(acima), 1)
        self.assertFalse(acima[0]["cabe"])
        self.assertTrue(dentro[0]["cabe"])
        self.assertLessEqual(corpo["prato"]["kcal"], 455)
        self.assertTrue(corpo["cabe"])

    def _app(self, handler) -> object:
        app = main.create_app(transport=httpx2.MockTransport(handler))
        self._apps.append(app)
        return app


def _cliente(app: object) -> httpx.AsyncClient:
    return httpx.AsyncClient(
        transport=httpx.ASGITransport(app=app),
        base_url="http://test",
    )


def _envelope(texto: str) -> dict:
    return {
        "id": "resp_test",
        "object": "response",
        "created_at": 0,
        "model": "gpt-6-luna",
        "parallel_tool_calls": True,
        "tool_choice": "none",
        "tools": [],
        "output": [
            {
                "id": "msg_test",
                "type": "message",
                "role": "assistant",
                "status": "completed",
                "content": [
                    {"type": "output_text", "text": texto, "annotations": []},
                ],
            }
        ],
    }


def _responde(payload: dict, capturado: list[httpx2.Request]):
    def handler(request: httpx2.Request) -> httpx2.Response:
        capturado.append(request)
        return httpx2.Response(
            200,
            json=_envelope(json.dumps(payload, ensure_ascii=False)),
        )

    return handler


def _estoura(erro: Exception):
    def handler(request: httpx2.Request) -> httpx2.Response:
        raise erro

    return handler


def _recusa(request: httpx2.Request) -> httpx2.Response:
    raise AssertionError("health nao chama o modelo")


class _Coletor(logging.Handler):
    def __init__(self) -> None:
        super().__init__()
        self.linhas: list[str] = []

    def emit(self, record: logging.LogRecord) -> None:
        self.linhas.append(record.getMessage())


def _assert_fora_do_disco(sentinela: str) -> None:
    blob = sentinela.encode()
    raiz = Path(__file__).resolve().parents[1]
    for caminho in _arquivos_do_servidor(raiz):
        if blob in caminho.read_bytes():
            raise AssertionError(f"foto persistida em {caminho.name}")


def _arquivos_do_servidor(diretorio: Path):
    ignorar = {"tests", ".venv", "__pycache__"}
    for caminho in diretorio.iterdir():
        if caminho.name in ignorar:
            continue
        if caminho.is_dir():
            yield from _arquivos_do_servidor(caminho)
        elif caminho.is_file():
            yield caminho


if __name__ == "__main__":
    unittest.main()
