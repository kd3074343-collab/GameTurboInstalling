package com.example;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

public class SettingsHelper {

    public static void openDisplaySettings(Context context) {
        safeLaunchIntent(context, new Intent(Settings.ACTION_DISPLAY_SETTINGS), "Display Settings");
    }

    public static void openRefreshRateSettings(Context context) {
        // Most Android devices expose peak refresh rate inside Developer Options or Display
        Intent devIntent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        if (!safeLaunchIntent(context, devIntent, null)) {
            openDisplaySettings(context);
        }
    }

    public static void openBatterySettings(Context context) {
        Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
        if (!safeLaunchIntent(context, intent, null)) {
            safeLaunchIntent(context, new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), "Battery Settings");
        }
    }

    public static void openBatteryOptimizationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        safeLaunchIntent(context, intent, "Battery Optimization");
    }

    public static void openDndSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        safeLaunchIntent(context, intent, "Do Not Disturb Access");
    }

    public static void openDeveloperOptions(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        if (!safeLaunchIntent(context, intent, null)) {
            Toast.makeText(context, "Please enable Developer Options in Android Settings > About Phone (tap Build Number 7 times)", Toast.LENGTH_LONG).show();
            safeLaunchIntent(context, new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS), "About Phone");
        }
    }

    public static void openGameDashboardSettings(Context context) {
        // Try Android 12+ Game Mode / Game Dashboard shortcuts or settings
        Intent[] gameIntents = new Intent[] {
                new Intent("android.settings.GAME_SETTINGS"),
                new Intent("com.google.android.gms.games.GAME_DASHBOARD"),
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        };

        for (Intent intent : gameIntents) {
            if (safeLaunchIntent(context, intent, null)) {
                return;
            }
        }
        Toast.makeText(context, "Game Dashboard settings available in system Quick Settings or Game Space", Toast.LENGTH_SHORT).show();
    }

    public static void openAppInfo(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open App Info", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openWriteSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Write Settings shortcut unavailable on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean safeLaunchIntent(Context context, Intent intent, String fallbackTitle) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {}

        if (fallbackTitle != null) {
            Toast.makeText(context, fallbackTitle + " is not directly accessible on this device ROM", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
