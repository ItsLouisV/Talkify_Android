package com.example.talkify.models;

public class UserProfile {
    private String user_id;      // user_id (UUID)
    private String email;        // email
    private String user_name;    // user_name
    private String avatar_url;   // avatar_url
    private String last_seen;    // timestamp
    private String created_at;   // timestamp
    private String dob;          // date of birth (nullable)
    private String gender;       // male, female, other, prefer_not_to_say
    private String full_name;    // full_name
    private String bio;          // bio (nullable)

    public UserProfile() {}

    public UserProfile(String user_id, String email, String user_name, String avatar_url,
                       String last_seen, String created_at, String dob,
                       String gender, String full_name, String bio) {
        this.user_id = user_id;
        this.email = email;
        this.user_name = user_name;
        this.avatar_url = avatar_url;
        this.last_seen = last_seen;
        this.created_at = created_at;
        this.dob = dob;
        this.gender = gender;
        this.full_name = full_name;
        this.bio = bio;
    }

    // Getters
    public String getUserId() {
        return user_id;
    }

    public String getEmail() {
        return email;
    }

    public String getUserName() {
        return user_name;
    }

    public String getAvatarUrl() {
        return avatar_url;
    }

    public String getLastSeen() {
        return last_seen;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public String getDob() {
        return dob;
    }

    public String getGender() {
        return gender;
    }

    public String getFullName() {
        return full_name;
    }

    public String getBio() {
        return bio;
    }

    // Setters
    public void setUserId(String user_id) {
        this.user_id = user_id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserName(String user_name) {
        this.user_name = user_name;
    }

    public void setAvatarUrl(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public void setLastSeen(String last_seen) {
        this.last_seen = last_seen;
    }

    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setFullName(String full_name) {
        this.full_name = full_name;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
