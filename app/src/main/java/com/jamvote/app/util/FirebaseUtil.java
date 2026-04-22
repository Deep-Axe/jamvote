package com.jamvote.app.util;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jamvote.app.model.Song;
import com.jamvote.app.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FirebaseUtil {

    private static final String TAG = "FirebaseUtil";
    private static final Map<String, Boolean> cleanupRegistered = new HashMap<>();

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface RoomCallback {
        void onSuccess(String roomId);
        void onFailure(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface QueueListener {
        void onUpdate(List<Song> songs);
    }

    public interface UserListener {
        void onUpdate(List<User> users);
    }

    public interface SummaryCallback {
        void onSuccess(String crowdFavName, String topJammerName);
        void onFailure(String error);
    }

    public static void login(String email, String password, AuthCallback callback) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    public static void register(String email, String password, String displayName, AuthCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            saveUserToFirestore(user, displayName, callback);
                                        } else {
                                            callback.onFailure(profileTask.getException() != null ? profileTask.getException().getMessage() : "Failed to set display name");
                                        }
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    private static void saveUserToFirestore(FirebaseUser user, String displayName, AuthCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("displayName", displayName);
        userData.put("email", user.getEmail());
        userData.put("reputation", 0);
        userData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    public static void createRoom(String roomName, boolean sfwMode, RoomCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure("User not authenticated");
            return;
        }

        String roomCode = generateRoomCode();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("rooms").whereEqualTo("roomCode", roomCode).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Map<String, Object> roomData = new HashMap<>();
                            roomData.put("roomCode", roomCode);
                            roomData.put("roomName", roomName);
                            roomData.put("hostUid", user.getUid());
                            roomData.put("status", "active");
                            roomData.put("sfwMode", sfwMode);
                            roomData.put("activeUsers", new ArrayList<String>());
                            roomData.put("createdAt", FieldValue.serverTimestamp());
                            roomData.put("playing", false);
                            roomData.put("playbackPosition", 0L);
                            roomData.put("playbackVersion", 0L);
                            roomData.put("maxQueuePosition", -1L);

                            db.collection("rooms").add(roomData)
                                    .addOnSuccessListener(documentReference -> {
                                        joinRoomById(documentReference.getId(), callback);
                                    })
                                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                        } else {
                            createRoom(roomName, sfwMode, callback);
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to check room code uniqueness");
                    }
                });
    }

    public static void joinRoom(String roomCode, RoomCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure("User not authenticated");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms")
                .whereEqualTo("roomCode", roomCode.toUpperCase())
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String roomId = task.getResult().getDocuments().get(0).getId();
                        joinRoomById(roomId, callback);
                    } else {
                        callback.onFailure("Room not found or session ended");
                    }
                });
    }

    private static void joinRoomById(String roomId, RoomCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms").document(roomId)
                .update("activeUsers", FieldValue.arrayUnion(user.getUid()))
                .addOnSuccessListener(aVoid -> {
                    updatePresence(roomId);
                    callback.onSuccess(roomId);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void updatePresence(String roomId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // RTDB update (source of truth for active count/crash safety)
        DatabaseReference rtdb = FirebaseDatabase.getInstance()
                .getReference("presence/" + roomId + "/" + user.getUid());
        
        rtdb.setValue(user.getDisplayName());
        
        // Only register onDisconnect once per session to reduce write volume
        if (!cleanupRegistered.getOrDefault(roomId + "_" + user.getUid(), false)) {
            rtdb.onDisconnect().removeValue();
            cleanupRegistered.put(roomId + "_" + user.getUid(), true);
        }
    }

    public static ListenerRegistration listenToPresence(String roomId, UserListener listener) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("presence/" + roomId);
        ValueEventListener rtdbListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User u = new User();
                    u.setUid(child.getKey());
                    u.setDisplayName(child.getValue(String.class));
                    users.add(u);
                }
                listener.onUpdate(users);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "RTDB presence listen failed", error.toException());
            }
        };
        
        ref.addValueEventListener(rtdbListener);
        
        // Return a lambda wrapper that implements ListenerRegistration
        return () -> ref.removeEventListener(rtdbListener);
    }

    public static void leaveRoom(String roomId, SimpleCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        DocumentReference roomRef = db.collection("rooms").document(roomId);

        batch.update(roomRef, "activeUsers", FieldValue.arrayRemove(user.getUid()));

        batch.commit().addOnSuccessListener(aVoid -> {
            // Clean up RTDB and local state
            FirebaseDatabase.getInstance()
                    .getReference("presence/" + roomId + "/" + user.getUid())
                    .removeValue();
            cleanupRegistered.remove(roomId + "_" + user.getUid());
            
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    public static void endSession(String roomId, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms").document(roomId)
                .update("status", "ended")
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    private static String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 4) {
            int index = (int) (rnd.nextFloat() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }

    public static ListenerRegistration listenToQueue(String roomId, QueueListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("rooms").document(roomId).collection("queue")
                .whereEqualTo("status", "queued")
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("addedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen to queue failed", error);
                        return;
                    }
                    if (value != null) {
                        try {
                            List<Song> queuedSongs = new ArrayList<>();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                try {
                                    Song s = doc.toObject(Song.class);
                                    if (s != null) {
                                        s.setSongId(doc.getId());
                                        queuedSongs.add(s);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Mapping error for song " + doc.getId() + ": " + e.getMessage());
                                    Log.d(TAG, "Raw data: " + doc.getData());
                                }
                            }
                            listener.onUpdate(queuedSongs);
                        } catch (Exception e) {
                            Log.e(TAG, "Queue processing error", e);
                        }
                    }
                });
    }

    public static ListenerRegistration listenToHistory(String roomId, QueueListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("rooms").document(roomId).collection("queue")
                .whereEqualTo("status", "played")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen to history failed", error);
                        return;
                    }
                    if (value != null) {
                        try {
                            List<Song> played = new ArrayList<>();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                try {
                                    Song s = doc.toObject(Song.class);
                                    if (s != null) {
                                        s.setSongId(doc.getId());
                                        played.add(s);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "History mapping error", e);
                                }
                            }
                            Collections.sort(played, (a, b) -> {
                                if (a.getPlayedAt() == null) return 1;
                                if (b.getPlayedAt() == null) return -1;
                                return b.getPlayedAt().compareTo(a.getPlayedAt());
                            });
                            listener.onUpdate(played);
                        } catch (Exception e) {
                            Log.e(TAG, "History processing error", e);
                        }
                    }
                });
    }

    public static void addSong(String roomId, Song song, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference roomRef = db.collection("rooms").document(roomId);
        CollectionReference queueRef = roomRef.collection("queue");
        DocumentReference newSongRef = queueRef.document(); // pre-generate ID before transaction

        db.runTransaction(transaction -> {
            DocumentSnapshot roomSnap = transaction.get(roomRef);
            long maxPos = roomSnap.getLong("maxQueuePosition") != null
                    ? roomSnap.getLong("maxQueuePosition") : -1L;
            long newPosition = maxPos + 1;

            Map<String, Object> songData = new HashMap<>();
            songData.put("songId", newSongRef.getId());
            songData.put("youtubeVideoId", song.getYoutubeVideoId());
            songData.put("title", song.getTitle());
            songData.put("artist", song.getArtist());
            songData.put("thumbnailUrl", song.getThumbnailUrl());
            songData.put("addedByUid", song.getAddedByUid());
            songData.put("addedByName", song.getAddedByName());
            songData.put("score", 0L);
            songData.put("upvotes", 0L);
            songData.put("downvotes", 0L);
            songData.put("position", newPosition);
            songData.put("status", "queued");
            songData.put("addedAt", FieldValue.serverTimestamp());

            transaction.set(newSongRef, songData);
            transaction.update(roomRef, "maxQueuePosition", newPosition);
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    public static void updateRoomSettings(String roomId, boolean sfwMode, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms").document(roomId)
                .update("sfwMode", sfwMode)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void popNextSong(String roomId, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference queueRef = db.collection("rooms").document(roomId).collection("queue");
        DocumentReference roomRef = db.collection("rooms").document(roomId);

        queueRef.whereEqualTo("status", "queued").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        DocumentSnapshot selectedDoc = docs.get(0);
                        // Find lowest position manually to avoid index requirement
                        for (DocumentSnapshot d : docs) {
                            Long p = d.getLong("position");
                            Long nextP = selectedDoc.getLong("position");
                            if (p != null && nextP != null && p < nextP) {
                                selectedDoc = d;
                            }
                        }
                        
                        final DocumentSnapshot nextSongDoc = selectedDoc;
                        try {
                            Song nextSong = nextSongDoc.toObject(Song.class);
                            if (nextSong != null) nextSong.setSongId(nextSongDoc.getId());
                            
                            db.runTransaction(transaction -> {
                                transaction.update(roomRef, "nowPlaying", nextSong);
                                transaction.update(nextSongDoc.getReference(), "status", "played");
                                return null;
                            }).addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess();
                            }).addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                        } catch (Exception e) {
                            if (callback != null) callback.onFailure(e.getMessage());
                        }
                    } else {
                        roomRef.update("nowPlaying", null)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    }
                });
    }

    public static void reorderAndPopNextSong(String roomId, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference roomRef = db.collection("rooms").document(roomId);
        CollectionReference queueRef = roomRef.collection("queue");

        queueRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                List<Song> songs = new ArrayList<>();
                for (DocumentSnapshot d : docs) {
                    try {
                        if ("queued".equals(d.getString("status"))) {
                            Song s = d.toObject(Song.class);
                            if (s != null) {
                                s.setSongId(d.getId());
                                songs.add(s);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Reorder deserialization error", e);
                    }
                }

                if (songs.isEmpty()) {
                    roomRef.update("nowPlaying", null);
                    if (callback != null) callback.onSuccess();
                    return;
                }

                Collections.sort(songs, (s1, s2) -> {
                    if (!s1.getScore().equals(s2.getScore())) {
                        return Long.compare(s2.getScore(), s1.getScore());
                    }
                    if (s1.getAddedAt() != null && s2.getAddedAt() != null) {
                        return s1.getAddedAt().compareTo(s2.getAddedAt());
                    }
                    return 0;
                });

                Song nextToPlay = songs.get(0);

                WriteBatch batch = db.batch();
                Map<String, Object> playedUpdate = new HashMap<>();
                playedUpdate.put("status", "played");
                playedUpdate.put("playedAt", FieldValue.serverTimestamp());
                batch.update(roomRef, "nowPlaying", nextToPlay);
                batch.update(queueRef.document(nextToPlay.getSongId()), playedUpdate);

                long pos = 0;
                for (Song s : songs) {
                    if (!s.getSongId().equals(nextToPlay.getSongId())) {
                        batch.update(queueRef.document(s.getSongId()), "position", pos++);
                    }
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                }).addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
            } else {
                roomRef.update("nowPlaying", null);
                if (callback != null) callback.onSuccess();
            }
        });
    }

    public static ListenerRegistration listenToLeaderboard(UserListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.collection("users")
                .orderBy("reputation", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        try {
                            List<User> users = value.toObjects(User.class);
                            listener.onUpdate(users);
                        } catch (Exception e) {
                            Log.e(TAG, "Leaderboard deserialization error", e);
                        }
                    }
                });
    }

    public static void voteSong(String roomId, String userId, String songId, boolean isUpvote, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.runTransaction(transaction -> {
            DocumentReference songRef = db.collection("rooms").document(roomId).collection("queue").document(songId);
            DocumentReference voteRef = db.collection("rooms").document(roomId).collection("votes").document(userId + "_" + songId);

            DocumentSnapshot voteDoc = transaction.get(voteRef);
            DocumentSnapshot songDoc = transaction.get(songRef);

            if (!songDoc.exists()) return null;

            if (voteDoc.exists()) {
                throw new RuntimeException("Already voted");
            }

            transaction.update(songRef,
                "upvotes", FieldValue.increment(isUpvote ? 1L : 0L),
                "downvotes", FieldValue.increment(isUpvote ? 0L : 1L),
                "score", FieldValue.increment(isUpvote ? 1L : -1L)
            );

            Map<String, Object> voteData = new HashMap<>();
            voteData.put("type", isUpvote ? "up" : "down");
            voteData.put("value", isUpvote ? 10L : -5L);
            voteData.put("userId", userId);
            voteData.put("songId", songId);
            voteData.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(voteRef, voteData);

            return null;
        }).addOnSuccessListener(result -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    public static void updatePlaybackState(String roomId, boolean isPlaying, long position, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("playing", isPlaying);
        updates.put("playbackPosition", position);

        db.collection("rooms").document(roomId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void updatePlaybackStateIntentional(String roomId, boolean isPlaying, SimpleCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("playing", isPlaying);
        updates.put("playbackVersion", FieldValue.increment(1));

        db.collection("rooms").document(roomId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public static void endSessionWithSummary(String roomId, SummaryCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference roomRef = db.collection("rooms").document(roomId);
        CollectionReference queueRef = roomRef.collection("queue");

        queueRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            try {
                List<Song> allSongs = queryDocumentSnapshots.toObjects(Song.class);
                if (allSongs.isEmpty()) {
                    roomRef.update("status", "ended")
                            .addOnSuccessListener(aVoid -> callback.onSuccess("None", "None"))
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    return;
                }

                Song crowdFav = null;
                Map<String, Long> repDeltas = new HashMap<>();

                for (Song s : allSongs) {
                    if (crowdFav == null || s.getUpvotes() > crowdFav.getUpvotes()) {
                        crowdFav = s;
                    }
                    String uid = s.getAddedByUid();
                    if (uid != null) {
                        long participationBonus = 5L;
                        long delta = (s.getUpvotes() * 10L) - (s.getDownvotes() * 5L) + participationBonus;
                        repDeltas.put(uid, repDeltas.getOrDefault(uid, 0L) + delta);
                    }
                }

                String topJammerUid = null;
                long maxPoints = Long.MIN_VALUE;
                for (Map.Entry<String, Long> entry : repDeltas.entrySet()) {
                    if (entry.getValue() > maxPoints) {
                        maxPoints = entry.getValue();
                        topJammerUid = entry.getKey();
                    }
                }

                final String favName = crowdFav != null ? crowdFav.getTitle() : "None";
                final Map<String, Long> finalRepDeltas = repDeltas;
                final String jammerUid = topJammerUid;

                // Fetch jammer display name, then commit status + all reputation deltas atomically
                if (jammerUid != null) {
                    db.collection("users").document(jammerUid).get()
                            .addOnSuccessListener(userDoc -> {
                                String jammerName = userDoc.getString("displayName");
                                commitSessionEnd(db, roomRef, finalRepDeltas, favName,
                                        jammerName != null ? jammerName : "Unknown", callback);
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                } else {
                    commitSessionEnd(db, roomRef, finalRepDeltas, favName, "None", callback);
                }
            } catch (Exception e) {
                callback.onFailure("Summary calculation failed: " + e.getMessage());
            }
        }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private static void commitSessionEnd(FirebaseFirestore db, DocumentReference roomRef,
            Map<String, Long> repDeltas, String favName, String jammerName, SummaryCallback callback) {
        WriteBatch batch = db.batch();
        batch.update(roomRef, "status", "ended");
        for (Map.Entry<String, Long> entry : repDeltas.entrySet()) {
            DocumentReference userRef = db.collection("users").document(entry.getKey());
            batch.update(userRef, "reputation", FieldValue.increment(entry.getValue()));
        }
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(favName, jammerName))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}