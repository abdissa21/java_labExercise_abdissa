package com.chatapp.database;

import com.chatapp.model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database access. No UI or networking code here.
 */
public class DatabaseHandler {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DB_URL = "jdbc:sqlite:chat.db";

    public DatabaseHandler() {
        initSchema();
    }

    private void initSchema() {
        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init database", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public synchronized void saveMessage(String username, String content) {
        String sql = "INSERT INTO messages (username, content, created_at) VALUES (?, ?, ?)";
        String now = LocalDateTime.now().format(FORMAT);
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, content);
            ps.setString(3, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public synchronized List<Message> getAllMessages() {
        String sql = "SELECT id, username, content, created_at FROM messages ORDER BY id ASC";
        List<Message> messages = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("content"),
                        LocalDateTime.parse(rs.getString("created_at"), FORMAT)
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load messages", e);
        }
        return messages;
    }
}
