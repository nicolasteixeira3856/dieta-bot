"""S28: a whole-meal estimate whose items carry no usable grams keeps its totals instead of failing."""

import pytest
from pydantic import ValidationError

from meal_changes import totals_only, usable_items
from tests.test_meal_changes import delta, model, request, shape


def _copied(**kw):
    """A copied record as the model returns it: one item with the whole text and no grams."""
    raw = model(meal_change=dict(operation='new', base_slot=None, addition=None))
    raw['estimate'] = dict(
        meal_text='2 ovos mexidos, 1 pão (40 g), 250 ml de leite', kcal=490, p=29, c=43, g=22,
        confidence='high', question=None, suggested_slot='cafe',
        items=[dict(name='2 ovos mexidos, 1 pão (40 g), 250 ml de leite', g=0, kcal=490)],
    )
    raw['estimate'].update(kw)
    return raw


def _empty_request():
    req = request()
    req['day']['slots'] = [dict(id='cafe', status='empty')]
    return req


def test_new_copied_record_without_grams_keeps_totals_and_empty_items():
    out = shape(_empty_request(), _copied())
    assert out['reply'] == 'Estimativa.'
    assert out['estimate']['kcal'] == 490
    assert [out['estimate'][k] for k in ('p', 'c', 'g')] == [29, 43, 22]
    assert out['estimate']['items'] == []
    assert out['estimate']['meal_text'] == '2 ovos mexidos, 1 pão (40 g), 250 ml de leite'
    assert out['record'] == 'auto'
    assert out['meal_change'] == dict(operation='new', base_slot=None, addition=None)


def test_new_copy_of_a_recent_record_keeps_its_numbers_and_drops_invented_items():
    # The model copied yesterday's 430 kcal breakfast (a RECENT row) and invented item grams summing to 416.
    req = _empty_request()
    req['recent'] = [dict(date='2026-09-29', slot_id='cafe', slot_name='Cafe', text='pão, ovos, leite',
                          kcal=430, p=24, c=38, g=20)]
    raw = _copied(kcal=430, p=24, c=38, g=20, meal_text='pão, ovos, leite',
                  items=[dict(name='pão', g=50, kcal=150), dict(name='ovos', g=100, kcal=146),
                         dict(name='leite', g=200, kcal=120)])
    out = shape(req, raw)
    assert out['estimate']['kcal'] == 430
    assert [out['estimate'][k] for k in ('p', 'c', 'g')] == [24, 38, 20]
    assert out['estimate']['items'] == []
    assert out['record'] == 'auto'


def test_new_drops_only_the_unusable_items_and_keeps_the_sum():
    raw = _copied(items=[dict(name='ovos', g=100, kcal=146), dict(name='resto', g=0, kcal=344)])
    out = shape(_empty_request(), raw)
    # ADR-042: the total is the sum of the usable items; the 0 g item never counted.
    assert out['estimate']['kcal'] == 146
    assert out['estimate']['items'] == [dict(name='ovos', g=100, kcal=146)]
    assert out['record'] == 'auto'


def test_revise_drops_a_zero_gram_item():
    raw = _copied(items=[dict(name='ovos', g=100, kcal=146), dict(name='sem peso', g=0, kcal=10)])
    raw['meal_change'] = dict(operation='revise', base_slot='cafe', addition=None)
    out = shape(request(), raw)
    assert out['estimate']['kcal'] == 146
    assert out['estimate']['items'] == [dict(name='ovos', g=100, kcal=146)]
    assert out['reply'].startswith('Atualizar Cafe da manha?')


def test_add_still_needs_usable_items():
    # The validation of a delta is unchanged: the route turns this into the fallback, as before.
    raw = model()
    raw['meal_change']['addition'] = delta(items=[dict(name='fruta', g=0, kcal=40)])
    with pytest.raises((ValueError, ValidationError)):
        shape(payload=raw)


@pytest.mark.parametrize('bad', [dict(kcal=float('inf')), dict(p=-1), dict(meal_text='')])
def test_totals_still_validate_the_numbers(bad):
    with pytest.raises((ValueError, ValidationError)):
        shape(_empty_request(), _copied(**bad))


def test_usable_items_filter():
    items = [
        dict(name='a', g=10, kcal=5), dict(name='b', g=0, kcal=5), dict(name='c', g=-1, kcal=5),
        dict(name='d', g=float('nan'), kcal=5), dict(name='e', g=10, kcal=-1), dict(name='f', g=True, kcal=5),
        'not a dict', dict(name='g', g=10, kcal=0),
    ]
    assert [it['name'] for it in usable_items(items)] == ['a', 'g']
    assert usable_items(None) == []


def test_totals_rounds_once_and_returns_no_items():
    out = totals_only(dict(meal_text='x', kcal=489.5, p=28.5, c=43.4, g=22))
    assert out == dict(meal_text='x', kcal=490, p=29, c=43, g=22, items=[])
