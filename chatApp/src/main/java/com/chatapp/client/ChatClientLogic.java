package com.chatapp.client;

import com.chatapp.logic.ChatLogic;
import com.chatapp.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Network and chat protocol for the client. No JavaFX code.
 */
public class ChatClientLogic {

    private final ChatLogic logic = new ChatLogic();
    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private Thread listenerThread;
    private volatile boolean connected;

    private Consumer<String> onSystemMessage;
    private Consumer<Message> onChatMessage;
    private Consumer<String> onError;

    public ChatClientLogic(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnSystemMessage(Consumer<String> onSystemMessage) {
        this.onSystemMessage = onSystemMessage;
    }

    public void setOnChatMessage(Consumer<Message> onChatMessage) {
        this.onChatMessage = onChatMessage;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    public boolean connect(String username) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        connected = true;

        listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();

        out.println(logic.buildLogin(username));
        return true;
    }

    public void sendMessage(String text) {
        if (connected && out != null) {
            out.println(logic.buildChat(text));
        }
    }

    public void requestHistory() {
        if (connected && out != null) {
            out.println(logic.buildHistoryRequest());
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void listen() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while (connected && (line = in.readLine()) != null) {
                handleIncoming(line);
            }
        } catch (IOException e) {
            if (connected) {
                notifyError("Connection lost");
            }
        }
    }

    private void handleIncoming(String line) {
        ChatLogic.ParsedCommand cmd = logic.parseIncoming(line);

        switch (cmd.type()) {
            case MSG -> {
                Message msg = Message.decode(cmd.payload());
                if (msg != null && onChatMessage != null) {
                    onChatMessage.accept(msg);
                }
            }
            case OK -> {
                if (cmd.payload().startsWith("HISTORY")) {
                    String body = cmd.payload().substring("HISTORY".length()).trim();
                    if (!body.isEmpty()) {
                        for (String encoded : body.split("\n")) {
                            Message msg = Message.decode(encoded.trim());
                            if (msg != null && onChatMessage != null) {
                                onChatMessage.accept(msg);
                            }
                        }
                    }
                } else if (onSystemMessage != null) {
                    onSystemMessage.accept(cmd.payload());
                }
            }
            case JOIN, LEAVE -> {
                if (onSystemMessage != null) {
                    onSystemMessage.accept(cmd.type() + ": " + cmd.payload());
                }
            }
            case ERR -> notifyError(cmd.payload());
            default -> { }
        }
    }

    private void notifyError(String msg) {
        if (onError != null) {
            onError.accept(msg);
        }
    }
}
