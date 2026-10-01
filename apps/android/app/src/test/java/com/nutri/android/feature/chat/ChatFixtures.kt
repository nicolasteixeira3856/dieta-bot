package com.nutri.android.feature.chat

import com.nutri.android.domain.Macros
import com.nutri.android.domain.ProjectedDay

/** States drawn in the Stitch chat golds (Café 07:30 ... Jantar 20:00, 25 de setembro). */
object ChatFixtures {
    private val slots = listOf(
        SlotRef(1, "Café da manhã", "07:30", 450),
        SlotRef(2, "Almoço", "12:30", 750),
        SlotRef(3, "Lanche", "16:00", 960),
        SlotRef(4, "Jantar", "20:00", 1200),
    )
    private val date = ChatItem.DateSeparator("Hoje, 25 de setembro")
    private val user = ChatItem.User(1, "2 pães franceses com 2 ovos mexidos no café da manhã", "20:15")
    private val bot = ChatItem.Assistant(
        id = 2,
        text = "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:",
        time = "20:15",
        highlights = listOf("2 pães franceses", "2 ovos mexidos"),
        estimate = EstimateView(380, 22, 36, 16, "Deseja registrar essa refeição no Café da manhã?", null),
    )
    private val actions = EstimateActions(2, record = slots[0], skip = slots[0])

    val chat0 = ChatUiState(
        items = listOf(date, ChatItem.Greeting("20:14")),
        emptyDay = true,
        metaRemaining = 1450,
        metaTotal = 2100,
        slots = slots,
    )
    val chatL = ChatUiState(items = listOf(date, user.copy(pending = true), ChatItem.Loading), sending = true, emptyDay = false, slots = slots)
    /** Estimate without a follow-up question: the chatT / chatP background (unchanged by ST1). */
    private val estimated = ChatUiState(items = listOf(date, user, bot), emptyDay = false, actions = actions, slots = slots, currentSlotId = 4)

    /** chatE after ST7 (A30): the estimate comes after the questions, with no question bubble. */
    val chatE = estimated

    /** chatQ (ST7, A30): the second question before the estimate; Forçar estimativa in the actions slot. */
    val chatQ = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(11, "Jantei macarrão com frango ao molho branco", "20:12"),
            ChatItem.Question(12, "O molho branco levou creme de leite ou requeijão? E o macarrão, foi 1 prato raso ou fundo?", "20:12", standalone = true),
            ChatItem.User(13, "Creme de leite, prato fundo", "20:14"),
            ChatItem.Question(14, "O frango foi grelhado ou empanado?", "20:15", standalone = true),
        ),
        emptyDay = false,
        forceEstimate = true,
        slots = slots,
        currentSlotId = 4,
    )
    val chatT = estimated.copy(sheetFor = 2, sheetSelection = 4)
    val chatP = estimated.copy(skipConfirm = SlotRef(3, "Lanche da tarde", "16:00", 960))

    /** A18 / ADR-017: Gravar on a taken slot. chatP layout, new copy (no gold of its own). */
    val chatReplace = estimated.copy(replaceConfirm = ReplaceConfirm(2, slots[3], oldKcal = 880, newKcal = 1220))
    val chatG = ChatUiState(
        items = listOf(date, user, bot.copy(estimate = bot.estimate!!.copy(slotQuestion = null)), ChatItem.Receipt(3, false, "Café da manhã", "07:30", 380)),
        emptyDay = false,
        slots = slots,
    )

    /** chatA: empty day, the photo attached in the composer with a caption, not sent yet (A19). */
    val chatA = chat0.copy(composer = "almoço de hoje, comi tudo", attachment = CHAT_A_PHOTO)

    /** chatX: empty day, a pasted message over 2000 characters (A25, ADR-022). The gold shows its first 5 lines. */
    private const val CHAT_X_TEXT = "Hoje no almoço comi arroz branco, feijão carioca, duas coxas de frango assadas sem pele, " +
        "salada de alface com tomate e cebola, uma colher de farofa, meio bife acebolado e de sobremesa um pedaço de pudim de leite. " +
        "No lanche da tarde tomei um café com leite e comi um pão de queijo grande e uma banana. "
    val chatX = chat0.copy(composer = CHAT_X_TEXT.repeat(10).trim(), composerTooLong = true)

    /** Thumbnail of the chatA gold (the photo the owner attached in Stitch). */
    const val CHAT_A_PHOTO = "src/test/resources/chatA-photo.jpg"

    /** chatF: photo of a prato feito captioned "Almoço de hoje", estimate 780 kcal for Almoço (A6). */
    const val CHAT_F_PHOTO = "src/test/resources/chatF-photo.jpg"
    val chatF = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(5, "Almoço de hoje", "12:41", photoPath = CHAT_F_PHOTO),
            ChatItem.Assistant(
                id = 6,
                text = "Identifiquei um Prato Feito com filé de frango grelhado, arroz, feijão e salada verde.",
                time = "12:41",
                highlights = listOf("Prato Feito"),
                estimate = EstimateView(780, 48, 82, 18, "Deseja registrar essa refeição no Almoço?", null),
            ),
        ),
        emptyDay = false,
        actions = EstimateActions(6, record = slots[1], skip = slots[1]),
        slots = slots,
        currentSlotId = 2,
    )

    /** chatR (ST6): a plan with the day projected by the app and one Registrar assim. */
    val chatR = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(7, "Vou fazer uma pizza de pão sírio na janta. Quantas gramas de cada item?", "20:15"),
            ChatItem.Assistant(
                id = 8,
                text = "Para caber nas 560 kcal que sobram hoje:\n" +
                    "• 1 pão sírio (60 g)\n" +
                    "• 2 colheres de sopa de molho de tomate (30 g)\n" +
                    "• 100 g de frango desfiado\n" +
                    "• 30 g de milho\n" +
                    "• 30 g de muçarela\n" +
                    "Monte e leve ao forno a 200 °C por 8 a 10 min.\n" +
                    "Total: ~420 kcal · 40P · 38C · 12G",
                time = "20:15",
                plan = ProjectedDay(1640, Macros(2060, 126, 190, 58), 2200, Macros(0, 167, 223, 74)),
            ),
        ),
        emptyDay = false,
        actions = EstimateActions(8, record = slots[3], skip = slots[3], plan = true),
        slots = slots,
        currentSlotId = 4,
    )

    /** chatM (ST6): the memory changed this turn and the answer used both memories. */
    val chatM = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(9, "Café da manhã igual ao de sempre, mas hoje com pão integral", "20:15"),
            ChatItem.Assistant(
                id = 10,
                text = "Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar.",
                time = "20:15",
                estimate = EstimateView(430, 26, 36, 20, "Deseja registrar essa refeição no Café da manhã?", null),
                memory = MemoryNotice(updated = true, permanent = true, dynamic = true),
            ),
        ),
        emptyDay = false,
        actions = EstimateActions(10, record = slots[0], skip = slots[0]),
        slots = slots,
        currentSlotId = 4,
    )

    /** chatS (ST6): empty day at breakfast time, the usual breakfast suggested. */
    private val routine = RoutineSuggestion(
        factId = "D1",
        slot = slots[0],
        text = "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, café",
        kcal = 440,
        p = 25,
        c = 38,
        g = 22,
        permanent = false,
    )
    val chatS = chat0.copy(items = chat0.items + ChatItem.Routine(routine), routine = routine, currentSlotId = 1)
}
