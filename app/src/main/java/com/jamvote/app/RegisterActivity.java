package com.jamvote.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.jamvote.app.util.FirebaseUtil;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etDisplayName, etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String displayName = etDisplayName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(displayName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!FirebaseUtil.isPasswordValid(password)) {
                Toast.makeText(this, R.string.error_password_length, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUtil.register(email, password, displayName, new FirebaseUtil.AuthCallback() {
                @Override
                public void onSuccess() {
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finishAffinity(); // Clear stack
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
        });

        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            finish(); // Go back to LoginActivity
        });
    }
}