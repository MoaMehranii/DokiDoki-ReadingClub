package shared.model;

import java.io.Serializable;
import java.util.UUID;

public class JoinRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String clubId;
    private String userId;
    private String username;

    public JoinRequest(String clubId, String userId, String username) {
        if(clubId==null || clubId.isBlank()){
            throw new IllegalArgumentException("id cannot be black.");
        }
        if(userId==null || userId.isBlank()){
            throw new IllegalArgumentException("id cannot be black.");
        }
        if(username==null || username.isBlank()){
            throw new IllegalArgumentException("username cannot be black.");
        }
        this.id = UUID.randomUUID().toString();
        this.clubId = clubId;
        this.userId = userId;
        this.username = username;
    }

    public String getId() { return id; }
    public String getClubId() { return clubId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}