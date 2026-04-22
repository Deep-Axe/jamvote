package com.jamvote.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.jamvote.app.model.Song;
import com.jamvote.app.music.SearchAdapter;
import com.jamvote.app.music.YouTubeProvider;
import com.jamvote.app.util.FirebaseUtil;

import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private String roomId;
    private boolean sfwMode;
    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private SearchAdapter adapter;
    private ProgressBar pbSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        roomId = getIntent().getStringExtra("roomId");
        sfwMode = getIntent().getBooleanExtra("sfwMode", false);

        if (roomId == null) {
            finish();
            return;
        }

        etSearch = findViewById(R.id.etSearch);
        rvResults = findViewById(R.id.rvSearchResults);
        pbSearch = findViewById(R.id.pbSearch);
        ImageButton btnClose = findViewById(R.id.btnCloseSearch);

        setupRecyclerView();

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new SearchAdapter(song -> {
            song.setAddedByUid(FirebaseAuth.getInstance().getCurrentUser().getUid());
            song.setAddedByName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            addSongToFirebase(song);
        });
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);
    }

    private void addSongToFirebase(Song song) {
        if (sfwMode && song.isExplicit()) {
            Snackbar.make(findViewById(R.id.search_root), R.string.sfw_blocked, Snackbar.LENGTH_LONG).show();
            return;
        }
        FirebaseUtil.addSong(roomId, song, new FirebaseUtil.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(SearchActivity.this, "Song added to queue!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(SearchActivity.this, "Failed to add song: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) return;

        pbSearch.setVisibility(View.VISIBLE);
        YouTubeProvider.searchVideos(query, sfwMode, getPackageName(), BuildConfig.SHA1_FINGERPRINT, new YouTubeProvider.SearchCallback() {
            @Override
            public void onSuccess(List<Song> songs) {
                runOnUiThread(() -> {
                    pbSearch.setVisibility(View.GONE);
                    adapter.submitList(songs);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    pbSearch.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "Search failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}