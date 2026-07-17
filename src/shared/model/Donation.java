package shared.model;

import java.io.Serializable;

public class Donation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String username;
    private long amount;
    private long timestamp;

    public Donation(String userId, String username, long amount) {
        this.userId = userId;
        this.username = username;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public long getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }
}