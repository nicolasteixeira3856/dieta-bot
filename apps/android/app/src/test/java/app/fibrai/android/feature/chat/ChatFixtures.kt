package app.fibrai.android.feature.chat

import app.fibrai.android.domain.Macros
import app.fibrai.android.domain.ProjectedDay
import app.fibrai.android.domain.ReceiptAction

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

    private const val CHAT_E_TEXT = "Identifiquei 2 pães franceses (**100 g**) e 2 ovos mexidos (**100 g**). A estimativa total é de:"
    /** chatE (A34): an `ask` estimate, one Registrar pill. */
    private val actions = EstimateActions(2, record = slots[0])
    private val recordActions = listOf(ReceiptAction.DELETE, ReceiptAction.MOVE, ReceiptAction.EDIT)

    val chat0 = ChatUiState(
        items = listOf(date, ChatItem.Greeting("20:14")),
        emptyDay = true,
        metaRemaining = 1450,
        metaTotal = 2100,
        slots = slots,
    )
    val chatL = ChatUiState(items = listOf(date, user.copy(pending = true), ChatItem.Loading), sending = true, emptyDay = false, slots = slots)
    /** Estimate without a follow-up question: the chatT background (unchanged by ST1). */
    private val estimated = ChatUiState(items = listOf(date, user, bot), emptyDay = false, actions = actions, slots = slots, currentSlotId = 4)

    /** chatE (D17, A60 part C): the estimate after the questions, the grams in bold, no accent on the names. */
    val chatE = estimated.copy(
        items = listOf(
            date,
            user,
            bot.copy(text = CHAT_E_TEXT, highlights = emptyList(), blocks = app.fibrai.android.domain.ReplyMarkup.parse(CHAT_E_TEXT)),
        ),
    )

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
    val chatT = estimated.copy(sheetFor = 2, sheetSelection = 4, sheetCurrent = 4)

    /** chatG (ST9, A34): recorded by itself; the receipt carries Excluir · Trocar refeição · Editar. */
    val chatG = ChatUiState(
        items = listOf(
            date,
            user,
            bot.copy(estimate = bot.estimate!!.copy(slotQuestion = null)),
            ChatItem.Receipt(3, ReceiptKind.LOGGED, "Café da manhã", "07:30", 380, actions = recordActions),
        ),
        emptyDay = false,
        slots = slots,
    )

    /** chatCP (D20, A61 part B): the chatG thread with the user message and the Tali reply selected; the receipt is not. */
    val chatCP = chatG.copy(selected = setOf(user.key, "a-${bot.id}"))

    /** chatCC (D20, A61 part B): the selection ended by Copiar, the app's own confirmation above the composer. */
    val chatCC = chatG.copy(copied = 1)

    /** chatSK (D19, A59): breakfast recorded by itself and the pre-workout skipped in the same message, two receipts. */
    val chatSK = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(50, "Pulei o pré-treino. No café comi 2 ovos mexidos e 1 pão francês.", "07:41"),
            ChatItem.Assistant(
                id = 51,
                text = "Pré-treino de hoje fora. Identifiquei 2 ovos mexidos e 1 pão francês. A estimativa total é de:",
                time = "07:42",
                highlights = listOf("2 ovos mexidos", "1 pão francês"),
                estimate = EstimateView(320, 17, 29, 16, null, null),
            ),
            ChatItem.Receipt(52, ReceiptKind.LOGGED, "Café da manhã", "07:30", 320, actions = recordActions),
            ChatItem.Receipt(53, ReceiptKind.SKIPPED, "Pré-treino", "06:00", null, actions = listOf(ReceiptAction.UNDO)),
        ),
        emptyDay = false,
        slots = slots,
    )

    /** chatSD (D19, A59): the lunch is skipped over its 640 kcal record; Excluir e pular asks inside the conversation. */
    val chatSD = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(60, "Acabei não almoçando hoje.", "14:04"),
            ChatItem.Assistant(61, "Almoço de hoje fora.", "14:05"),
            ChatItem.SkipDeletePrompt(61, SkipDeleteConfirm(slots[1], kcal = 640)),
        ),
        emptyDay = false,
        slots = slots,
    )

    private val pudding = ChatItem.User(20, "Também comi um pudim de leite no jantar", "21:02")
    private val puddingEstimate = EstimateView(620, 30, 82, 19, null, null)

    /** chatU (ST9, A34): the dinner already has 380 kcal; Substituir asks inside the conversation. */
    val chatU = ChatUiState(
        items = listOf(
            date,
            pudding,
            ChatItem.Assistant(21, "Juntei o pudim ao jantar. A estimativa total é de:", "21:02", estimate = puddingEstimate),
            ChatItem.ReplacePrompt(21, ReplaceConfirm(slots[3], oldKcal = 380, newKcal = 620)),
        ),
        emptyDay = false,
        slots = slots,
    )

    /** chatD (ST9, A34): the replacement undone, the dinner restored with the actions. */
    val chatD = ChatUiState(
        items = listOf(
            date,
            pudding,
            ChatItem.Assistant(21, "Juntei o pudim ao jantar.", "21:02", estimate = puddingEstimate),
            ChatItem.Receipt(22, ReceiptKind.REPLACED, "Jantar", "20:00", 620, fromKcal = 380, mark = ReceiptMark.UNDONE),
            ChatItem.Receipt(23, ReceiptKind.RESTORED, "Jantar", "20:00", 380, actions = recordActions),
        ),
        emptyDay = false,
        slots = slots,
    )

    /** chatI (D9, A47): a pudding added to the dinner that already has 380 kcal; Adicionar asks inside the conversation. */
    private const val PUDDING = "Pudim de leite, 1 fatia média (100 g)"
    val chatI = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(30, "Também comi uma fatia de pudim de leite no jantar", "21:02"),
            ChatItem.Assistant(
                id = 31,
                text = "",
                time = "21:02",
                estimate = EstimateView(240, 6, 38, 7, null, null, EstimateKind.ADDITION, PUDDING),
                prose = false,
            ),
            ChatItem.AdditionPrompt(31, AdditionConfirm(slots[3], previousKcal = 380, total = Macros(620, 28, 78, 21))),
        ),
        emptyDay = false,
        slots = slots,
    )

    /**
     * chatTI (D9, A47): Escolher outra refeição over chatI, directing the pudding only; Lanche picked. The frame blurs the
     * top of the chatI thread; with the confirmation below it the 844 dp thread would rest at its end, so the background
     * stops at the answer.
     */
    val chatTI = chatI.copy(
        items = chatI.items.filterNot { it is ChatItem.AdditionPrompt },
        sheetFor = 31,
        sheetSelection = 3,
        sheetCurrent = null,
        sheetAddition = SheetAddition(PUDDING, 240, "Jantar"),
    )

    /** chatIC (D9, A47): a correction of the dinner, Atualizar | Cancelar. */
    val chatIC = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(40, "Corrigindo: o omelete do jantar foi de 3 ovos, não 2", "21:10"),
            ChatItem.Assistant(
                id = 41,
                text = "",
                time = "21:10",
                estimate = EstimateView(455, 28, 40, 19, null, null, EstimateKind.REVISION, "Arroz, feijão e omelete de 3 ovos"),
                prose = false,
            ),
            ChatItem.RevisionPrompt(41, RevisionConfirm(slots[3], beforeKcal = 380, newKcal = 455)),
        ),
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

    /** chatF (ST9, A34): photo of a prato feito, recorded by itself in the Almoço; a photo has no Editar. */
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
                estimate = EstimateView(680, 48, 82, 18, null, null),
            ),
            ChatItem.Receipt(7, ReceiptKind.LOGGED, "Almoço", "12:30", 680, actions = listOf(ReceiptAction.DELETE, ReceiptAction.MOVE)),
        ),
        emptyDay = false,
        slots = slots,
        currentSlotId = 2,
    )

    private val optionsText = "Duas opções para o jantar:\n" +
        "- **Pizza de pão sírio**: 1 pão sírio (60 g), 30 g de molho de tomate, 100 g de frango desfiado, 30 g de milho e 30 g de muçarela · **420 kcal**\n" +
        "- **Omelete de forno**: 3 ovos, 50 g de ricota e 1 fatia de pão integral (25 g) · **360 kcal**\n" +
        "Primeira opção: ~**420 kcal** · 40P · 38C · 12G"

    private fun optionsBubble(reservedFor: String? = null) = ChatItem.Assistant(
        id = 8,
        text = optionsText,
        time = "20:15",
        plan = ProjectedDay(1640, Macros(2060, 126, 190, 58), 2200, Macros(0, 167, 223, 74)),
        blocks = app.fibrai.android.domain.ReplyMarkup.parse(optionsText),
        reservedFor = reservedFor,
    )

    private val ideas = ChatItem.User(7, "Não sei o que jantar. Me dá umas ideias?", "20:15")

    /** chatR (D17, D18, A60 parts C and D): two options as bullets with bold totals; Registrar assim and Reservar. */
    val chatR = ChatUiState(
        items = listOf(date, ideas, optionsBubble()),
        emptyDay = false,
        actions = EstimateActions(8, record = slots[3], plan = true, reserve = slots[3]),
        slots = slots,
        currentSlotId = 4,
    )

    /** chatO (D25, A67): an open request answered with two options, each with its items, total and own actions. */
    val chatO = ChatUiState(
        items = listOf(
            date,
            ideas,
            ChatItem.Assistant(
                id = 8,
                text = "Duas opções para o jantar:",
                time = "20:15",
                plan = ProjectedDay(1640, Macros(2060, 126, 190, 58), 2200, Macros(0, 167, 223, 74)),
                options = listOf(
                    OptionView(
                        "o1", "Opção 1: Pizza de pão sírio",
                        listOf("1 pão sírio (60 g)", "30 g de molho de tomate", "100 g de frango desfiado", "30 g de milho", "30 g de muçarela"),
                        420, 40, 38, 12, canRecord = true, canReserve = true,
                    ),
                    OptionView(
                        "o2", "Opção 2: Omelete de forno",
                        listOf("3 ovos", "50 g de ricota", "1 fatia de pão integral (25 g)"),
                        360, 28, 14, 20, canRecord = true, canReserve = true,
                    ),
                ),
            ),
        ),
        emptyDay = false,
        slots = slots,
        currentSlotId = 4,
    )

    /** chatRL (D18, A60 part D): the plan reserved for the dinner, Registrar assim kept. */
    val chatRL = chatR.copy(
        items = listOf(date, ideas, optionsBubble(reservedFor = "Jantar")),
        actions = EstimateActions(8, record = slots[3], plan = true),
    )

    private val recipeText = "Frango com brócolis e arroz\n" +
        "| Item | Gramas |\n| --- | --- |\n" +
        "| Peito de frango | 120 g |\n| Arroz cozido | 120 g |\n| Brócolis | 100 g |\n| Azeite | 5 g |\n| Alho | 5 g |\n" +
        "| Queijo ralado (opcional) | 15 g |\n" +
        "1. Corte o frango em cubos e grelhe por 8 min.\n" +
        "2. Refogue o alho no azeite e junte o brócolis por 3 min.\n" +
        "3. Misture o arroz e o frango e finalize com o queijo.\n" +
        "Total: ~**520 kcal** · 46P · 41C · 17G"

    /** chatRK (D17, A60 part C): a recipe with the portions table and numbered steps. */
    val chatRK = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(7, "Me passa uma receita de frango com brócolis pro jantar?", "20:15"),
            ChatItem.Assistant(
                id = 8,
                text = recipeText,
                time = "20:15",
                plan = ProjectedDay(1640, Macros(2160, 132, 193, 63), 2200, Macros(0, 167, 223, 74)),
                blocks = app.fibrai.android.domain.ReplyMarkup.parse(recipeText),
            ),
        ),
        emptyDay = false,
        actions = EstimateActions(8, record = slots[3], plan = true),
        slots = slots,
        currentSlotId = 4,
    )

    private val budgetText = "Macarrão com atum ao sugo:\n" +
        "- 80 g de macarrão cru\n" +
        "- 1 lata de atum em água (120 g)\n" +
        "- 150 g de molho de tomate\n" +
        "- 20 g de queijo ralado (opcional)\n" +
        "1. Cozinhe o macarrão por 9 min.\n" +
        "2. Aqueça o molho com o atum por 5 min.\n" +
        "3. Misture e finalize com o queijo.\n" +
        "Total: ~**620 kcal** · 42P · 70C · 18G"

    /** chatRB (D12, D20, A60 part A): the plan in D17 blocks over its window with a reserved slice of cake; the pills under it. */
    val chatRB = ChatUiState(
        items = listOf(
            date,
            ChatItem.User(7, "Me passa uma receita de macarrão com atum pro jantar? Mais tarde ainda como uma fatia de bolo.", "20:15"),
            ChatItem.Assistant(
                id = 8,
                text = budgetText,
                time = "20:15",
                plan = ProjectedDay(1640, Macros(2260, 128, 222, 64), 2200, Macros(0, 167, 223, 74)),
                blocks = app.fibrai.android.domain.ReplyMarkup.parse(budgetText),
                budget = BudgetNote(310, listOf(250 to "fatia de bolo")),
            ),
        ),
        emptyDay = false,
        actions = EstimateActions(8, record = slots[3], plan = true, choice = BudgetChoice(310, 310)),
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
        actions = EstimateActions(10, record = slots[0]),
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
