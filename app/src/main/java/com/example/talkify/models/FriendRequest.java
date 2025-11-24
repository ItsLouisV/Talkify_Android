package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;

public class FriendRequest {

    // Thay đổi từ "id" sang "request_id" và kiểu "String"
    @SerializedName("request_id")
    private String requestId;

    @SerializedName("sender_id")
    private String senderId;

    @SerializedName("receiver_id")
    private String receiverId;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    // Thêm trường mới
    @SerializedName("responded_at")
    private String respondedAt;

    // Đối tượng User này vẫn giữ nguyên, dùng để join thông tin người gửi
    @SerializedName("sender")
    private User sender;

    // --- Getters ---

    public String getRequestId() {
        return requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getRespondedAt() {
        return respondedAt;
    }

    public User getSender() {
        return sender;
    }

    // --- Setters ---

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setRespondedAt(String respondedAt) {
        this.respondedAt = respondedAt;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }
}