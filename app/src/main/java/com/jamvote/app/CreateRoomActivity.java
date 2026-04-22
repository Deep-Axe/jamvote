package com.jamvote.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.jamvote.app.queue.QueueActivity;
import com.jamvote.app.util.FirebaseUtil;

public class CreateRoomActivity extends AppCompatActivity {

    private TextInputEditText etRoomName;
    private SwitchCompat switchSfw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        etRoomName = findViewById(R.id.etRoomName);
        switchSfw = findViewById(R.id.switchSfw);

        findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String roomName = etRoomName.getText().toString().trim();
            boolean sfwMode = switchSfw.isChecked();

            if (TextUtils.isEmpty(roomName)) {
                Toast.makeText(this, R.string.error_room_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUtil.createRoom(roomName, sfwMode, new FirebaseUtil.RoomCallback() {
                @Override
                public void onSuccess(String roomId) {
                    Intent intent = new Intent(CreateRoomActivity.this, QueueActivity.class);
                    intent.putExtra("roomId", roomId);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(CreateRoomActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}