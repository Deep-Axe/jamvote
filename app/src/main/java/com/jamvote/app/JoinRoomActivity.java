package com.jamvote.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.jamvote.app.queue.QueueActivity;
import com.jamvote.app.util.FirebaseUtil;

public class JoinRoomActivity extends AppCompatActivity {

    private TextInputEditText etRoomCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        etRoomCode = findViewById(R.id.etRoomCode);

        findViewById(R.id.btnJoin).setOnClickListener(v -> {
            String code = etRoomCode.getText().toString().trim();

            if (TextUtils.isEmpty(code) || code.length() != 4) {
                Toast.makeText(this, R.string.error_invalid_code, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUtil.joinRoom(code, new FirebaseUtil.RoomCallback() {
                @Override
                public void onSuccess(String roomId) {
                    Intent intent = new Intent(JoinRoomActivity.this, QueueActivity.class);
                    intent.putExtra("roomId", roomId);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(JoinRoomActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}