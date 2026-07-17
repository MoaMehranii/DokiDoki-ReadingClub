package server.storage;

import java.io.*;
import java.nio.file.*;

public class BackupService {
    private static final BackupService instance = new BackupService();
    private static final String BACKUP_FILE = "backup.ser";
    private static final String TEMP_FILE = "backup.ser.tmp";
    private Thread backupThread;
    private volatile boolean running = true;

    private BackupService() {}

    public static BackupService getInstance() {
        return instance;
    }

    public void start() {
        backupThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(120000);
                    saveBackup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Backup failed: " + e.getMessage());
                }
            }
        }, "Backup-Service-Thread");
        backupThread.setDaemon(true);
        backupThread.start();
    }

    public void stop() {
        running = false;
        if (backupThread != null) {
            backupThread.interrupt();
        }
        saveBackup();
    }

    public synchronized void saveBackup() {
        StateManager.BackupData data = StateManager.getInstance().getBackupData();
        File tempFile = new File(TEMP_FILE);
        File destFile = new File(BACKUP_FILE);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            oos.writeObject(data);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Error writing temp backup file: " + e.getMessage());
            return;
        }

        try {

            Files.move(tempFile.toPath(), destFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[BackupService] Atomic state backup completed successfully.");
        } catch (IOException e) {
            System.err.println("Failed to atomically rename backup file: " + e.getMessage());
        }
    }

    public synchronized void loadBackup() {
        File destFile = new File(BACKUP_FILE);
        if (!destFile.exists()) {
            System.out.println("[BackupService] No backup file found. Initializing empty state.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(destFile))) {
            StateManager.BackupData data = (StateManager.BackupData) ois.readObject();
            StateManager.getInstance().restoreBackupData(data);
            System.out.println("[BackupService] State successfully loaded from backup.");
        } catch (Exception e) {
            System.err.println("[BackupService] Error restoring backup: " + e.getMessage());
        }
    }
}