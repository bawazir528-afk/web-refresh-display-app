package com.bawazir.webrefresh;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * يحمّل الإعدادات من ملف config.json (إن وُجد) بترتيب الأولوية:
 *   1) /sdcard/web-refresh-display/config.json
 *   2) <external files dir>/config.json
 *   3) assets/config.json (افتراضي مضمّن)
 * إن لم يوجد أي ملف، يُستخدم ما في Prefs (أو قيم افتراضية).
 */
public final class ConfigLoader {

    private static final String TAG = "ConfigLoader";
    private static final String FILE_NAME = "config.json";

    private ConfigLoader() { }

    public static Config load(Context ctx) {
        Config cfg = new Config();

        // 1) مسار عام على بطاقة التخزين (سهل التعديل من قبل المشغّل)
        File pub = new File("/sdcard/web-refresh-display/" + FILE_NAME);
        if (tryFile(pub, cfg)) return cfg;

        // 2) مجلد ملفات التطبيق (Android/data/.../files)
        File ext = ctx.getExternalFilesDir(null);
        if (ext != null) {
            File f = new File(ext, FILE_NAME);
            if (tryFile(f, cfg)) return cfg;
        }

        // 3) ملفات داخلية
        File internal = new File(ctx.getFilesDir(), FILE_NAME);
        if (tryFile(internal, cfg)) return cfg;

        // 4) assets/config.json (افتراضي)
        try (InputStream is = ctx.getAssets().open(FILE_NAME)) {
            if (parseStream(is, cfg)) {
                Log.d(TAG, "loaded from assets");
                return cfg;
            }
        } catch (Exception ignored) { }

        // 5) Fallback إلى Prefs
        String u = Prefs.getUrl(ctx);
        if (u != null && !u.isEmpty()) cfg.url = u;
        int r = Prefs.getRefreshSeconds(ctx);
        if (r > 0) cfg.refreshSeconds = r;
        cfg.toolbar = Prefs.getToolbar(ctx);
        return cfg;
    }

    private static boolean tryFile(File f, Config cfg) {
        if (f == null || !f.exists() || !f.canRead()) return false;
        try (InputStream is = new FileInputStream(f)) {
            if (parseStream(is, cfg)) {
                Log.d(TAG, "loaded from " + f.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "failed reading " + f, e);
        }
        return false;
    }

    private static boolean parseStream(InputStream is, Config cfg) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) return false;
        JSONObject o = new JSONObject(text);
        if (o.has("url")) cfg.url = o.optString("url", cfg.url).trim();
        if (o.has("refreshSeconds")) cfg.refreshSeconds = o.optInt("refreshSeconds", cfg.refreshSeconds);
        if (o.has("toolbar")) cfg.toolbar = o.optBoolean("toolbar", cfg.toolbar);
        if (o.has("keepScreenOn")) cfg.keepScreenOn = o.optBoolean("keepScreenOn", cfg.keepScreenOn);
        if (o.has("fullscreen")) cfg.fullscreen = o.optBoolean("fullscreen", cfg.fullscreen);
        return !cfg.url.isEmpty();
    }
}