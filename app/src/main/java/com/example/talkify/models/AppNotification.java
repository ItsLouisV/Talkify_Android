package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;
import org.json.JSONObject;

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

    @SerializedName("conversation_id")
    private String conversationId;


    // --- Constructors ---
    public AppNotification() {}


    // --- Setter (Cần thiết cho Gson/Realtime) ---

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }


    // --- Getters ---

    public String getNotificationId() { return notificationId; }
    public String getUserId() { return userId; }
    public String getActorId() { return actorId; }
    public String getType() { return type; }
    public String getContent() { return content; }
    public boolean isRead() { return isRead; }
    public User getActor() { return actor; }
    public String getCreatedAt() { return createdAt; }
    public String getConversationId() { return conversationId; }


    /**
     * Tạo đối tượng AppNotification từ Realtime JSONObject
     * Dùng khi nhận Realtime event (INSERT) mà không có dữ liệu JOIN (actor)
     */
    public static AppNotification fromRealtimeRecord(JSONObject json) {
        AppNotification notif = new AppNotification();

        // Lấy các trường cơ bản từ payload Realtime
        notif.setNotificationId(json.optString("notification_id"));
        notif.setUserId(json.optString("user_id"));
        notif.setActorId(json.optString("actor_id"));
        notif.setType(json.optString("type"));
        notif.setContent(json.optString("content"));
        notif.setConversationId(json.optString("conversation_id"));
        notif.setCreatedAt(json.optString("created_at"));

        // Mặc định: Coi như chưa đọc (Realtime INSERT) và không có thông tin Actor JOIN
        notif.setRead(false);
        notif.setActor(null);

        return notif;
    }
}