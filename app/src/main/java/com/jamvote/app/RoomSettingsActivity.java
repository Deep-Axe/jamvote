package com.jamvote.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.jamvote.app.util.FirebaseUtil;

public class RoomSettingsActivity extends AppCompatActivity {

    private String roomId;
    private SwitchCompat switchSfw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_room_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        roomId = getIntent().getStringExtra("roomId");
        if (roomId == null) {
            finish();
            return;
        }

        switchSfw = findViewById(R.id.switchSfwSettings);
        Button btnSave = findViewById(R.id.btnSaveSettings);
        Button btnEnd = findViewById(R.id.btnEndSession);

        fetchInitialSettings();

        btnSave.setOnClickListener(v -> {
            boolean sfwMode = switchSfw.isChecked();
            FirebaseUtil.updateRoomSettings(roomId, sfwMode, new FirebaseUtil.SimpleCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(RoomSettingsActivity.this, "Settings saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(RoomSettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnEnd.setOnClickListener(v -> {
            FirebaseUtil.endSessionWithSummary(roomId, new FirebaseUtil.SummaryCallback() {
                @Override
                public void onSuccess(String crowdFavName, String topJammerName) {
                    Intent intent = new Intent(RoomSettingsActivity.this, SessionSummaryActivity.class);
                    intent.putExtra("crowdFav", crowdFavName);
                    intent.putExtra("topJammer", topJammerName);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(RoomSettingsActivity.this, "Failed to end session: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void fetchInitialSettings() {
        FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean sfw = doc.getBoolean("sfwMode");
                        if (sfw != null) {
                            switchSfw.setChecked(sfw);
                        }
                    }
                });
    }
}