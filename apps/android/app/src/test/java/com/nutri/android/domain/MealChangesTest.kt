package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.junit.Test

/** A47: meal_change parsing, checks against the captured base and composition (S18 contract, ADR-032). */
class MealChangesTest {
    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private val dinner = SlotState(listOf(SlotRecord("arroz, feijão e omelete; 1 lata de refrigerante", 380, 22, 40, 14)))
    private val lunch = SlotState(listOf(SlotRecord("macarrão com carne moída", 650, 35, 80, 18)))
    private val states = mapOf(1L to SlotState.EMPTY, 2L to lunch, 3L to SlotState.SKIPPED, 4L to dinner)

    private val pudding = """{"meal_text":"Pudim de leite, 1 fatia média (100 g)","kcal":240,"p":6,"c":38,"g":7,
        "items":[{"name":"pudim de leite","g":100,"kcal":240}]}"""

    private fun change(op: String = "add", base: String? = "4", addition: String? = pudding) =
        json("""{"operation":"$op","base_slot":${base?.let { "\"$it\"" } ?: "null"},"addition":${addition ?: "null"}}""")

    private fun estimate(kcal: Double, p: Double, c: Double, g: Double, text: String?, slot: String?) =
        EstimateNumbers(kcal, p, c, g, text, slot)

    private fun proposal(wire: MealChanges.Wire, e: EstimateNumbers?, recordable: Boolean = true) =
        MealChanges.proposal(wire, e, recordable, states, "2026-09-25", wipeId = 7L)

    private val composedDinner = "arroz, feijão e omelete; 1 lata de refrigerante; Pudim de leite, 1 fatia média (100 g)"

    // ------------------------------------------------------------------ wire

    @Test fun absentNullAndObject_areThreeDifferentThings() {
        assertThat(MealChanges.parse(null)).isEqualTo(MealChanges.Wire.Absent)
        assertThat(MealChanges.parse(JsonNull)).isEqualTo(MealChanges.Wire.None)
        val parsed = MealChanges.parse(change()) as MealChanges.Wire.Change
        assertThat(parsed.operation).isEqualTo("add")
        assertThat(parsed.baseSlot).isEqualTo("4")
        assertThat(parsed.addition).isEqualTo(
            MealAddition("Pudim de leite, 1 fatia média (100 g)", 240, 6, 38, 7, listOf(AdditionItem("pudim de leite", 100.0, 240))),
        )
    }

    @Test fun malformedShapes_areMalformed_neverAGuess() {
        val bad = listOf(
            """{"operation":"add","base_slot":"4"}""",
            """{"operation":"add","base_slot":"4","addition":null}""",
            """{"operation":"add","base_slot":4,"addition":$pudding}""",
            """{"operation":"merge","base_slot":"4","addition":$pudding}""",
            """{"operation":"revise","base_slot":"4","addition":$pudding}""",
            """{"operation":"add","base_slot":"4","addition":$pudding,"extra":1}""",
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":"240","p":6,"c":38,"g":7,"items":[{"name":"x","g":100,"kcal":240}]}}""",
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":true,"p":6,"c":38,"g":7,"items":[{"name":"x","g":100,"kcal":240}]}}""",
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":240,"p":-1,"c":38,"g":7,"items":[{"name":"x","g":100,"kcal":240}]}}""",
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":240,"p":6,"c":38,"g":7,"items":[]}}""",
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":240,"p":6,"c":38,"g":7,"items":[{"name":"x","g":0,"kcal":240}]}}""",
            // Rounded item kcal must sum to the rounded delta.
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":240,"p":6,"c":38,"g":7,"items":[{"name":"x","g":100,"kcal":200}]}}""",
            // A caloric input never rounds to zero.
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"x","kcal":0.4,"p":0,"c":0,"g":0,"items":[{"name":"x","g":1,"kcal":0.4}]}}""",
            """{"operation":"add","base_slot":"4","addition":{"meal_text":"  ","kcal":240,"p":6,"c":38,"g":7,"items":[{"name":"x","g":100,"kcal":240}]}}""",
            "[]",
            "\"add\"",
        )
        bad.forEach { assertThat(MealChanges.parse(json(it))).isEqualTo(MealChanges.Wire.Malformed) }
    }

    @Test fun rounding_once_tiesUpward_zeroEnergyAllowed_alcoholNotForcedToMacros() {
        val a = MealChanges.parse(
            json("""{"operation":"add","base_slot":null,"addition":{"meal_text":"1 taça de vinho tinto (150 ml)","kcal":124.5,"p":0.1,"c":3.8,"g":0,"items":[{"name":"vinho","g":150,"kcal":124.5}]}}"""),
        ) as MealChanges.Wire.Change
        assertThat(a.addition!!.kcal).isEqualTo(125)
        assertThat(a.addition!!.p).isEqualTo(0)
        assertThat(a.addition!!.c).isEqualTo(4)
        assertThat(a.addition!!.items.single().kcal).isEqualTo(125)
        val water = MealChanges.parse(
            json("""{"operation":"add","base_slot":null,"addition":{"meal_text":"água","kcal":0,"p":0,"c":0,"g":0,"items":[{"name":"água","g":300,"kcal":0}]}}"""),
        )
        assertThat(water).isInstanceOf(MealChanges.Wire.Change::class.java)
    }

    @Test fun descriptionBound_isCodePoints_500WithSupplementaryCharacters() {
        fun addition(text: String) = """{"operation":"add","base_slot":null,"addition":{"meal_text":"$text","kcal":1,"p":0,"c":0,"g":0,"items":[{"name":"x","g":1,"kcal":1}]}}"""
        val emoji = "🍮" // two UTF-16 units, one code point
        val at500 = emoji.repeat(500)
        assertThat(at500.length).isEqualTo(1000)
        assertThat(MealChanges.parse(json(addition(at500)))).isInstanceOf(MealChanges.Wire.Change::class.java)
        assertThat(MealChanges.parse(json(addition(emoji.repeat(501))))).isEqualTo(MealChanges.Wire.Malformed)
        assertThat(MealChanges.parse(json(addition("a".repeat(499) + "é")))).isInstanceOf(MealChanges.Wire.Change::class.java)
    }

    // ------------------------------------------------------------------ proposal

    @Test fun occupiedAddition_matchingBaseAndTotal_isAnAddition_withTheCapturedBase() {
        val p = proposal(MealChanges.parse(change()), estimate(620.0, 28.0, 78.0, 21.0, composedDinner, "4"))!!
        assertThat(p.operation).isEqualTo(MealProposal.ADD)
        assertThat(p.sourceSlotId).isEqualTo(4L)
        assertThat(p.source).isEqualTo(dinner)
        assertThat(p.targetSlotId).isEqualTo(4L)
        assertThat(p.target).isEqualTo(dinner)
        assertThat(p.date).isEqualTo("2026-09-25")
        assertThat(p.wipeId).isEqualTo(7L)
        assertThat(MealProposal.decode(p.encode())).isEqualTo(p)
    }

    @Test fun contradictingTotalOrText_isInvalid_neverRebasedOrRecomputed() {
        val wrongTotal = proposal(MealChanges.parse(change()), estimate(850.0, 28.0, 78.0, 21.0, composedDinner, "4"))!!
        assertThat(wrongTotal.operation).isEqualTo(MealProposal.INVALID)
        assertThat(wrongTotal.reason).isEqualTo(MealProposal.CONTRADICTS)
        // The 056d2750 incident: lunch foods composed into the dinner.
        val lunchText = proposal(MealChanges.parse(change()), estimate(620.0, 28.0, 78.0, 21.0, "macarrão com carne moída; pudim", "4"))!!
        assertThat(lunchText.operation).isEqualTo(MealProposal.INVALID)
        val wrongBase = proposal(MealChanges.parse(change(base = "2")), estimate(620.0, 28.0, 78.0, 21.0, composedDinner, "4"))!!
        assertThat(wrongBase.operation).isEqualTo(MealProposal.INVALID)
        val fraction = proposal(MealChanges.parse(change()), estimate(620.4, 28.0, 78.0, 21.0, composedDinner, "4"))!!
        assertThat(fraction.operation).isEqualTo(MealProposal.INVALID)
    }

    @Test fun additionToEmptySkippedOrUnknownTarget_hasNoBase_andTheEstimateIsTheAdditionAlone() {
        val text = "Pudim de leite, 1 fatia média (100 g)"
        val empty = proposal(MealChanges.parse(change(base = null)), estimate(240.0, 6.0, 38.0, 7.0, text, "1"))!!
        assertThat(empty.operation).isEqualTo(MealProposal.ADD)
        assertThat(empty.sourceSlotId).isNull()
        assertThat(empty.target).isEqualTo(SlotState.EMPTY)
        val skipped = proposal(MealChanges.parse(change(base = null)), estimate(240.0, 6.0, 38.0, 7.0, text, "3"))!!
        assertThat(skipped.target).isEqualTo(SlotState.SKIPPED)
        val unknown = proposal(MealChanges.parse(change(base = null)), estimate(240.0, 6.0, 38.0, 7.0, text, null))!!
        assertThat(unknown.targetSlotId).isNull()
        assertThat(unknown.destinationSlotId).isNull()
        // A null base must not land on an occupied target, and an occupied target must keep its base.
        assertThat(proposal(MealChanges.parse(change(base = null)), estimate(240.0, 6.0, 38.0, 7.0, text, "4"))!!.operation)
            .isEqualTo(MealProposal.INVALID)
        assertThat(proposal(MealChanges.parse(change(base = null)), estimate(240.0, 6.0, 38.0, 7.0, text, "9"))!!.operation)
            .isEqualTo(MealProposal.INVALID)
    }

    @Test fun revisionAndNew_areChecked_againstTheirTargets() {
        val revise = proposal(MealChanges.parse(change("revise", "4", null)), estimate(455.0, 28.0, 40.0, 19.0, "Arroz, feijão e omelete de 3 ovos", "4"))!!
        assertThat(revise.operation).isEqualTo(MealProposal.REVISE)
        assertThat(revise.source).isEqualTo(dinner)
        assertThat(proposal(MealChanges.parse(change("revise", null, null)), estimate(455.0, 28.0, 40.0, 19.0, "x", "4"))!!.operation)
            .isEqualTo(MealProposal.INVALID)
        val new = proposal(MealChanges.parse(change("new", null, null)), estimate(300.0, 10.0, 40.0, 9.0, "pão com ovo", "1"))!!
        assertThat(new.operation).isEqualTo(MealProposal.NEW)
        assertThat(proposal(MealChanges.parse(change("new", null, null)), estimate(300.0, 10.0, 40.0, 9.0, "pão com ovo", "4"))!!.operation)
            .isEqualTo(MealProposal.INVALID)
        assertThat(proposal(MealChanges.parse(change("new", null, null)), estimate(300.0, 10.0, 40.0, 9.0, "x".repeat(501), "1"))!!.operation)
            .isEqualTo(MealProposal.INVALID)
    }

    @Test fun missingOrMalformedMetadata_onARecordableEstimate_isInvalid_elseNoProposal() {
        val e = estimate(240.0, 6.0, 38.0, 7.0, "pudim", "1")
        assertThat(proposal(MealChanges.Wire.Absent, e)).isNull()
        assertThat(proposal(MealChanges.Wire.None, e)!!.reason).isEqualTo(MealProposal.MALFORMED)
        assertThat(proposal(MealChanges.Wire.Malformed, e)!!.operation).isEqualTo(MealProposal.INVALID)
        // Other day, plan, question: record none, null change, nothing to propose.
        assertThat(proposal(MealChanges.Wire.None, e, recordable = false)).isNull()
        assertThat(proposal(MealChanges.Wire.None, null)).isNull()
    }

    // ------------------------------------------------------------------ composition

    @Test fun compose_keepsTheBase_addsOnce_oneRecord() {
        val base = SlotState(listOf(SlotRecord("refrigerante", 140, 0, 35, 0), SlotRecord("arroz", 240, 22, 5, 14)))
        val addition = MealAddition("pudim", 240, 6, 38, 7, listOf(AdditionItem("pudim", 100.0, 240)))
        val record = MealChanges.compose(base, addition, "photo")!!
        assertThat(record).isEqualTo(SlotRecord("refrigerante; arroz; pudim", 620, 28, 78, 21, "photo"))
        assertThat(MealChanges.compose(SlotState.SKIPPED, addition, "user")).isEqualTo(SlotRecord("pudim", 240, 6, 38, 7, "user"))
    }

    @Test fun compose_at2000CodePoints_isComplete_at2001_isNull() {
        val emoji = "🍮"
        val addition = MealAddition("${emoji}x", 10, 0, 1, 0, listOf(AdditionItem("x", 1.0, 10)))
        // base + "; " + 2 code points = 2000
        val base = SlotState(listOf(SlotRecord(emoji.repeat(1996), 100, 1, 1, 1)))
        val record = MealChanges.compose(base, addition, "user")!!
        assertThat(MealText.length(record.text)).isEqualTo(2000)
        assertThat(record.text).endsWith("; ${emoji}x")
        val longer = SlotState(listOf(SlotRecord(emoji.repeat(1997), 100, 1, 1, 1)))
        assertThat(MealChanges.compose(longer, addition, "user")).isNull()
    }

    @Test fun matches_checksDayWipeSourceAndDestination() {
        val p = proposal(MealChanges.parse(change()), estimate(620.0, 28.0, 78.0, 21.0, composedDinner, "4"))!!
        val now = states::getValue
        assertThat(p.matches("2026-09-25", 7L, now)).isTrue()
        assertThat(p.matches("2026-09-26", 7L, now)).isFalse()
        assertThat(p.matches("2026-09-25", 8L, now)).isFalse()
        assertThat(p.matches("2026-09-25", 7L) { if (it == 4L) SlotState.EMPTY else now(it) }).isFalse()
        val chosen = p.choose(1L, SlotState.EMPTY)
        assertThat(chosen.destinationSlotId).isEqualTo(1L)
        assertThat(chosen.matches("2026-09-25", 7L) { if (it == 1L) lunch else now(it) }).isFalse()
        assertThat(chosen.choose(4L, dinner)).isEqualTo(p)
    }
}
