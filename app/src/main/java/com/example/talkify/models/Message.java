package com.example.talkify.models;
import org.json.JSONObject;

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
    private String createdAt;

    @SerializedName("message_type")
    private String messageType;

    // --- ĐỐI TƯỢNG JOIN QUAN TRỌNG ---
    // Đây là đối tượng User được join từ 'sender_id'
    @SerializedName("sender")
    private User sender;

    // (Constructor rỗng)
    public Message() {}

    public Message(Message other) {
        this.messageId = other.messageId;
        this.conversationId = other.conversationId;
        this.senderId = other.senderId;
        this.content = other.content;
        this.createdAt = other.createdAt;
        this.messageType = other.messageType;
        this.sender = other.sender;
        this.clientTempId = other.clientTempId;
        this.status = other.status;
    }


    @SerializedName("client_temp_id")
    private String clientTempId;

    public String getClientTempId() {
        return clientTempId;
    }

    public void setClientTempId(String clientTempId) {
        this.clientTempId = clientTempId;
    }

    private long localCreatedAt;

    public long getLocalCreatedAt() {
        return localCreatedAt;
    }

    public void setLocalCreatedAt(long localCreatedAt) {
        this.localCreatedAt = localCreatedAt;
    }

    public enum SendStatus {
        SENDING,   // đang gửi
        SENT,      // server xác nhận
        FAILED     // lỗi
    }

    private SendStatus status = SendStatus.SENT;

    public SendStatus getStatus() {
        return status;
    }

    public void setStatus(SendStatus status) {
        this.status = status;
    }

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



    public static Message fromRealtimeJson(JSONObject json) {
        Message m = new Message();

        m.setMessageId(json.optString("message_id"));
        m.setConversationId(json.optString("conversation_id"));
        m.setSenderId(json.optString("sender_id"));
        m.setContent(json.optString("content"));
        m.setCreatedAt(json.optString("created_at"));
        m.setMessageType(json.optString("message_type"));
        m.setClientTempId(json.optString("client_temp_id", null));

        m.setStatus(SendStatus.SENT);
        m.setSender(null);

        return m;
    }
}
