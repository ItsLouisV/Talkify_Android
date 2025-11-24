package com.example.talkify.models;

public class ChatItem {
    private String conversationId;
    private String name;
    private String avatarUrl;
    private String lastMessage;
    private String lastMessageTime;
    private boolean lastMessageMine;
    private boolean isRead;

    public ChatItem(String conversationId, String name, String avatarUrl,
                    String lastMessage, String lastMessageTime,
                    boolean lastMessageMine, boolean isRead) {
        this.conversationId = conversationId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageMine = lastMessageMine;
        this.isRead = isRead;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public boolean isLastMessageMine() {
        return lastMessageMine;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // Setters (needed by ChatsFragment realtime updater)
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setName(String name) { this.name = name; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public void setLastMessageMine(boolean lastMessageMine) { this.lastMessageMine = lastMessageMine; }

    private long lastMessageTimestamp;

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long ts) { this.lastMessageTimestamp = ts; }

    @Override
    public String toString() {
        return "ChatItem{" +
                "conversationId='" + conversationId + '\'' +
                ", name='" + name + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", lastMessageTime='" + lastMessageTime + '\'' +
                ", lastMessageMine=" + lastMessageMine +
                ", isRead=" + isRead +
                '}';
    }
}
