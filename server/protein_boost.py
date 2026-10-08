"""Protein boost of a named dish: the model proposes, the server validates (ADR-055, S34).

When a plan for today covers too little of the protein still missing and its meal window has room, the model adds
one or two protein foods marked `(opcional)` and one reply line `Para a proteína (opcional): …`. `validate()` reads
the foods of that line, drops the ones that break the rule (dish at or above the floor, room under 60 kcal, a third
food, a repeat, a food the dish has, a diet fact, grams above 150 or off the kitchen step, another day), sizes the
survivors to the room and the gap with the table numbers, and rewrites items, totals, meal_text and the line. When
nothing survives and the boost is due, `apply()` (S24) appends one food of its own table. Pure functions, no I/O.

Values per 100 g from the Tabela Brasileira de Composição de Alimentos (TACO, 4th ed.), the same rows the
reference-portions rule carries: peito de frango grelhado 159/32/0/2.5 (served shredded), ovo cozido
146/13/0.6/9.5 (1 unit 50 g), patinho grelhado 219/36/0/7 (served as lean ground beef).
"""

from __future__ import annotations

import math
import re
import unicodedata
from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal
from typing import Any

# Below this share of the missing protein a named dish gets the boost (ADR-043 decision 4).
FLOOR_PCT = 30
# Room in the window the boost needs to be worth adding, and the most grams of one food.
MIN_ROOM_KCAL = 60
MAX_GRAMS = 150
MAX_FOODS = 2
# S33: a plan of one food under this many kcal answers "posso adicionar X?" with that food alone: not a dish.
MIN_DISH_KCAL = 150


@dataclass(frozen=True)
class Food:
    name: str
    kcal: float  # per 100 g
    p: float
    c: float
    g: float
    step: int  # grams per step (an egg is 50 g)
    keys: tuple[str, ...]  # words that mean the dish already has this food
    group: str = ""  # diet group: meat, poultry, fish, egg, dairy, soy, legume


FOODS: tuple[Food, ...] = (
    Food("frango desfiado", 159, 32, 0, 2.5, 25, ("frango", "chicken"), "poultry"),
    Food("ovo cozido", 146, 13, 0.6, 9.5, 50, ("ovo", "ovos", "omelete", "mexido"), "egg"),
    Food("patinho moído cozido", 219, 36, 0, 7, 25, ("carne", "bife", "patinho", "acém", "moída", "moido"), "meat"),
)

# Foods the model may propose (S34), per 100 g. TACO 4th ed. where it has the row (atum and sardinha em conserva,
# iogurte natural desnatado, ricota, queijo minas frescal, lentilha cozida); USDA FoodData Central for the rest
# (queijo cottage, whey concentrado, tofu firme, grão-de-bico cozido, peito de peru, edamame). Specific keys first.
CANDIDATES: tuple[Food, ...] = (
    Food("peito de peru", 110, 20, 2, 2, 25, ("peru",), "poultry"),
    Food("atum em lata", 117, 26, 0, 1, 25, ("atum",), "fish"),
    Food("sardinha em lata", 285, 16, 0, 24, 25, ("sardinha",), "fish"),
    Food("queijo cottage", 98, 11, 3.4, 4.3, 25, ("cottage",), "dairy"),
    Food("ricota", 140, 12.6, 3.8, 8.1, 25, ("ricota",), "dairy"),
    Food("queijo minas frescal", 264, 17, 3, 20, 25, ("minas", "frescal"), "dairy"),
    Food("iogurte natural desnatado", 41, 3.8, 5.8, 0.3, 25, ("iogurte",), "dairy"),
    Food("whey protein", 400, 80, 7, 6, 25, ("whey",), "dairy"),
    Food("tofu", 76, 8, 1.9, 4.8, 25, ("tofu",), "soy"),
    Food("edamame", 121, 11.9, 8.9, 5.2, 25, ("edamame",), "soy"),
    Food("grão-de-bico cozido", 164, 8.9, 27, 2.6, 25, ("grao-de-bico", "grao de bico"), "legume"),
    Food("lentilha cozida", 93, 6.3, 16.3, 0.5, 25, ("lentilha",), "legume"),
    *FOODS,
)
VEGETARIAN_OUT = frozenset({"meat", "poultry", "fish"})
VEGAN_OUT = VEGETARIAN_OUT | {"egg", "dairy"}
# A diet fact excludes a group when it has a negative cue and one of these words (normalized, no accents).
_NEGATIVE = ("nao como", "nao come", "nao comemos", "sem ", "alergi", "intoleran", "evito", "nao gosto", "nao pode")
_GROUP_WORDS = {
    "meat": ("carne",), "poultry": ("frango", "aves", "ave "), "fish": ("peixe", "pescado", "frutos do mar"),
    "egg": ("ovo",), "dairy": ("lactose", "leite", "laticin", "derivados"), "soy": ("soja",),
}
_FUTURE_DAY = re.compile(r"\b(amanha|depois de amanha|semana que vem|proxima semana)\b")
# The model's boost sentence, anywhere in a line, up to its protein figure ("P 16 g.") or the end of the line.
_SENTENCE = re.compile(r"para a prote[ií]na\b.*?(?:\bP\s*\d+(?:[.,]\d+)?\s*g\b\.?|$)", re.IGNORECASE | re.MULTILINE)
_MARK = re.compile(r"\(?\bopcional\b\)?", re.IGNORECASE)
_SEPARATOR = re.compile(r";|,|\s+e\s+|\s+com\s+")
_LINE_PART = re.compile(r"^\s*(\d+(?:[.,]\d+)?)\s*g\s+de\s+(.+?)\s*$")


def _rounded(value: float) -> int:
    return int(Decimal(repr(float(value))).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def _number(value: Any) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        return None
    return float(value)


def _dish_text(estimate: dict[str, Any]) -> str:
    parts = [str(estimate.get("meal_text") or "")]
    parts += [str(item.get("name") or "") for item in estimate.get("items") or [] if isinstance(item, dict)]
    return " ".join(parts).casefold()


def _grams(food: Food, room_kcal: float, gap_p: float) -> int:
    """The most grams that fit the room and do not exceed the gap, in the food's step; 0 when a step does not fit."""
    by_room = room_kcal / food.kcal * 100
    by_gap = gap_p / food.p * 100
    grams = min(MAX_GRAMS, by_room, by_gap)
    steps = int(grams // food.step)
    return steps * food.step


def floor_p(remaining_p: float) -> int:
    return math.ceil(max(0.0, remaining_p) * FLOOR_PCT / 100)


def apply(
    payload: Any, window_kcal: int | None, remaining_p: int | None,
    exclude: tuple[frozenset[str], frozenset[str]] = (frozenset(), frozenset()),
) -> dict[str, Any] | None:
    """Append `(opcional)` protein foods to payload.estimate and the reply when the dish needs them.

    Applies only to an in-scope plan with an estimate, a known window and missing protein. Returns the record
    `{gap_p, room_kcal, added: [{name, g, kcal, p}]}` when something was added, else None. Mutates payload.
    """
    if not isinstance(payload, dict) or payload.get("intent") != "plan" or window_kcal is None or remaining_p is None:
        return None
    estimate = payload.get("estimate")
    if not isinstance(estimate, dict) or remaining_p <= 0:
        return None
    kcal, p = _number(estimate.get("kcal")), _number(estimate.get("p"))
    if kcal is None or p is None or kcal <= 0:
        return None
    if p >= floor_p(remaining_p):
        return None
    if len(estimate.get("items") or []) == 1 and kcal < MIN_DISH_KCAL:
        return None
    room = window_kcal - kcal
    gap = remaining_p - p
    if room < MIN_ROOM_KCAL or gap <= 0:
        return None
    dish = _dish_text(estimate)
    added: list[dict[str, Any]] = []
    for food in FOODS:
        if len(added) == MAX_FOODS or p >= floor_p(remaining_p):
            break
        if any(key in dish for key in food.keys) or food.group in exclude[0] or food.name in exclude[1]:
            continue
        grams = _grams(food, room, gap)
        if grams <= 0:
            continue
        item_kcal = _rounded(food.kcal * grams / 100)
        item_p = _rounded(food.p * grams / 100)
        added.append({"name": food.name, "g": grams, "kcal": item_kcal, "p": item_p,
                      "c": _rounded(food.c * grams / 100), "gfat": _rounded(food.g * grams / 100)})
        room -= item_kcal
        gap -= item_p
        p += item_p
    if not added:
        return None
    items = list(estimate.get("items") or [])
    for entry in added:
        items.append({"name": f"{entry['name']} (opcional)", "g": entry["g"], "kcal": entry["kcal"]})
    estimate["items"] = items
    estimate["kcal"] = _rounded(kcal + sum(e["kcal"] for e in added))
    estimate["p"] = _rounded((_number(estimate.get("p")) or 0) + sum(e["p"] for e in added))
    estimate["c"] = _rounded((_number(estimate.get("c")) or 0) + sum(e["c"] for e in added))
    estimate["g"] = _rounded((_number(estimate.get("g")) or 0) + sum(e["gfat"] for e in added))
    text = str(estimate.get("meal_text") or "").rstrip()
    extra = "; ".join(f"{e['g']} g de {e['name']} (opcional)" for e in added)
    estimate["meal_text"] = f"{text}; {extra}" if text else extra
    line = " e ".join(f"{e['g']} g de {e['name']}" for e in added)
    boost_kcal = sum(e["kcal"] for e in added)
    boost_p = sum(e["p"] for e in added)
    reply = payload.get("reply")
    note = f"Para a proteína (opcional): {line}, +{boost_kcal} kcal · P {boost_p} g."
    payload["reply"] = f"{reply.rstrip()}\n{note}" if isinstance(reply, str) and reply.strip() else note
    return {
        "gap_p": _rounded(remaining_p - (p - boost_p)),
        "room_kcal": _rounded(window_kcal - kcal),
        "added": [{"name": e["name"], "g": e["g"], "kcal": e["kcal"], "p": e["p"]} for e in added],
    }


def _norm(text: Any) -> str:
    decomposed = unicodedata.normalize("NFKD", str(text).casefold())
    return " ".join("".join(ch for ch in decomposed if not unicodedata.combining(ch)).split())


def _unmarked(text: Any) -> str:
    return _norm(_MARK.sub(" ", str(text).replace("**", " ")))


def match(name: Any) -> Food | None:
    """The table food a label names, specific keys first; None for a food outside the table."""
    label = _unmarked(name)
    for food in CANDIDATES:
        if any(_norm(key) in label for key in food.keys):
            return food
    return None


def other_day(text: str) -> bool:
    """A plan named for another day ("amanhã", "semana que vem") never gets a boost."""
    return bool(_FUTURE_DAY.search(_norm(text)))


def _fact_field(fact: Any, name: str) -> Any:
    return fact.get(name) if isinstance(fact, dict) else getattr(fact, name, None)


def excluded(facts: Any) -> tuple[frozenset[str], frozenset[str]]:
    """(groups, food names) excluded by permanent preference facts that state a diet or an excluded food."""
    groups: set[str] = set()
    names: set[str] = set()
    for fact in facts or ():
        if _fact_field(fact, "kind") != "permanent" or _fact_field(fact, "category") != "preference":
            continue
        text = _norm(_fact_field(fact, "text") or "") + " "
        if "vegan" in text:
            groups |= VEGAN_OUT
        elif "vegetarian" in text:
            groups |= VEGETARIAN_OUT
        if any(cue in text for cue in _NEGATIVE):
            groups |= {group for group, words in _GROUP_WORDS.items() if any(w in text for w in words)}
            names |= {food.name for food in CANDIDATES if any(_norm(k) in text for k in food.keys)}
    return frozenset(groups), frozenset(names)


def line_foods(reply: Any) -> list[tuple[float | None, str]]:
    """The (grams, food) pairs of the model's `Para a proteína (opcional): …` sentence, wherever it sits."""
    if not isinstance(reply, str):
        return []
    out: list[tuple[float | None, str]] = []
    for found in _SENTENCE.finditer(reply.replace("**", "")):
        sentence = found.group(0)
        head = re.match(r"para a prote[ií]na[^:,]*[:,]", sentence, re.IGNORECASE)
        body = sentence[head.end():] if head else ""
        body = _MARK.sub(" ", re.split(r",?\s*\+\s*\d", body)[0])
        for part in re.split(r"\s+e\s+|,|;", body):
            part = part.strip().rstrip(".").strip()
            if not part:
                continue
            grams = _LINE_PART.match(part)
            out.append((float(grams.group(1).replace(",", ".")), grams.group(2)) if grams else (None, part))
    return out


def _strip_line(reply: Any) -> Any:
    """The reply without the model's boost sentence; a line left empty goes too."""
    if not isinstance(reply, str):
        return reply
    lines = []
    for line in reply.split("\n"):
        plain = line.replace("**", "")
        if not _SENTENCE.search(plain):
            lines.append(line)
            continue
        # Keep the line's own bold when the sentence itself carries none.
        rest = " ".join(_SENTENCE.sub(" ", line if _SENTENCE.search(line) else plain).split())
        if rest.strip(" .,;:"):
            lines.append(rest)
    return "\n".join(lines).rstrip()


def _drop_spans(text: str, is_boost: Any) -> str:
    """meal_text without each boost span: from the separator before an `(opcional)` mark to the mark."""
    while True:
        for mark in _MARK.finditer(text):
            start = 0
            for sep in _SEPARATOR.finditer(text, 0, mark.start()):
                start = sep.start()
            if is_boost(text[start:mark.end()]):
                text = (text[:start] + text[mark.end():]).strip()
                break
        else:
            return re.sub(r"^[;,\s]+|[;,\s]+$", "", text)


def _candidates(items: list[Any], named: list[tuple[float | None, str]]) -> list[int]:
    """Item indexes the line names: the same table food, else the line's label inside the item name."""
    taken: list[int] = []
    for _, food_text in named:
        food = match(food_text)
        wanted = _unmarked(food_text)
        options = [i for i, item in enumerate(items) if isinstance(item, dict) and i not in taken and (
            (food is not None and match(item.get("name") or "") == food)
            or (wanted and wanted in _unmarked(item.get("name") or "")))]
        # A marked item wins over an unmarked one with the same food (the dish may have it too).
        options.sort(key=lambda i: "(opcional)" not in str(items[i].get("name") or "").casefold())
        if options:
            taken.append(options[0])
    return taken


def _num_text(value: float) -> str:
    return str(int(value)) if float(value).is_integer() else str(value)


def _macros(food: Food, grams: float) -> dict[str, int]:
    return {"kcal": _rounded(food.kcal * grams / 100), "p": _rounded(food.p * grams / 100),
            "c": _rounded(food.c * grams / 100), "g": _rounded(food.g * grams / 100)}


def validate(
    payload: Any, window_kcal: int | None, remaining_p: int | None, facts: Any = (), *, another_day: bool = False,
) -> dict[str, Any] | None:
    """Check the model's boost against ADR-055 and complete it; mutates payload. Returns the log record or None.

    Record: {source: model|server|none, gap_p, room_kcal, added: [{name, g, kcal, p}], dropped: [{name, reason}]}.
    `another_day`: the plan is named for another day; the model's foods are dropped and nothing is appended.
    """
    if not isinstance(payload, dict) or payload.get("intent") != "plan":
        return None
    estimate = payload.get("estimate")
    if not isinstance(estimate, dict):
        return None
    items = list(estimate.get("items") or [])
    named = line_foods(payload.get("reply"))
    another_day = another_day or payload.get("meal_day") == "other"
    due_inputs = window_kcal is not None and remaining_p is not None and remaining_p > 0 and not another_day
    if not named:
        if not due_inputs:
            return None
        record = apply(payload, window_kcal, remaining_p, excluded(facts))
        return {"source": "server", **record, "dropped": []} if record else None

    kcal, p = _number(estimate.get("kcal")), _number(estimate.get("p"))
    if kcal is None or p is None:
        return None
    c, g = _number(estimate.get("c")) or 0.0, _number(estimate.get("g")) or 0.0
    picked = _candidates(items, named)
    proposals = [(items[i], match(items[i].get("name") or ""), _number(items[i].get("g")) or 0.0,
                  _number(items[i].get("kcal")) or 0.0) for i in picked]
    known = [(food, grams) for _, food, grams, _ in proposals if food is not None]
    base_kcal = kcal - sum(item_kcal for *_, item_kcal in proposals)
    base_p = max(0.0, p - sum(food.p * grams / 100 for food, grams in known))
    base_c = max(0.0, c - sum(food.c * grams / 100 for food, grams in known))
    base_g = max(0.0, g - sum(food.g * grams / 100 for food, grams in known))
    base_items = [item for i, item in enumerate(items) if i not in picked]
    labels = [_unmarked(item.get("name") or "") for item, *_ in proposals] + [_unmarked(f) for _, f in named]
    grams_text = {f"{_num_text(grams)} g" for _, _, grams, _ in proposals}

    def has_label(span: str) -> bool:
        return any(label and label in _unmarked(span) for label in labels)

    # The model's boost leaves meal_text: marked spans, and unmarked ";" parts that carry a label and its grams.
    meal_text = ";".join(
        part for part in str(estimate.get("meal_text") or "").split(";")
        if _MARK.search(part) or not (has_label(part) and any(g in _unmarked(part) for g in grams_text)))
    meal_text = _drop_spans(meal_text, has_label)
    base_text = " ".join(_unmarked(item.get("name") or "") for item in base_items if isinstance(item, dict))
    base_text += " " + _unmarked(meal_text)

    floor = floor_p(remaining_p) if due_inputs else 0
    room = window_kcal - base_kcal if due_inputs else 0.0
    gap = remaining_p - base_p if due_inputs else 0.0
    if not due_inputs:
        reason_all = "other_day" if another_day else "not_due"
    elif base_p >= floor:
        reason_all = "above_floor"
    elif room < MIN_ROOM_KCAL:
        reason_all = "no_room"
    else:
        reason_all = None

    out_groups, out_names = excluded(facts)
    dropped: list[dict[str, str]] = []
    survivors: list[tuple[str, Food, int, dict[str, int]]] = []
    for item, food, grams, _ in proposals:
        label = str(item.get("name") or "")
        reason = reason_all
        if reason is None:
            if food is None:
                reason = "unknown"
            elif food.group in out_groups or food.name in out_names:
                reason = "diet"
            elif any(_norm(key) in base_text for key in food.keys):
                reason = "in_dish"
            elif any(s[1] == food for s in survivors):
                reason = "repeat"
            elif len(survivors) == MAX_FOODS:
                reason = "third"
            elif grams <= 0 or grams > MAX_GRAMS or grams % food.step:
                reason = "grams"
        if reason is None:
            size = int(grams)
            while size > 0 and (_macros(food, size)["kcal"] > room or _macros(food, size)["p"] > gap):
                size -= food.step
            if size > 0:
                numbers = _macros(food, size)
                name = " ".join(_MARK.sub(" ", label).replace("**", " ").split())
                survivors.append((name or food.name, food, size, numbers))
                room -= numbers["kcal"]
                gap -= numbers["p"]
                continue
            reason = "room"
        dropped.append({"name": _unmarked(label)[:60], "reason": reason})

    # Rebuild from the base dish: the model's boost items, meal_text parts and line go; survivors come back.
    estimate["items"] = base_items
    estimate["kcal"] = _rounded(base_kcal)
    estimate["p"], estimate["c"], estimate["g"] = _rounded(base_p), _rounded(base_c), _rounded(base_g)
    estimate["meal_text"] = meal_text
    payload["reply"] = _strip_line(payload.get("reply"))
    if not survivors:
        record = apply(payload, window_kcal, remaining_p, (out_groups, out_names)) if reason_all is None else None
        if record:
            return {"source": "server", **record, "dropped": dropped}
        return {"source": "none", "gap_p": None, "room_kcal": None, "added": [], "dropped": dropped}
    added = {k: sum(numbers[k] for *_, numbers in survivors) for k in ("kcal", "p", "c", "g")}
    estimate["items"] = base_items + [
        {"name": f"{name} (opcional)", "g": size, "kcal": numbers["kcal"]} for name, _, size, numbers in survivors]
    estimate["kcal"] = _rounded(base_kcal + added["kcal"])
    estimate["p"] = _rounded(base_p + added["p"])
    estimate["c"] = _rounded(base_c + added["c"])
    estimate["g"] = _rounded(base_g + added["g"])
    extra = "; ".join(f"{size} g de {name} (opcional)" for name, _, size, _ in survivors)
    estimate["meal_text"] = f"{estimate['meal_text']}; {extra}" if estimate["meal_text"] else extra
    line = " e ".join(f"{size} g de {name}" for name, _, size, _ in survivors)
    note = f"Para a proteína (opcional): {line}, +{added['kcal']} kcal · P {added['p']} g."
    reply = payload.get("reply")
    payload["reply"] = f"{reply.rstrip()}\n{note}" if isinstance(reply, str) and reply.strip() else note
    return {
        "source": "model",
        "gap_p": _rounded(remaining_p - base_p),
        "room_kcal": _rounded(window_kcal - base_kcal),
        "added": [{"name": name, "g": size, "kcal": n["kcal"], "p": n["p"]} for name, _, size, n in survivors],
        "dropped": dropped,
    }
