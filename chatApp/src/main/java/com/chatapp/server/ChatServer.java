package com.chatapp.server;

import com.chatapp.database.DatabaseHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    public static final int PORT = 5000;

    private final DatabaseHandler database = new DatabaseHandler();
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        System.out.println("Chat server starting on port " + PORT + " ...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);
                handler.start();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public DatabaseHandler getDatabase() {
        return database;
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    public void broadcast(String line, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude && client.isLoggedIn()) {
                client.sendLine(line);
            }
        }
    }

    public void broadcast(String line) {
        broadcast(line, null);
    }
}
