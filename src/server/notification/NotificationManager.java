package server.notification;

import shared.network.Notification;
import shared.util.JsonUtil;

import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationManager {
    private static final NotificationManager instance = new NotificationManager();

    private final Map<String, PrintWriter> tcpClients = new ConcurrentHashMap<>();
    private final Map<String, UdpEndpoint> udpClients = new ConcurrentHashMap<>();
    private DatagramSocket udpSocket;

    private NotificationManager() {
        try {
            this.udpSocket = new DatagramSocket();
        } catch (Exception e) {
            System.err.println("Failed to initialize UDP socket for notifications: " + e.getMessage());
        }
    }

    public static NotificationManager getInstance() {
        return instance;
    }

    public void registerTcpClient(String userId, PrintWriter out) {
        tcpClients.put(userId, out);
    }

    public void unregisterTcpClient(String userId) {
        tcpClients.remove(userId);
    }

    public void registerUdpClient(String userId, String ip, int port) {
        udpClients.put(userId, new UdpEndpoint(ip, port));
        System.out.println("Registered UDP Notification target for " + userId + " at " + ip + ":" + port);
    }

    public void unregisterUdpClient(String userId) {
        udpClients.remove(userId);
    }

    public void sendNotification(String userId, String type, String message) {
        Notification notif = new Notification(type, message);
        String jsonPayload = JsonUtil.toJson(notif);

        boolean sentUdp = false;
        UdpEndpoint udpEndpoint = udpClients.get(userId);
        if (udpEndpoint != null && udpSocket != null) {
            try {
                byte[] data = jsonPayload.getBytes(StandardCharsets.UTF_8);
                InetAddress address = InetAddress.getByName(udpEndpoint.ip);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, udpEndpoint.port);
                udpSocket.send(packet);
                sentUdp = true;
            } catch (Exception e) {
                System.err.println("Failed sending UDP packet: " + e.getMessage());
            }
        }


    }

    public void broadcastToClub(Set<String> memberIds, String type, String message) {
        for (String memberId : memberIds) {
            sendNotification(memberId, type, message);
        }
    }

    public static class UdpEndpoint {
        public final String ip;
        public final int port;

        public UdpEndpoint(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }
}