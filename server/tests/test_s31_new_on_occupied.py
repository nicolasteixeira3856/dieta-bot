"""S31: a new meal aimed at an eaten slot asks add or replace instead of failing. Synthetic data only."""

import pytest

import main
from tests.test_meal_changes import model, request


def new_on_occupied():
    raw = model(meal_change=dict(operation='new', base_slot=None, addition=None))
    raw['estimate'].update(meal_text='1 iogurte e 2 frutas', kcal=40)
    return raw


def turn(req):
    return main.shape_chat_turn(main.ChatIn.model_validate(req), new_on_occupied())


def test_new_on_eaten_slot_asks_add_or_replace():
    out, clarify, record_log = turn(request())
    assert out['estimate'] is None
    assert out['meal_change'] is None
    assert out['record'] == 'none'
    assert out['memory_updates'] == []
    assert out['question'] == out['reply']
    assert out['reply'] == 'Cafe da manha de hoje já tem registro: Base, íntegra 🥣 (301 kcal). Somo a esse registro ou substituo?'
    assert clarify == main.CLARIFY_ASKED
    assert record_log == main.RECORD_NONE_INTENT


def test_no_round_left_states_the_choice_without_question():
    out, clarify, _ = turn(request(force_estimate=True, clarify_rounds=3))
    assert out['estimate'] is None
    assert out['question'] is None
    assert out['reply'] == 'Informe se quer acrescentar alimentos ou corrigir a refeição.'
    assert clarify == main.CLARIFY_NONE


def test_long_or_missing_base_text_never_fails():
    req = request()
    req['day']['slots'][0]['text'] = 'arroz, ' * 300
    out, _, _ = turn(req)
    held = out['reply'].split(': ', 1)[1].split(' (', 1)[0]
    assert held.endswith('…') and len(held) <= 60
    req['day']['slots'][0]['text'] = None
    out, _, _ = turn(req)
    assert out['reply'] == 'Cafe da manha de hoje já tem registro (301 kcal). Somo a esse registro ou substituo?'


@pytest.mark.parametrize('status', ['empty', 'skipped', 'planned'])
def test_unoccupied_slot_still_records_new(status):
    req = request()
    req['day']['slots'] = [dict(id='cafe', status=status, text='plano', kcal=100, p=1, c=1, g=1)
                           if status == 'planned' else dict(id='cafe', status=status)]
    out, _, _ = turn(req)
    assert out['estimate']['kcal'] == 40
    assert out['record'] == 'auto'


def test_error_record_logs_only_fixed_server_reasons():
    assert main._error_record(ValueError('new meal cannot overwrite occupied target')) == {
        'type': 'ValueError', 'reason': 'new meal cannot overwrite occupied target'}
    assert main._error_record(ValueError("could not convert string to float: 'abc'")) == {'type': 'ValueError'}
    assert main._error_record(ValueError('Texto Do Usuario')) == {'type': 'ValueError'}
    assert main._error_record(ValueError('a', 'b')) == {'type': 'ValueError'}
    import json
    assert main._error_record(json.JSONDecodeError('no json', '', 0)) == {'type': 'JSONDecodeError'}
    assert main._error_record(KeyError('estimate')) == {'type': 'KeyError'}
