"""Monta o JSON do contrato a partir do payload do modelo. Sem segunda conta nutricional."""

from __future__ import annotations

from typing import Any

from config import MODEL, PERGUNTA_FALHA


def montar_estimate(payload: dict[str, Any]) -> dict[str, Any]:
    confianca = _confianca(payload.get("confianca"))
    corpo: dict[str, Any] = {
        "kcal": _numero(payload.get("kcal")),
        "p": _numero(payload.get("p")),
        "c": _numero(payload.get("c")),
        "g": _numero(payload.get("g")),
        "confianca": confianca,
        "itens": [_item(item) for item in _lista(payload.get("itens"))],
        "model": MODEL,
    }
    if confianca != "alto":
        pergunta = payload.get("pergunta")
        corpo["pergunta"] = pergunta.strip() if isinstance(pergunta, str) and pergunta.strip() else PERGUNTA_FALHA
    return corpo


def falha_estimate() -> dict[str, Any]:
    return {
        "kcal": 0,
        "p": 0,
        "c": 0,
        "g": 0,
        "confianca": "baixa",
        "pergunta": PERGUNTA_FALHA,
        "itens": [],
        "model": MODEL,
    }


def montar_fit(payload: dict[str, Any], *, orcamento_kcal: float, mode: str) -> dict[str, Any]:
    pergunta = payload.get("pergunta")
    if not isinstance(pergunta, str) or not pergunta.strip():
        raise ValueError("fit sem pergunta")
    if mode == "surprise":
        bruto = payload.get("opcoes")
        if not isinstance(bruto, list) or len(bruto) != 2:
            raise ValueError("surprise precisa de 2 opcoes")
        opcoes = [_opcao(item, orcamento_kcal) for item in bruto]
        encaixam = [opcao for opcao in opcoes if opcao["cabe"]]
        escolhido = encaixam[0] if encaixam else opcoes[0]
        return {
            "prato": _prato(escolhido),
            "cabe": escolhido["cabe"],
            "pergunta": pergunta.strip(),
            "opcoes": opcoes,
        }
    prato = _opcao(payload.get("prato"), orcamento_kcal)
    return {
        "prato": _prato(prato),
        "cabe": prato["cabe"],
        "pergunta": pergunta.strip(),
    }


def falha_fit(mode: str) -> dict[str, Any]:
    vazio = {"nome": "", "porcoes": [], "kcal": 0, "p": 0}
    corpo: dict[str, Any] = {
        "prato": dict(vazio),
        "cabe": False,
        "pergunta": PERGUNTA_FALHA,
    }
    if mode == "surprise":
        corpo["opcoes"] = [{**vazio, "cabe": False}, {**vazio, "cabe": False}]
    return corpo


def _confianca(valor: Any) -> str:
    if not isinstance(valor, str):
        raise TypeError("confianca ausente")
    normal = valor.strip().lower()
    mapa = {
        "alto": "alto",
        "alta": "alto",
        "medio": "medio",
        "média": "medio",
        "media": "medio",
        "baixa": "baixa",
        "baixo": "baixa",
    }
    if normal not in mapa:
        raise ValueError("confianca desconhecida")
    return mapa[normal]


def _numero(valor: Any) -> int | float:
    if isinstance(valor, bool):
        raise TypeError("numero ausente")
    if isinstance(valor, (int, float)):
        return valor
    if isinstance(valor, str):
        try:
            numero = float(valor.strip().replace(",", "."))
        except ValueError:
            raise TypeError("numero ausente") from None
        if numero.is_integer():
            return int(numero)
        return numero
    raise TypeError("numero ausente")


def _lista(valor: Any) -> list[Any]:
    if valor is None:
        return []
    if not isinstance(valor, list):
        raise TypeError("lista ausente")
    return valor


def _item(raw: Any) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise TypeError("item invalido")
    return {"nome": str(raw["nome"]), "g": _numero(raw.get("g")), "kcal": _numero(raw.get("kcal"))}


def _opcao(raw: Any, orcamento_kcal: float) -> dict[str, Any]:
    if not isinstance(raw, dict):
        raise TypeError("prato invalido")
    porcoes = []
    for porcao in _lista(raw.get("porcoes")):
        if not isinstance(porcao, dict):
            raise TypeError("porcao invalida")
        porcoes.append({"nome": str(porcao["nome"]), "quantidade": str(porcao["quantidade"])})
    kcal = _numero(raw.get("kcal"))
    return {
        "nome": str(raw.get("nome") or ""),
        "porcoes": porcoes,
        "kcal": kcal,
        "p": _numero(raw.get("p")),
        "cabe": kcal <= orcamento_kcal,
    }


def _prato(opcao: dict[str, Any]) -> dict[str, Any]:
    return {
        "nome": opcao["nome"],
        "porcoes": opcao["porcoes"],
        "kcal": opcao["kcal"],
        "p": opcao["p"],
    }
