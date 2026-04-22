package com.jamvote.app.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private int reputation;
    private int totalSongsAdded;
    private int totalUpvotes;
    private int totalSkips;
    private List<String> badges;
    private Timestamp createdAt;

    public User() {}

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    public int getTotalSongsAdded() { return totalSongsAdded; }
    public void setTotalSongsAdded(int totalSongsAdded) { this.totalSongsAdded = totalSongsAdded; }
    public int getTotalUpvotes() { return totalUpvotes; }
    public void setTotalUpvotes(int totalUpvotes) { this.totalUpvotes = totalUpvotes; }
    public int getTotalSkips() { return totalSkips; }
    public void setTotalSkips(int totalSkips) { this.totalSkips = totalSkips; }
    public List<String> getBadges() { return badges; }
    public void setBadges(List<String> badges) { this.badges = badges; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}