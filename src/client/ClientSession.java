package client;

import shared.util.JsonUtil;
import shared.network.Request;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    private String sessionToken;
    private String currentUsername;
    private String currentClubId;

    public ClientSession(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    public void sendRequest(Request request) {
        if (isLoggedIn()) {
            request.setToken(sessionToken);
        }


        if (isInClubMode() && currentClubId != null) {
            request.addArgument("clubId", currentClubId);
        }

        try {
            String jsonPayload = JsonUtil.toJson(request);
            out.println(jsonPayload);
        } catch (Exception e) {
            System.err.println("Error serializing or sending request: " + e.getMessage());
        }
    }

    public void login(String username, String token) {
        this.currentUsername = username;
        this.sessionToken = token;
    }

    public void logout() {
        this.currentUsername = null;
        clearToken();
        leaveClub();
    }

    public void setToken(String token) {
        this.sessionToken = token;
    }

    public void clearToken() {
        this.sessionToken = null;
    }

    public boolean isLoggedIn() {
        return sessionToken != null && !sessionToken.isEmpty();
    }

    public void enterClub(String clubId) {
        this.currentClubId = clubId;
    }

    public void leaveClub() {
        this.currentClubId = null;
    }

    public boolean isInClubMode() {
        return currentClubId != null;
    }

    public BufferedReader getInputStream() {
        return in;
    }
    public Socket getSocket() {
        return socket;
    }
    public String getToken() {
        return sessionToken;
    }
    public String getCurrentUsername() {
        return currentUsername;
    }
    public String getCurrentClubId() {
        return currentClubId;
    }

}