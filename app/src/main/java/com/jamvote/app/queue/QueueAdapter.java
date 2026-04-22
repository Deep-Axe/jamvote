package com.jamvote.app.queue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.jamvote.app.R;
import com.jamvote.app.model.Song;

public class QueueAdapter extends ListAdapter<Song, QueueAdapter.ViewHolder> {

    public interface OnVoteListener {
        void onVote(Song song, boolean isUpvote);
    }

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    private final OnVoteListener voteListener;
    private final OnItemClickListener itemClickListener;

    public QueueAdapter(OnVoteListener voteListener, OnItemClickListener itemClickListener) {
        super(new DiffUtil.ItemCallback<Song>() {
            @Override
            public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                return oldItem.getSongId().equals(newItem.getSongId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                return oldItem.getScore().equals(newItem.getScore()) &&
                        oldItem.getUpvotes().equals(newItem.getUpvotes()) &&
                        oldItem.getDownvotes().equals(newItem.getDownvotes());
            }
        });
        this.voteListener = voteListener;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_song, parent, false);
        return new ViewHolder(view, voteListener, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = getItem(position);
        holder.bind(song, position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View goldAccentBar;
        ImageView ivAlbumArt;
        TextView tvSongTitle, tvArtistInfo, tvScore;
        ImageButton btnUpvote, btnDownvote;
        OnVoteListener voteListener;
        OnItemClickListener itemClickListener;

        public ViewHolder(@NonNull View itemView, OnVoteListener voteListener, OnItemClickListener itemClickListener) {
            super(itemView);
            this.voteListener = voteListener;
            this.itemClickListener = itemClickListener;
            goldAccentBar = itemView.findViewById(R.id.goldAccentBar);
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtistInfo = itemView.findViewById(R.id.tvArtistInfo);
            tvScore = itemView.findViewById(R.id.tvScore);
            btnUpvote = itemView.findViewById(R.id.btnUpvote);
            btnDownvote = itemView.findViewById(R.id.btnDownvote);
        }

        public void bind(Song song, int position) {
            tvSongTitle.setText(song.getTitle());
            tvArtistInfo.setText(song.getArtist() + " · Added by " + (song.getAddedByName() != null ? song.getAddedByName() : "User"));
            tvScore.setText(String.valueOf(song.getScore()));

            goldAccentBar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

            Glide.with(itemView.getContext())
                    .load(song.getThumbnailUrl())
                    .placeholder(android.R.color.darker_gray)
                    .transform(new RoundedCorners(9))
                    .into(ivAlbumArt);
            
            btnUpvote.setOnClickListener(v -> {
                if (voteListener != null) voteListener.onVote(song, true);
            });

            btnDownvote.setOnClickListener(v -> {
                if (voteListener != null) voteListener.onVote(song, false);
            });

            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) itemClickListener.onItemClick(song);
            });
        }
    }
}