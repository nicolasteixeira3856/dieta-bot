package com.nutri.android.feature.chat

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
    val chatE = ChatUiState(items = listOf(date, user, bot), emptyDay = false, actions = actions, slots = slots, currentSlotId = 4)
    val chatT = chatE.copy(sheetFor = 2, sheetSelection = 4)
    val chatP = chatE.copy(skipConfirm = SlotRef(3, "Lanche da tarde", "16:00", 960))

    /** A18 / ADR-017: Gravar on a taken slot. chatP layout, new copy (no gold of its own). */
    val chatReplace = chatE.copy(replaceConfirm = ReplaceConfirm(2, slots[3], oldKcal = 880, newKcal = 1220))
    val chatG = ChatUiState(
        items = listOf(date, user, bot.copy(estimate = bot.estimate!!.copy(slotQuestion = null)), ChatItem.Receipt(3, false, "Café da manhã", "07:30", 380)),
        emptyDay = false,
        slots = slots,
    )

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
}
