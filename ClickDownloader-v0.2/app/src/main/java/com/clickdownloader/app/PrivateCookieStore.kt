package com.clickdownloader.app

import android.content.Context
import android.net.Uri

class PrivateCookieStore(private val context: Context) {
    private val cookieFile = context.filesDir.resolve("cookies.txt")

    fun importFrom(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open cookie file" }
            cookieFile.outputStream().use { output -> input.copyTo(output) }
        }
        cookieFile.setReadable(false, false)
        cookieFile.setReadable(true, true)
        true
    }.getOrDefault(false)

    fun pathOrNull(): String? =
        cookieFile.takeIf { it.exists() && it.length() > 0 }?.absolutePath

    fun exists(): Boolean = cookieFile.exists() && cookieFile.length() > 0

    fun clear() {
        if (cookieFile.exists()) cookieFile.delete()
    }
}
