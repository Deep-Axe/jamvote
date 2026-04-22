package com.jamvote.app.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class Room {
    private String roomId;
    private String roomCode;
    private String roomName;
    private String hostUid;
    private String status;
    private boolean sfwMode;
    private List<String> activeUsers;
    private Timestamp createdAt;
    private Song nowPlaying;
    private boolean playing; // Task 12: Sync play/pause
    private long playbackPosition; // Task 12: Sync seeking (in seconds)

    public Room() {}

    // Getters and Setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getHostUid() { return hostUid; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSfwMode() { return sfwMode; }
    public void setSfwMode(boolean sfwMode) { this.sfwMode = sfwMode; }
    public List<String> getActiveUsers() { return activeUsers; }
    public void setActiveUsers(List<String> activeUsers) { this.activeUsers = activeUsers; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Song getNowPlaying() { return nowPlaying; }
    public void setNowPlaying(Song nowPlaying) { this.nowPlaying = nowPlaying; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    public long getPlaybackPosition() { return playbackPosition; }
    public void setPlaybackPosition(long playbackPosition) { this.playbackPosition = playbackPosition; }
}