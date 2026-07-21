package com.novadb.security;

import com.novadb.exception.NovaDbException;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    public enum Role {
        ADMIN,
        READ_ONLY
    }

    private final Map<String, User> users = new HashMap<>();

    public UserManager() {
        // Default admin user
        users.put("admin", new User("admin", "admin123", Role.ADMIN));
        users.put("guest", new User("guest", "guest", Role.READ_ONLY));
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user != null && user.password().equals(password)) {
            return user;
        }
        throw new NovaDbException("Invalid username or password");
    }

    public void requireAdmin(User user) {
        if (user == null || user.role() != Role.ADMIN) {
            throw new NovaDbException("Permission denied. Admin role required.");
        }
    }

    public record User(String username, String password, Role role) {}
}
