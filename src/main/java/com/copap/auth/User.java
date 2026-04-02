package com.copap.auth;

public class User {

    private final String userId;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final String role;
    private final String passwordAlgo;

    public User(String userId, String username, String passwordHash, String email, String role) {
        this(userId, username, passwordHash, email, role, "sha256");
    }

    public User(String userId, String username, String passwordHash, String email, String role, String passwordAlgo) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role != null ? role : "USER";
        this.passwordAlgo = passwordAlgo != null ? passwordAlgo : "sha256";
    }

    public String getUserId()       { return userId; }
    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail()        { return email; }
    public String getRole()         { return role; }
    public String getPasswordAlgo() { return passwordAlgo; }
    public boolean isAdmin()        { return "ADMIN".equalsIgnoreCase(role); }
}
