# تطبيق شاشة ذكية — Web Refresh Display

تطبيق أندرويد يعرض صفحة ويب داخل WebView ويقوم بتحديثها تلقائياً وفق فاصل قابل للضبط، مع دعم التحديث اليدوي وتحديث جزئي للبيانات.

## الميزات
- تحميل رابط من ملف config.json (أو Prefs كـ fallback).
- تحديث تلقائي دوري (فاصل قابل للضبط، افتراضي 60 ثانية).
- زر تحديث يدوي + زر إعدادات.
- جسر JS: window.AndroidApp.refresh() / reload() / setRefreshSeconds(n).
- وضع كشك (kiosk): ملء الشاشة، إبقاء الشاشة مضاءة، لا خروج عند Back.
- دعم Android TV (LEANBACK launcher).
- إعادة محاولة عند فشل التحميل.

## تحديث جزئي (اختياري)
إذا أردت تحديث جزء من الصفحة (بيانات) بدلاً من إعادة تحميل كاملة، عرّف في صفحتك:
```js
window.onRefresh = function(){ /* جلب بيانات جديدة وتحديث DOM */ };
```
وإلا سيُعاد تحميل الصفحة كاملة.

## الإعداد (config.json)
ضع ملف config.json في أحد المسارات:
1. /sdcard/web-refresh-display/config.json
2. مجلد ملفات التطبيق (external files)
3. داخل assets (افتراضي)

مثال:
```json
{
  "url": "https://example.com/dashboard",
  "refreshSeconds": 60,
  "toolbar": true,
  "keepScreenOn": true,
  "fullscreen": true
}
```

## البناء
استخدم .\publish-apk.ps1 (GitHub Actions). أو بناء محلي مع JDK 17 + Android SDK.