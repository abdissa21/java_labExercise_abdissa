package com.chatapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final int id;
    private final String username;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(int id, String username, String content, LocalDateTime timestamp) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Message(String username, String content, LocalDateTime timestamp) {
        this(0, username, content, timestamp);
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String display() {
        return "[" + timestamp.format(FORMAT) + "] " + username + ": " + content;
    }

    /** Wire format: username|content|timestamp */
    public String encode() {
        return username + "|" + content.replace("|", "\\|") + "|" + timestamp.format(FORMAT);
    }

    public static Message decode(String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) {
            return null;
        }
        String user = parts[0];
        String text = parts[1].replace("\\|", "|");
        LocalDateTime time = LocalDateTime.parse(parts[2], FORMAT);
        return new Message(user, text, time);
    }
}
