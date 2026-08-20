package com.clickdownloader.app

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

data class AppSettings(
    val wifiOnly: Boolean = false,
    val useAria2: Boolean = true,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val subtitles: Boolean = false,
    val sponsorBlock: Boolean = false,
    val outputTreeUri: String = "",
    val filenameTemplate: String = "%(title).180B [%(id)s].%(ext)s"
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val ARIA2 = booleanPreferencesKey("aria2")
        val THUMB = booleanPreferencesKey("thumb")
        val META = booleanPreferencesKey("meta")
        val SUBS = booleanPreferencesKey("subs")
        val SPONSOR = booleanPreferencesKey("sponsor")
        val TREE = stringPreferencesKey("tree_uri")
        val TEMPLATE = stringPreferencesKey("template")
    }

    val flow: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            wifiOnly = p[Keys.WIFI_ONLY] ?: false,
            useAria2 = p[Keys.ARIA2] ?: true,
            embedThumbnail = p[Keys.THUMB] ?: true,
            embedMetadata = p[Keys.META] ?: true,
            subtitles = p[Keys.SUBS] ?: false,
            sponsorBlock = p[Keys.SPONSOR] ?: false,
            outputTreeUri = p[Keys.TREE] ?: "",
            filenameTemplate = p[Keys.TEMPLATE] ?: "%(title).180B [%(id)s].%(ext)s"
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val current = AppSettings(
                wifiOnly = p[Keys.WIFI_ONLY] ?: false,
                useAria2 = p[Keys.ARIA2] ?: true,
                embedThumbnail = p[Keys.THUMB] ?: true,
                embedMetadata = p[Keys.META] ?: true,
                subtitles = p[Keys.SUBS] ?: false,
                sponsorBlock = p[Keys.SPONSOR] ?: false,
                outputTreeUri = p[Keys.TREE] ?: "",
                filenameTemplate = p[Keys.TEMPLATE] ?: "%(title).180B [%(id)s].%(ext)s"
            )
            val n = transform(current)
            p[Keys.WIFI_ONLY] = n.wifiOnly
            p[Keys.ARIA2] = n.useAria2
            p[Keys.THUMB] = n.embedThumbnail
            p[Keys.META] = n.embedMetadata
            p[Keys.SUBS] = n.subtitles
            p[Keys.SPONSOR] = n.sponsorBlock
            p[Keys.TREE] = n.outputTreeUri
            p[Keys.TEMPLATE] = n.filenameTemplate
        }
    }
}
