package com.jamvote.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.jamvote.app.util.FirebaseUtil;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show();
                return;
            }

            // Enforce password requirements
            if (!FirebaseUtil.isPasswordValid(password)) {
                Toast.makeText(this, R.string.error_password_length, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUtil.login(email, password, new FirebaseUtil.AuthCallback() {
                @Override
                public void onSuccess() {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            });
        });

        findViewById(R.id.tvGoToRegister).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}