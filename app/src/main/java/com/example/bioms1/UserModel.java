package com.example.bioms1;

public class UserModel {
    private String userId;
    private String username;
    private String passwordHash; // In production, use proper hashing
    private String email;
    private boolean isLoggedIn;

    // Constructor
    public UserModel(String userId, String username, String password, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = String.valueOf(password.hashCode()); // Simple hash
        this.email = email;
        this.isLoggedIn = false;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public boolean isLoggedIn() { return isLoggedIn; }
    public void setLoggedIn(boolean loggedIn) { isLoggedIn = loggedIn; }

    public boolean checkPassword(String password) {
        return passwordHash.equals(String.valueOf(password.hashCode()));
    }
}
