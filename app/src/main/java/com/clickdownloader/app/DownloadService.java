package com.clickdownloader.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;

public class DownloadService extends Service {

    public static final String ACTION_START = "com.clickdownloader.app.START";
    public static final String ACTION_CANCEL = "com.clickdownloader.app.CANCEL";
    public static final String ACTION_PROGRESS = "com.clickdownloader.app.PROGRESS";

    public static final String EXTRA_URLS = "urls";
    public static final String EXTRA_QUALITY = "quality";
    public static final String EXTRA_ARIA2 = "aria2";
    public static final String EXTRA_THUMB = "thumb";
    public static final String EXTRA_META = "meta";
    public static final String EXTRA_SUBS = "subs";

    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_ETA = "eta";

    private static final String CHANNEL_ID = "click_downloader_downloads";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "DownloadService";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile String activeProcessId;
    private volatile boolean cancelled;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        if (ACTION_CANCEL.equals(intent.getAction())) {
            cancelled = true;

            String processId = activeProcessId;
            if (processId != null) {
                try {
                    YoutubeDL.getInstance().destroyProcessById(processId);
                } catch (Exception ignored) {
                }
            }

            sendStatus("Cancelled", 0, -1);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!ACTION_START.equals(intent.getAction())) {
            return START_NOT_STICKY;
        }

        ArrayList<String> urls = intent.getStringArrayListExtra(EXTRA_URLS);
        if (urls == null || urls.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int qualityIndex = intent.getIntExtra(EXTRA_QUALITY, 0);
        boolean aria2 = intent.getBooleanExtra(EXTRA_ARIA2, true);
        boolean thumb = intent.getBooleanExtra(EXTRA_THUMB, true);
        boolean meta = intent.getBooleanExtra(EXTRA_META, true);
        boolean subs = intent.getBooleanExtra(EXTRA_SUBS, false);

        cancelled = false;

        startAsForeground("Preparing downloads", 0, -1);

        executor.execute(() ->
                runQueue(urls, qualityIndex, aria2, thumb, meta, subs)
        );

        return START_NOT_STICKY;
    }

    private void runQueue(
            ArrayList<String> urls,
            int qualityIndex,
            boolean aria2,
            boolean thumb,
            boolean meta,
            boolean subs
    ) {
        File outputDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ClickDownloader"
        );

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            sendStatus("Cannot create Downloads/ClickDownloader", 0, -1);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }

        int total = urls.size();

        for (int i = 0; i < total; i++) {
            if (cancelled) {
                break;
            }

            String url = urls.get(i);
            String queuePrefix = "[" + (i + 1) + "/" + total + "] ";
            activeProcessId = "click_" + System.currentTimeMillis() + "_" + i;

            try {
                YoutubeDLRequest request = new YoutubeDLRequest(url);

                request.addOption("--no-warnings");
                request.addOption("--newline");
                request.addOption("--continue");
                request.addOption("--part");
                request.addOption("--retries", "10");
                request.addOption("--fragment-retries", "10");
                request.addOption("--concurrent-fragments", "4");

                request.addOption("-f", DownloadOptions.formatForIndex(qualityIndex));
                request.addOption(
                        "-o",
                        new File(
                                outputDir,
                                "%(title).180B [%(id)s].%(ext)s"
                        ).getAbsolutePath()
                );

                if (DownloadOptions.isAudioOnly(qualityIndex)) {
                    request.addOption("--extract-audio");
                    request.addOption("--audio-format", "mp3");
                    request.addOption("--audio-quality", "0");
                } else {
                    request.addOption("--merge-output-format", "mp4");
                }

                if (aria2) {
                    request.addOption("--downloader", "libaria2c.so");
                }

                if (thumb) {
                    request.addOption("--embed-thumbnail");
                }

                if (meta) {
                    request.addOption("--embed-metadata");
                }

                if (subs) {
                    request.addOption("--write-subs");
                    request.addOption("--write-auto-subs");
                    request.addOption("--sub-langs", "all,-live_chat");
                    request.addOption("--embed-subs");
                }

                if (CookieStore.exists(this)) {
                    request.addOption(
                            "--cookies",
                            CookieStore.getFile(this).getAbsolutePath()
                    );
                }

                Function3<Float, Long, String, Unit> callback =
                        new Function3<Float, Long, String, Unit>() {
                            @Override
                            public Unit invoke(Float progress, Long eta, String line) {
                                int p = progress == null ? 0 : Math.round(progress);
                                long etaValue = eta == null ? -1 : eta;

                                String status = queuePrefix
                                        + (line == null || line.trim().isEmpty()
                                        ? "Downloading"
                                        : line);

                                updateForeground(status, p, etaValue);
                                sendStatus(status, p, etaValue);
                                return Unit.INSTANCE;
                            }
                        };

                YoutubeDL.getInstance().execute(
                        request,
                        activeProcessId,
                        callback
                );

                HistoryStore.add(
                        this,
                        "Completed",
                        url,
                        true
                );

                sendStatus(queuePrefix + "Completed", 100, 0);

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);

                if (!cancelled) {
                    HistoryStore.add(
                            this,
                            "Failed: " + e.getMessage(),
                            url,
                            false
                    );

                    sendStatus(
                            queuePrefix + "Failed: "
                                    + (e.getMessage() == null
                                    ? e.getClass().getSimpleName()
                                    : e.getMessage()),
                            0,
                            -1
                    );
                }
            }
        }

        activeProcessId = null;

        if (!cancelled) {
            sendStatus("Queue completed", 100, 0);
            updateForeground("Queue completed", 100, 0);
        }

        stopForeground(STOP_FOREGROUND_DETACH);
        stopSelf();
    }

    private void sendStatus(String status, int progress, long eta) {
        Intent broadcast = new Intent(ACTION_PROGRESS);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_STATUS, status);
        broadcast.putExtra(EXTRA_PROGRESS, progress);
        broadcast.putExtra(EXTRA_ETA, eta);
        sendBroadcast(broadcast);
    }

    private void startAsForeground(String status, int progress, long eta) {
        Notification notification = buildNotification(status, progress, eta);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateForeground(String status, int progress, long eta) {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(
                    NOTIFICATION_ID,
                    buildNotification(status, progress, eta)
            );
        }
    }

    private Notification buildNotification(String status, int progress, long eta) {
        Intent openIntent = new Intent(this, MainActivity.class);

        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent cancelIntent = new Intent(this, DownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL);

        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this,
                1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String content = status;
        if (eta >= 0) {
            content = status + " • ETA " + eta + "s";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Click Downloader")
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(openPendingIntent)
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Cancel",
                        cancelPendingIntent
                )
                .setOnlyAlertOnce(true)
                .setOngoing(progress < 100)
                .setProgress(100, Math.max(0, Math.min(progress, 100)), false)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Downloads",
                        NotificationManager.IMPORTANCE_LOW
                );

                channel.setDescription("Click Downloader progress");
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        String processId = activeProcessId;
        if (cancelled && processId != null) {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId);
            } catch (Exception ignored) {
            }
        }
    }
}
