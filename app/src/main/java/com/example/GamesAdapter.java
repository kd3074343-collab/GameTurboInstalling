package com.example;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.GameViewHolder> {

    public interface OnGameActionListener {
        void onPlayClicked(GameItem game);
        void onProfileClicked(GameItem game);
        void onFavoriteClicked(GameItem game);
        void onRemoveClicked(GameItem game);
        void onCardClicked(GameItem game);
    }

    private final Context context;
    private final List<GameItem> gameList = new ArrayList<>();
    private final OnGameActionListener listener;

    public GamesAdapter(Context context, OnGameActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<GameItem> items) {
        gameList.clear();
        if (items != null) {
            gameList.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        GameItem item = gameList.get(position);

        holder.tvTitle.setText(item.getAppName());
        holder.tvPackage.setText(item.getPackageName());

        if (item.getIcon() != null) {
            holder.imgIcon.setImageDrawable(item.getIcon());
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_nav_games);
        }

        // Last played
        if (item.getLastPlayedTime() > 0) {
            CharSequence relative = DateUtils.getRelativeTimeSpanString(
                    item.getLastPlayedTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            holder.tvLastPlayed.setText("Last played: " + relative);
        } else {
            holder.tvLastPlayed.setText("Last played: Never");
        }

        // Profile summary
        GameProfile profile = GameProfileManager.getProfile(context, item.getPackageName());
        holder.tvProfileSummary.setText("Profile: " + profile.getGamingMode() + " • " + profile.getScreenTimeoutPreference());

        // Install state
        if (item.isInstalled()) {
            holder.tvInstallStatus.setText("Installed");
            holder.tvInstallStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.neon_emerald));
            holder.btnPlay.setEnabled(true);
            holder.btnPlay.setAlpha(1.0f);
        } else {
            holder.tvInstallStatus.setText("Not Installed");
            holder.tvInstallStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.neon_crimson));
            holder.btnPlay.setEnabled(false);
            holder.btnPlay.setAlpha(0.5f);
        }

        // Favorite star
        if (item.isFavorite()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_outline);
        }

        // Clicks
        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) listener.onPlayClicked(item);
        });

        holder.btnProfile.setOnClickListener(v -> {
            if (listener != null) listener.onProfileClicked(item);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClicked(item);
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveClicked(item);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClicked(item);
        });
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvTitle;
        TextView tvPackage;
        TextView tvLastPlayed;
        TextView tvProfileSummary;
        TextView tvInstallStatus;
        ImageButton btnFavorite;
        Button btnProfile;
        Button btnRemove;
        Button btnPlay;

        GameViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_game_icon);
            tvTitle = itemView.findViewById(R.id.tv_game_title);
            tvPackage = itemView.findViewById(R.id.tv_game_package);
            tvLastPlayed = itemView.findViewById(R.id.tv_game_last_played);
            tvProfileSummary = itemView.findViewById(R.id.tv_profile_summary);
            tvInstallStatus = itemView.findViewById(R.id.tv_install_status);
            btnFavorite = itemView.findViewById(R.id.btn_game_favorite);
            btnProfile = itemView.findViewById(R.id.btn_game_profile);
            btnRemove = itemView.findViewById(R.id.btn_game_remove);
            btnPlay = itemView.findViewById(R.id.btn_game_play);
        }
    }
}
