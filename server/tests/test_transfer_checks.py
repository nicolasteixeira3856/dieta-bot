"""Transfer checks reject fallback, unsupported memory and mismatched ledgers."""
from copy import deepcopy

import pytest

from evals.checks import evaluate
from evals.run import load_cases


def meal():
    return {"estimate": {"kcal": 90, "p": 2, "c": 18, "g": 1,
                         "items": [{"name": "synthetic food", "g": 75, "kcal": 90}]},
            "memory_used": ["T8"], "memory_updates": []}


@pytest.mark.parametrize("key,want", [
    ("estimate_values", {"kcal": 90, "p": 2}),
    ("item_portions", {"synthetic": 75}),
])
def test_nutrition_requires_items_and_exact_values(key, want):
    assert evaluate({key: want}, meal())[key]["status"] == "pass"
    for output in ({}, {"estimate": None}, {"estimate": {"kcal": 90, "items": []}}):
        assert evaluate({key: want}, output)[key]["status"] == "fail"
    for field, value in (("g", 0), ("g", True), ("g", float("inf")), ("kcal", 89)):
        output = deepcopy(meal())
        output["estimate"]["items"][0][field] = value
        assert evaluate({key: want}, output)[key]["status"] == "fail"
    assert evaluate({"item_portions": {"absent": None}}, meal())["item_portions"]["status"] == "fail"
    assert evaluate({"estimate_values": {"p": 3}}, meal())["estimate_values"]["status"] == "fail"


def test_memory_allowlist_is_not_presence_check():
    assert evaluate({"memory_used_only": ["T8"]}, meal())["memory_used_only"]["status"] == "pass"
    assert evaluate({"memory_used_only": []}, meal())["memory_used_only"]["status"] == "fail"
    assert evaluate({"memory_used_only": []}, {})["memory_used_only"]["status"] == "fail"


def test_partial_reference_requires_product_basis_and_given_values_in_same_fact():
    want = [{"match": {"kind": "temp", "category": "portion"},
             "has": ["Fictional", "30 g", "95 kcal", "4 g"]}]
    output = {"memory_updates": [{"kind": "temp", "category": "portion",
                                 "text": "Fictional: 95 kcal e 4 g P por 30 g"}]}
    assert evaluate({"memory_update_text": want}, output)["memory_update_text"]["status"] == "pass"
    for text in ("Fictional: 95 kcal por 100 g", "95 kcal e 4 g P por 30 g"):
        output["memory_updates"][0]["text"] = text
        assert evaluate({"memory_update_text": want}, output)["memory_update_text"]["status"] == "fail"
    assert evaluate({"memory_update_text": want}, {})["memory_update_text"]["status"] == "fail"


def test_transfer_inventory_keeps_pairs_continuations_photos_and_reserved_coverage():
    cases = [case for case in load_cases() if "s19" in case.get("tags", [])]
    paired = [case for case in cases if "s19-pairs" in case["tags"]]
    groups = {case["pair"] for case in paired}
    assert len(groups) >= 12
    for pair in groups:
        assert {case["variant"] for case in paired if case["pair"] == pair} == {0, 1}
    assert len({case["pair"] for case in paired if case["request"]["messages"]
                and not case["request"].get("compact")}) >= 4
    assert len({case["pair"] for case in paired if case["request"].get("compact")}) >= 2
    assert any(case.get("image") for case in paired)
    assert len([case for case in cases if "s19-reserved" in case["tags"]]) >= 8
    assert {len(case["request"]["profile"]["slots"]) for case in paired} >= {3, 6}
    for case in cases:
        assert case["strict"] and set(case["required"]) == set(case["expect"])
        expect = case["expect"]
        assert (expect.get("estimate") == "present" or expect.get("digest") == "present"
                or "memory_update_text" in expect or expect.get("top_question") == "present")
