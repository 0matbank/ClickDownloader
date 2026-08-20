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
import android.net.Uri;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.yausername.youtubedl_android.YoutubeDL;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int BG = Color.rgb(11, 13, 16);
    private static final int SURFACE = Color.rgb(20, 24, 32);
    private static final int SURFACE_2 = Color.rgb(27, 34, 48);
    private static final int TEXT = Color.rgb(247, 248, 250);
    private static final int MUTED = Color.rgb(170, 178, 192);
    private static final int ACCENT = Color.rgb(91, 140, 255);
    private static final int DANGER = Color.rgb(255, 90, 107);

    private final ExecutorService analyzerExecutor = Executors.newSingleThreadExecutor();

    private EditText urlInput;
    private Spinner qualitySpinner;
    private Switch aria2Switch;
    private Switch thumbSwitch;
    private Switch metaSwitch;
    private Switch subsSwitch;
    private TextView statusText;
    private TextView infoText;
    private TextView cookieText;
    private TextView historyText;
    private ProgressBar progressBar;
    private Button downloadButton;
    private Button cancelButton;

    private ActivityResultLauncher<String[]> cookiePicker;

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

            if (status == null) {
                status = "Working";
            }

            if (eta >= 0) {
                statusText.setText(status + "\nETA: " + eta + " seconds");
            } else {
                statusText.setText(status);
            }

            if (status.contains("Queue completed")
                    || status.contains("Cancelled")
                    || status.contains("Failed")) {
                refreshHistory();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cookiePicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) {
                        return;
                    }

                    boolean ok = CookieStore.importFile(this, uri);
                    updateCookieText();

                    Toast.makeText(
                            this,
                            ok ? "cookies.txt imported" : "Cookie import failed",
                            Toast.LENGTH_LONG
                    ).show();
                }
        );

        setContentView(buildUi());
        consumeShareIntent(getIntent());
        requestNotificationPermissionIfNeeded();
        refreshHistory();
        updateCookieText();
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(DownloadService.ACTION_PROGRESS);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    progressReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
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
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(32));
        scroll.addView(root);

        TextView title = text("Click Downloader", 28, TEXT, true);
        root.addView(title);

        TextView subtitle = text(
                "Advanced media downloader • yt-dlp + FFmpeg + aria2",
                13,
                MUTED,
                false
        );
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        LinearLayout inputCard = card();
        root.addView(inputCard);

        inputCard.addView(sectionTitle("Media links"));

        urlInput = new EditText(this);
        urlInput.setTextColor(TEXT);
        urlInput.setHintTextColor(MUTED);
        urlInput.setHint("Paste one or multiple URLs, one per line");
        urlInput.setTextSize(15);
        urlInput.setMinLines(4);
        urlInput.setGravity(Gravity.TOP);
        urlInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_VARIATION_URI
        );
        urlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        urlInput.setBackground(roundRect(SURFACE_2, 14));
        inputCard.addView(
                urlInput,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        Button analyzeButton = button("Analyze first link", false);
        analyzeButton.setOnClickListener(v -> analyzeFirstUrl());
        addTopMargin(inputCard, analyzeButton, 10);

        infoText = text(
                "Paste a link and tap Analyze.",
                13,
                MUTED,
                false
        );
        infoText.setPadding(0, dp(10), 0, 0);
        inputCard.addView(infoText);

        LinearLayout optionsCard = card();
        addTopMargin(root, optionsCard, 14);

        optionsCard.addView(sectionTitle("Download options"));

        qualitySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                DownloadOptions.LABELS
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(TEXT);
                view.setTextSize(15);
                view.setPadding(dp(12), dp(10), dp(12), dp(10));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                view.setTextSize(15);
                view.setPadding(dp(12), dp(12), dp(12), dp(12));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qualitySpinner.setAdapter(adapter);
        qualitySpinner.setBackground(roundRect(SURFACE_2, 12));
        optionsCard.addView(qualitySpinner);

        aria2Switch = optionSwitch(
                "aria2 acceleration",
                "Parallel external downloader where supported.",
                true
        );
        addTopMargin(optionsCard, aria2Switch, 8);

        thumbSwitch = optionSwitch(
                "Embed thumbnail",
                "Attach thumbnail to supported output formats.",
                true
        );
        optionsCard.addView(thumbSwitch);

        metaSwitch = optionSwitch(
                "Embed metadata",
                "Write available title/uploader metadata.",
                true
        );
        optionsCard.addView(metaSwitch);

        subsSwitch = optionSwitch(
                "Subtitles",
                "Download and embed available subtitles.",
                false
        );
        optionsCard.addView(subsSwitch);

        LinearLayout authCard = card();
        addTopMargin(root, authCard, 14);

        authCard.addView(sectionTitle("Authentication cookies"));

        cookieText = text("", 13, MUTED, false);
        authCard.addView(cookieText);

        Button importCookies = button("Import cookies.txt", false);
        importCookies.setOnClickListener(v ->
                cookiePicker.launch(new String[]{"text/plain", "*/*"})
        );
        addTopMargin(authCard, importCookies, 8);

        Button clearCookies = button("Remove cookies", true);
        clearCookies.setOnClickListener(v -> {
            CookieStore.clear(this);
            updateCookieText();
            Toast.makeText(this, "Cookies removed", Toast.LENGTH_SHORT).show();
        });
        addTopMargin(authCard, clearCookies, 8);

        LinearLayout actionCard = card();
        addTopMargin(root, actionCard, 14);

        downloadButton = button("START DOWNLOAD QUEUE", false);
        downloadButton.setOnClickListener(v -> startDownloads());
        actionCard.addView(downloadButton);

        cancelButton = button("CANCEL ACTIVE DOWNLOAD", true);
        cancelButton.setOnClickListener(v -> cancelDownload());
        addTopMargin(actionCard, cancelButton, 8);

        progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );
        progressBar.setMax(100);
        progressBar.setProgress(0);
        addTopMargin(actionCard, progressBar, 14);

        statusText = text("Idle", 13, MUTED, false);
        statusText.setPadding(0, dp(10), 0, 0);
        actionCard.addView(statusText);

        LinearLayout historyCard = card();
        addTopMargin(root, historyCard, 14);

        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setOrientation(LinearLayout.HORIZONTAL);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView historyTitle = sectionTitle("History");
        historyHeader.addView(
                historyTitle,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                )
        );

        Button clearHistory = smallButton("Clear");
        clearHistory.setOnClickListener(v -> {
            HistoryStore.clear(this);
            refreshHistory();
        });
        historyHeader.addView(clearHistory);

        historyCard.addView(historyHeader);

        historyText = text("", 12, MUTED, false);
        historyText.setTextIsSelectable(true);
        historyCard.addView(historyText);

        TextView footer = text(
                "Downloads are saved to Internal Storage/Download/ClickDownloader. "
                        + "Use only for media you are allowed to download. "
                        + "This app does not include DRM or paywall bypass.",
                11,
                MUTED,
                false
        );
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        return scroll;
    }

    private void analyzeFirstUrl() {
        ArrayList<String> urls = extractUrls(urlInput.getText().toString());

        if (urls.isEmpty()) {
            urlInput.setError("Enter a valid http/https URL");
            return;
        }

        String url = urls.get(0);
        infoText.setText("Analyzing…");

        analyzerExecutor.execute(() -> {
            try {
                String title = YoutubeDL.getInstance()
                        .getInfo(url)
                        .getTitle();

                runOnUiThread(() ->
                        infoText.setText(
                                "Detected:\n"
                                        + (title == null || title.trim().isEmpty()
                                        ? url
                                        : title)
                        )
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                        infoText.setText(
                                "Analyze failed:\n"
                                        + (e.getMessage() == null
                                        ? e.getClass().getSimpleName()
                                        : e.getMessage())
                        )
                );
            }
        });
    }

    private void startDownloads() {
        ArrayList<String> urls = extractUrls(urlInput.getText().toString());

        if (urls.isEmpty()) {
            urlInput.setError("Enter at least one valid URL");
            return;
        }

        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_START);
        serviceIntent.putStringArrayListExtra(DownloadService.EXTRA_URLS, urls);
        serviceIntent.putExtra(
                DownloadService.EXTRA_QUALITY,
                qualitySpinner.getSelectedItemPosition()
        );
        serviceIntent.putExtra(
                DownloadService.EXTRA_ARIA2,
                aria2Switch.isChecked()
        );
        serviceIntent.putExtra(
                DownloadService.EXTRA_THUMB,
                thumbSwitch.isChecked()
        );
        serviceIntent.putExtra(
                DownloadService.EXTRA_META,
                metaSwitch.isChecked()
        );
        serviceIntent.putExtra(
                DownloadService.EXTRA_SUBS,
                subsSwitch.isChecked()
        );

        ContextCompat.startForegroundService(this, serviceIntent);

        statusText.setText(
                urls.size() == 1
                        ? "Queued 1 download"
                        : "Queued " + urls.size() + " downloads"
        );

        progressBar.setProgress(0);
    }

    private void cancelDownload() {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_CANCEL);
        startService(intent);
    }

    private void consumeShareIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())
                && "text/plain".equals(intent.getType())) {

            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (text != null && !text.trim().isEmpty()) {
                urlInput.setText(text);
            }
        }
    }

    private ArrayList<String> extractUrls(String text) {
        ArrayList<String> urls = new ArrayList<>();

        Pattern pattern = Pattern.compile("https?://[^\\s]+");
        Matcher matcher = pattern.matcher(text == null ? "" : text);

        while (matcher.find()) {
            String value = matcher.group();

            while (value.endsWith(",")
                    || value.endsWith(";")
                    || value.endsWith(")")
                    || value.endsWith("]")) {
                value = value.substring(0, value.length() - 1);
            }

            if (!urls.contains(value)) {
                urls.add(value);
            }
        }

        return urls;
    }

    private void updateCookieText() {
        cookieText.setText(
                CookieStore.exists(this)
                        ? "Private cookies.txt loaded."
                        : "No cookie file loaded."
        );
    }

    private void refreshHistory() {
        if (historyText == null) {
            return;
        }

        String history = HistoryStore.get(this);

        historyText.setText(
                history.trim().isEmpty()
                        ? "No completed downloads yet."
                        : history
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

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(roundRect(SURFACE, 18));
        return card;
    }

    private TextView sectionTitle(String value) {
        TextView v = text(value, 17, TEXT, true);
        v.setPadding(0, 0, 0, dp(10));
        return v;
    }

    private Switch optionSwitch(
            String title,
            String subtitle,
            boolean checked
    ) {
        Switch sw = new Switch(this);
        sw.setText(title + "\n" + subtitle);
        sw.setTextColor(TEXT);
        sw.setTextSize(14);
        sw.setChecked(checked);
        sw.setPadding(0, dp(7), 0, dp(7));
        return sw;
    }

    private Button button(String label, boolean danger) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setBackground(roundRect(danger ? DANGER : ACCENT, 14));
        return button;
    }

    private Button smallButton(String label) {
        Button button = button(label, true);
        button.setTextSize(12);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(42)
        );

        button.setLayoutParams(lp);
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
}
