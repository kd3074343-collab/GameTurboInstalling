package com.example;

import android.graphics.drawable.Drawable;

public class GameItem {
    private String packageName;
    private String appName;
    private Drawable icon;
    private boolean isFavorite;
    private long lastPlayedTime;
    private int playCount;
    private boolean isInstalled;

    public GameItem(String packageName, String appName, Drawable icon, boolean isFavorite, long lastPlayedTime, int playCount, boolean isInstalled) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isFavorite = isFavorite;
        this.lastPlayedTime = lastPlayedTime;
        this.playCount = playCount;
        this.isInstalled = isInstalled;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public long getLastPlayedTime() {
        return lastPlayedTime;
    }

    public void setLastPlayedTime(long lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public void setInstalled(boolean installed) {
        isInstalled = installed;
    }
}
