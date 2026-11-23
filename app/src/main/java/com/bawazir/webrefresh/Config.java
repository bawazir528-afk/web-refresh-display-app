package com.bawazir.webrefresh;

/** إعدادات الشاشة: رابط الصفحة، فاصل التحديث، إظهار شريط الأدوات، بقاء الشاشة مضاءة. */
public class Config {
    public String url = "";
    public int refreshSeconds = 60;
    public boolean toolbar = true;
    public boolean keepScreenOn = true;
    public boolean fullscreen = true;

    public int refreshMs() {
        int s = refreshSeconds <= 0 ? 60 : refreshSeconds;
        if (s < 5) s = 5;
        return s * 1000;
    }
}