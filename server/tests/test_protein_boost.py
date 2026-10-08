"""Protein boost (ADR-043 decision 4, S24): server arithmetic over a named dish. Synthetic foods and numbers."""

import protein_boost as pb


def plan(kcal=200, p=6, c=30, g=5, text='2 torradas com geleia e um chá', items=None, intent='plan', reply='Pode ser.'):
    return dict(
        reply=reply, intent=intent, meal_day='today', scope='in_scope',
        estimate=dict(kcal=kcal, p=p, c=c, g=g, confidence='medium', question=None, suggested_slot='j',
                      meal_text=text, items=items if items is not None else [dict(name='torrada', g=50, kcal=140), dict(name='geleia', g=20, kcal=60)]),
    )


def test_low_protein_dish_with_room_gets_shredded_chicken_first():
    payload = plan()
    record = pb.apply(payload, window_kcal=700, remaining_p=110)
    assert record is not None
    assert [e['name'] for e in record['added']] == ['frango desfiado']
    assert record['added'][0]['g'] == 150  # capped at MAX_GRAMS: room (500 kcal) and gap (104 g) both allow more
    est = payload['estimate']
    assert est['items'][-1] == dict(name='frango desfiado (opcional)', g=150, kcal=239)
    assert est['kcal'] == 200 + 239
    assert est['p'] == 6 + 48
    assert est['meal_text'].endswith('; 150 g de frango desfiado (opcional)')
    assert payload['reply'].endswith('Para a proteína (opcional): 150 g de frango desfiado, +239 kcal · P 48 g.')


def test_dish_already_above_the_floor_is_untouched():
    payload = plan(p=40)
    assert pb.apply(payload, window_kcal=700, remaining_p=110) is None
    assert payload['estimate']['items'][-1]['name'] == 'geleia'


def test_no_room_in_the_window_adds_nothing():
    payload = plan(kcal=250)
    assert pb.apply(payload, window_kcal=300, remaining_p=90) is None
    assert '(opcional)' not in payload['reply']


def test_dish_with_chicken_gets_eggs_instead():
    payload = plan(text='sanduíche de frango', items=[dict(name='pão', g=50, kcal=150), dict(name='frango grelhado', g=30, kcal=48)], p=12)
    record = pb.apply(payload, window_kcal=600, remaining_p=100)
    assert [e['name'] for e in record['added']] == ['ovo cozido']
    assert record['added'][0]['g'] % 50 == 0


def test_second_food_only_while_still_below_the_floor():
    # Small room: 80 kcal fit 50 g of chicken (80 kcal, 16 g P); the floor is 33 g and 16 + 6 stays below it,
    # but no second food fits the remaining room, so one item only.
    payload = plan(kcal=220)
    record = pb.apply(payload, window_kcal=300, remaining_p=110)
    assert record is not None
    assert len(record['added']) == 1 and record['added'][0]['g'] == 50


def test_two_foods_when_the_first_is_capped_and_the_dish_is_still_below_the_floor():
    payload = plan(p=2)
    record = pb.apply(payload, window_kcal=1200, remaining_p=200)  # floor 60 g; 150 g chicken gives 48 g
    assert [e['name'] for e in record['added']] == ['frango desfiado', 'ovo cozido']
    assert payload['estimate']['p'] == 2 + 48 + record['added'][1]['p']


def test_only_plans_of_today_with_missing_protein():
    assert pb.apply(plan(intent='log'), 700, 110) is None
    assert pb.apply(plan(), 700, 0) is None
    assert pb.apply(plan(), None, 110) is None
    assert pb.apply({'intent': 'plan', 'estimate': None}, 700, 110) is None


def test_grams_step_and_caps():
    chicken = pb.FOODS[0]
    assert pb._grams(chicken, room_kcal=500, gap_p=100) == 150
    assert pb._grams(chicken, room_kcal=100, gap_p=100) == 50  # 100 kcal buys 62 g -> 50 g step
    assert pb._grams(chicken, room_kcal=30, gap_p=100) == 0
    egg = pb.FOODS[1]
    assert pb._grams(egg, room_kcal=200, gap_p=10) == 50  # 10 g of protein is 76 g of egg -> one 50 g egg


def test_one_small_food_added_to_a_stated_dish_is_not_a_dish():
    # S33 ONE ITEM: "posso pôr uma colher de azeite?" is answered with the oil alone; no boost on it.
    payload = plan(kcal=119, p=0, c=0, g=13, text='13 g de azeite', items=[dict(name='azeite', g=13, kcal=119)])
    assert pb.apply(payload, window_kcal=700, remaining_p=80) is None
    single_dish = plan(kcal=320, p=8, text='1 tapioca com coco', items=[dict(name='tapioca com coco', g=120, kcal=320)])
    assert pb.apply(single_dish, window_kcal=700, remaining_p=80) is not None
