package com.nutri.android.feature.chat

import com.nutri.android.core.designsystem.aero.Aero
import com.nutri.android.core.designsystem.aero.AeroButtonPrimary
import com.nutri.android.core.designsystem.aero.AeroIcon
import com.nutri.android.core.designsystem.aero.AeroIconName
import com.nutri.android.core.designsystem.aero.AeroText
import com.nutri.android.core.designsystem.aero.aeroGlass
import com.nutri.android.core.designsystem.aero.AeroDimens
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotType
import com.nutri.android.core.designsystem.dietaClick
import com.nutri.android.core.photo.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Camera + gallery entry points. Lives in the route, not in [ChatScreen], so gold renders need no activity results. */
class PhotoLaunchers(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberPhotoLaunchers(vm: ChatViewModel): PhotoLaunchers {
    val context = LocalContext.current
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { vm.onCaptured(it) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) take.launch(vm.newCapture()) else vm.cameraDenied()
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> vm.onPicked(uri) }
    return remember(vm) {
        PhotoLaunchers(
            camera = {
                // CAMERA is declared, so TakePicture needs it granted (runtime dialog on first use).
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    take.launch(vm.newCapture())
                } else {
                    permission.launch(Manifest.permission.CAMERA)
                }
            },
            gallery = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}

/** ≤720 px previews, decoded off the main thread and kept while the thread scrolls. */
object PhotoPreviews {
    private val cache = LruCache<String, ImageBitmap>(24)

    fun cached(path: String): ImageBitmap? = cache.get(path)

    /** Blocking decode; call off the main thread (or in tests). */
    fun load(path: String): ImageBitmap? =
        cached(path) ?: PhotoStore.preview(path)?.asImageBitmap()?.also { cache.put(path, it) }
}

/** chatF, Chat/Photo: tinted glass user bubble (265 dp), the photo with the "Visão Computacional" tag, caption, time and ticks. */
@Composable
internal fun PhotoBubble(item: ChatItem.User, photoPath: String) {
    val c = Aero.colors
    val type = Aero.type
    val image by produceState(PhotoPreviews.cached(photoPath), photoPath) {
        if (value == null) value = withContext(Dispatchers.IO) { PhotoPreviews.load(photoPath) }
    }
    val r = AeroDimens.radiusCard
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier
                .width(265.dp)
                .aeroGlass(RoundedCornerShape(topStart = r, topEnd = r, bottomEnd = 6.dp, bottomStart = r), fill = c.surfaceTint)
                .padding(11.dp)
                .testTag("chat-user-${item.id}"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(135.dp).clip(PhotoShape).background(c.surface2).testTag("chat-photo-${item.id}")) {
                image?.let {
                    Image(it, contentDescription = "Foto da refeição", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp)
                        .height(26.dp)
                        .aeroGlass(CircleShape, shadow = false, backdropBlurred = true)
                        .padding(horizontal = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AeroIcon(AeroIconName.Sparkle, c.accentDefault, size = 12.dp)
                    AeroText("Visão Computacional", style = type.captionStrong.copy(color = c.textPrimary), maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                AeroText(item.text, Modifier.weight(1f), style = type.body.copy(color = c.textPrimary))
                AeroText(item.time, style = type.caption.copy(color = c.textDim))
                AeroIcon(AeroIconName.Checks, c.accentDefault, size = 14.dp)
            }
        }
    }
}

private val PhotoShape = RoundedCornerShape(12.dp)
private val ThumbShape = RoundedCornerShape(12.dp)

/** chatA, Chat/Composer State=Attached: 64 dp thumbnail and its ✕ (Remover foto) on the top-right corner. */
@Composable
internal fun AttachmentThumb(path: String, onRemove: () -> Unit) {
    val c = Aero.colors
    // key(path): a replaced attachment must not keep the previous photo (produceState keeps its value across keys).
    val image by key(path) {
        produceState(PhotoPreviews.cached(path), path) {
            if (value == null) value = withContext(Dispatchers.IO) { PhotoPreviews.load(path) }
        }
    }
    Box(Modifier.size(82.dp, 72.dp)) {
        Box(
            Modifier
                .offset(6.dp, 8.dp)
                .size(64.dp)
                .clip(ThumbShape)
                .background(c.surface2)
                .border(1.dp, c.borderLine, ThumbShape)
                .testTag("chat-attachment"),
        ) {
            image?.let {
                Image(it, contentDescription = "Foto anexada", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Box(
            Modifier
                .offset(56.dp, 0.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(c.surface2)
                .border(1.dp, c.borderLine, CircleShape)
                .dietaClick(onClick = onRemove)
                .testTag("chat-attachment-remove"),
            contentAlignment = Alignment.Center,
        ) {
            AeroIcon(AeroIconName.X, c.iconPrimary, size = 14.dp, contentDescription = "Remover foto")
        }
    }
}
