package com.example;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DpiManager {
    private static final String PREFS_NAME = "GameTurboDpiPrefs";
    private static final String KEY_ORIGINAL_DPI = "original_dpi";
    private static final String KEY_SAVED_DPI = "saved_target_dpi";

    public static class DpiInfo {
        public int currentDpi;
        public float density;
        public int smallestWidthDp;
        public int widthPixels;
        public int heightPixels;
        public int originalDpi;
    }

    public static DpiInfo getDpiInfo(Context context) {
        DpiInfo info = new DpiInfo();
        if (context == null) return info;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        Configuration config = context.getResources().getConfiguration();

        info.currentDpi = dm.densityDpi;
        info.density = dm.density;
        info.smallestWidthDp = config.smallestScreenWidthDp;
        info.widthPixels = dm.widthPixels;
        info.heightPixels = dm.heightPixels;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        info.originalDpi = prefs.getInt(KEY_ORIGINAL_DPI, info.currentDpi);

        if (!prefs.contains(KEY_ORIGINAL_DPI)) {
            prefs.edit().putInt(KEY_ORIGINAL_DPI, info.currentDpi).apply();
        }

        return info;
    }

    public static void saveTargetDpi(Context context, int targetDpi) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SAVED_DPI, targetDpi).apply();
    }

    public static int getSavedTargetDpi(Context context, int defaultVal) {
        if (context == null) return defaultVal;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SAVED_DPI, defaultVal);
    }

    public static int getOriginalDpi(Context context) {
        if (context == null) return 400;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_ORIGINAL_DPI, context.getResources().getDisplayMetrics().densityDpi);
    }

    public static String getAdbCommand(int targetDpi) {
        return "adb shell wm density " + targetDpi;
    }

    public static String getAdbResetCommand() {
        return "adb shell wm density reset";
    }

    public static boolean copyToClipboard(Context context, String text, String label) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(label, text);
                clipboard.setPrimaryClip(clip);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
