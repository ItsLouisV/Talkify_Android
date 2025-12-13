package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("user_id")
    private String userId;      // user_id (UUID)

    @SerializedName("email")
    private String email;        // email

    @SerializedName("full_name")
    private String fullName;     // full_name

    @SerializedName("user_name")
    private String userName;     // user_name

    @SerializedName("avatar_url")
    private String avatarUrl;    // avatar_url

    @SerializedName("bio")
    private String bio;          // bio (nullable)


    @SerializedName("last_seen")
    private String lastSeen;     // timestamp

    @SerializedName("created_at")
    private String createdAt;    // timestamp

    @SerializedName("dob")
    private String dob;          // date of birth

    @SerializedName("gender")
    private String gender;       // gender


    // --- Trường dùng cho mục đích quan hệ (từ model User ban đầu) ---

    @SerializedName("request_status")
    private String requestStatus; // Sẽ nhận giá trị "new", "sent", v.v.


    // --- Getters và Setters ---

    // Getters
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getUserName() { return userName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public String getLastSeen() { return lastSeen; }
    public String getCreatedAt() { return createdAt; }
    public String getDob() { return dob; }
    public String getGender() { return gender; }
    public String getRequestStatus() { return requestStatus; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setDob(String dob) { this.dob = dob; }
    public void setGender(String gender) { this.gender = gender; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
}