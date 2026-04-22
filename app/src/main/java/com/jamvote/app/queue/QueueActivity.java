package com.jamvote.app.queue;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jamvote.app.R;
import com.jamvote.app.RoomSettingsActivity;
import com.jamvote.app.SearchActivity;
import com.jamvote.app.model.Song;
import com.jamvote.app.util.FirebaseUtil;

import java.util.List;
import java.util.Map;

public class QueueActivity extends AppCompatActivity {

    private static final String TAG = "QueueActivity";
    private String roomId;
    private boolean isHost = false;
    private boolean sfwMode = false;
    private TextView tvRoomName, tvRoomCode, tvNowPlayingTitle, tvUserCount, tvUpNext, tvNavQueue;
    private ImageView ivNowPlayingArt;
    private ImageButton btnPlayTest, btnSkip, btnSettings;
    private ProgressBar pbSong;
    private RecyclerView rvQueue;
    private QueueAdapter adapter;
    private WebView youtubeWebView;
    private QueueViewModel viewModel;
    private boolean localIsPlaying = false;
    private Song currentSong;
    private long lastKnownPosition = 0;

    private boolean showingHistory = false;

    private ListenerRegistration roomListener, presenceListener;
    private long lastHandledVersion = -1;
    private boolean isReorderInProgress = false;
    private final Handler playbackDebounceHandler = new Handler();
    private Runnable pendingPlaybackRunnable;
    
    private final Handler seekHandler = new Handler();
    private final Runnable seekRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHost && localIsPlaying) {
                youtubeWebView.evaluateJavascript(
                    "typeof player !== 'undefined' && player.getCurrentTime ? String(player.getCurrentTime()) : null",
                    value -> {
                        if (value != null && !value.equals("null")) {
                            try {
                                long secs = (long) Float.parseFloat(value.replace("\"", ""));
                                lastKnownPosition = secs;
                                FirebaseUtil.updatePlaybackState(roomId, localIsPlaying, secs, null);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                );
            }
            seekHandler.postDelayed(this, 2000);
        }
    };

    private final Handler heartbeatHandler = new Handler();
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            FirebaseUtil.updatePresence(roomId);
            heartbeatHandler.postDelayed(this, 30000); // 30 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_queue);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.queue_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        roomId = getIntent().getStringExtra("roomId");
        if (roomId == null) {
            finish();
            return;
        }

        tvRoomName = findViewById(R.id.tvRoomName);
        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvUserCount = findViewById(R.id.tvUserCount);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvUpNext = findViewById(R.id.tvUpNext);
        tvNavQueue = findViewById(R.id.tvNavQueue);
        ivNowPlayingArt = findViewById(R.id.ivNowPlayingArt);
        btnPlayTest = findViewById(R.id.btnPlayTest);
        btnSkip = findViewById(R.id.btnSkip);
        btnSettings = findViewById(R.id.btnSettings);
        pbSong = findViewById(R.id.pbSong);
        rvQueue = findViewById(R.id.rvQueue);
        youtubeWebView = findViewById(R.id.youtubeWebView);

        setupRecyclerView();
        setupWebView();
        setupViewModel();
        fetchRoomDetails();
        listenToPresence();

        btnPlayTest.setOnClickListener(v -> togglePlayback());

        btnSkip.setOnClickListener(v -> {
            currentSong = null;
            FirebaseUtil.reorderAndPopNextSong(roomId, null);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoomSettingsActivity.class);
            intent.putExtra("roomId", roomId);
            startActivity(intent);
        });

        findViewById(R.id.navQueue).setOnClickListener(v -> {
            showingHistory = !showingHistory;
            if (showingHistory) {
                tvUpNext.setText("HISTORY");
                tvNavQueue.setText("Queue");
                List<Song> history = viewModel.getHistoryItems().getValue();
                adapter.submitList(history != null ? history : java.util.Collections.emptyList());
            } else {
                tvUpNext.setText("UP NEXT");
                tvNavQueue.setText("History");
                List<Song> queue = viewModel.getQueueItems().getValue();
                adapter.submitList(queue != null ? queue : java.util.Collections.emptyList());
                rvQueue.smoothScrollToPosition(0);
            }
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("roomId", roomId);
            intent.putExtra("sfwMode", sfwMode);
            startActivity(intent);
        });

        findViewById(R.id.navLeaderboard).setOnClickListener(v -> {
            startActivity(new Intent(this, com.jamvote.app.LeaderboardActivity.class));
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leaveRoom();
            }
        });

        seekHandler.post(seekRunnable);
        heartbeatHandler.post(heartbeatRunnable);
    }

    private void setupRecyclerView() {
        adapter = new QueueAdapter(
            (song, isUpvote) -> {
                if (!showingHistory) {
                    FirebaseUtil.voteSong(roomId, FirebaseAuth.getInstance().getCurrentUser().getUid(), song.getSongId(), isUpvote, null);
                } else {
                    Toast.makeText(QueueActivity.this, "Cannot vote on played songs", Toast.LENGTH_SHORT).show();
                }
            },
            song -> {
                if (showingHistory) {
                    if (sfwMode && song.isExplicit()) {
                        Toast.makeText(QueueActivity.this, "SFW mode is on — explicit songs are blocked", Toast.LENGTH_LONG).show();
                        return;
                    }
                    song.setAddedByUid(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    song.setAddedByName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                    FirebaseUtil.addSong(roomId, song, new FirebaseUtil.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(QueueActivity.this, "Song re-added to queue!", Toast.LENGTH_SHORT).show();
                            findViewById(R.id.navQueue).performClick();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(QueueActivity.this, "Failed to add song: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        );
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        rvQueue.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(QueueViewModel.class);
        viewModel.startListening(roomId);
        viewModel.getQueueItems().observe(this, songs -> {
            try {
                if (!showingHistory) adapter.submitList(songs);
                if (isHost && currentSong == null && !songs.isEmpty() && !isReorderInProgress) {
                    isReorderInProgress = true;
                    FirebaseUtil.reorderAndPopNextSong(roomId, new FirebaseUtil.SimpleCallback() {
                        @Override public void onSuccess() { isReorderInProgress = false; }
                        @Override public void onFailure(String error) { isReorderInProgress = false; }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "ViewModel observer error", e);
            }
        });
        viewModel.getHistoryItems().observe(this, history -> {
            if (showingHistory) adapter.submitList(history);
        });
    }

    private void fetchRoomDetails() {
        roomListener = FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Room listener error", error);
                        return;
                    }
                    if (doc == null || !doc.exists()) return;

                    try {
                        String status = doc.getString("status");
                        if ("ended".equals(status)) {
                            Toast.makeText(this, "Session has been ended by host", Toast.LENGTH_LONG).show();
                            leaveRoom();
                            return;
                        }

                        tvRoomName.setText(doc.getString("roomName"));
                        tvRoomCode.setText(doc.getString("roomCode"));
                        sfwMode = Boolean.TRUE.equals(doc.getBoolean("sfwMode"));

                        String hostUid = doc.getString("hostUid");
                        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                        isHost = myUid != null && myUid.equals(hostUid);
                        if (isHost) btnSettings.setVisibility(View.VISIBLE);
                        btnPlayTest.setVisibility(isHost ? View.VISIBLE : View.GONE);
                        btnSkip.setVisibility(isHost ? View.VISIBLE : View.GONE);

                        boolean remoteIsPlaying = Boolean.TRUE.equals(doc.getBoolean("playing"));
                        long remotePosition = doc.getLong("playbackPosition") != null ? doc.getLong("playbackPosition") : 0;
                        long playbackVersion = doc.getLong("playbackVersion") != null ? doc.getLong("playbackVersion") : 0;

                        Song nowPlaying = null;
                        if (doc.contains("nowPlaying")) {
                            Map<String, Object> songMap = (Map<String, Object>) doc.get("nowPlaying");
                            if (songMap != null) {
                                nowPlaying = doc.get("nowPlaying", Song.class);
                            }
                        }

                        handleRemotePlaybackUpdate(nowPlaying, remoteIsPlaying, remotePosition, playbackVersion);
                    } catch (Exception e) {
                        Log.e(TAG, "fetchRoomDetails processing error", e);
                    }
                });
    }

    private void listenToPresence() {
        presenceListener = FirebaseUtil.listenToPresence(roomId, users -> {
            if (users != null) {
                tvUserCount.setText(String.valueOf(users.size()));
            }
        });
    }

    private void leaveRoom() {
        FirebaseUtil.leaveRoom(roomId, new FirebaseUtil.SimpleCallback() {
            @Override
            public void onSuccess() {
                finish();
            }

            @Override
            public void onFailure(String error) {
                finish();
            }
        });
    }

    private void handleRemotePlaybackUpdate(Song song, boolean remoteIsPlaying, long remotePosition, long playbackVersion) {
        if (song == null) {
            currentSong = null;
            tvNowPlayingTitle.setText("No song playing");
            ivNowPlayingArt.setImageResource(android.R.color.darker_gray);
            stopPlayback();
            return;
        }

        if (currentSong == null || !currentSong.getYoutubeVideoId().equals(song.getYoutubeVideoId())) {
            currentSong = song;
            tvNowPlayingTitle.setText(song.getTitle());
            Glide.with(this).load(song.getThumbnailUrl()).transform(new RoundedCorners(10)).into(ivNowPlayingArt);
            startPlayback(song.getYoutubeVideoId(), remotePosition);
            lastHandledVersion = playbackVersion;
            return;
        }

        if (!isHost && playbackVersion != lastHandledVersion) {
            long previousVersion = lastHandledVersion;
            lastHandledVersion = playbackVersion;
            if (pendingPlaybackRunnable != null) {
                playbackDebounceHandler.removeCallbacks(pendingPlaybackRunnable);
            }
            final boolean shouldPlay = remoteIsPlaying;
            pendingPlaybackRunnable = () -> {
                if (shouldPlay && !localIsPlaying) {
                    youtubeWebView.evaluateJavascript(
                        "(function() { if(typeof player !== 'undefined' && player.playVideo) { player.playVideo(); return 'true'; } return 'false'; })()",
                        value -> {
                            if ("\"true\"".equals(value)) {
                                localIsPlaying = true;
                                btnPlayTest.setImageResource(R.drawable.ic_pause);
                            } else {
                                lastHandledVersion = previousVersion; // Reset so next snapshot retries
                            }
                        }
                    );
                } else if (!shouldPlay && localIsPlaying) {
                    youtubeWebView.evaluateJavascript(
                        "(function() { if(typeof player !== 'undefined' && player.pauseVideo) { player.pauseVideo(); return 'true'; } return 'false'; })()",
                        value -> {
                            if ("\"true\"".equals(value)) {
                                localIsPlaying = false;
                                btnPlayTest.setImageResource(R.drawable.ic_play);
                            } else {
                                lastHandledVersion = previousVersion; // Reset so next snapshot retries
                            }
                        }
                    );
                }
            };
            playbackDebounceHandler.postDelayed(pendingPlaybackRunnable, 300);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");

        youtubeWebView.setWebViewClient(new WebViewClient());
        youtubeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message());
                return true;
            }
        });
        
        youtubeWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onPlayerStateChange(int state) {
                runOnUiThread(() -> {
                    if (state == 0) handleSongEnd(); // Ended
                    if (isHost) {
                        if (state == 1) updateHostPlaybackState(true); // Playing
                        if (state == 2) updateHostPlaybackState(false); // Paused
                    }
                });
            }

            @JavascriptInterface
            public void onLog(String message) {
                Log.d(TAG, "YT: " + message);
            }

            @JavascriptInterface
            public void onPlayerError(int code) {
                runOnUiThread(() -> {
                    Log.w(TAG, "YT player error: " + code);
                    if (code == 100 || code == 101 || code == 150 || code == 151 || code == 152) {
                        handleSongEnd();
                    }
                });
            }
        }, "AndroidInterface");
    }

    private void updateHostPlaybackState(boolean playing) {
        localIsPlaying = playing;
        FirebaseUtil.updatePlaybackStateIntentional(roomId, playing, null);
    }

    private void togglePlayback() {
        if (localIsPlaying) {
            youtubeWebView.evaluateJavascript(
                "(function(){if(typeof player!=='undefined'&&player.pauseVideo){player.pauseVideo();return 'ok';}return 'no-player';})()",
                value -> Log.d(TAG, "pause result: " + value)
            );
            btnPlayTest.setImageResource(R.drawable.ic_play);
        } else {
            youtubeWebView.evaluateJavascript(
                "(function(){if(typeof player!=='undefined'&&player.playVideo){player.playVideo();return 'ok';}return 'no-player';})()",
                value -> Log.d(TAG, "play result: " + value)
            );
            btnPlayTest.setImageResource(R.drawable.ic_pause);
        }
    }

    private void startPlayback(String videoId, long position) {
        String baseUrl = "https://" + getPackageName();
        String html = "<html>" +
                "<head><meta name='referrer' content='strict-origin-when-cross-origin'></head>" +
                "<body style='margin:0;padding:0;'>" +
                "<div id='player'></div>" +
                "<script>" +
                "var tag = document.createElement('script');" +
                "tag.src = 'https://www.youtube.com/iframe_api';" +
                "var firstScriptTag = document.getElementsByTagName('script')[0];" +
                "firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);" +
                "var player;" +
                "function onYouTubeIframeAPIReady() {" +
                "  player = new YT.Player('player', {" +
                "    height: '1', width: '1', videoId: '" + videoId + "'," +
                "    playerVars: { 'autoplay': 1, 'controls': 0, 'start': " + position +
                "      , 'enablejsapi': 1, 'origin': '" + baseUrl + "'" +
                "      , 'widget_referrer': '" + baseUrl + "' }," +
                "    events: { 'onReady': onPlayerReady, 'onStateChange': onPlayerStateChange, 'onError': onPlayerError }" +
                "  });" +
                "}" +
                "function onPlayerReady(event) {" +
                "  AndroidInterface.onLog('ready');" +
                "  event.target.playVideo();" +
                "}" +
                "function onPlayerStateChange(event) {" +
                "  AndroidInterface.onPlayerStateChange(event.data);" +
                "}" +
                "function onPlayerError(event) {" +
                "  AndroidInterface.onPlayerError(event.data);" +
                "}" +
                "</script></body></html>";
        youtubeWebView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null);
        localIsPlaying = true;
        btnPlayTest.setImageResource(R.drawable.ic_pause);
    }

    private void stopPlayback() {
        youtubeWebView.loadUrl("about:blank");
        localIsPlaying = false;
        btnPlayTest.setImageResource(R.drawable.ic_play);
    }

    private void handleSongEnd() {
        if (isHost) {
            FirebaseUtil.reorderAndPopNextSong(roomId, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        youtubeWebView.onResume();
        if (currentSong != null && localIsPlaying) {
            youtubeWebView.evaluateJavascript(
                "(function(){" +
                "  if(typeof player!=='undefined'&&player.getPlayerState){" +
                "    if(player.getPlayerState()!==1) player.playVideo();" +
                "    return 'ok';" +
                "  }" +
                "  return 'dead';" +
                "})()",
                value -> {
                    if ("\"dead\"".equals(value) && currentSong != null) {
                        runOnUiThread(() -> startPlayback(currentSong.getYoutubeVideoId(), lastKnownPosition));
                    }
                }
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        youtubeWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null) roomListener.remove();
        if (presenceListener != null) presenceListener.remove();
        seekHandler.removeCallbacks(seekRunnable);
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        if (pendingPlaybackRunnable != null) playbackDebounceHandler.removeCallbacks(pendingPlaybackRunnable);
    }
}
