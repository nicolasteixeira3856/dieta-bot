package app.fibrai.android.core.photo

import android.net.Uri
import java.io.File

/** Scripted [PhotoFiles] for ViewModel tests. */
class FakePhotoFiles : PhotoFiles {
    var nextImport: PhotoResult = PhotoResult.Failed
    val deleted = mutableListOf<String>()

    override fun newCapture(): Pair<File, Uri> = File("/tmp/capture.jpg") to Uri.parse("content://test/capture.jpg")

    override suspend fun acceptCapture(file: File): PhotoResult = nextImport

    override suspend fun import(uri: Uri): PhotoResult = nextImport

    override suspend fun base64(path: String): String? = "B64:$path"

    override fun delete(path: String) {
        deleted += path
    }
}
