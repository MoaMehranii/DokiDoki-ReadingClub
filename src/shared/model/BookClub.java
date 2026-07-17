package shared.model;

import java.io.Serializable;
import java.util.*;

public class BookClub implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String ownerId;
    private final Set<String> memberIds;


    private Map<String, String> activeFundraisers = new HashMap<>();

    public BookClub(String name, String ownerId) {
        if(name==null || name.isBlank()){
            throw new IllegalArgumentException("Club name cannot be empty.");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.ownerId = ownerId;
        this.memberIds = new HashSet<>();
        this.memberIds.add(ownerId);
    }

    public synchronized boolean addMember(String userId) {
        return memberIds.add(userId);
    }

    public synchronized boolean removeMember(String userId) {

        if (userId.equals(ownerId)) {
            return false;
        }
        return memberIds.remove(userId);
    }

    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }


    public String getId() { return id; }
    public String getName() { return name; }
    public String getOwnerId() { return ownerId; }
    public Set<String> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public synchronized String getActiveFundraiserId(String bookId) {
        return activeFundraisers.get(bookId);
    }

    public synchronized void setActiveFundraiserId(String bookId, String fundraiserId) {
        activeFundraisers.put(bookId, fundraiserId);
    }
    public synchronized void removeActiveFundraiser(String bookId) {
        activeFundraisers.remove(bookId);
    }
    public boolean isOwner(String userId){
        return ownerId.equals(userId);
    }
    public int getMemberCount(){
        return memberIds.size();
    }
}