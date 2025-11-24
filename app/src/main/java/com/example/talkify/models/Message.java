package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("message_id")
    private String messageId;

    @SerializedName("conversation_id")
    private String conversationId;

    @SerializedName("sender_id")
    private String senderId;

    @SerializedName("content")
    private String content;

    @SerializedName("created_at")
    private String createdAt; // Hoặc dùng Date

    @SerializedName("message_type")
    private String messageType;

    // --- ĐỐI TƯỢNG JOIN QUAN TRỌNG ---
    // Đây là đối tượng User được join từ 'sender_id'
    @SerializedName("sender")
    private User sender;

    // (Constructor rỗng)
    public Message() {}

    // (Getters và Setters)

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
}