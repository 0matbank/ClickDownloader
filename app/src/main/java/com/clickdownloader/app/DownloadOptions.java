package com.clickdownloader.app;

public final class DownloadOptions {

    public static final String[] LABELS = {
            "Auto / Best",
            "1080p",
            "720p",
            "480p",
            "360p",
            "Audio MP3"
    };

    private DownloadOptions() {
    }

    public static String formatForIndex(int index) {
        switch (index) {
            case 1:
                return "bestvideo*[height<=1080]+bestaudio/best[height<=1080]";
            case 2:
                return "bestvideo*[height<=720]+bestaudio/best[height<=720]";
            case 3:
                return "bestvideo*[height<=480]+bestaudio/best[height<=480]";
            case 4:
                return "bestvideo*[height<=360]+bestaudio/best[height<=360]";
            case 5:
                return "bestaudio/best";
            default:
                return "bestvideo*+bestaudio/best";
        }
    }

    public static boolean isAudioOnly(int index) {
        return index == 5;
    }
}
