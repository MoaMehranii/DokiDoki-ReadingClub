package server.storage;

import shared.model.*;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {
    private static final StateManager instance = new StateManager();

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Book> books = new ConcurrentHashMap<>();
    private final Map<String, BookClub> clubs = new ConcurrentHashMap<>();
    private final Map<String, JoinRequest> joinRequests = new ConcurrentHashMap<>();
    private final Map<String, Fundraiser> fundraisers = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToUserId = new ConcurrentHashMap<>();

    private StateManager() {}

    public static StateManager getInstance() {
        return instance;
    }

    public Map<String, User> getUsers() { return users; }
    public Map<String, Book> getBooks() { return books; }
    public Map<String, BookClub> getClubs() { return clubs; }
    public Map<String, JoinRequest> getJoinRequests() { return joinRequests; }
    public Map<String, Fundraiser> getFundraisers() { return fundraisers; }
    public Map<String, String> getTokenToUserId() { return tokenToUserId; }

    public User getUserByToken(String token) {
        if (token == null) return null;
        String userId = tokenToUserId.get(token);
        if (userId == null) return null;
        return users.get(userId);
    }


    public static class BackupData implements Serializable {
        private static final long serialVersionUID = 1L;
        public Map<String, User> users;
        public Map<String, BookClub> clubs;
        public Map<String, JoinRequest> joinRequests;
        public Map<String, Fundraiser> fundraisers;
    }

    public BackupData getBackupData() {
        BackupData data = new BackupData();
        data.users = new ConcurrentHashMap<>(users);
        data.clubs = new ConcurrentHashMap<>(clubs);
        data.joinRequests = new ConcurrentHashMap<>(joinRequests);
        data.fundraisers = new ConcurrentHashMap<>(fundraisers);
        return data;
    }

    public void restoreBackupData(BackupData data) {
        if (data == null) return;
        if (data.users != null) {
            users.clear();
            users.putAll(data.users);
        }
        if (data.clubs != null) {
            clubs.clear();
            clubs.putAll(data.clubs);
        }
        if (data.joinRequests != null) {
            joinRequests.clear();
            joinRequests.putAll(data.joinRequests);
        }
        if (data.fundraisers != null) {
            fundraisers.clear();
            fundraisers.putAll(data.fundraisers);
        }
    }
}