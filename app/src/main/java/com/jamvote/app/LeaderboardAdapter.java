package com.jamvote.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.jamvote.app.model.User;

import java.util.Locale;

public class LeaderboardAdapter extends ListAdapter<User, LeaderboardAdapter.ViewHolder> {

    public LeaderboardAdapter() {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getUid().equals(newItem.getUid());
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getReputation() == newItem.getReputation() &&
                        oldItem.getDisplayName().equals(newItem.getDisplayName());
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position + 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvUserName, tvReputation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvReputation = itemView.findViewById(R.id.tvReputation);
        }

        public void bind(User user, int rank) {
            tvRank.setText(String.valueOf(rank));
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Unknown Jammer");
            tvReputation.setText(String.format(Locale.getDefault(), "%,d", user.getReputation()));
        }
    }
}