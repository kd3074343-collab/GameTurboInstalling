package com.example;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppSelectAdapter extends RecyclerView.Adapter<AppSelectAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppSelected(GameLauncher.InstalledAppInfo appInfo);
    }

    private final List<GameLauncher.InstalledAppInfo> fullList = new ArrayList<>();
    private final List<GameLauncher.InstalledAppInfo> displayList = new ArrayList<>();
    private final OnAppClickListener listener;

    public AppSelectAdapter(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void setApps(List<GameLauncher.InstalledAppInfo> apps) {
        fullList.clear();
        displayList.clear();
        if (apps != null) {
            fullList.addAll(apps);
            displayList.addAll(apps);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        displayList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lower = query.toLowerCase().trim();
            for (GameLauncher.InstalledAppInfo app : fullList) {
                if (app.appName.toLowerCase().contains(lower) || app.packageName.toLowerCase().contains(lower)) {
                    displayList.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_select, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        GameLauncher.InstalledAppInfo app = displayList.get(position);
        holder.tvName.setText(app.appName);
        holder.tvPackage.setText(app.packageName);
        if (app.icon != null) {
            holder.imgIcon.setImageDrawable(app.icon);
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_nav_games);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppSelected(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        TextView tvPackage;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_dialog_app_icon);
            tvName = itemView.findViewById(R.id.tv_dialog_app_name);
            tvPackage = itemView.findViewById(R.id.tv_dialog_package_name);
        }
    }
}
