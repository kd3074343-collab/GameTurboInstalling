package com.example;

public class GameProfile {
    private String packageName;
    private String gamingMode; // "Performance", "Balanced", "Battery Saver"
    private String dpiPreference; // "Default", "360", "400", "440", "480", "600"
    private String refreshRatePreference; // "Default", "60Hz", "90Hz", "120Hz", "Max Available"
    private String brightnessPreference; // "Default", "50%", "80%", "100%"
    private String screenTimeoutPreference; // "Default", "30s", "1m", "5m", "Keep Screen Awake"
    private boolean dndEnabled;
    private String sensitivityNotes;
    private String graphicsNotes;

    public GameProfile(String packageName) {
        this.packageName = packageName;
        this.gamingMode = "Performance";
        this.dpiPreference = "Default";
        this.refreshRatePreference = "Max Available";
        this.brightnessPreference = "80%";
        this.screenTimeoutPreference = "Keep Screen Awake";
        this.dndEnabled = false;
        this.sensitivityNotes = "";
        this.graphicsNotes = "";
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getGamingMode() {
        return gamingMode;
    }

    public void setGamingMode(String gamingMode) {
        this.gamingMode = gamingMode;
    }

    public String getDpiPreference() {
        return dpiPreference;
    }

    public void setDpiPreference(String dpiPreference) {
        this.dpiPreference = dpiPreference;
    }

    public String getRefreshRatePreference() {
        return refreshRatePreference;
    }

    public void setRefreshRatePreference(String refreshRatePreference) {
        this.refreshRatePreference = refreshRatePreference;
    }

    public String getBrightnessPreference() {
        return brightnessPreference;
    }

    public void setBrightnessPreference(String brightnessPreference) {
        this.brightnessPreference = brightnessPreference;
    }

    public String getScreenTimeoutPreference() {
        return screenTimeoutPreference;
    }

    public void setScreenTimeoutPreference(String screenTimeoutPreference) {
        this.screenTimeoutPreference = screenTimeoutPreference;
    }

    public boolean isDndEnabled() {
        return dndEnabled;
    }

    public void setDndEnabled(boolean dndEnabled) {
        this.dndEnabled = dndEnabled;
    }

    public String getSensitivityNotes() {
        return sensitivityNotes;
    }

    public void setSensitivityNotes(String sensitivityNotes) {
        this.sensitivityNotes = sensitivityNotes;
    }

    public String getGraphicsNotes() {
        return graphicsNotes;
    }

    public void setGraphicsNotes(String graphicsNotes) {
        this.graphicsNotes = graphicsNotes;
    }
}
