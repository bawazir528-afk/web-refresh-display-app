package com.bawazir.webrefresh;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class Prefs {
    private static final String FILE = "web_refresh_display";
    private static final String KEY_URL = "page_url";
    private static final String KEY_REFRESH = "refresh_seconds";
    private static final String KEY_TOOLBAR = "show_toolbar";

    private Prefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getUrl(Context c) { return sp(c).getString(KEY_URL, ""); }
    public static void setUrl(Context c, String url) {
        sp(c).edit().putString(KEY_URL, url == null ? "" : url.trim()).apply();
    }

    public static int getRefreshSeconds(Context c) { return sp(c).getInt(KEY_REFRESH, 60); }
    public static void setRefreshSeconds(Context c, int s) {
        if (s < 5) s = 5;
        sp(c).edit().putInt(KEY_REFRESH, s).apply();
    }

    public static boolean getToolbar(Context c) { return sp(c).getBoolean(KEY_TOOLBAR, true); }
    public static void setToolbar(Context c, boolean v) { sp(c).edit().putBoolean(KEY_TOOLBAR, v).apply(); }

    public static boolean isValidUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        String u = url.trim().toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://");
    }
}