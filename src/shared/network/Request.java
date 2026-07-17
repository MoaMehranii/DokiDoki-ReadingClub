package shared.network;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String command;
    private String token;
    private Map<String, Object> data;
    public Request() {
        this.data = new HashMap<>();
    }

    public Request(String command, String token, Map<String, Object> data) {
        this.command = command;
        this.token = token;
        this.data = data;
    }
    public Request(String command) {
        this.command = command;
        this.data = new HashMap<>();
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public void addArgument(String key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(key, value);
    }
}