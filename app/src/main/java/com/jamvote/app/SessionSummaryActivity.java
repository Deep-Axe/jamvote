package com.jamvote.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SessionSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_session_summary);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.summary_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String crowdFav = getIntent().getStringExtra("crowdFav");
        String topJammer = getIntent().getStringExtra("topJammer");

        TextView tvCrowdFav = findViewById(R.id.tvCrowdFav);
        TextView tvTopJammer = findViewById(R.id.tvTopJammer);

        tvCrowdFav.setText(crowdFav != null ? crowdFav : "None");
        tvTopJammer.setText(topJammer != null ? topJammer : "None");

        findViewById(R.id.btnDoneSummary).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}