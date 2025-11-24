package com.example.talkify.models;

import java.util.Date;
import java.util.UUID;

public class Friend {
    private UUID userId;
    private UUID friendId;
    private Date createdAt;

    public Friend() {}

    public Friend(UUID userId, UUID friendId, Date createdAt) {
        this.userId = userId;
        this.friendId = friendId;
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getFriendId() { return friendId; }
    public void setFriendId(UUID friendId) { this.friendId = friendId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
