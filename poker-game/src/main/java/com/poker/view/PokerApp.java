package com.poker.view;

import com.poker.controller.GameController;
import com.poker.model.Card;
import com.poker.model.GameState;
import com.poker.model.Player;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class PokerApp extends Application {

    private GameController game;

    // Card areas
    private HBox computerCardArea;
    private HBox communityCardArea;
    private HBox humanCardArea;

    // Labels
    private Label potLabel;
    private Label humanChipsLabel;
    private Label computerChipsLabel;
    private Label statusLabel;
    private Label humanBetLabel;
    private Label computerBetLabel;

    // Buttons
    private Button foldBtn;
    private Button checkBtn;
    private Button callBtn;
    private Button raiseBtn;
    private Button newHandBtn;
    private Slider raiseSlider;
    private Label raiseAmountLabel;

    // Log
    private TextArea logArea;

    @Override
    public void start(Stage stage) {
        game = new GameController();
        game.addListener(new GameController.GameListener() {
            @Override public void onStateChanged(GameState state) { Platform.runLater(() -> handleStateChange(state)); }
            @Override public void onMessageLogged(String msg)    { Platform.runLater(() -> appendLog(msg)); }
            @Override public void onPotUpdated(int pot)          { Platform.runLater(() -> potLabel.setText("Pot  $" + pot)); }
            @Override public void onPlayerTurn(Player p)         { Platform.runLater(() -> setActionEnabled(p.isHuman())); }
        });

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1b5e20;");
        root.setPadding(new Insets(15));

        root.setTop(buildTopBar());
        root.setCenter(buildTable());
        root.setBottom(buildControls());
        root.setRight(buildLog());

        Scene scene = new Scene(root, 980, 680);
        stage.setTitle("♠ Texas Hold'em Poker ♠");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        setActionEnabled(false);
    }

    // ── Layout builders ──────────────────────────────────────────────────────

    private HBox buildTopBar() {
        computerChipsLabel = styledLabel("Computer: $1000", 16);
        potLabel = styledLabel("Pot  $0", 20);
        humanChipsLabel = styledLabel("You: $1000", 16);

        newHandBtn = new Button("New Hand ▶");
        newHandBtn.setStyle(btnStyle("#4caf50"));
        newHandBtn.setOnAction(e -> startHand());

        HBox bar = new HBox(20, computerChipsLabel, new Spacer(), potLabel, new Spacer(), humanChipsLabel, newHandBtn);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(0, 0, 10, 0));
        return bar;
    }

    private VBox buildTable() {
        // Computer area
        Text cpuLabel = styledText("Computer", 14);
        computerBetLabel = styledLabel("", 13);
        computerCardArea = new HBox(8);
        computerCardArea.setAlignment(Pos.CENTER);
        fillPlaceholders(computerCardArea, 2);
        VBox cpuSection = new VBox(6, cpuLabel, computerBetLabel, computerCardArea);
        cpuSection.setAlignment(Pos.CENTER);

        // Community cards
        Text commLabel = styledText("Community Cards", 14);
        communityCardArea = new HBox(8);
        communityCardArea.setAlignment(Pos.CENTER);
        fillPlaceholders(communityCardArea, 5);
        statusLabel = styledLabel("Press 'New Hand' to start", 15);
        statusLabel.setStyle("-fx-text-fill: #ffe082; -fx-font-weight: bold;");
        VBox commSection = new VBox(6, commLabel, communityCardArea, statusLabel);
        commSection.setAlignment(Pos.CENTER);

        // Human area
        Text youLabel = styledText("Your Hand", 14);
        humanBetLabel = styledLabel("", 13);
        humanCardArea = new HBox(8);
        humanCardArea.setAlignment(Pos.CENTER);
        fillPlaceholders(humanCardArea, 2);
        VBox youSection = new VBox(6, youLabel, humanBetLabel, humanCardArea);
        youSection.setAlignment(Pos.CENTER);

        VBox table = new VBox(20, cpuSection, commSection, youSection);
        table.setAlignment(Pos.CENTER);
        table.setPadding(new Insets(10, 20, 10, 20));
        return table;
    }

    private HBox buildControls() {
        foldBtn  = actionBtn("Fold",  "#e53935", e -> game.humanFold());
        checkBtn = actionBtn("Check", "#1565c0", e -> game.humanCheck());
        callBtn  = actionBtn("Call",  "#0288d1", e -> game.humanCall());
        raiseBtn = actionBtn("Raise", "#e65100", e -> doRaise());

        raiseSlider = new Slider(0, 1000, 20);
        raiseSlider.setPrefWidth(160);
        raiseSlider.setStyle("-fx-control-inner-background: #2e7d32;");
        raiseAmountLabel = styledLabel("$20", 13);
        raiseSlider.valueProperty().addListener((o, ov, nv) -> {
            int val = nv.intValue();
            raiseAmountLabel.setText("$" + val);
        });

        VBox raiseBox = new VBox(4, styledLabel("Raise Amount", 12), raiseSlider, raiseAmountLabel);
        raiseBox.setAlignment(Pos.CENTER);

        HBox controls = new HBox(12, foldBtn, checkBtn, callBtn, raiseBtn, raiseBox);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10, 0, 0, 0));

        setActionEnabled(false);
        return controls;
    }

    private VBox buildLog() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefWidth(200);
        logArea.setPrefHeight(400);
        logArea.setStyle("-fx-control-inner-background: #0d3318; -fx-text-fill: #a5d6a7; -fx-font-family: Monospaced; -fx-font-size: 12;");
        logArea.setWrapText(true);
        Label title = styledLabel("Game Log", 13);
        VBox box = new VBox(6, title, logArea);
        box.setPadding(new Insets(0, 0, 0, 10));
        return box;
    }

    // ── Game event handlers ──────────────────────────────────────────────────

    private void startHand() {
        game.startNewHand();
        refreshAll();
        newHandBtn.setDisable(true);
        newHandBtn.setText("In Progress…");
    }

    private void handleStateChange(GameState state) {
        refreshAll();
        String msg = switch (state) {
            case PRE_FLOP  -> "Pre-Flop";
            case FLOP      -> "The Flop";
            case TURN      -> "The Turn";
            case RIVER     -> "The River";
            case SHOWDOWN  -> "Showdown";
            case GAME_OVER -> "Game Over";
            default        -> "";
        };
        statusLabel.setText(msg);

        if (state == GameState.SHOWDOWN || state == GameState.GAME_OVER) {
            setActionEnabled(false);
            newHandBtn.setDisable(false);
            newHandBtn.setText("New Hand ▶");
        }
    }

    private void doRaise() {
        int amount = (int) raiseSlider.getValue();
        int min = game.minRaise();
        if (amount < min) { amount = min; raiseSlider.setValue(amount); }
        game.humanRaise(amount);
        refreshAll();
    }

    // ── UI refresh ───────────────────────────────────────────────────────────

    private void refreshAll() {
        refreshCards(humanCardArea,     game.getHuman().getHand(),     false);
        refreshCards(computerCardArea,  game.getComputer().getHand(), false);
        refreshCommunity();
        humanChipsLabel.setText("You: $" + game.getHuman().getChips());
        computerChipsLabel.setText("Computer: $" + game.getComputer().getChips());
        potLabel.setText("Pot  $" + game.getPot());
        int hb = game.getHuman().getCurrentBet();
        int cb = game.getComputer().getCurrentBet();
        humanBetLabel.setText(hb > 0 ? "Bet: $" + hb : "");
        computerBetLabel.setText(cb > 0 ? "Bet: $" + cb : "");

        // Update call button label
        int callAmt = game.callAmount();
        callBtn.setText(callAmt > 0 ? "Call $" + callAmt : "Call");
        checkBtn.setVisible(game.canCheck());
        callBtn.setVisible(!game.canCheck());

        // Update raise slider bounds
        int minR = game.minRaise();
        int maxR = Math.max(minR, game.getHuman().getChips() + game.getHuman().getCurrentBet());
        raiseSlider.setMin(minR);
        raiseSlider.setMax(maxR);
        if (raiseSlider.getValue() < minR) raiseSlider.setValue(minR);
    }

    private void refreshCards(HBox area, List<Card> cards, boolean placeholder) {
        area.getChildren().clear();
        if (cards.isEmpty()) {
            fillPlaceholders(area, 2);
            return;
        }
        for (Card c : cards) area.getChildren().add(new CardView(c));
    }

    private void refreshCommunity() {
        communityCardArea.getChildren().clear();
        List<Card> comm = game.getCommunityCards();
        for (Card c : comm) communityCardArea.getChildren().add(new CardView(c));
        for (int i = comm.size(); i < 5; i++) communityCardArea.getChildren().add(CardView.placeholder());
    }

    private void setActionEnabled(boolean enabled) {
        foldBtn.setDisable(!enabled);
        checkBtn.setDisable(!enabled);
        callBtn.setDisable(!enabled);
        raiseBtn.setDisable(!enabled);
        raiseSlider.setDisable(!enabled);
    }

    private void appendLog(String msg) {
        logArea.appendText(msg + "\n");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void fillPlaceholders(HBox area, int count) {
        area.getChildren().clear();
        for (int i = 0; i < count; i++) area.getChildren().add(CardView.placeholder());
    }

    private Label styledLabel(String text, double size) {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font("System", FontWeight.BOLD, size));
        return l;
    }

    private Text styledText(String text, double size) {
        Text t = new Text(text);
        t.setFill(Color.web("#c8e6c9"));
        t.setFont(Font.font("System", FontWeight.BOLD, size));
        return t;
    }

    private Button actionBtn(String label, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(label);
        b.setStyle(btnStyle(color));
        b.setPrefWidth(90);
        b.setPrefHeight(40);
        b.setOnAction(handler);
        return b;
    }

    private String btnStyle(String hex) {
        return "-fx-background-color: " + hex + "; -fx-text-fill: white; " +
               "-fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 6;";
    }

    /** Horizontal spacer */
    private static class Spacer extends Region {
        Spacer() { HBox.setHgrow(this, Priority.ALWAYS); }
    }

    public static void main(String[] args) { launch(args); }
}
