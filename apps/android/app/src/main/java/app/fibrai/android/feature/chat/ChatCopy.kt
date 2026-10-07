package app.fibrai.android.feature.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * A61 part B (ADR-048): Copiar puts the selected messages on the clipboard as one plain-text clip labelled `Fibrai`.
 * Android 13 and later confirm it with the system overlay; Android 12 and earlier get the app's own pill (chatCC).
 */
@Composable
fun rememberCopy(vm: ChatViewModel): () -> Unit {
    val context = LocalContext.current
    return remember(vm, context) {
        {
            val count = vm.uiState.value.selected.size
            vm.copySelection()?.let { text ->
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) vm.showCopied(count)
            }
        }
    }
}

private const val CLIP_LABEL = "Fibrai"
