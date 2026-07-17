package server;

import server.storage.BackupService;
import server.storage.StateManager;
import shared.model.Book;
import shared.util.JsonUtil;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerMain {
    private static final int PORT = 8080;
    private static final String BOOKS_DATASET_FILE = "books.json";

    public static void main(String[] args) {
        System.out.println("  DOKI DOKI READING CLUB SERVER STARTED  ");


        BackupService.getInstance().loadBackup();


        loadBooksDataset();


        BackupService.getInstance().start();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[System Shutdown Alert] Initiating graceful state preservation...");
            BackupService.getInstance().stop();
            System.out.println("[System Shutdown Alert] Final checkpoint persistent backup completed.");
        }, "Shutdown-Hook-Thread"));


        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[TCP Engine] Listening on Port: " + PORT);
            System.out.println("[TCP Engine] Multi-threading controller online. Waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Connection Manager] Accepted incoming connection from: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler, "Client-" + clientSocket.getInetAddress()).start();
            }
        } catch (IOException e) {
            System.err.println("[TCP Engine Critical Error]: " + e.getMessage());
        }
    }

    private static void loadBooksDataset() {
        File file = new File(BOOKS_DATASET_FILE);
        if (!file.exists()) {
            System.out.println("[ServerMain] Books dataset '" + BOOKS_DATASET_FILE + "' not found. Generating defaults.");
            generateDefaultBooksDataset();
        }

        try {
            String content = Files.readString(Paths.get(BOOKS_DATASET_FILE));
            Book[] loadedBooks = JsonUtil.fromJson(content, Book[].class);
            if (loadedBooks != null) {
                for (Book b : loadedBooks) {
                    StateManager.getInstance().getBooks().put(b.getId(), b);
                }
                System.out.println("[ServerMain] Successfully imported " + loadedBooks.length + " books from JSON database.");
            }
        } catch (Exception e) {
            System.err.println("[ServerMain Error] Loading books.json file failed: " + e.getMessage());
        }
    }

    private static void generateDefaultBooksDataset() {
        Book[] defaults = new Book[] {
                new Book("1", "Clean Code", "Robert C. Martin", 464, "Software Development", 2008, 150),
                new Book("2", "Design Patterns", "Erich Gamma", 395, "Computer Science", 1994, 180),
                new Book("FAUST_001", "Faust", "Goethe", 500, "Classic Tragedy Drama", 1808, 200)
        };
        try {
            String json = JsonUtil.toJson(defaults);
            Files.writeString(Paths.get(BOOKS_DATASET_FILE), json);
            System.out.println("[ServerMain] Created default JSON dataset template: " + BOOKS_DATASET_FILE);
        } catch (Exception e) {
            System.err.println("[ServerMain Exception] Failed to write fallback books dataset file: " + e.getMessage());
        }
    }
}