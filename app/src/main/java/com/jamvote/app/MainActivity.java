package com.jamvote.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jamvote.app.util.FirebaseUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WebView youtubeWebView;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvWelcome = findViewById(R.id.tvWelcome);
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = user.getEmail();
        }
        tvWelcome.setText("Welcome, " + name);

        setupWebView();

        // Create Room
        findViewById(R.id.cardCreateRoom).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateRoomActivity.class));
        });

        // Join Room
        findViewById(R.id.cardJoinRoom).setOnClickListener(v -> {
            startActivity(new Intent(this, JoinRoomActivity.class));
        });

        // Logout
        findViewById(R.id.btnLogoutIcon).setOnClickListener(v -> {
            FirebaseUtil.logout();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        youtubeWebView = findViewById(R.id.youtubeWebView);
        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");

        youtubeWebView.setWebViewClient(new WebViewClient());
        youtubeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message());
                return true;
            }
        });
    }
}