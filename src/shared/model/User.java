package shared.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String username;
    private String hashedPassword;
    private long balance;

    private transient String sessionToken;
    private final Map<String, LibraryBook> library;

    public User(String username, String hashedPassword) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("cannot have a blank username");

        if (hashedPassword == null || hashedPassword.isBlank())
            throw new IllegalArgumentException("cannot have a blank password");
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.balance = 0;
        this.library = new HashMap<>();
    }

    public String getId() { return id; }
    public boolean ownsBook(String id){
        if (library.containsKey(id))
                return true;
        return false;
    }

    public String getUsername() { return username; }

    public String getHashedPassword() { return hashedPassword; }

    public long getBalance() { return balance; }

    public synchronized void chargeAccount(long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("cannot charge 0 or less.");

        balance += amount;
    }

    public synchronized boolean deductBalance(long amount) {
        if(amount<=0)
            throw new IllegalArgumentException("cannot deduct 0 or less");
        if (amount > 0 && this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }
    public void clearSession() {
        sessionToken = null;
    }
    public Map<String, LibraryBook> getLibrary() {
        return Collections.unmodifiableMap(library);
    }
    public synchronized void addBook(LibraryBook book){
        String Id = book.getBookId();
        library.put(Id , book);
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}