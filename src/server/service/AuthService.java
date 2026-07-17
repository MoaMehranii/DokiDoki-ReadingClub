package server.service;

import server.storage.StateManager;
import shared.model.User;
import shared.network.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {
    private final StateManager state = StateManager.getInstance();

    public synchronized Response register(String username, String hashedPassword) {
        if (username == null || username.trim().isEmpty() || hashedPassword == null || hashedPassword.trim().isEmpty()) {
            return new Response(false, "Invalid credentials configuration.", null);
        }

        for (User user : state.getUsers().values()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return new Response(false, "Username already exists.", null);
            }
        }

        User newUser = new User(username, hashedPassword);
        state.getUsers().put(newUser.getId(), newUser);
        return new Response(true, "Registration successful.", null);
    }

    public synchronized Response login(String username, String hashedPassword) {
        User targetUser = null;
        for (User user : state.getUsers().values()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                targetUser = user;
                break;
            }
        }

        if (targetUser == null || !targetUser.getHashedPassword().equals(hashedPassword)) {
            return new Response(false, "Invalid username or password.", null);
        }

        synchronized (targetUser) {
            if (targetUser.getSessionToken() != null) {
                state.getTokenToUserId().remove(targetUser.getSessionToken());
            }

            String token = UUID.randomUUID().toString();
            targetUser.setSessionToken(token);
            state.getTokenToUserId().put(token, targetUser.getId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("userId", targetUser.getId());
            responseData.put("balance", targetUser.getBalance());

            return new Response(true, "Login successful.", responseData);
        }
    }

    public Response logout(String token) {
        if (token == null) {
            return new Response(false, "Token cannot be null.", null);
        }

        String userId = state.getTokenToUserId().remove(token);
        if (userId != null) {
            User user = state.getUsers().get(userId);
            if (user != null) {
                synchronized (user) {
                    user.clearSession();
                }
            }
            return new Response(true, "Successfully logged out.", null);
        }
        return new Response(false, "Invalid session token.", null);
    }
}