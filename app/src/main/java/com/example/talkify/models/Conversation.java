package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Conversation {

    @SerializedName("conversation_id")
    private String conversationId;

    @SerializedName("type")
    private String type; // "direct" hoặc "group"

    @SerializedName("group_details")
    private GroupDetails groupDetails; // Sẽ null nếu là "direct"

    @SerializedName("conversation_participants")
    private List<ParticipantWrapper> participants;

    // Để hứng dữ liệu từ query: messages:messages(...)
    @SerializedName("messages")
    private List<Message> messages;
    // --------------------------------

    // --- Getters ---
    public String getConversationId() { return conversationId; }
    public String getType() { return type; }
    public GroupDetails getGroupDetails() { return groupDetails; }
    public List<ParticipantWrapper> getParticipants() { return participants; }
    public List<Message> getMessages() { return messages; }

    // --- Helper: Kiểm tra loại chat ---
    public boolean isGroup() {
        return "group".equalsIgnoreCase(type);
    }

    // --- Helper: Lấy tin nhắn cuối cùng (Để hiển thị lên danh sách) ---
    public Message getLastMessage() {
        if (messages != null && !messages.isEmpty()) {
            // Lấy phần tử cuối cùng của mảng (thường là tin mới nhất)
            return messages.get(messages.size() - 1);
        }
        return null;
    }

    // ==================================================================
    // HÀM Tự tính toán Tên hiển thị
    // ==================================================================
    public String getDisplayName(String currentUserId) {
        if (isGroup()) {
            if (groupDetails != null && groupDetails.name != null) {
                return groupDetails.name;
            }
            return "Nhóm chưa đặt tên";
        } else {
            if (participants != null) {
                for (ParticipantWrapper p : participants) {
                    if (!p.getUserId().equals(currentUserId)) {
                        if (p.getUser() != null) {
                            return p.getUser().getFullName();
                        }
                    }
                }
            }
            return "Người dùng";
        }
    }

    // ==================================================================
    //  HÀM Tự tính toán Avatar hiển thị
    // ==================================================================
    public String getDisplayAvatar(String currentUserId) {
        if (isGroup()) {
            if (groupDetails != null) {
                return groupDetails.avatarUrl;
            }
        } else {
            if (participants != null) {
                for (ParticipantWrapper p : participants) {
                    if (!p.getUserId().equals(currentUserId)) {
                        if (p.getUser() != null) {
                            return p.getUser().getAvatarUrl();
                        }
                    }
                }
            }
        }
        return null;
    }

    // --- NESTED CLASSES ---

    public static class GroupDetails {
        @SerializedName("name")
        public String name;

        @SerializedName("avatar_url")
        public String avatarUrl;

        public String getName() { return name; }
        public String getAvatarUrl() { return avatarUrl; }
    }

    public static class ParticipantWrapper {
        @SerializedName("user_id")
        private String userId;

        @SerializedName("role")
        private String role;

        @SerializedName("users")
        private User user;

        public String getUserId() { return userId; }

        // --- Getter cho Role ---
        public String getRole() { return role; }

        public User getUser() { return user; }

    }

    public static class User {
        @SerializedName("full_name")
        private String fullName;

        @SerializedName("username")
        private String username;

        @SerializedName("avatar_url")
        private String avatarUrl;

        public String getFullName() { return fullName; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getUsername() { return username; }
    }
}