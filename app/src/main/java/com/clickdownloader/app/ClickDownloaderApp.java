package com.clickdownloader.app;

import android.app.Application;
import android.util.Log;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.aria2c.Aria2c;
import com.yausername.ffmpeg.FFmpeg;

public class ClickDownloaderApp extends Application {

    private static final String TAG = "ClickDownloader";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
            Aria2c.getInstance().init(this);
        } catch (Exception e) {
            Log.e(TAG, "Downloader engine initialization failed", e);
        }
    }
}
