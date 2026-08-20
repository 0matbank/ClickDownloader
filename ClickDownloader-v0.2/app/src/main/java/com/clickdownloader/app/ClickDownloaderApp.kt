package com.clickdownloader.app

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.aria2c.Aria2c

class ClickDownloaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Aria2c.getInstance().init(this)
        } catch (t: Throwable) {
            Log.e("ClickDownloader", "Downloader engine init failed", t)
        }
    }
}
