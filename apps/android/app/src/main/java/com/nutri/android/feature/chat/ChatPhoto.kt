package com.nutri.android.feature.chat

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.nutri.android.core.designsystem.NutriType
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

private val PhotoBubbleShape = RoundedCornerShape(24.dp)
private val PhotoShape = RoundedCornerShape(14.dp)

/** chatF: photo card inside the user bubble, "Visão Computacional" tag, caption + time. */
@Composable
internal fun PhotoBubble(item: ChatItem.User, photoPath: String) {
    val p = LocalPalette.current
    val image by produceState(PhotoPreviews.cached(photoPath), photoPath) {
        if (value == null) value = withContext(Dispatchers.IO) { PhotoPreviews.load(photoPath) }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Column(
            Modifier
                .width(265.dp)
                .clip(PhotoBubbleShape)
                .background(p.surf2)
                .border(1.dp, p.line, PhotoBubbleShape)
                .padding(start = 11.5.dp, end = 11.5.dp, top = 11.dp, bottom = 4.dp)
                .testTag("chat-user-${item.id}"),
        ) {
            Box(Modifier.fillMaxWidth().height(135.dp).clip(PhotoShape).background(p.panel).testTag("chat-photo-${item.id}")) {
                image?.let {
                    Image(it, contentDescription = "Foto da refeição", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 7.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = p.gold, modifier = Modifier.size(12.dp))
                    Text("Visão Computacional", style = NutriType.labelMd.copy(fontSize = 9.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), color = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.text,
                    style = NutriType.bodyLg.copy(fontSize = 13.5.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
                    color = p.text,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(item.time, style = NutriType.labelMd.copy(fontSize = 10.5.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), color = p.muted)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = p.gold, modifier = Modifier.size(12.dp))
            }
        }
    }
}
