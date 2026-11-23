package com.bawazir.webrefresh;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";

    private WebView web;
    private ProgressBar progress;
    private View errorBox;
    private TextView errorDetail;
    private View toolbar;
    private Button refreshBtn;
    private Button settingsBtn;
    private Button exitBtn;
    private TextView statusText;

    private Config config;
    private String url;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;

    private long lastRefreshAt = 0;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        config = ConfigLoader.load(this);

        url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) url = config.url;
        if (url == null || url.isEmpty()) url = Prefs.getUrl(this);
        config.url = url;

        if (url == null || url.isEmpty()) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        if (config.keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_main);

        web = findViewById(R.id.web);
        progress = findViewById(R.id.progress);
        errorBox = findViewById(R.id.errorBox);
        errorDetail = findViewById(R.id.errorDetail);
        toolbar = findViewById(R.id.toolbar);
        refreshBtn = findViewById(R.id.refreshBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        exitBtn = findViewById(R.id.exitBtn);
        statusText = findViewById(R.id.statusText);

        toolbar.setVisibility(config.toolbar ? View.VISIBLE : View.GONE);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (u == null) return false;
                String host = u.getHost();
                if (host != null && url != null && host.equals(Uri.parse(url).getHost())) return false;
                if (host != null && (host.contains("accounts.") || host.contains("login"))) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String u) {
                progress.setVisibility(View.GONE);
                errorBox.setVisibility(View.GONE);
                markRefreshed();
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (req != null && req.isForMainFrame()) {
                    showError(err != null ? String.valueOf(err.getDescription()) : "unknown");
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView v, int errorCode, String description, String failingUrl) {
                if (errorCode != -1) showError(errorCode + ": " + description);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
                progress.setProgress(p);
            }
        });

        web.addJavascriptInterface(new Bridges(), "AndroidApp");

        errorBox.findViewById(R.id.retry).setOnClickListener(b -> load());
        errorBox.findViewById(R.id.changeUrl).setOnClickListener(b -> {
            startActivity(new Intent(this, SetupActivity.class));
        });

        refreshBtn.setOnClickListener(b -> refreshNow());
        settingsBtn.setOnClickListener(b -> startActivity(new Intent(this, SetupActivity.class)));
        if (exitBtn != null) exitBtn.setOnClickListener(b -> finishAndRemoveTask());

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            load();
        }

        scheduleAutoRefresh();
        applyFullscreen();
    }

    private void load() {
        errorBox.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        web.loadUrl(url);
    }

    private void showError(String detail) {
        progress.setVisibility(View.GONE);
        errorBox.setVisibility(View.VISIBLE);
        if (errorDetail != null) errorDetail.setText(detail == null ? "" : detail);
    }

    private void markRefreshed() {
        lastRefreshAt = System.currentTimeMillis();
        if (statusText != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            statusText.setText(getString(R.string.last_refresh, sdf.format(new Date(lastRefreshAt))));
        }
    }

    private void refreshNow() {
        progress.setVisibility(View.VISIBLE);
        web.evaluateJavascript(
                "(function(){try{if(typeof window.onRefresh==='function'){window.onRefresh();return 'js';}}catch(e){return 'err';} " +
                        "return 'reload';})();",
                v -> {
                    String r = (v == null) ? "" : v;
                    if (r.contains("reload") || r.contains("err") || r.contains("null")) {
                        load();
                    }
                    if (r.contains("js")) {
                        markRefreshed();
                    }
                });
        Toast.makeText(this, R.string.refreshing, Toast.LENGTH_SHORT).show();
    }

    private void scheduleAutoRefresh() {
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshNow();
                handler.postDelayed(this, config.refreshMs());
            }
        };
        handler.postDelayed(refreshRunnable, config.refreshMs());
    }

    private void applyFullscreen() {
        if (!config.fullscreen) return;
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decor = w.getDecorView();
        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decor.setSystemUiVisibility(flags);
        decor.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decor.setSystemUiVisibility(flags);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFullscreen();
        if (refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
            handler.postDelayed(refreshRunnable, config.refreshMs());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshRunnable != null) handler.removeCallbacks(refreshRunnable);
        if (web != null) {
            web.destroy();
            web = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            load();
        }
    }

    private class Bridges {
        @android.webkit.JavascriptInterface
        public void refresh() { runOnUiThread(() -> refreshNow()); }

        @android.webkit.JavascriptInterface
        public void reload() { runOnUiThread(() -> load()); }

        @android.webkit.JavascriptInterface
        public String getUrl() { return url; }

        @android.webkit.JavascriptInterface
        public void setRefreshSeconds(int s) {
            runOnUiThread(() -> {
                if (s >= 5) {
                    config.refreshSeconds = s;
                    Prefs.setRefreshSeconds(MainActivity.this, s);
                    scheduleAutoRefresh();
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void toast(String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }

        @android.webkit.JavascriptInterface
        public void closeApp() {
            runOnUiThread(MainActivity.this::finishAndRemoveTask);
        }

        @android.webkit.JavascriptInterface
        public void openSettings() {
            runOnUiThread(() -> startActivity(new Intent(MainActivity.this, SetupActivity.class)));
        }
    }
}