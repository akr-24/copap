package com.copap.auth;

public class User {

    private final String userId;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final String role; // "USER" or "ADMIN"

    public User(String userId, String username, String passwordHash) {
        this(userId, username, passwordHash, null, "USER");
    }

    public User(String userId, String username, String passwordHash, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role != null ? role : "USER";
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
}