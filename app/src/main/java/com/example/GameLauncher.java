package com.example;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameLauncher {
    private static final String PREFS_GAMES = "GameTurboVault";
    private static final String KEY_GAMES_JSON = "saved_games_list";
    private static final String KEY_SELECTED_TARGET = "selected_game_target";

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static volatile List<GameItem> IN_MEMORY_GAMES = null;
    private static final Map<String, Drawable> ICON_CACHE = new ConcurrentHashMap<>();

    public interface LaunchResultListener {
        void onSuccess(String gameName);
        void onGameNotInstalled(String packageName);
        void onError(String message);
    }

    public interface GamesCallback {
        void onGamesLoaded(List<GameItem> games);
    }

    public static class InstalledAppInfo {
        public String packageName;
        public String appName;
        public Drawable icon;
        public boolean isGameCategory;

        public InstalledAppInfo(String packageName, String appName, Drawable icon, boolean isGameCategory) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.isGameCategory = isGameCategory;
        }
    }

    /**
     * Asynchronously loads saved games from cache or disk without freezing the UI thread.
     */
    public static void getSavedGamesAsync(Context context, GamesCallback callback) {
        if (IN_MEMORY_GAMES != null) {
            final List<GameItem> cachedCopy = new ArrayList<>(IN_MEMORY_GAMES);
            if (callback != null) {
                callback.onGamesLoaded(cachedCopy);
            }
            return;
        }

        EXECUTOR.execute(() -> {
            List<GameItem> games = loadGamesFromDisk(context);
            MAIN_HANDLER.post(() -> {
                if (callback != null) {
                    callback.onGamesLoaded(games);
                }
            });
        });
    }

    /**
     * Synchronous retrieval with fast-path memory cache.
     */
    public static List<GameItem> getSavedGames(Context context) {
        if (IN_MEMORY_GAMES != null) {
            return new ArrayList<>(IN_MEMORY_GAMES);
        }
        return loadGamesFromDisk(context);
    }

    private static synchronized List<GameItem> loadGamesFromDisk(Context context) {
        if (IN_MEMORY_GAMES != null) {
            return new ArrayList<>(IN_MEMORY_GAMES);
        }

        List<GameItem> games = new ArrayList<>();
        if (context == null) return games;

        PackageManager pm = context.getPackageManager();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_GAMES, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_GAMES_JSON, null);

        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            // First time initialization: scan for games in background or detect installed games
            List<InstalledAppInfo> installed = getAllInstalledLaunchableApps(context);
            for (InstalledAppInfo app : installed) {
                if (app.isGameCategory) {
                    games.add(new GameItem(app.packageName, app.appName, app.icon, false, 0, 0, true));
                }
            }
            saveGamesList(context, games);
            IN_MEMORY_GAMES = new ArrayList<>(games);
            return games;
        }

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String pkg = obj.optString("packageName");
                String name = obj.optString("appName");
                boolean isFav = obj.optBoolean("isFavorite", false);
                long lastPlayed = obj.optLong("lastPlayedTime", 0);
                int count = obj.optInt("playCount", 0);

                Drawable icon = ICON_CACHE.get(pkg);
                boolean isInstalled = false;
                try {
                    pm.getPackageInfo(pkg, 0);
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    name = pm.getApplicationLabel(ai).toString();
                    if (icon == null) {
                        icon = pm.getApplicationIcon(pkg);
                        if (icon != null) {
                            ICON_CACHE.put(pkg, icon);
                        }
                    }
                    isInstalled = true;
                } catch (PackageManager.NameNotFoundException e) {
                    isInstalled = false;
                    if (icon == null) {
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_nav_games);
                    }
                }

                games.add(new GameItem(pkg, name, icon, isFav, lastPlayed, count, isInstalled));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sort: favorites first, then last played
        Collections.sort(games, (g1, g2) -> {
            if (g1.isFavorite() != g2.isFavorite()) {
                return g1.isFavorite() ? -1 : 1;
            }
            return Long.compare(g2.getLastPlayedTime(), g1.getLastPlayedTime());
        });

        IN_MEMORY_GAMES = new ArrayList<>(games);
        return games;
    }

    /**
     * Retrieves all launchable installed apps on the device with full Android 7 to 37 compatibility.
     */
    public static List<InstalledAppInfo> getAllInstalledLaunchableApps(Context context) {
        List<InstalledAppInfo> appList = new ArrayList<>();
        if (context == null) return appList;

        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        Set<String> addedPackages = new HashSet<>();

        for (ResolveInfo ri : activities) {
            if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                String pkg = ri.activityInfo.packageName;
                if (!addedPackages.contains(pkg) && !pkg.equals(context.getPackageName())) {
                    addedPackages.add(pkg);
                    String name = ri.loadLabel(pm).toString();
                    
                    Drawable icon = ICON_CACHE.get(pkg);
                    if (icon == null) {
                        icon = ri.loadIcon(pm);
                        if (icon != null) {
                            ICON_CACHE.put(pkg, icon);
                        }
                    }

                    boolean isGame = false;
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                        // Check Android 8.0+ (API 26) category
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (ai.category == ApplicationInfo.CATEGORY_GAME) {
                                isGame = true;
                            }
                        }
                        // Check Android 7.0 - 7.1 (API 24/25) flag
                        if (!isGame) {
                            if ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                                isGame = true;
                            }
                        }
                        // Heuristic keyword match
                        if (!isGame) {
                            String lower = pkg.toLowerCase();
                            if (lower.contains("game") || lower.contains(".gaming") || lower.contains("unity")
                                    || lower.contains("craft") || lower.contains("pubg") || lower.contains("freefire")
                                    || lower.contains("genshin") || lower.contains("asphalt") || lower.contains("roblox")
                                    || lower.contains("racer") || lower.contains("fifa") || lower.contains("brawl")) {
                                isGame = true;
                            }
                        }
                    } catch (Exception ignored) {}

                    appList.add(new InstalledAppInfo(pkg, name, icon, isGame));
                }
            }
        }

        Collections.sort(appList, (a, b) -> a.appName.compareToIgnoreCase(b.appName));
        return appList;
    }

    public static synchronized void saveGamesList(Context context, List<GameItem> games) {
        if (context == null || games == null) return;
        IN_MEMORY_GAMES = new ArrayList<>(games);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_GAMES, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (GameItem g : games) {
                JSONObject obj = new JSONObject();
                obj.put("packageName", g.getPackageName());
                obj.put("appName", g.getAppName());
                obj.put("isFavorite", g.isFavorite());
                obj.put("lastPlayedTime", g.getLastPlayedTime());
                obj.put("playCount", g.getPlayCount());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_GAMES_JSON, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static boolean addGame(Context context, String packageName, String appName) {
        List<GameItem> current = getSavedGames(context);
        for (GameItem item : current) {
            if (item.getPackageName().equals(packageName)) {
                return false; // Already in vault
            }
        }
        Drawable icon = ICON_CACHE.get(packageName);
        boolean installed = true;
        try {
            if (icon == null) {
                icon = context.getPackageManager().getApplicationIcon(packageName);
                if (icon != null) {
                    ICON_CACHE.put(packageName, icon);
                }
            }
        } catch (Exception e) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_nav_games);
            installed = false;
        }

        current.add(new GameItem(packageName, appName, icon, false, 0, 0, installed));
        saveGamesList(context, current);
        return true;
    }

    public static void removeGame(Context context, String packageName) {
        List<GameItem> current = getSavedGames(context);
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getPackageName().equals(packageName)) {
                current.remove(i);
                break;
            }
        }
        saveGamesList(context, current);

        if (packageName.equals(getSelectedGamePackage(context))) {
            setSelectedGamePackage(context, null);
        }
    }

    public static void toggleFavorite(Context context, String packageName) {
        List<GameItem> current = getSavedGames(context);
        for (GameItem g : current) {
            if (g.getPackageName().equals(packageName)) {
                g.setFavorite(!g.isFavorite());
                break;
            }
        }
        saveGamesList(context, current);
    }

    public static String getSelectedGamePackage(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_GAMES, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_TARGET, null);
    }

    public static void setSelectedGamePackage(Context context, String packageName) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_GAMES, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_TARGET, packageName).apply();
    }

    public static void launchGame(Context context, String packageName, LaunchResultListener listener) {
        if (context == null || packageName == null) {
            if (listener != null) listener.onError("Invalid game reference");
            return;
        }

        PackageManager pm = context.getPackageManager();

        // 1. Verify app installation
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            if (listener != null) {
                listener.onGameNotInstalled(packageName);
            }
            return;
        }

        // 2. Query official launch intent
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            if (listener != null) {
                listener.onError("Cannot find launcher activity for " + packageName);
            }
            return;
        }

        // 3. Update last played timestamp & play count
        List<GameItem> current = getSavedGames(context);
        String gameName = packageName;
        for (GameItem g : current) {
            if (g.getPackageName().equals(packageName)) {
                g.setLastPlayedTime(System.currentTimeMillis());
                g.incrementPlayCount();
                gameName = g.getAppName();
                break;
            }
        }
        saveGamesList(context, current);
        setSelectedGamePackage(context, packageName);

        // 4. Launch safely
        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            if (listener != null) {
                listener.onSuccess(gameName);
            }
        } catch (Exception ex) {
            if (listener != null) {
                listener.onError("Launch failed: " + ex.getMessage());
            }
        }
    }
}
