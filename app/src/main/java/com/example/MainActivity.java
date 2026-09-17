package com.example;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ActivityMainBinding;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements GamesAdapter.OnGameActionListener {

    private ActivityMainBinding binding;
    private GamesAdapter gamesAdapter;
    private int currentAccentColor;
    private String selectedGamePackage = null;
    private boolean isShowingFavoritesOnly = false;
    private ObjectAnimator boostPulseAnim;

    private static final String PREFS_THEME = "GameTurboTheme";
    private static final String KEY_ACCENT = "accent_color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadAccentColor();
        initNavigation();
        initHeader();
        initHomeScreen();
        initBoostScreen();
        initDpiScreen();
        initGamesScreen();
        initSettingsScreen();

        refreshAllMetrics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllMetrics();
        loadGamesData();
    }

    // ==========================================
    // ACCENT COLOR & THEME MANAGEMENT
    // ==========================================
    private void loadAccentColor() {
        SharedPreferences prefs = getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
        currentAccentColor = prefs.getInt(KEY_ACCENT, getColor(R.color.neon_cyan));
        applyAccentColor(currentAccentColor);
    }

    private void saveAccentColor(int color) {
        currentAccentColor = color;
        SharedPreferences prefs = getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_ACCENT, color).apply();
        applyAccentColor(color);
        Toast.makeText(this, "Accent theme updated", Toast.LENGTH_SHORT).show();
    }

    private void applyAccentColor(int color) {
        binding.headerIcon.setImageTintList(ColorStateList.valueOf(color));
        binding.btnAccentPicker.setImageTintList(ColorStateList.valueOf(color));
        
        // Update Bottom Nav active indicator tint
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                color,
                getColor(R.color.text_muted)
        };
        ColorStateList navTint = new ColorStateList(states, colors);
        binding.bottomNavigation.setItemIconTintList(navTint);
        binding.bottomNavigation.setItemTextColor(navTint);
    }

    // ==========================================
    // NAVIGATION & HEADER
    // ==========================================
    private void initNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                binding.viewFlipper.setDisplayedChild(0);
                refreshAllMetrics();
                return true;
            } else if (id == R.id.nav_boost) {
                binding.viewFlipper.setDisplayedChild(1);
                return true;
            } else if (id == R.id.nav_dpi) {
                binding.viewFlipper.setDisplayedChild(2);
                updateDpiScreenMetrics();
                return true;
            } else if (id == R.id.nav_games) {
                binding.viewFlipper.setDisplayedChild(3);
                loadGamesData();
                return true;
            } else if (id == R.id.nav_settings) {
                binding.viewFlipper.setDisplayedChild(4);
                return true;
            }
            return false;
        });
    }

    private void initHeader() {
        binding.btnQuickRefresh.setOnClickListener(v -> {
            refreshAllMetrics();
            Toast.makeText(this, "Telemetry refreshed", Toast.LENGTH_SHORT).show();
        });

        binding.btnAccentPicker.setOnClickListener(v -> {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_settings);
        });
    }

    // ==========================================
    // 0: HOME DASHBOARD
    // ==========================================
    private void initHomeScreen() {
        View homeView = binding.viewFlipper.getChildAt(0);

        // Pulse Animation for Boost Ring
        View pulseRing = homeView.findViewById(R.id.boost_pulse_ring);
        if (pulseRing != null) {
            boostPulseAnim = ObjectAnimator.ofPropertyValuesHolder(
                    pulseRing,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.06f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.25f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.45f, 0.1f)
            );
            boostPulseAnim.setDuration(1200);
            boostPulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            boostPulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            boostPulseAnim.start();
        }

        // BOOST NOW Button
        Button btnBoostNow = homeView.findViewById(R.id.btn_home_boost_now);
        btnBoostNow.setOnClickListener(v -> performBoostAction(true));

        // Selected Game Card Quick Play
        Button btnPlayTarget = homeView.findViewById(R.id.btn_home_play_target);
        btnPlayTarget.setOnClickListener(v -> {
            if (selectedGamePackage != null) {
                launchTargetGame(selectedGamePackage);
            } else {
                binding.bottomNavigation.setSelectedItemId(R.id.nav_games);
                Toast.makeText(this, "Please select a game from your library", Toast.LENGTH_SHORT).show();
            }
        });

        View cardSelectedGame = homeView.findViewById(R.id.card_selected_game);
        cardSelectedGame.setOnClickListener(v -> {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_games);
        });

        // Ping Test Button
        Button btnPing = homeView.findViewById(R.id.btn_home_ping_test);
        btnPing.setOnClickListener(v -> runHomePingTest());
    }

    private void runHomePingTest() {
        View homeView = binding.viewFlipper.getChildAt(0);
        TextView tvPing = homeView.findViewById(R.id.tv_home_ping_latency);
        TextView tvQuality = homeView.findViewById(R.id.tv_home_ping_quality);
        Button btnPing = homeView.findViewById(R.id.btn_home_ping_test);

        tvPing.setText("Testing...");
        btnPing.setEnabled(false);

        NetworkMonitor.runPingTest((latencyMs, quality, advice) -> {
            btnPing.setEnabled(true);
            if (latencyMs >= 0) {
                tvPing.setText(latencyMs + " ms");
                tvQuality.setText("Rating: " + quality);
                if (latencyMs < 50) {
                    tvPing.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_emerald));
                } else if (latencyMs < 90) {
                    tvPing.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_gold));
                } else {
                    tvPing.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_crimson));
                }
            } else {
                tvPing.setText("Unreachable");
                tvQuality.setText(quality + " • " + advice);
                tvPing.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_crimson));
            }
        });
    }

    private void performBoostAction(boolean showResultBanner) {
        // Safe OS memory cache trim
        System.gc();
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(getPackageName());
            }
        } catch (Exception ignored) {}

        // Keep screen awake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Feedback
        binding.tvTurboStatus.setText("● TURBO BOOST ACTIVE");
        binding.tvTurboStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_emerald));

        if (showResultBanner) {
            View homeView = binding.viewFlipper.getChildAt(0);
            View banner = homeView.findViewById(R.id.card_boost_result);
            if (banner != null) {
                banner.setVisibility(View.VISIBLE);
                banner.setAlpha(0f);
                banner.animate().alpha(1f).setDuration(300).start();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (banner != null) {
                        banner.animate().alpha(0f).setDuration(400).withEndAction(() -> banner.setVisibility(View.GONE)).start();
                    }
                }, 5000);
            }
            Toast.makeText(this, "⚡ Turbo Boost Engaged: Memory trimmed, Screen awake locked", Toast.LENGTH_SHORT).show();
        }

        refreshAllMetrics();
    }

    // ==========================================
    // 1: BOOST SCREEN
    // ==========================================
    private void initBoostScreen() {
        View boostView = binding.viewFlipper.getChildAt(1);

        Button btnTrigger = boostView.findViewById(R.id.btn_trigger_boost);
        btnTrigger.setOnClickListener(v -> performBoostAction(true));

        Button btnBoostPlay = boostView.findViewById(R.id.btn_boost_and_play);
        btnBoostPlay.setOnClickListener(v -> {
            if (selectedGamePackage != null) {
                performBoostAction(false);
                launchTargetGame(selectedGamePackage);
            } else {
                binding.bottomNavigation.setSelectedItemId(R.id.nav_games);
                Toast.makeText(this, "Select a game first from the library", Toast.LENGTH_SHORT).show();
            }
        });

        // Switch listeners
        SwitchMaterial switchAwake = boostView.findViewById(R.id.switch_keep_awake);
        switchAwake.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Toast.makeText(this, "Screen awake locked", Toast.LENGTH_SHORT).show();
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        SwitchMaterial switchImmersive = boostView.findViewById(R.id.switch_immersive_mode);
        switchImmersive.setOnCheckedChangeListener((btn, isChecked) -> {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            if (controller != null) {
                if (isChecked) {
                    controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    controller.hide(WindowInsetsCompat.Type.systemBars());
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars());
                }
            }
        });

        // Shortcut buttons
        boostView.findViewById(R.id.btn_shortcut_battery_opt).setOnClickListener(v -> SettingsHelper.openBatteryOptimizationSettings(this));
        boostView.findViewById(R.id.btn_shortcut_dnd).setOnClickListener(v -> SettingsHelper.openDndSettings(this));
        boostView.findViewById(R.id.btn_shortcut_refresh_rate).setOnClickListener(v -> SettingsHelper.openRefreshRateSettings(this));
        boostView.findViewById(R.id.btn_shortcut_game_mode).setOnClickListener(v -> SettingsHelper.openGameDashboardSettings(this));
    }

    // ==========================================
    // 2: DPI SCREEN
    // ==========================================
    private void initDpiScreen() {
        View dpiView = binding.viewFlipper.getChildAt(2);

        EditText etTargetDpi = dpiView.findViewById(R.id.et_target_dpi);
        Button btnMinus = dpiView.findViewById(R.id.btn_dpi_minus);
        Button btnPlus = dpiView.findViewById(R.id.btn_dpi_plus);
        Button btnApply = dpiView.findViewById(R.id.btn_apply_dpi);
        Button btnRestore = dpiView.findViewById(R.id.btn_restore_dpi);
        TextView tvAdbCmd = dpiView.findViewById(R.id.tv_adb_command);
        Button btnCopyAdb = dpiView.findViewById(R.id.btn_copy_adb_cmd);
        Button btnDevOptions = dpiView.findViewById(R.id.btn_open_dev_options_dpi);

        // Stepper -10
        btnMinus.setOnClickListener(v -> {
            int current = parseDpi(etTargetDpi.getText().toString(), 400);
            int updated = Math.max(160, current - 10);
            etTargetDpi.setText(String.valueOf(updated));
        });

        // Stepper +10
        btnPlus.setOnClickListener(v -> {
            int current = parseDpi(etTargetDpi.getText().toString(), 400);
            int updated = Math.min(800, current + 10);
            etTargetDpi.setText(String.valueOf(updated));
        });

        // TextWatcher to update ADB preview
        etTargetDpi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int dpi = parseDpi(s.toString(), 400);
                tvAdbCmd.setText(DpiManager.getAdbCommand(dpi));
            }
        });

        // Presets
        dpiView.findViewById(R.id.btn_preset_360).setOnClickListener(v -> etTargetDpi.setText("360"));
        dpiView.findViewById(R.id.btn_preset_400).setOnClickListener(v -> etTargetDpi.setText("400"));
        dpiView.findViewById(R.id.btn_preset_440).setOnClickListener(v -> etTargetDpi.setText("440"));
        dpiView.findViewById(R.id.btn_preset_480).setOnClickListener(v -> etTargetDpi.setText("480"));

        // Apply DPI: Saves profile and explains authorized mechanism per security mandate
        btnApply.setOnClickListener(v -> {
            int dpi = parseDpi(etTargetDpi.getText().toString(), 400);
            DpiManager.saveTargetDpi(this, dpi);
            DpiManager.copyToClipboard(this, DpiManager.getAdbCommand(dpi), "ADB Command");
            showDpiInstructionDialog(dpi);
        });

        // Restore Original
        btnRestore.setOnClickListener(v -> {
            int original = DpiManager.getOriginalDpi(this);
            etTargetDpi.setText(String.valueOf(original));
            DpiManager.copyToClipboard(this, DpiManager.getAdbResetCommand(), "ADB Reset Command");
            Toast.makeText(this, "Original DPI restored: " + original + " DPI (Reset command copied)", Toast.LENGTH_LONG).show();
        });

        btnCopyAdb.setOnClickListener(v -> {
            int dpi = parseDpi(etTargetDpi.getText().toString(), 400);
            DpiManager.copyToClipboard(this, DpiManager.getAdbCommand(dpi), "ADB Command");
            Toast.makeText(this, "Copied: " + DpiManager.getAdbCommand(dpi), Toast.LENGTH_SHORT).show();
        });

        btnDevOptions.setOnClickListener(v -> SettingsHelper.openDeveloperOptions(this));
    }

    private void updateDpiScreenMetrics() {
        View dpiView = binding.viewFlipper.getChildAt(2);
        DpiManager.DpiInfo info = DpiManager.getDpiInfo(this);

        TextView tvCurrentDpi = dpiView.findViewById(R.id.tv_dpi_current_val);
        TextView tvDensityScale = dpiView.findViewById(R.id.tv_dpi_density_scale);
        TextView tvSmallestWidth = dpiView.findViewById(R.id.tv_dpi_smallest_width);
        TextView tvResolution = dpiView.findViewById(R.id.tv_dpi_resolution_label);

        tvCurrentDpi.setText(info.currentDpi + " DPI");
        tvDensityScale.setText("Density Scale: " + String.format("%.2fx", info.density));
        tvSmallestWidth.setText("sw" + info.smallestWidthDp + "dp");
        tvResolution.setText(info.widthPixels + " x " + info.heightPixels + " px");
    }

    private void showDpiInstructionDialog(int targetDpi) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_game); // reuse dialog container or programmatically construct
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Custom alert dialog layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.bg_dialog);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("🛡 DPI APPLICATION GUIDE");
        title.setTextColor(getColor(R.color.neon_cyan));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Target DPI: " + targetDpi + " DPI saved to Game Turbo profile.\n\nPer official Android security policies, system-wide display density requires user authorization via Developer Options ('Smallest width') or an authorized ADB shell command.\n\nCommand copied to clipboard:\nadb shell wm density " + targetDpi);
        desc.setTextColor(getColor(R.color.text_medium));
        desc.setTextSize(13);
        desc.setPadding(0, 24, 0, 24);
        layout.addView(desc);

        Button btnDev = new Button(this);
        btnDev.setText("OPEN DEVELOPER OPTIONS");
        btnDev.setBackgroundResource(R.drawable.bg_button_cyber);
        btnDev.setTextColor(getColor(R.color.neon_cyan));
        btnDev.setOnClickListener(v -> {
            dialog.dismiss();
            SettingsHelper.openDeveloperOptions(this);
        });
        layout.addView(btnDev);

        Button btnClose = new Button(this);
        btnClose.setText("GOT IT");
        btnClose.setBackgroundResource(R.drawable.bg_pill_dark);
        btnClose.setTextColor(getColor(R.color.text_high));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 16;
        btnClose.setLayoutParams(lp);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnClose);

        dialog.setContentView(layout);
        dialog.show();
    }

    private int parseDpi(String input, int defaultVal) {
        try {
            return Integer.parseInt(input.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    // ==========================================
    // 3: GAMES VAULT
    // ==========================================
    private void initGamesScreen() {
        View gamesView = binding.viewFlipper.getChildAt(3);

        RecyclerView rvGames = gamesView.findViewById(R.id.rv_games);
        rvGames.setLayoutManager(new LinearLayoutManager(this));
        gamesAdapter = new GamesAdapter(this, this);
        rvGames.setAdapter(gamesAdapter);

        // Add Game Button
        gamesView.findViewById(R.id.btn_add_game).setOnClickListener(v -> showAddGameDialog());
        gamesView.findViewById(R.id.btn_scan_installed_games).setOnClickListener(v -> showAddGameDialog());

        // Filter chips
        Button btnAll = gamesView.findViewById(R.id.btn_filter_all);
        Button btnFav = gamesView.findViewById(R.id.btn_filter_favorites);

        btnAll.setOnClickListener(v -> {
            isShowingFavoritesOnly = false;
            btnAll.setBackgroundResource(R.drawable.bg_badge_neon);
            btnAll.setTextColor(currentAccentColor);
            btnFav.setBackgroundResource(R.drawable.bg_button_cyber);
            btnFav.setTextColor(getColor(R.color.text_medium));
            loadGamesData();
        });

        btnFav.setOnClickListener(v -> {
            isShowingFavoritesOnly = true;
            btnFav.setBackgroundResource(R.drawable.bg_badge_neon);
            btnFav.setTextColor(getColor(R.color.neon_gold));
            btnAll.setBackgroundResource(R.drawable.bg_button_cyber);
            btnAll.setTextColor(getColor(R.color.text_medium));
            loadGamesData();
        });

        loadGamesData();
    }

    private void loadGamesData() {
        List<GameItem> list = GameLauncher.getSavedGames(this);
        View gamesView = binding.viewFlipper.getChildAt(3);
        TextView tvCount = gamesView.findViewById(R.id.tv_games_count_label);
        View emptyLayout = gamesView.findViewById(R.id.layout_empty_games);
        RecyclerView rv = gamesView.findViewById(R.id.rv_games);

        if (isShowingFavoritesOnly) {
            List<GameItem> favorites = new ArrayList<>();
            for (GameItem item : list) {
                if (item.isFavorite()) favorites.add(item);
            }
            gamesAdapter.updateList(favorites);
            tvCount.setText(favorites.size() + " favorite games");
        } else {
            gamesAdapter.updateList(list);
            tvCount.setText(list.size() + " games in vault");
        }

        if (list.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }

        // Update active target game on Home Screen
        updateHomeSelectedGame(list);
    }

    private void updateHomeSelectedGame(List<GameItem> list) {
        View homeView = binding.viewFlipper.getChildAt(0);
        TextView tvName = homeView.findViewById(R.id.tv_selected_game_name);
        TextView tvSub = homeView.findViewById(R.id.tv_selected_game_sub);
        ImageView imgIcon = homeView.findViewById(R.id.img_selected_game_icon);

        selectedGamePackage = GameLauncher.getSelectedGamePackage(this);

        GameItem target = null;
        if (selectedGamePackage != null) {
            for (GameItem g : list) {
                if (g.getPackageName().equals(selectedGamePackage)) {
                    target = g;
                    break;
                }
            }
        }

        if (target == null && !list.isEmpty()) {
            target = list.get(0);
            selectedGamePackage = target.getPackageName();
            GameLauncher.setSelectedGamePackage(this, selectedGamePackage);
        }

        if (target != null) {
            tvName.setText(target.getAppName());
            GameProfile profile = GameProfileManager.getProfile(this, target.getPackageName());
            tvSub.setText(profile.getGamingMode() + " Mode • " + profile.getScreenTimeoutPreference());
            if (target.getIcon() != null) {
                imgIcon.setImageDrawable(target.getIcon());
                imgIcon.setImageTintList(null);
            }
        } else {
            tvName.setText("No Game Selected");
            tvSub.setText("Tap to choose or add games from library");
            imgIcon.setImageResource(R.drawable.ic_nav_games);
            imgIcon.setImageTintList(ColorStateList.valueOf(currentAccentColor));
        }
    }

    // GamesAdapter.OnGameActionListener implementations
    @Override
    public void onPlayClicked(GameItem game) {
        launchTargetGame(game.getPackageName());
    }

    @Override
    public void onProfileClicked(GameItem game) {
        showProfileDialog(game);
    }

    @Override
    public void onFavoriteClicked(GameItem game) {
        GameLauncher.toggleFavorite(this, game.getPackageName());
        loadGamesData();
    }

    @Override
    public void onRemoveClicked(GameItem game) {
        GameLauncher.removeGame(this, game.getPackageName());
        Toast.makeText(this, game.getAppName() + " removed from vault", Toast.LENGTH_SHORT).show();
        loadGamesData();
    }

    @Override
    public void onCardClicked(GameItem game) {
        selectedGamePackage = game.getPackageName();
        GameLauncher.setSelectedGamePackage(this, selectedGamePackage);
        loadGamesData();
        Toast.makeText(this, "Set as active target: " + game.getAppName(), Toast.LENGTH_SHORT).show();
    }

    private void launchTargetGame(String packageName) {
        GameProfile profile = GameProfileManager.getProfile(this, packageName);
        List<String> report = GameProfileManager.applyProfile(this, profile);

        GameLauncher.launchGame(this, packageName, new GameLauncher.LaunchResultListener() {
            @Override
            public void onSuccess(String gameName) {
                StringBuilder msg = new StringBuilder("Launching " + gameName + " in " + profile.getGamingMode() + " Mode\n");
                for (String r : report) {
                    msg.append(r).append("\n");
                }
                Toast.makeText(MainActivity.this, msg.toString().trim(), Toast.LENGTH_LONG).show();
                loadGamesData();
            }

            @Override
            public void onGameNotInstalled(String pkg) {
                Toast.makeText(MainActivity.this, "Error: Game is Not Installed on this device (" + pkg + ")", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddGameDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_game);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etSearch = dialog.findViewById(R.id.et_search_app);
        ProgressBar pbLoading = dialog.findViewById(R.id.pb_loading_apps);
        RecyclerView rvApps = dialog.findViewById(R.id.rv_select_apps);
        Button btnDone = dialog.findViewById(R.id.btn_close_add_dialog);

        rvApps.setLayoutManager(new LinearLayoutManager(this));
        AppSelectAdapter adapter = new AppSelectAdapter(appInfo -> {
            boolean added = GameLauncher.addGame(this, appInfo.packageName, appInfo.appName);
            if (added) {
                Toast.makeText(this, "Added " + appInfo.appName + " to vault", Toast.LENGTH_SHORT).show();
                loadGamesData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, appInfo.appName + " is already in your vault", Toast.LENGTH_SHORT).show();
            }
        });
        rvApps.setAdapter(adapter);

        pbLoading.setVisibility(View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<GameLauncher.InstalledAppInfo> apps = GameLauncher.getAllInstalledLaunchableApps(this);
            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                adapter.setApps(apps);
            });
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showProfileDialog(GameItem game) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_profile);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_profile_game_name);
        ImageView imgIcon = dialog.findViewById(R.id.img_profile_game_icon);
        Spinner spMode = dialog.findViewById(R.id.spinner_profile_mode);
        Spinner spDpi = dialog.findViewById(R.id.spinner_profile_dpi);
        Spinner spRefresh = dialog.findViewById(R.id.spinner_profile_refresh_rate);
        Spinner spBrightness = dialog.findViewById(R.id.spinner_profile_brightness);
        Spinner spTimeout = dialog.findViewById(R.id.spinner_profile_timeout);
        SwitchMaterial swDnd = dialog.findViewById(R.id.switch_profile_dnd);
        EditText etSensitivity = dialog.findViewById(R.id.et_profile_sensitivity);
        EditText etGraphics = dialog.findViewById(R.id.et_profile_graphics);
        Button btnSave = dialog.findViewById(R.id.btn_save_profile);
        Button btnPlay = dialog.findViewById(R.id.btn_launch_from_profile);

        tvTitle.setText(game.getAppName());
        if (game.getIcon() != null) imgIcon.setImageDrawable(game.getIcon());

        // Setup Spinners with custom arrays
        setupSpinner(spMode, new String[]{"Performance (Pro Mode)", "Balanced Gaming", "Battery Saver"});
        setupSpinner(spDpi, new String[]{"Default", "360 DPI", "400 DPI", "440 DPI", "480 DPI", "600 DPI"});
        setupSpinner(spRefresh, new String[]{"Max Available", "60 Hz", "90 Hz", "120 Hz", "Default"});
        setupSpinner(spBrightness, new String[]{"80%", "100%", "50%", "Default"});
        setupSpinner(spTimeout, new String[]{"Keep Screen Awake", "5m", "1m", "30s", "Default"});

        // Load existing
        GameProfile current = GameProfileManager.getProfile(this, game.getPackageName());
        selectSpinnerValue(spMode, current.getGamingMode());
        selectSpinnerValue(spDpi, current.getDpiPreference());
        selectSpinnerValue(spRefresh, current.getRefreshRatePreference());
        selectSpinnerValue(spBrightness, current.getBrightnessPreference());
        selectSpinnerValue(spTimeout, current.getScreenTimeoutPreference());
        swDnd.setChecked(current.isDndEnabled());
        etSensitivity.setText(current.getSensitivityNotes());
        etGraphics.setText(current.getGraphicsNotes());

        btnSave.setOnClickListener(v -> {
            saveProfileFromInputs(current, spMode, spDpi, spRefresh, spBrightness, spTimeout, swDnd, etSensitivity, etGraphics);
            GameProfileManager.saveProfile(this, current);
            Toast.makeText(this, "Profile saved for " + game.getAppName(), Toast.LENGTH_SHORT).show();
            loadGamesData();
            dialog.dismiss();
        });

        btnPlay.setOnClickListener(v -> {
            saveProfileFromInputs(current, spMode, spDpi, spRefresh, spBrightness, spTimeout, swDnd, etSensitivity, etGraphics);
            GameProfileManager.saveProfile(this, current);
            dialog.dismiss();
            launchTargetGame(game.getPackageName());
        });

        dialog.show();
    }

    private void saveProfileFromInputs(GameProfile profile, Spinner spMode, Spinner spDpi, Spinner spRefresh,
                                       Spinner spBrightness, Spinner spTimeout, SwitchMaterial swDnd,
                                       EditText etSensitivity, EditText etGraphics) {
        profile.setGamingMode(spMode.getSelectedItem().toString());
        profile.setDpiPreference(spDpi.getSelectedItem().toString());
        profile.setRefreshRatePreference(spRefresh.getSelectedItem().toString());
        profile.setBrightnessPreference(spBrightness.getSelectedItem().toString());
        profile.setScreenTimeoutPreference(spTimeout.getSelectedItem().toString());
        profile.setDndEnabled(swDnd.isChecked());
        profile.setSensitivityNotes(etSensitivity.getText().toString().trim());
        profile.setGraphicsNotes(etGraphics.getText().toString().trim());
    }

    private void setupSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().contains(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // ==========================================
    // 4: SETTINGS SCREEN
    // ==========================================
    private void initSettingsScreen() {
        View settingsView = binding.viewFlipper.getChildAt(4);

        // Theme accent color selectors
        settingsView.findViewById(R.id.btn_theme_cyan).setOnClickListener(v -> saveAccentColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_cyan)));
        settingsView.findViewById(R.id.btn_theme_crimson).setOnClickListener(v -> saveAccentColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_crimson)));
        settingsView.findViewById(R.id.btn_theme_emerald).setOnClickListener(v -> saveAccentColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_emerald)));
        settingsView.findViewById(R.id.btn_theme_gold).setOnClickListener(v -> saveAccentColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_gold)));
        settingsView.findViewById(R.id.btn_theme_purple).setOnClickListener(v -> saveAccentColor(androidx.core.content.ContextCompat.getColor(this, R.color.neon_purple)));

        // Official Settings shortcuts
        settingsView.findViewById(R.id.row_setting_display).setOnClickListener(v -> SettingsHelper.openDisplaySettings(this));
        settingsView.findViewById(R.id.row_setting_refresh_rate).setOnClickListener(v -> SettingsHelper.openRefreshRateSettings(this));
        settingsView.findViewById(R.id.row_setting_battery).setOnClickListener(v -> SettingsHelper.openBatterySettings(this));
        settingsView.findViewById(R.id.row_setting_battery_opt).setOnClickListener(v -> SettingsHelper.openBatteryOptimizationSettings(this));
        settingsView.findViewById(R.id.row_setting_dnd).setOnClickListener(v -> SettingsHelper.openDndSettings(this));
        settingsView.findViewById(R.id.row_setting_dev_options).setOnClickListener(v -> SettingsHelper.openDeveloperOptions(this));
        settingsView.findViewById(R.id.row_setting_game_dashboard).setOnClickListener(v -> SettingsHelper.openGameDashboardSettings(this));
        settingsView.findViewById(R.id.row_setting_app_info).setOnClickListener(v -> SettingsHelper.openAppInfo(this));
    }

    // ==========================================
    // SYSTEM TELEMETRY REFRESH
    // ==========================================
    private void refreshAllMetrics() {
        DeviceMonitor.getSnapshotAsync(this, snap -> {
            if (isFinishing() || isDestroyed()) return;
            View homeView = binding.viewFlipper.getChildAt(0);
            if (homeView == null) return;

            // Model & OS
            TextView tvModel = homeView.findViewById(R.id.tv_home_device_model);
            TextView tvAndroid = homeView.findViewById(R.id.tv_home_android_ver);
            if (tvModel != null) tvModel.setText(snap.deviceModel);
            if (tvAndroid != null) tvAndroid.setText(snap.androidVersion);

            // Display
            TextView tvRes = homeView.findViewById(R.id.tv_home_screen_res);
            TextView tvDpi = homeView.findViewById(R.id.tv_home_current_dpi);
            TextView tvFps = homeView.findViewById(R.id.tv_home_refresh_rate);
            if (tvRes != null) tvRes.setText(snap.screenResolution);
            if (tvDpi != null) tvDpi.setText(snap.currentDpi);
            if (tvFps != null) tvFps.setText(snap.refreshRate);

            // RAM
            TextView tvRamVal = homeView.findViewById(R.id.tv_home_ram_val);
            TextView tvRamAvail = homeView.findViewById(R.id.tv_home_ram_avail);
            ProgressBar pbRam = homeView.findViewById(R.id.pb_home_ram);
            if (tvRamVal != null) tvRamVal.setText(snap.ramUsed + " / " + snap.ramTotal);
            if (tvRamAvail != null) tvRamAvail.setText("Free: " + snap.ramAvailable + " (" + (100 - snap.ramUsedPercent) + "%)");
            if (pbRam != null) pbRam.setProgress(snap.ramUsedPercent);

            // Battery
            TextView tvBatVal = homeView.findViewById(R.id.tv_home_battery_val);
            TextView tvBatTemp = homeView.findViewById(R.id.tv_home_battery_temp);
            ProgressBar pbBat = homeView.findViewById(R.id.pb_home_battery);
            if (tvBatVal != null) tvBatVal.setText(snap.batteryPercentage + " (" + snap.batteryStatus + ")");
            if (tvBatTemp != null) tvBatTemp.setText("Temp: " + snap.batteryTemperature);
            if (pbBat != null) pbBat.setProgress(snap.batteryPercentInt);

            // CPU & Gaming Mode
            TextView tvCpu = homeView.findViewById(R.id.tv_home_cpu_info);
            if (tvCpu != null) tvCpu.setText(snap.cpuArchitecture + " (" + snap.cpuCores + ")");

            // Network
            TextView tvNet = homeView.findViewById(R.id.tv_home_network_type);
            if (tvNet != null) tvNet.setText(NetworkMonitor.getNetworkType(this));

            // DPI Screen metrics
            updateDpiScreenMetrics();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (boostPulseAnim != null) {
            boostPulseAnim.cancel();
        }
    }
}
