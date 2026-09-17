package com.example;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameProfileManager {
    private static final String PREFS_PROFILES = "GameTurboProfiles";
    private static final Map<String, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();

    public static GameProfile getProfile(Context context, String packageName) {
        if (packageName == null) return new GameProfile("");
        if (PROFILE_CACHE.containsKey(packageName)) {
            return PROFILE_CACHE.get(packageName);
        }
        if (context == null) return new GameProfile(packageName);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE);
        String json = prefs.getString(packageName, null);
        GameProfile profile = new GameProfile(packageName);

        if (json != null) {
            try {
                JSONObject obj = new JSONObject(json);
                profile.setGamingMode(obj.optString("gamingMode", "Performance"));
                profile.setDpiPreference(obj.optString("dpiPreference", "Default"));
                profile.setRefreshRatePreference(obj.optString("refreshRatePreference", "Max Available"));
                profile.setBrightnessPreference(obj.optString("brightnessPreference", "80%"));
                profile.setScreenTimeoutPreference(obj.optString("screenTimeoutPreference", "Keep Screen Awake"));
                profile.setDndEnabled(obj.optBoolean("dndEnabled", false));
                profile.setSensitivityNotes(obj.optString("sensitivityNotes", ""));
                profile.setGraphicsNotes(obj.optString("graphicsNotes", ""));
            } catch (Exception ignored) {}
        }
        PROFILE_CACHE.put(packageName, profile);
        return profile;
    }

    public static void saveProfile(Context context, GameProfile profile) {
        if (context == null || profile == null || profile.getPackageName() == null) return;

        PROFILE_CACHE.put(profile.getPackageName(), profile);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE);
        try {
            JSONObject obj = new JSONObject();
            obj.put("gamingMode", profile.getGamingMode());
            obj.put("dpiPreference", profile.getDpiPreference());
            obj.put("refreshRatePreference", profile.getRefreshRatePreference());
            obj.put("brightnessPreference", profile.getBrightnessPreference());
            obj.put("screenTimeoutPreference", profile.getScreenTimeoutPreference());
            obj.put("dndEnabled", profile.isDndEnabled());
            obj.put("sensitivityNotes", profile.getSensitivityNotes());
            obj.put("graphicsNotes", profile.getGraphicsNotes());

            prefs.edit().putString(profile.getPackageName(), obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<String> applyProfile(Activity activity, GameProfile profile) {
        List<String> report = new ArrayList<>();
        if (activity == null || profile == null) return report;

        Window window = activity.getWindow();

        // 1. Screen Timeout / Awake (Android 7 - 37 compatible)
        if ("Keep Screen Awake".equalsIgnoreCase(profile.getScreenTimeoutPreference())) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            report.add("✓ Screen Awake: Active");
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            report.add("✓ Screen Timeout: Auto");
        }

        // 2. Immersive Fullscreen Mode (Android 7 to 37 compatibility)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
                if (controller != null) {
                    controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    controller.hide(WindowInsetsCompat.Type.systemBars());
                }
            } else {
                // Fallback for Android 7.0 - 10 (API 24-29)
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                );
            }
            report.add("✓ Immersive Mode: Engaged");
        } catch (Exception e) {
            report.add("! Immersive Mode: Default");
        }

        // 3. Brightness Preference (Android 7 - 37 compatible)
        try {
            WindowManager.LayoutParams lp = window.getAttributes();
            if ("100%".equals(profile.getBrightnessPreference())) {
                lp.screenBrightness = 1.0f;
                window.setAttributes(lp);
                report.add("✓ Brightness: 100%");
            } else if ("80%".equals(profile.getBrightnessPreference())) {
                lp.screenBrightness = 0.8f;
                window.setAttributes(lp);
                report.add("✓ Brightness: 80%");
            } else if ("50%".equals(profile.getBrightnessPreference())) {
                lp.screenBrightness = 0.5f;
                window.setAttributes(lp);
                report.add("✓ Brightness: 50%");
            } else {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                window.setAttributes(lp);
                report.add("✓ Brightness: System Auto");
            }
        } catch (Exception e) {
            report.add("! Brightness: OS Managed");
        }

        // 4. Do Not Disturb Preference (Android 7 - 37 compatible)
        if (profile.isDndEnabled()) {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted()) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                report.add("✓ DND: Priority Only Active");
            } else {
                report.add("! DND: Needs Notification Policy Access");
            }
        }

        return report;
    }
}
