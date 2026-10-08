"""S34 (ADR-055): the model proposes (opcional) protein foods, protein_boost.validate checks and completes them.

Synthetic dishes and numbers in the shapes the model returns.
"""

import protein_boost as pb

DISH = [dict(name='pão sírio', g=70, kcal=190), dict(name='homus', g=60, kcal=150), dict(name='salada', g=80, kcal=20)]
LINE = 'Para a proteína (opcional): {}, +{} kcal · P {} g.'


def plan(extra=(), line=None, meal_text='70 g de pão sírio; 60 g de homus; 80 g de salada', p=10, intent='plan',
         meal_day='today', reply='Wrap de homus: **360 kcal** · P 10 g.'):
    items = [dict(i) for i in DISH] + [dict(i) for i in extra]
    kcal = sum(i['kcal'] for i in items)
    if line:
        reply = f'{reply}\n{line}'
    return dict(reply=reply, intent=intent, meal_day=meal_day, scope='in_scope',
                estimate=dict(kcal=kcal, p=p, c=45, g=14, confidence='medium', question=None, suggested_slot='j',
                              meal_text=meal_text, items=items))


def test_valid_model_food_survives_with_table_numbers():
    payload = plan(extra=[dict(name='atum em lata (opcional)', g=100, kcal=120)],
                   meal_text='70 g de pão sírio; 60 g de homus; 80 g de salada; 100 g de atum em lata (opcional)',
                   p=36, line=LINE.format('100 g de atum em lata', 120, 26))
    record = pb.validate(payload, window_kcal=700, remaining_p=100)
    assert record['source'] == 'model' and record['dropped'] == []
    assert record['added'] == [dict(name='atum em lata', g=100, kcal=117, p=26)]
    est = payload['estimate']
    assert est['items'][-1] == dict(name='atum em lata (opcional)', g=100, kcal=117)
    assert est['kcal'] == 360 + 117 and est['p'] == 10 + 26
    assert est['meal_text'] == '70 g de pão sírio; 60 g de homus; 80 g de salada; 100 g de atum em lata (opcional)'
    assert payload['reply'].endswith('Para a proteína (opcional): 100 g de atum em lata, +117 kcal · P 26 g.')
    assert payload['reply'].count('Para a proteína') == 1


def test_item_without_the_mark_is_found_by_the_line():
    payload = plan(extra=[dict(name='ovo cozido', g=50, kcal=73)], p=16,
                   meal_text='70 g de pão sírio; 60 g de homus; 80 g de salada; 50 g de ovo cozido',
                   line='**Para a proteína (opcional):** 50 g de ovo cozido, +73 kcal · P 7 g.')
    record = pb.validate(payload, window_kcal=700, remaining_p=100)
    assert [a['name'] for a in record['added']] == ['ovo cozido']
    est = payload['estimate']
    assert est['items'][-1]['name'] == 'ovo cozido (opcional)'
    assert est['meal_text'].count('ovo cozido') == 1 and est['meal_text'].endswith('50 g de ovo cozido (opcional)')


def test_two_foods_kept_a_third_and_a_repeat_dropped():
    extra = [dict(name='ovo cozido (opcional)', g=50, kcal=73), dict(name='queijo cottage (opcional)', g=50, kcal=49),
             dict(name='ovos (opcional)', g=50, kcal=73), dict(name='tofu (opcional)', g=50, kcal=38)]
    line = LINE.format('50 g de ovo cozido e 50 g de queijo cottage e 50 g de ovos e 50 g de tofu', 233, 23)
    payload = plan(extra=extra, line=line, p=33)
    record = pb.validate(payload, window_kcal=900, remaining_p=120)
    assert [a['name'] for a in record['added']] == ['ovo cozido', 'queijo cottage']
    assert [d['reason'] for d in record['dropped']] == ['repeat', 'third']
    assert len(payload['estimate']['items']) == len(DISH) + 2


def test_vegetarian_fact_drops_fish_and_the_server_completes_without_meat():
    facts = [dict(id='P1', kind='permanent', category='preference', key='dieta', text='Sou vegetariana')]
    payload = plan(extra=[dict(name='sardinha em lata (opcional)', g=50, kcal=143)], p=18,
                   line=LINE.format('50 g de sardinha em lata', 143, 8))
    record = pb.validate(payload, window_kcal=900, remaining_p=120, facts=facts)
    assert record['dropped'] == [dict(name='sardinha em lata', reason='diet')]
    # Nothing survived and the boost is due: the S24 table appends a food the diet allows (egg, not chicken).
    assert record['source'] == 'server' and [a['name'] for a in record['added']] == ['ovo cozido']
    assert 'sardinha' not in payload['estimate']['meal_text']
    assert payload['reply'].count('Para a proteína') == 1


def test_excluded_food_fact():
    facts = [dict(kind='permanent', category='preference', text='não como peixe nem frutos do mar')]
    groups, _ = pb.excluded(facts)
    assert 'fish' in groups and 'meat' not in groups
    vegan = pb.excluded([dict(kind='permanent', category='preference', text='vegano')])[0]
    assert {'egg', 'dairy', 'meat'} <= vegan
    assert pb.excluded([dict(kind='dynamic', category='preference', text='vegano')])[0] == frozenset()


def test_food_the_dish_already_has_is_dropped():
    payload = plan(extra=[dict(name='grão-de-bico cozido (opcional)', g=100, kcal=164)], p=19,
                   meal_text='70 g de pão sírio; 60 g de homus de grão de bico; 80 g de salada; 100 g de grão-de-bico cozido (opcional)',
                   line=LINE.format('100 g de grão-de-bico cozido', 164, 9))
    record = pb.validate(payload, window_kcal=900, remaining_p=120)
    assert record['dropped'][0]['reason'] == 'in_dish'


def test_above_the_floor_drops_the_model_food_and_adds_nothing():
    payload = plan(extra=[dict(name='whey (opcional)', g=25, kcal=100)], p=60,
                   line=LINE.format('25 g de whey', 100, 20))
    record = pb.validate(payload, window_kcal=900, remaining_p=100)
    assert record['source'] == 'none' and record['dropped'][0]['reason'] == 'above_floor'
    assert payload['estimate']['kcal'] == 360 and 'Para a proteína' not in payload['reply']
    assert payload['estimate']['p'] == 40  # 60 minus the table protein of 25 g of whey


def test_no_room_and_room_cap():
    payload = plan(extra=[dict(name='frango desfiado (opcional)', g=100, kcal=159)], p=42,
                   line=LINE.format('100 g de frango desfiado', 159, 32))
    record = pb.validate(payload, window_kcal=400, remaining_p=120)
    assert record['dropped'][0]['reason'] == 'no_room'
    capped = plan(extra=[dict(name='frango desfiado (opcional)', g=150, kcal=239)], p=58,
                  line=LINE.format('150 g de frango desfiado', 239, 48))
    record = pb.validate(capped, window_kcal=500, remaining_p=120)
    assert record['added'] == [dict(name='frango desfiado', g=75, kcal=119, p=24)]  # room 140 kcal: 75 g fits


def test_grams_off_the_step_or_unknown_food_are_dropped():
    payload = plan(extra=[dict(name='atum (opcional)', g=120, kcal=140), dict(name='seitan (opcional)', g=50, kcal=180)],
                   p=50, line=LINE.format('120 g de atum e 50 g de seitan', 320, 50))
    record = pb.validate(payload, window_kcal=1200, remaining_p=150)
    assert [d['reason'] for d in record['dropped']] == ['grams', 'unknown']
    assert record['source'] == 'server'


def test_another_day_and_other_intent():
    line = LINE.format('50 g de ovo cozido', 73, 7)
    payload = plan(extra=[dict(name='ovo cozido (opcional)', g=50, kcal=73)], p=17, line=line)
    record = pb.validate(payload, window_kcal=900, remaining_p=120, another_day=True)
    assert record['dropped'][0]['reason'] == 'other_day' and record['source'] == 'none'
    assert 'Para a proteína' not in payload['reply']
    assert pb.validate(plan(), window_kcal=900, remaining_p=120, another_day=True) is None
    assert pb.validate(plan(intent='log', line=line), window_kcal=900, remaining_p=120) is None
    assert pb.other_day('o que janto amanhã?') and not pb.other_day('o que janto hoje?')


def test_no_proposal_due_falls_back_to_the_table():
    payload = plan()
    record = pb.validate(payload, window_kcal=900, remaining_p=120)
    assert record['source'] == 'server' and record['added'][0]['name'] == 'frango desfiado'


def test_inline_sentence_and_unparenthesized_mark_are_one_boost():
    # Shape seen in the smoke: the sentence sits at the end of the dish line, the item says "opcional" bare.
    payload = plan(extra=[dict(name='frango desfiado opcional', g=50, kcal=80)], p=22,
                   meal_text='Wrap com 70 g de pão sírio, 60 g de homus e 50 g de frango desfiado (opcional)',
                   reply='Wrap: **440 kcal**. Para a proteína faltante, 50 g de frango desfiado (opcional), +80 kcal · P 16 g.')
    record = pb.validate(payload, window_kcal=900, remaining_p=120)
    assert record['source'] == 'model' and [a['name'] for a in record['added']] == ['frango desfiado']
    assert payload['reply'] == 'Wrap: **440 kcal**.\nPara a proteína (opcional): 50 g de frango desfiado, +80 kcal · P 16 g.'
    est = payload['estimate']
    assert est['meal_text'] == 'Wrap com 70 g de pão sírio, 60 g de homus; 50 g de frango desfiado (opcional)'
    assert [i['name'] for i in est['items']].count('frango desfiado (opcional)') == 1


def test_one_part_meal_text_keeps_the_dish():
    payload = plan(extra=[dict(name='frango (opcional)', g=75, kcal=119)], p=34,
                   meal_text='Omelete com 1 ovo, espinafre, 1 torrada e 75 g de frango (opcional)',
                   line=LINE.format('75 g de frango', 119, 24))
    pb.validate(payload, window_kcal=900, remaining_p=93)
    assert payload['estimate']['meal_text'] == 'Omelete com 1 ovo, espinafre, 1 torrada; 75 g de frango (opcional)'
