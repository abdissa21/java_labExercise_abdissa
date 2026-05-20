package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.server.ChatServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChatClientApp extends Application {

    private ChatClientLogic clientLogic;
    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Simple Chat");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        TextField serverField = new TextField("localhost");
        serverField.setPromptText("Server host");
        TextField portField = new TextField(String.valueOf(ChatServer.PORT));
        portField.setPromptText("Port");
        TextField userField = new TextField();
        userField.setPromptText("Username");
        Button connectButton = new Button("Connect");

        messageField = new TextField();
        messageField.setPromptText("Type a message...");
        sendButton = new Button("Send");
        sendButton.setDisable(true);

        statusLabel = new Label("Not connected");

        connectButton.setOnAction(e -> connect(serverField, portField, userField, connectButton));
        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());

        HBox top = new HBox(8, new Label("Host:"), serverField,
                new Label("Port:"), portField,
                new Label("User:"), userField, connectButton);
        top.setPadding(new Insets(8));

        HBox bottom = new HBox(8, messageField, sendButton);
        bottom.setPadding(new Insets(8));
        HBox.setHgrow(messageField, javafx.scene.layout.Priority.ALWAYS);

        VBox center = new VBox(4, chatArea, statusLabel);
        VBox.setVgrow(chatArea, javafx.scene.layout.Priority.ALWAYS);
        center.setPadding(new Insets(0, 8, 0, 8));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(bottom);

        stage.setScene(new Scene(root, 520, 420));
        stage.setOnCloseRequest(e -> {
            if (clientLogic != null) {
                clientLogic.disconnect();
            }
        });
        stage.show();
    }

    private void connect(TextField serverField, TextField portField, TextField userField, Button connectButton) {
        String user = userField.getText().trim();
        if (user.isEmpty()) {
            appendSystem("Enter a username.");
            return;
        }

        try {
            int port = Integer.parseInt(portField.getText().trim());
            clientLogic = new ChatClientLogic(serverField.getText().trim(), port);
            wireCallbacks();

            clientLogic.connect(user);
            connectButton.setDisable(true);
            sendButton.setDisable(false);
            messageField.requestFocus();
            statusLabel.setText("Connected as " + user);
            clientLogic.requestHistory();
        } catch (Exception ex) {
            appendSystem("Could not connect: " + ex.getMessage());
        }
    }

    private void wireCallbacks() {
        clientLogic.setOnChatMessage(msg -> Platform.runLater(() -> appendMessage(msg)));
        clientLogic.setOnSystemMessage(msg -> Platform.runLater(() -> appendSystem(msg)));
        clientLogic.setOnError(msg -> Platform.runLater(() -> appendSystem("Error: " + msg)));
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty() || clientLogic == null) {
            return;
        }
        clientLogic.sendMessage(text);
        messageField.clear();
    }

    private void appendMessage(Message msg) {
        chatArea.appendText(msg.display() + "\n");
    }

    private void appendSystem(String text) {
        chatArea.appendText("--- " + text + " ---\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
