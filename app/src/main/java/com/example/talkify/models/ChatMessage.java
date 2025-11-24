package com.example.talkify.models;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChatMessage {
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    private String content;
    private MessageType messageType;
    private String fileUrl;
    private UUID replyTo;
    private boolean isEdited;
    private Date deletedAt;
    private Date createdAt;

    private String senderName;
    private String senderAvatarUrl;
    private List<MessageReaction> reactions;
    private boolean isMine;

    public ChatMessage(UUID messageId, UUID conversationId, UUID senderId,
                       String content, MessageType messageType, String fileUrl,
                       UUID replyTo, boolean isEdited, Date deletedAt, Date createdAt,
                       String senderName, String senderAvatarUrl, List<MessageReaction> reactions,
                       boolean isMine) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.replyTo = replyTo;
        this.isEdited = isEdited;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.senderName = senderName;
        this.senderAvatarUrl = senderAvatarUrl;
        this.reactions = reactions;
        this.isMine = isMine;
    }


    // Getters
    public UUID getMessageId() { return messageId; }
    public UUID getConversationId() { return conversationId; }
    public UUID getSenderId() { return senderId; }
    public String getContent() { return content; }
    public MessageType getMessageType() { return messageType; }
    public String getFileUrl() { return fileUrl; }
    public UUID getReplyTo() { return replyTo; }
    public boolean isEdited() { return isEdited; }
    public Date getDeletedAt() { return deletedAt; }
    public Date getCreatedAt() { return createdAt; }
    public String getSenderName() { return senderName; }
    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public List<MessageReaction> getReactions() { return reactions; }
    public boolean isMine() { return isMine; }

    // Enum
    public enum MessageType {
        TEXT,
        IMAGE,
        VIDEO,
        FILE,
        SYSTEM
    }

    public static class MessageReaction {
        private UUID userId;
        private String reaction;
        private Date createdAt;

        public MessageReaction(UUID userId, String reaction, Date createdAt) {
            this.userId = userId;
            this.reaction = reaction;
            this.createdAt = createdAt;
        }

        public UUID getUserId() { return userId; }
        public String getReaction() { return reaction; }
        public Date getCreatedAt() { return createdAt; }
    }
}
