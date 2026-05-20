package com.example.notepad;

import javafx.application.Application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;

public class NotepadApp extends Application {

    public NotepadApp() {
    }

    private TextArea textArea = new TextArea();
    private File currentFile = null;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Notepad");

        BorderPane root = new BorderPane();
        root.setTop(createMenu(stage));
        root.setCenter(textArea);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar createMenu(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");

        MenuItem newFile = new MenuItem("New");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem exitApp = new MenuItem("Exit");

        newFile.setOnAction(e -> {
            textArea.clear();
            currentFile = null;
        });

        openFile.setOnAction(e -> openFile(stage));
        saveFile.setOnAction(e -> saveFile(stage));
        exitApp.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(newFile, openFile, saveFile, new SeparatorMenuItem(), exitApp);
        menuBar.getMenus().add(fileMenu);

        return menuBar;
    }

    private void openFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                textArea.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    textArea.appendText(line + "\n");
                }
                currentFile = file;
            } catch (IOException e) {
                showError("Error opening file");
            }
        }
    }

    private void saveFile(Stage stage) {
        try {
            if (currentFile == null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save File");
                currentFile = fileChooser.showSaveDialog(stage);
            }

            if (currentFile != null) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile));
                writer.write(textArea.getText());
                writer.close();
            }
        } catch (IOException e) {
            showError("Error saving file");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        Application.launch(NotepadApp.class, args);
    }
}

