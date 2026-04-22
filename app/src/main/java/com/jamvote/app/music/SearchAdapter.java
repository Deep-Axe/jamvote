package com.jamvote.app.music;

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

public class SearchAdapter extends ListAdapter<Song, SearchAdapter.ViewHolder> {

    public interface OnAddListener {
        void onAdd(Song song);
    }

    private final OnAddListener listener;

    public SearchAdapter(OnAddListener listener) {
        super(new DiffUtil.ItemCallback<Song>() {
            @Override
            public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                return oldItem.getYoutubeVideoId().equals(newItem.getYoutubeVideoId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArt;
        TextView tvTitle, tvArtist;
        ImageButton btnAdd;
        OnAddListener listener;

        public ViewHolder(@NonNull View itemView, OnAddListener listener) {
            super(itemView);
            this.listener = listener;
            ivArt = itemView.findViewById(R.id.ivSearchArt);
            tvTitle = itemView.findViewById(R.id.tvSearchTitle);
            tvArtist = itemView.findViewById(R.id.tvSearchArtist);
            btnAdd = itemView.findViewById(R.id.btnAddSong);
        }

        public void bind(Song song) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());

            Glide.with(itemView.getContext())
                    .load(song.getThumbnailUrl())
                    .placeholder(android.R.color.darker_gray)
                    .transform(new RoundedCorners(10))
                    .into(ivArt);

            btnAdd.setOnClickListener(v -> {
                if (listener != null) listener.onAdd(song);
            });
        }
    }
}