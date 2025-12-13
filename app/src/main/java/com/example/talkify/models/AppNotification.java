package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;

public class AppNotification {

    @SerializedName("notification_id")
    private String notificationId;

    @SerializedName("user_id")
    private String userId; // Người nhận

    @SerializedName("actor_id")
    private String actorId; // Người gửi

    @SerializedName("type")
    private String type; // "friend_request", "friend_accepted", v.v.

    @SerializedName("content")
    private String content;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    // Join bảng Users
    @SerializedName("actor")
    private User actor;

    // THÊM TRƯỜNG NÀY
    @SerializedName("conversation_id")
    private String conversationId;

    // Getter
    public String getConversationId() {
        return conversationId;
    }


    // --- Getters ---

    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getActorId() { return actorId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public boolean isRead() { return isRead; }
    public User getActor() { return actor; }

    public String getCreatedAt() {
        return createdAt;
    }

}