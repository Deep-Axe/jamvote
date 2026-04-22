package com.jamvote.app;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.jamvote.app.util.FirebaseUtil;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView rvLeaderboard;
    private LeaderboardAdapter adapter;
    private ListenerRegistration leaderboardListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.leaderboard_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        ImageButton btnClose = findViewById(R.id.btnCloseLeaderboard);

        adapter = new LeaderboardAdapter();
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);

        btnClose.setOnClickListener(v -> finish());

        leaderboardListener = FirebaseUtil.listenToLeaderboard(users -> {
            adapter.submitList(users);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (leaderboardListener != null) {
            leaderboardListener.remove();
        }
    }
}