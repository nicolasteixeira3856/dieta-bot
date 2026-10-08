"""S32: an occupied target and the answer to the add-or-replace question never end in the error fallback.
All food data is synthetic."""

import pytest

import main
from meal_changes import occupied_question
from tests.test_meal_changes import delta, model, request


def turn(raw, req=None):
    return main.shape_chat_turn(main.ChatIn.model_validate(req or request()), raw)


def test_question_carries_the_draft_for_the_answer_turn():
    slot, state = dict(name='Lanche'), dict(text='150 g de fruta', kcal=60.4)
    draft = dict(meal_text='1 pote de coalhada (170 g) e 1 maçã pequena (110 g)', kcal=223)
    assert occupied_question(slot, state, draft) == (
        'Lanche de hoje já tem registro: 150 g de fruta (60 kcal). Somo 1 pote de coalhada (170 g) e '
        '1 maçã pequena (110 g), ~223 kcal, a esse registro ou substituo o registro por isso?')
    assert occupied_question(slot, state, dict(meal_text='  ', kcal=10)) == (
        'Lanche de hoje já tem registro: 150 g de fruta (60 kcal). Somo a esse registro ou substituo?')
    long = occupied_question(slot, state, dict(meal_text='arroz, ' * 100, kcal=float('nan')))
    assert '…' in long and '~' not in long


def test_add_with_null_addition_uses_the_draft():
    raw = model(meal_change=dict(operation='add', base_slot='cafe', addition=None))
    out, _, record_log = turn(raw)
    assert out['meal_change']['operation'] == 'add'
    assert out['meal_change']['addition']['items'] == delta()['items']
    assert out['estimate']['kcal'] == 341.25
    assert out['record'] == 'auto'


def test_add_with_null_addition_and_zero_draft_asks_the_model_question():
    # Incident 2026-10-08 (request 4e47517c), synthetic foods: "Pode somar" answered with zeros and a question.
    raw = model(meal_change=dict(operation='add', base_slot='cafe', addition=None))
    raw['estimate'].update(kcal=0, p=0, c=0, g=0, items=[], confidence='low', question='Qual a porção da fruta?')
    out, clarify, _ = turn(raw)
    assert out['estimate'] is None and out['meal_change'] is None
    assert out['question'] == out['reply'] == 'Qual a porção da fruta?'
    assert out['record'] == 'none'
    assert clarify == main.CLARIFY_ASKED


def test_add_with_null_addition_zero_draft_and_no_question_still_fails():
    raw = model(meal_change=dict(operation='add', base_slot='cafe', addition=None))
    raw['estimate'].update(kcal=0, p=0, c=0, g=0, items=[])
    with pytest.raises(ValueError):  # the route turns it into the error fallback, as before S32
        turn(raw)


def test_invalid_present_addition_is_not_replaced_by_the_draft():
    raw = model()
    raw['meal_change']['addition']['items'] = []
    with pytest.raises(ValueError):
        turn(raw)


def test_null_base_on_occupied_target_takes_the_target():
    out, _, _ = turn(model(meal_change=dict(operation='add', base_slot=None, addition=delta())))
    assert out['meal_change']['base_slot'] == 'cafe'
    assert out['estimate']['kcal'] == 341.25
    raw = model(meal_change=dict(operation='revise', base_slot=None, addition=None))
    raw['estimate'].update(meal_text='50 g de fruta e 1 iogurte')
    out, _, _ = turn(raw)
    assert out['meal_change']['operation'] == 'revise' and out['meal_change']['base_slot'] == 'cafe'
    assert out['estimate']['kcal'] == 40
    assert out['record'] == 'auto'


def test_null_base_on_empty_target_stays_unknown_target_add():
    req = request()
    req['day']['slots'] = [dict(id='cafe', status='empty')]
    out, _, _ = turn(model(meal_change=dict(operation='add', base_slot=None, addition=delta())), req)
    assert out['meal_change']['base_slot'] is None
    assert out['estimate']['kcal'] == 40


def test_operation_without_estimate_is_the_unresolved_question():
    raw = model(estimate=None, reply='Somo ou substituo?')
    out, clarify, _ = turn(raw)
    assert out['estimate'] is None and out['meal_change'] is None
    assert out['question'] == 'Somo ou substituo?'
    assert clarify == main.CLARIFY_ASKED


def test_addition_total_is_the_sum_of_its_items():
    # Eval 2026-10-08 (s32-occupied-ambiguous): addition kcal 145 with items 87 + 56.
    raw = model()
    raw['meal_change']['addition'].update(kcal=45, p=2, c=10, g=0)
    out, _, _ = turn(raw)
    assert out['meal_change']['addition']['kcal'] == 40
    assert out['estimate']['kcal'] == 341.25
    assert out['record'] == 'auto'
