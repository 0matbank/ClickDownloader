package com.clickdownloader.app

data class DownloadRequestModel(
    val url: String,
    val preset: DownloadPreset,
    val useAria2: Boolean = true,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val writeSubtitles: Boolean = false,
    val sponsorBlock: Boolean = false,
    val cookiePath: String? = null,
    val filenameTemplate: String = "%(title).180B [%(id)s].%(ext)s"
)
