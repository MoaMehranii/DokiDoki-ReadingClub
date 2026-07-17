package client;

import shared.network.Notification;
import shared.network.Response;
import shared.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public class ServerListener implements Runnable {

    private final ClientSession session;

    public ServerListener(ClientSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        BufferedReader in = session.getInputStream();

        try {
            String json;
            while ((json = in.readLine()) != null) {
                processIncomingMessage(json);
            }

            System.out.println("\n[SYSTEM] Connection closed by server.");

        } catch (IOException e) {
            System.out.println("\n[SYSTEM] Disconnected from server.");
        }
    }

    private void processIncomingMessage(String json) {
        try {


            if (json.contains("\"type\"") && json.contains("\"message\"")) {

                Notification notification =
                        JsonUtil.fromJson(json, Notification.class);

                if (notification != null) {
                    System.out.println("\n[NOTIFICATION] "
                            + notification.getMessage());
                }

            } else {

                Response response =
                        JsonUtil.fromJson(json, Response.class);

                if (response != null) {
                    handleResponse(response);
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to parse server message.");
        }

        System.out.print("> ");
    }

    @SuppressWarnings("unchecked")
    private void handleResponse(Response response) {

        if (response.isSuccess()) {

            System.out.println("\n[SUCCESS] " + response.getMessage());

            Map<String, Object> data = response.getData();

            if (data != null) {


                if (data.containsKey("token")) {
                    session.setToken((String) data.get("token"));
                }


                if (data.containsKey("club")) {
                    Object clubObj = data.get("club");

                    if (clubObj instanceof Map) {
                        Map<String, Object> club =
                                (Map<String, Object>) clubObj;

                        Object clubId = club.get("id");

                        if (clubId instanceof String) {
                            session.enterClub((String) clubId);
                        }
                    }
                }


                if (!data.isEmpty()) {
                    System.out.println(data);
                }
            }

        } else {

            System.out.println("\n[SERVER ERROR] "
                    + response.getMessage());
        }
    }
}