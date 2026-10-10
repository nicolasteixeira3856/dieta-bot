package app.fibrai.android.feature.onboarding

import androidx.compose.runtime.Immutable
import app.fibrai.android.core.network.ProfileIn
import app.fibrai.android.core.network.ProfileOut
import app.fibrai.android.domain.OnboardingStep
import app.fibrai.android.domain.SummaryBlock

/** A71 (S41): `POST /v1/profile`, once, on the summary's confirm. */
fun interface ProfileService {
    suspend fun build(body: ProfileIn): ProfileOut
}

/** The onboarding's screens (D27): welcome `ob0`, chat `ob1`/`ob1e`/`ob2`, summary `ob3`, building `ob4`, success `ob5`, error `ob6`. */
enum class OnboardingScreen { LOADING, WELCOME, CHAT, SUMMARY, BUILDING, SUCCESS, ERROR }

/** One bubble of the onboarding chat. [anchor]: the thread scrolls so this bubble opens the viewport (the gold's layout). */
@Immutable
data class OnboardingMessage(val key: String, val text: String, val fromUser: Boolean, val time: String)

@Immutable
data class OnboardingUiState(
    val screen: OnboardingScreen = OnboardingScreen.LOADING,
    val messages: List<OnboardingMessage> = emptyList(),
    /** Index in [messages] of the bubble that opens the viewport: the Tali message before the latest user message. */
    val anchor: Int = 0,
    val step: OnboardingStep? = null,
    val quickReplies: List<String> = emptyList(),
    val selectedReply: String? = null,
    val composer: String = "",
    val summary: List<SummaryBlock> = emptyList(),
    val goalRefused: Boolean = false,
    /** A `sim` to the notifications: the screen asks POST_NOTIFICATIONS once (Android 13+). */
    val askNotifications: Boolean = false,
) {
    val composerLength: Int get() = composer.trim().let { it.codePointCount(0, it.length) }
    val tooLong: Boolean get() = composerLength > app.fibrai.android.domain.OnboardingScript.TEXT_MAX
    val canSend: Boolean get() = composer.isNotBlank() && !tooLong
    val counter: String? get() = step?.takeIf { it.counted }?.let { "$composerLength/${app.fibrai.android.domain.OnboardingScript.TEXT_MAX}" }
}
