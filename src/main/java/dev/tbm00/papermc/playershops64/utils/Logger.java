package dev.tbm00.papermc.playershops64.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import dev.tbm00.papermc.playershops64.PlayerShops64;

public class Logger {
    private static PlayerShops64 javaPlugin;

    private static Path editLogPath;
    private static final Object EDIT_LOG_LOCK = new Object();
    private static final DateTimeFormatter EDIT_LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void init(PlayerShops64 javaPlugin) {
        Logger.javaPlugin = javaPlugin;
    }

    public static void logEdit(String... strings) {
        if (strings == null || strings.length == 0) return;

        if (editLogPath == null) {
            for (String s : strings) {
                StaticUtils.log(ChatColor.YELLOW, "[edit-log] " + s);
            }
            return;
        }

        final String ts = LocalDateTime.now(ZoneId.systemDefault()).format(EDIT_LOG_TS);
        final StringBuilder chunk = new StringBuilder();
        for (String s : strings) {
            if (s == null) continue;
            chunk.append('[').append(ts).append("] ").append(s).append(System.lineSeparator());
        }
        if (chunk.length() == 0) return;

        // Do file IO off the main thread
        Bukkit.getScheduler().runTaskAsynchronously(javaPlugin, () -> {
            try {
                byte[] bytes = chunk.toString().getBytes(StandardCharsets.UTF_8);
                synchronized (EDIT_LOG_LOCK) {
                    Files.write(
                        editLogPath,
                        bytes,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                    );
                }
            } catch (IOException e) {
                StaticUtils.log(ChatColor.RED, "Failed to write to edit log: " + e.getMessage());
            }
        });
    }
    public static void setEditLogFile(File file) {
        if (file != null) {
            editLogPath = file.toPath();
        }
    }
}
