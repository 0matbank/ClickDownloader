package com.clickdownloader.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.yausername.youtubedl_android.YoutubeDL;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int BG = Color.rgb(7, 9, 18);
    private static final int CARD = Color.rgb(20, 24, 36);
    private static final int INPUT = Color.rgb(29, 35, 50);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(165, 174, 190);
    private static final int BLUE = Color.rgb(82, 132, 255);
    private static final int RED = Color.rgb(255, 86, 104);

    private final ExecutorService analyzerExecutor = Executors.newSingleThreadExecutor();

    private EditText urlInput;
    private TextView mediaTitle;
    private TextView statusText;
    private Spinner resolutionSpinner;
    private Button downloadButton;
    private Button cancelButton;
    private ProgressBar progressBar;
    private LinearLayout downloadPanel;

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DownloadService.ACTION_PROGRESS.equals(intent.getAction())) {
                return;
            }

            String status = intent.getStringExtra(DownloadService.EXTRA_STATUS);
            int progress = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, 0);
            long eta = intent.getLongExtra(DownloadService.EXTRA_ETA, -1);

            progressBar.setProgress(progress);

            if (status == null || status.trim().isEmpty()) {
                status = "Downloading…";
            }

            if (eta >= 0) {
                statusText.setText(status + "\nETA: " + eta + "s");
            } else {
                statusText.setText(status);
            }

            String lower = status.toLowerCase();
            boolean finished = lower.contains("completed")
                    || lower.contains("failed")
                    || lower.contains("cancelled");

            cancelButton.setVisibility(finished ? View.GONE : View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        consumeShareIntent(getIntent());
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(DownloadService.ACTION_PROGRESS);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(progressReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(progressReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        consumeShareIntent(intent);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(36));
        scroll.addView(root);

        TextView title = text("Click Downloader", 30, TEXT, true);
        root.addView(title);

        TextView subtitle = text(
                "Paste link, choose quality, download.",
                14,
                MUTED,
                false
        );
        subtitle.setPadding(0, dp(5), 0, dp(22));
        root.addView(subtitle);

        LinearLayout card = card();
        root.addView(card);

        TextView linkLabel = text("Video link", 16, TEXT, true);
        linkLabel.setPadding(0, 0, 0, dp(10));
        card.addView(linkLabel);

        urlInput = new EditText(this);
        urlInput.setTextColor(TEXT);
        urlInput.setHintTextColor(MUTED);
        urlInput.setHint("Paste YouTube or supported media link");
        urlInput.setTextSize(15);
        urlInput.setSingleLine(false);
        urlInput.setMinLines(2);
        urlInput.setGravity(Gravity.TOP);
        urlInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_VARIATION_URI
        );
        urlInput.setPadding(dp(14), dp(13), dp(14), dp(13));
        urlInput.setBackground(roundRect(INPUT, 14));
        card.addView(
                urlInput,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        Button analyzeButton = blueButton("ANALYZE LINK");
        analyzeButton.setOnClickListener(v -> analyzeLink());
        addTopMargin(card, analyzeButton, 12);

        mediaTitle = text(
                "Video information will appear here.",
                13,
                MUTED,
                false
        );
        mediaTitle.setPadding(0, dp(12), 0, 0);
        card.addView(mediaTitle);

        downloadPanel = card();
        downloadPanel.setVisibility(View.GONE);
        addTopMargin(root, downloadPanel, 14);

        TextView qualityLabel = text("Select quality", 16, TEXT, true);
        qualityLabel.setPadding(0, 0, 0, dp(10));
        downloadPanel.addView(qualityLabel);

        resolutionSpinner = new Spinner(this);

        String[] simpleLabels = {
                "Best available",
                "1080p",
                "720p",
                "480p",
                "360p",
                "Audio MP3"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                simpleLabels
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent
            ) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(TEXT);
                view.setTextSize(16);
                view.setPadding(dp(14), dp(13), dp(14), dp(13));
                return view;
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent
            ) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                view.setTextSize(16);
                view.setPadding(dp(14), dp(14), dp(14), dp(14));
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSpinner.setAdapter(adapter);
        resolutionSpinner.setBackground(roundRect(INPUT, 14));

        downloadPanel.addView(
                resolutionSpinner,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(54)
                )
        );

        downloadButton = blueButton("DOWNLOAD");
        downloadButton.setOnClickListener(v -> startDownload());
        addTopMargin(downloadPanel, downloadButton, 14);

        cancelButton = redButton("CANCEL");
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v -> cancelDownload());
        addTopMargin(downloadPanel, cancelButton, 8);

        progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );
        progressBar.setMax(100);
        progressBar.setProgress(0);
        addTopMargin(downloadPanel, progressBar, 16);

        statusText = text("Ready", 13, MUTED, false);
        statusText.setPadding(0, dp(10), 0, 0);
        downloadPanel.addView(statusText);

        TextView footer = text(
                "Downloads are saved inside the app's Android storage folder. "
                        + "Use only for media you are allowed to download.",
                11,
                MUTED,
                false
        );
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        return scroll;
    }

    private void analyzeLink() {
        String url = extractFirstUrl(urlInput.getText().toString());

        if (url == null) {
            urlInput.setError("Paste a valid http/https link");
            return;
        }

        mediaTitle.setText("Analyzing…");
        downloadPanel.setVisibility(View.GONE);

        analyzerExecutor.execute(() -> {
            try {
                String title = YoutubeDL.getInstance()
                        .getInfo(url)
                        .getTitle();

                runOnUiThread(() -> {
                    mediaTitle.setText(
                            title == null || title.trim().isEmpty()
                                    ? "Link detected successfully."
                                    : title
                    );
                    downloadPanel.setVisibility(View.VISIBLE);
                    statusText.setText("Choose quality and press Download.");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    mediaTitle.setText(
                            "Could not analyze link:\n"
                                    + readableError(e)
                    );
                    downloadPanel.setVisibility(View.GONE);
                });
            }
        });
    }

    private void startDownload() {
        String url = extractFirstUrl(urlInput.getText().toString());

        if (url == null) {
            urlInput.setError("Paste a valid link first");
            return;
        }

        ArrayList<String> urls = new ArrayList<>();
        urls.add(url);

        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_START);
        intent.putStringArrayListExtra(DownloadService.EXTRA_URLS, urls);
        intent.putExtra(
                DownloadService.EXTRA_QUALITY,
                resolutionSpinner.getSelectedItemPosition()
        );

        // Simple mode: native yt-dlp downloader only.
        intent.putExtra(DownloadService.EXTRA_ARIA2, false);

        // Keep useful metadata features automatic, with no extra UI.
        intent.putExtra(DownloadService.EXTRA_THUMB, true);
        intent.putExtra(DownloadService.EXTRA_META, true);
        intent.putExtra(DownloadService.EXTRA_SUBS, false);

        ContextCompat.startForegroundService(this, intent);

        progressBar.setProgress(0);
        statusText.setText("Starting download…");
        cancelButton.setVisibility(View.VISIBLE);
        downloadButton.setEnabled(false);

        // Re-enable shortly through UI state rather than blocking another task.
        downloadButton.postDelayed(
                () -> downloadButton.setEnabled(true),
                1500
        );
    }

    private void cancelDownload() {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_CANCEL);
        startService(intent);
        cancelButton.setVisibility(View.GONE);
    }

    private void consumeShareIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())
                && "text/plain".equals(intent.getType())) {

            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (shared != null && !shared.trim().isEmpty()) {
                urlInput.setText(shared);
            }
        }
    }

    private String extractFirstUrl(String text) {
        Pattern pattern = Pattern.compile("https?://[^\\s]+");
        Matcher matcher = pattern.matcher(text == null ? "" : text);

        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group();

        while (value.endsWith(",")
                || value.endsWith(";")
                || value.endsWith(")")
                || value.endsWith("]")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    private String readableError(Exception e) {
        if (e == null) {
            return "Unknown error";
        }

        String message = e.getMessage();

        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }

        message = message.replace('\n', ' ').replace('\r', ' ').trim();

        if (message.length() > 200) {
            message = message.substring(0, 200) + "…";
        }

        return message;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundRect(CARD, 18));
        return card;
    }

    private Button blueButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(BLUE, 14));
        return button;
    }

    private Button redButton(String label) {
        Button button = blueButton(label);
        button.setBackground(roundRect(RED, 14));
        return button;
    }

    private TextView text(
            String value,
            float size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void addTopMargin(
            LinearLayout parent,
            View child,
            int marginDp
    ) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.topMargin = dp(marginDp);
        parent.addView(child, params);
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    200
            );
        }
    }
}
