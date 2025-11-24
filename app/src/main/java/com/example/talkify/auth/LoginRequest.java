package com.example.talkify.auth;

public class LoginRequest {
    private String identifier;  // có thể là email hoặc username
    private String password;

    public LoginRequest() {}

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    // Getters
    public String getIdentifier() {
        return identifier;
    }

    public String getPassword() {
        return password;
    }

    // Setters
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
