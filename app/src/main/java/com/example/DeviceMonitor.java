package com.example;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class DeviceMonitor {

    private static final ExecutorService TELEMETRY_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface SnapshotCallback {
        void onSnapshot(DeviceSnapshot snapshot);
    }

    public static void getSnapshotAsync(Context context, SnapshotCallback callback) {
        TELEMETRY_EXECUTOR.execute(() -> {
            DeviceSnapshot snapshot = getSnapshot(context);
            MAIN_HANDLER.post(() -> {
                if (callback != null) {
                    callback.onSnapshot(snapshot);
                }
            });
        });
    }

    public static class DeviceSnapshot {
        public String deviceModel = "Not Available";
        public String androidVersion = "Not Available";
        public String screenResolution = "Not Available";
        public String currentDpi = "Not Available";
        public String refreshRate = "Not Available";
        public String batteryPercentage = "Not Available";
        public String batteryTemperature = "Not Available";
        public String batteryStatus = "Not Available";
        public String ramTotal = "Not Available";
        public String ramAvailable = "Not Available";
        public String ramUsed = "Not Available";
        public int ramUsedPercent = 0;
        public int batteryPercentInt = 0;
        public String cpuArchitecture = "Not Available";
        public String cpuCores = "Not Available";
    }

    public static DeviceSnapshot getSnapshot(Context context) {
        DeviceSnapshot snapshot = new DeviceSnapshot();
        if (context == null) {
            return snapshot;
        }

        // 1. Device Model & Manufacturer
        try {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                snapshot.deviceModel = capitalize(model);
            } else {
                snapshot.deviceModel = capitalize(manufacturer) + " " + model;
            }
        } catch (Exception e) {
            snapshot.deviceModel = "Not Available";
        }

        // 2. Android Version
        try {
            snapshot.androidVersion = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        } catch (Exception e) {
            snapshot.androidVersion = "Not Available";
        }

        // 3. Display Metrics (Resolution, DPI, Refresh Rate)
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Display display = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context instanceof Activity) {
                    display = ((Activity) context).getDisplay();
                }
                if (display == null) {
                    display = wm.getDefaultDisplay();
                }

                if (display != null) {
                    DisplayMetrics dm = new DisplayMetrics();
                    display.getRealMetrics(dm);
                    snapshot.screenResolution = dm.widthPixels + " x " + dm.heightPixels + " px";
                    snapshot.currentDpi = dm.densityDpi + " DPI (" + String.format(Locale.US, "%.2fx", dm.density) + ")";
                    
                    float fps = display.getRefreshRate();
                    if (fps > 0) {
                        snapshot.refreshRate = String.format(Locale.US, "%.1f Hz", fps);
                    } else {
                        snapshot.refreshRate = "Not Available";
                    }
                }
            }
        } catch (Exception e) {
            snapshot.screenResolution = "Not Available";
            snapshot.currentDpi = "Not Available";
            snapshot.refreshRate = "Not Available";
        }

        // 4. Battery Information via Sticky Intent
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, filter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int percent = (int) ((level / (float) scale) * 100);
                    snapshot.batteryPercentage = percent + "%";
                    snapshot.batteryPercentInt = percent;
                }

                int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                if (temp > 0) {
                    float celsius = temp / 10.0f;
                    snapshot.batteryTemperature = String.format(Locale.US, "%.1f°C", celsius);
                } else {
                    snapshot.batteryTemperature = "Not Available";
                }

                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    snapshot.batteryStatus = "Charging (AC/USB)";
                } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    snapshot.batteryStatus = "Full (100%)";
                } else {
                    snapshot.batteryStatus = "Discharging";
                }
            }
        } catch (Exception e) {
            snapshot.batteryPercentage = "Not Available";
            snapshot.batteryTemperature = "Not Available";
        }

        // 5. RAM Information
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(memInfo);

                double totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0);
                double availGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0);
                double usedGb = totalGb - availGb;
                int percent = (int) ((usedGb / totalGb) * 100);

                snapshot.ramTotal = String.format(Locale.US, "%.1f GB", totalGb);
                snapshot.ramAvailable = String.format(Locale.US, "%.1f GB", availGb);
                snapshot.ramUsed = String.format(Locale.US, "%.1f GB", usedGb);
                snapshot.ramUsedPercent = Math.max(0, Math.min(100, percent));
            }
        } catch (Exception e) {
            snapshot.ramTotal = "Not Available";
            snapshot.ramAvailable = "Not Available";
            snapshot.ramUsed = "Not Available";
        }

        // 6. CPU Information
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            snapshot.cpuCores = cores + " Cores";
            
            String arch = Build.HARDWARE;
            if (arch == null || arch.isEmpty() || arch.equalsIgnoreCase("unknown")) {
                if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
                    arch = Build.SUPPORTED_ABIS[0];
                } else {
                    arch = Build.BOARD;
                }
            }
            snapshot.cpuArchitecture = arch != null ? arch.toUpperCase(Locale.US) : "ARM64";
        } catch (Exception e) {
            snapshot.cpuCores = "Not Available";
            snapshot.cpuArchitecture = "Not Available";
        }

        return snapshot;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
