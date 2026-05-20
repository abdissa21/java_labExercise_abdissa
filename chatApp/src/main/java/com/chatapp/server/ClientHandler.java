package com.chatapp.server;

import com.chatapp.logic.ChatLogic;
import com.chatapp.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final ChatServer server;
    private final ChatLogic logic = new ChatLogic();

    private PrintWriter out;
    private String username;
    private boolean loggedIn;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void sendLine(String line) {
        if (out != null) {
            out.println(line);
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            onDisconnect();
        }
    }

    private void handleCommand(String line) {
        ChatLogic.ParsedCommand cmd = logic.parseIncoming(line);

        switch (cmd.type()) {
            case LOGIN -> handleLogin(cmd.payload());
            case CHAT -> handleChat(cmd.payload());
            case HISTORY -> handleHistory();
            case INVALID -> sendLine(logic.encodeError(cmd.error()));
            default -> sendLine(logic.encodeError("Not allowed before login"));
        }
    }

    private void handleLogin(String name) {
        if (loggedIn) {
            sendLine(logic.encodeError("Already logged in"));
            return;
        }
        username = name;
        loggedIn = true;
        sendLine(logic.encodeOk("Welcome " + username));
        server.broadcast(logic.encodeJoin(username), this);
        handleHistory();
    }

    private void handleChat(String text) {
        if (!loggedIn) {
            sendLine(logic.encodeError("Login first"));
            return;
        }
        server.getDatabase().saveMessage(username, text);
        Message message = new Message(username, text, LocalDateTime.now());
        server.broadcast(logic.encodeBroadcast(message));
    }

    private void handleHistory() {
        if (!loggedIn) {
            sendLine(logic.encodeError("Login first"));
            return;
        }
        var messages = server.getDatabase().getAllMessages();
        sendLine(logic.encodeHistory(messages));
    }

    private void onDisconnect() {
        if (loggedIn) {
            server.broadcast(logic.encodeLeave(username));
        }
        server.removeClient(this);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
