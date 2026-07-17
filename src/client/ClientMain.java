package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connecting to the server at " + HOST + ":" + PORT + "...");

            ClientSession session = new ClientSession(socket, in, out);
            CommandParser parser = new CommandParser(session);
            ServerListener listener = new ServerListener(session);


            Thread listenerThread = new Thread(listener, "ServerListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("Successfully connected! Type your commands below.");


            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }

                String line = scanner.nextLine();
                if (line.trim().equalsIgnoreCase("quit")) {
                    System.out.println("Terminating client...");
                    break;
                }

                parser.parseAndExecute(line);


                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }

        } catch (Exception e) {
            System.err.println("Fatal client error or disconnected: " + e.getMessage());
        }
    }
}