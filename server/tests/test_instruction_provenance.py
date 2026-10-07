"""Provenance declarations and request isolation; semantic origin needs source review."""
from dataclasses import replace
import json
import unicodedata
import unittest

import pytest

from chat_instructions import (BRANCHES, CUE_LOCALE, CUE_MAX_WORDS, CUES, EXAMPLES, RULES, Example,
                               assemble, validate_assembled, validate_inventory)
from evals.run import load_cases
from llm import chat_instructions
import llm
import main
from tests.test_api import INVITE, _client, _mock, _responds
from tests.test_chat import _base_chat_payload


def declared_example():
    example = Example("independent_synthetic", "intent", "Test registry assembly syntax",
                      "Illustration: an abstract slot identifier is data.", ("legacy",))
    examples = {**EXAMPLES, "syntax-fixture": example}
    branches = {**BRANCHES, "legacy": BRANCHES["legacy"] + (("example", "syntax-fixture"),)}
    return examples, branches


def test_all_live_branches_account_for_owned_rules_and_examples():
    validate_inventory(RULES, EXAMPLES, BRANCHES)
    assert chat_instructions() == assemble("legacy")
    assert chat_instructions(meal_changes=True) == assemble("meal_changes")
    for branch in BRANCHES:
        assert assemble(branch)


def test_declared_example_is_assembled_only_in_its_declared_branch():
    examples, branches = declared_example()
    validate_inventory(RULES, examples, branches)
    text = examples["syntax-fixture"].text
    assert assemble("legacy", examples=examples, branches=branches).count(text) == 1
    assert text not in assemble("compact", examples=examples, branches=branches)


@pytest.mark.parametrize("origin", ["unknown", "real_user", "anonymized_user", "", None])
def test_unknown_or_real_origin_is_rejected(origin):
    examples, branches = declared_example()
    examples["syntax-fixture"] = replace(examples["syntax-fixture"], provenance=origin)
    with pytest.raises(ValueError):
        validate_inventory(RULES, examples, branches)


@pytest.mark.parametrize("field,value", [("rule", "absent"), ("purpose", ""), ("text", ""),
                                         ("branches", ()), ("branches", ("compact",))])
def test_example_needs_owned_rule_purpose_and_exact_assembly_coverage(field, value):
    examples, branches = declared_example()
    examples["syntax-fixture"] = replace(examples["syntax-fixture"], **{field: value})
    with pytest.raises(ValueError):
        validate_inventory(RULES, examples, branches)


def test_undeclared_example_and_raw_snippets_cannot_enter_any_branch():
    for branch in BRANCHES:
        for fragment in (("example", "undeclared"), ("text", "a copied conversation"),
                         "a copied conversation"):
            branches = {**BRANCHES, branch: BRANCHES[branch] + (fragment,)}
            with pytest.raises(ValueError):
                assemble(branch, branches=branches)
        prompt = "digest" if branch == "compact" else "chat"
        validate_assembled(prompt, assemble(branch))
        with pytest.raises(ValueError):
            validate_assembled(prompt, assemble(branch) + "\nUSER-SPECIFIC-CONTEXT")


def test_unassembled_example_does_not_silently_pass_inventory():
    examples, _ = declared_example()
    with pytest.raises(ValueError):
        validate_inventory(RULES, examples, BRANCHES)


def _normalized(text):
    plain = "".join(ch for ch in unicodedata.normalize("NFD", text.casefold())
                    if unicodedata.category(ch) != "Mn")
    return " ".join("".join(ch if ch.isalnum() else " " for ch in plain).split())


def test_cues_are_declared_markers_assembled_once_per_declared_branch():
    for cue in CUES.values():
        assert cue.locale == CUE_LOCALE and cue.provenance == "independent_synthetic"
        for marker in cue.markers:
            assert len(marker.split()) <= CUE_MAX_WORDS and not any(ch.isdigit() for ch in marker)
            for branch in BRANCHES:
                assert (marker in assemble(branch)) or branch not in cue.branches
    assert f"{CUE_LOCALE} cues" not in assemble("compact")


@pytest.mark.parametrize("field,value", [
    ("provenance", "unknown"), ("provenance", "real_user"), ("locale", ""), ("locale", "en"),
    ("rule", "absent"), ("purpose", ""), ("meaning", ""), ("markers", ()),
    ("markers", ("comi 200 g",)), ("markers", ("uma frase inteira de usuário",)),
    ("markers", ("agora", "agora")), ("branches", ()), ("branches", ("absent",)),
    ("branches", ("legacy",)),
])
def test_cue_needs_provenance_locale_owner_marker_shape_and_exact_coverage(field, value):
    cues = {**CUES, "eating-now": replace(CUES["eating-now"], **{field: value})}
    if field == "branches" and value == ("legacy",):
        cues = {key: replace(cue, branches=value) for key, cue in CUES.items()}
    with pytest.raises(ValueError):
        validate_inventory(RULES, EXAMPLES, BRANCHES, cues)


def test_undeclared_cue_block_and_unassembled_cue_fail():
    branches = {**BRANCHES, "compact": BRANCHES["compact"] + (("cues", "digest"),)}
    with pytest.raises(ValueError):
        assemble("compact", branches=branches)
    extra = {**CUES, "fixture": replace(CUES["eating-now"], rule="plan")}
    with pytest.raises(ValueError):
        validate_inventory(RULES, EXAMPLES, BRANCHES, extra)


def test_no_cue_equals_a_whole_user_message_of_an_evaluation_fixture():
    messages = set()
    for case in load_cases():
        request = case["request"]
        texts = [request.get("text") or ""]
        texts += [m["text"] for m in request.get("messages", []) if m["role"] == "user"]
        messages.update(_normalized(text) for text in texts if text)
    assert len(messages) > 100
    for key, cue in CUES.items():
        for marker in cue.markers:
            assert _normalized(marker) not in messages, (key, marker)


def test_model_boundary_rejects_context_appended_to_chat_and_digest(monkeypatch):
    client = llm.LlmClient(api_key="")
    try:
        monkeypatch.setattr(llm, "chat_instructions", lambda **_: assemble("legacy") + " PRIVATE DATA")
        with pytest.raises(ValueError, match="unregistered"):
            client.chat_json(user_text="food", image_b64=None, slot_ids=[])
        monkeypatch.setattr(llm, "_DIGEST_INSTRUCTIONS", assemble("compact") + " PRIVATE DATA")
        with pytest.raises(ValueError, match="unregistered"):
            client.digest_json(history_text="food")
    finally:
        client.close()


class RequestIsolationTests(unittest.IsolatedAsyncioTestCase):
    async def test_personal_fields_change_context_and_schema_not_fixed_instructions(self):
        for modern in (False, True):
            calls = []
            for variant in ("ALPHA", "OMEGA"):
                sentinel = "ISOLATION_" + variant
                sid = sentinel + "_SLOT"
                req = _base_chat_payload(
                    text=sentinel + "_CURRENT", memory=sentinel + "_LEGACY_MEMORY",
                    facts=[dict(id="T11", kind="temp", category="portion", key="reference",
                                text=sentinel + "_FACT", slot=None)], temp_facts=True,
                    recent=[dict(date="2026-09-24",slot_id=sid,slot_name=sentinel+"_NAME",
                                 text=sentinel+"_RECENT",kcal=20,p=1,c=2,g=1)],
                    messages=[dict(role="user",text=sentinel+"_HISTORY")],
                    digests=[sentinel+"_DIGEST"],clarify_rounds=0,auto_record=True,
                    meal_changes=modern)
                req["profile"]["slots"]=[dict(id=sid,name=sentinel+"_NAME",time="17:40")]
                req["day"]["slots"]=[dict(id=sid,status="eaten",text=sentinel+"_DAY",kcal=10,p=1,c=1,g=0)]
                if modern:
                    req["pending_addition"]=dict(base_slot=sid,addition=dict(
                        meal_text=sentinel+"_PENDING",kcal=5,p=0,c=1,g=0,
                        items=[dict(name=sentinel+"_ITEM",g=5,kcal=5)]))
                model=dict(reply="Qual alimento?",intent="question",estimate=None,record_intent="unsure",
                           meal_day="today",skip_slots=[],memory_updates=[],memory_used=[],digest=None,
                           scope="in_scope",**({"meal_change":None} if modern else {}))
                captured=[]
                app=main.create_app(transport=_mock(_responds(model,captured)))
                try:
                    async with _client(app) as client:
                        response=await client.post("/v1/chat",headers={"X-Invite":INVITE},json=req)
                    self.assertEqual(response.status_code,200)
                    call=json.loads(captured[0].content)
                    self.assertNotIn(sentinel,call["instructions"])
                    context=call["input"][0]["content"][0]["text"]
                    for suffix in ("CURRENT","FACT","RECENT","HISTORY","DIGEST","DAY","NAME"):
                        self.assertIn(sentinel+"_"+suffix,context)
                    self.assertNotIn(sentinel+"_LEGACY_MEMORY",context)
                    if modern:self.assertIn(sentinel+"_PENDING",context)
                    self.assertIn(sid,json.dumps(call["text"]["format"]))
                    calls.append(call)
                finally:
                    app.state.llm.close()
            self.assertEqual(calls[0]["instructions"],calls[1]["instructions"])

    async def test_compact_accepts_only_history_and_uses_same_registered_prefix(self):
        instructions=[]
        for marker in ("FIRST-HISTORY", "SECOND-HISTORY"):
            req=_base_chat_payload(compact=True,text="PRIVATE-CURRENT",memory="PRIVATE-MEMORY",
                                   messages=[dict(role="user",text=marker)])
            captured=[]
            app=main.create_app(transport=_mock(_responds({"digest":"Resumo alimentar."},captured)))
            try:
                async with _client(app) as client:
                    response=await client.post("/v1/chat",headers={"X-Invite":INVITE},json=req)
                self.assertEqual(response.status_code,200)
                call=json.loads(captured[0].content)
                self.assertEqual(call["instructions"],assemble("compact"))
                self.assertNotIn(marker,call["instructions"])
                context=call["input"][0]["content"][0]["text"]
                self.assertIn(marker,context)
                self.assertNotIn("PRIVATE-",context)
                instructions.append(call["instructions"])
            finally:
                app.state.llm.close()
        self.assertEqual(instructions[0],instructions[1])
