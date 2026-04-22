package com.jamvote.app.model;

import com.google.firebase.Timestamp;

public class Song {
    private String songId;
    private String youtubeVideoId;
    private String title;
    private String artist;
    private String thumbnailUrl;
    private boolean isExplicit;
    private String addedByUid;
    private String addedByName;
    private Long upvotes;
    private Long downvotes;
    private Long score;
    private Long position;
    private Timestamp addedAt;
    private Timestamp playedAt;
    private String status; // "queued", "played"

    public Song() {} // Required for Firestore

    public Song(String songId, String youtubeVideoId, String title, String artist) {
        this.songId = songId;
        this.youtubeVideoId = youtubeVideoId;
        this.title = title;
        this.artist = artist;
    }

    // Getters and Setters
    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }
    public String getYoutubeVideoId() { return youtubeVideoId; }
    public void setYoutubeVideoId(String youtubeVideoId) { this.youtubeVideoId = youtubeVideoId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public boolean isExplicit() { return isExplicit; }
    public void setExplicit(boolean explicit) { isExplicit = explicit; }
    public String getAddedByUid() { return addedByUid; }
    public void setAddedByUid(String addedByUid) { this.addedByUid = addedByUid; }
    public String getAddedByName() { return addedByName; }
    public void setAddedByName(String addedByName) { this.addedByName = addedByName; }
    
    public Long getUpvotes() { return upvotes != null ? upvotes : 0L; }
    public void setUpvotes(Long upvotes) { this.upvotes = upvotes; }
    
    public Long getDownvotes() { return downvotes != null ? downvotes : 0L; }
    public void setDownvotes(Long downvotes) { this.downvotes = downvotes; }
    
    public Long getScore() { return score != null ? score : 0L; }
    public void setScore(Long score) { this.score = score; }
    
    public Long getPosition() { return position != null ? position : 0L; }
    public void setPosition(Long position) { this.position = position; }
    
    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }
    public Timestamp getPlayedAt() { return playedAt; }
    public void setPlayedAt(Timestamp playedAt) { this.playedAt = playedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}