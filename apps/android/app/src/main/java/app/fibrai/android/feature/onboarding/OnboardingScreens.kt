package app.fibrai.android.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.fibrai.android.R
import app.fibrai.android.core.designsystem.SplashBoot
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroBotLabel
import app.fibrai.android.core.designsystem.aero.AeroBubble
import app.fibrai.android.core.designsystem.aero.AeroBubbleSpec
import app.fibrai.android.core.designsystem.aero.AeroButtonPrimary
import app.fibrai.android.core.designsystem.aero.AeroChatBubble
import app.fibrai.android.core.designsystem.aero.AeroComposer
import app.fibrai.android.core.designsystem.aero.AeroIconName
import app.fibrai.android.core.designsystem.aero.AeroLoader
import app.fibrai.android.core.designsystem.aero.AeroOnboardingProgress
import app.fibrai.android.core.designsystem.aero.AeroPage
import app.fibrai.android.core.designsystem.aero.AeroPageBubbles
import app.fibrai.android.core.designsystem.aero.AeroQuickReply
import app.fibrai.android.core.designsystem.aero.AeroStatusScreen
import app.fibrai.android.core.designsystem.aero.AeroStatusTone
import app.fibrai.android.core.designsystem.aero.AeroSummaryBlock
import app.fibrai.android.core.designsystem.aero.AeroText
import app.fibrai.android.domain.OnboardingKind
import app.fibrai.android.domain.OnboardingScript
import app.fibrai.android.domain.OnboardingStep

/*
 * The conversational onboarding on Aero (A71, ADR-057): Figma `Design`, Release 2, section "Onboarding v2 · D27". Frames:
 * 20 dp margins, 24 dp from the top, 16 dp between blocks, the composer or the CTA 24–32 dp above the bottom.
 */

/** The edge bubbles of the ob0–ob3 frames. */
private val Bubbles = listOf(
    AeroBubbleSpec(200.dp, (-50).dp, 80.dp),
    AeroBubbleSpec(372.dp, 560.dp, 36.dp),
    AeroBubbleSpec((-18).dp, 420.dp, 36.dp),
)

/** The bot bubbles are 308 dp wide (D27); a user bubble hugs its text up to the same width. */
private val BubbleWidth = 308.dp

/** A short answer (`50 %`, `66 kg`) keeps the 120 dp of the gold's user bubble. */
private val UserBubbleMin = 120.dp

@androidx.compose.runtime.Immutable
data class OnboardingActions(
    val onStart: () -> Unit = {},
    val onComposer: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onReply: (String) -> Unit = {},
    val onBack: () -> Boolean = { false },
    val onEdit: (String) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onBackToSummary: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onNotificationsAsked: () -> Unit = {},
)

/** The screen of the current [OnboardingUiState.screen]. [animate] false freezes the logo (JVM gold renders). */
@Composable
fun OnboardingRoute(ui: OnboardingUiState, actions: OnboardingActions, animate: Boolean = true) {
    NotificationPermission(ui.askNotifications, actions.onNotificationsAsked)
    val context = LocalContext.current
    // The onboarding asked about notifications: the Home never asks again (A7 asked once from there).
    LaunchedEffect(ui.screen) {
        if (ui.screen == OnboardingScreen.SUCCESS) {
            context.getSharedPreferences("fibrai_push", android.content.Context.MODE_PRIVATE).edit().putBoolean("notifications_asked", true).apply()
        }
    }
    if (ui.screen == OnboardingScreen.CHAT || ui.screen == OnboardingScreen.SUMMARY) BackHandler { actions.onBack() }
    when (ui.screen) {
        OnboardingScreen.LOADING -> AeroPage(Modifier.fillMaxSize()) {}
        OnboardingScreen.WELCOME -> WelcomeScreen(actions.onStart, animate)
        OnboardingScreen.CHAT -> OnboardingChatScreen(ui, actions)
        OnboardingScreen.SUMMARY -> SummaryScreen(ui, actions.onEdit, actions.onConfirm)
        OnboardingScreen.BUILDING -> BuildingScreen(animate)
        OnboardingScreen.SUCCESS -> AeroStatusScreen(
            AeroStatusTone.Good,
            title = "Perfil pronto",
            body = "A Tali já sabe o seu teto, as suas refeições e o que você come." + if (ui.goalRefused) "\n${OnboardingScript.GOAL_REFUSED}" else "",
            primary = "Ir para a Home",
            onPrimary = actions.onHome,
            modifier = Modifier.fillMaxSize().testTag("ob5"),
            primaryTag = "ob5-home",
        )
        OnboardingScreen.ERROR -> AeroStatusScreen(
            AeroStatusTone.Bad,
            title = "Não deu certo",
            body = "Tente de novo mais tarde. Suas respostas ficam salvas.",
            primary = "Tentar de novo",
            onPrimary = actions.onRetry,
            secondary = "Voltar ao chat",
            onSecondary = actions.onBackToSummary,
            modifier = Modifier.fillMaxSize().testTag("ob6"),
            primaryTag = "ob6-retry",
            secondaryTag = "ob6-back",
        )
    }
}

/** A `sim` to the notifications asks POST_NOTIFICATIONS once, right there (Android 13+); the answer stays either way. */
@Composable
private fun NotificationPermission(ask: Boolean, onAsked: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        context.getSharedPreferences("fibrai_push", android.content.Context.MODE_PRIVATE).edit().putBoolean("notifications_asked", true).apply()
    }
    LaunchedEffect(ask) {
        if (!ask) return@LaunchedEffect
        onAsked()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** The logo with a slow float and breath in the theme (ADR-057 decision 1: the motion is the client's). */
@Composable
private fun AnimatedLogo(animate: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "logo")
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
        value
    } else {
        0f
    }
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = SplashBoot.LOGO_DESCRIPTION,
        modifier = modifier
            .size(SplashBoot.LOGO_DP.dp)
            .graphicsLayer {
                translationY = with(density) { (-6).dp.toPx() } * phase
                scaleX = 1f + 0.03f * phase
                scaleY = 1f + 0.03f * phase
            },
    )
}

/** ob0: the logo, `Bem-vindo ao Fibrai`, the pitch and `Vamos começar`. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, animate: Boolean = true) {
    val c = Aero.colors
    val type = Aero.type
    AeroPage(Modifier.fillMaxSize().testTag("ob0")) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            AnimatedLogo(animate)
            AeroText("Bem-vindo ao Fibrai", Modifier.fillMaxWidth(), style = type.title.copy(color = c.textPrimary, textAlign = TextAlign.Center))
            AeroText(
                "Umas perguntas rápidas e a Tali monta o seu perfil.",
                Modifier.fillMaxWidth(),
                style = type.body.copy(color = c.textMuted, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.weight(1f))
            AeroButtonPrimary("Vamos começar", onStart, icon = AeroIconName.ArrowRight, modifier = Modifier.testTag("ob0-start"))
        }
        AeroPageBubbles(Bubbles, null, Modifier.statusBarsPadding())
    }
}

/** ob4: the logo, `Estamos montando o seu perfil`, `Só mais alguns instantes…` and Loader/Aero, while the call runs. */
@Composable
fun BuildingScreen(animate: Boolean = true) {
    val c = Aero.colors
    val type = Aero.type
    AeroPage(Modifier.fillMaxSize().testTag("ob4")) {
        AeroBubble(80.dp, Modifier.offset(250.dp, (-30).dp))
        AeroBubble(46.dp, Modifier.offset(340.dp, 520.dp))
        AeroBubble(40.dp, Modifier.offset((-20).dp, 300.dp))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            AnimatedLogo(animate)
            AeroText("Estamos montando o seu perfil", Modifier.fillMaxWidth(), style = type.title.copy(color = c.textPrimary, textAlign = TextAlign.Center))
            AeroText("Só mais alguns instantes…", Modifier.fillMaxWidth(), style = type.body.copy(color = c.textMuted, textAlign = TextAlign.Center))
            AeroLoader()
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * ob1, ob1e, ob2: the progress, the thread (Tali's bubbles with the label, the user's bubbles, the quick replies under the
 * active question) and the text-only composer (no camera; the counter on free-text steps). The thread opens on the Tali
 * message before the latest user message, so each exchange starts at the top.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingChatScreen(ui: OnboardingUiState, actions: OnboardingActions) {
    val step = ui.step ?: return
    val list = rememberLazyListState()
    // Chat rule 3: sending closes the keyboard and takes the focus off the composer.
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val send = { focus.clearFocus(); actions.onSend() }
    LaunchedEffect(ui.messages.size, ui.anchor) {
        if (ui.messages.isNotEmpty()) list.scrollToItem(ui.anchor.coerceIn(0, ui.messages.lastIndex))
    }
    AeroPage(Modifier.fillMaxSize().testTag("ob-chat")) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
            AeroOnboardingProgress(
                step.section.label,
                step.number,
                OnboardingStep.TOTAL,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
            )
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("ob-thread"),
                state = list,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(ui.messages, key = { _, m -> m.key }) { _, m ->
                    if (m.fromUser) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            AeroChatBubble(m.text, m.time, fromUser = true, modifier = Modifier.widthIn(min = UserBubbleMin), maxWidth = BubbleWidth)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AeroBotLabel()
                            AeroChatBubble(m.text, m.time, fromUser = false, modifier = Modifier.width(BubbleWidth), maxWidth = BubbleWidth)
                        }
                    }
                }
                if (ui.quickReplies.isNotEmpty()) {
                    item(key = "replies") {
                        FlowRow(
                            Modifier.fillMaxWidth().testTag("ob-replies"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ui.quickReplies.forEachIndexed { i, label ->
                                AeroQuickReply(
                                    label,
                                    selected = label == ui.selectedReply,
                                    onClick = { focus.clearFocus(); actions.onReply(label) },
                                    modifier = Modifier.testTag("ob-reply-$i"),
                                )
                            }
                        }
                    }
                }
                // Room under the last message, so the anchor can reach the top of the viewport.
                item(key = "tail") { Spacer(Modifier.fillParentMaxHeight(0.9f)) }
            }
            AeroComposer(
                value = ui.composer,
                onValueChange = actions.onComposer,
                placeholder = "Escreva sua resposta",
                onCamera = {},
                onSend = send,
                sendEnabled = ui.canSend,
                showCamera = false,
                counter = ui.counter,
                counterOver = ui.tooLong,
                keyboardType = when {
                    step == OnboardingStep.WEIGHT -> KeyboardType.Decimal
                    step.kind == OnboardingKind.NUMBER -> KeyboardType.Number
                    else -> KeyboardType.Text
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp).testTag("ob-composer"),
            )
        }
        AeroPageBubbles(Bubbles, null, Modifier.statusBarsPadding())
    }
}

/** ob3: `Resumo do seu perfil`, one block per question group with its pencil, the caption and the confirm CTA. */
@Composable
fun SummaryScreen(ui: OnboardingUiState, onEdit: (String) -> Unit, onConfirm: () -> Unit) {
    val c = Aero.colors
    val type = Aero.type
    val scroll = rememberScrollState()
    AeroPage(Modifier.fillMaxSize().testTag("ob3"), scroll) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroText("Resumo do seu perfil", style = type.title.copy(color = c.textPrimary))
                AeroText("Confira antes de montar. Toque no lápis para corrigir.", style = type.body.copy(color = c.textMuted))
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.summary.forEach { block ->
                    AeroSummaryBlock(block.title, block.lines, onEdit = { onEdit(block.id) }, modifier = Modifier.testTag("ob3-edit-${block.id}"))
                }
            }
            AeroText("IMC e TMB são informação. Estimativa, não consulta.", style = type.caption.copy(color = c.textMuted))
            AeroButtonPrimary("Confirmar e montar o perfil", onConfirm, modifier = Modifier.testTag("ob3-confirm"))
        }
        AeroPageBubbles(Bubbles, scroll, Modifier.statusBarsPadding())
    }
}
