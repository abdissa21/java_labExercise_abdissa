package com.poker.view;

import com.poker.model.Card;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class CardView extends StackPane {

    private static final double CARD_W = 70;
    private static final double CARD_H = 100;
    private static final double ARC = 10;

    private final Rectangle bg;
    private final Text label;
    private final Text suitBig;

    public CardView(Card card) {
        setPrefSize(CARD_W, CARD_H);
        setMaxSize(CARD_W, CARD_H);

        bg = new Rectangle(CARD_W, CARD_H);
        bg.setArcWidth(ARC);
        bg.setArcHeight(ARC);
        bg.setStrokeWidth(1.5);

        label = new Text();
        label.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));

        suitBig = new Text();
        suitBig.setFont(Font.font("Monospaced", FontWeight.BOLD, 26));

        VBox content = new VBox(2, label, suitBig);
        content.setAlignment(Pos.CENTER);

        getChildren().addAll(bg, content);
        update(card);
    }

    public void update(Card card) {
        if (!card.isFaceUp()) {
            bg.setFill(Color.web("#1a237e"));
            bg.setStroke(Color.web("#5c6bc0"));
            label.setText("?");
            label.setFill(Color.WHITE);
            suitBig.setText("?");
            suitBig.setFill(Color.WHITE);
        } else {
            bg.setFill(Color.WHITE);
            bg.setStroke(Color.web("#bdbdbd"));

            String rankStr = switch (card.getRank()) {
                case TWO   -> "2";  case THREE -> "3"; case FOUR  -> "4";
                case FIVE  -> "5";  case SIX   -> "6"; case SEVEN -> "7";
                case EIGHT -> "8";  case NINE  -> "9"; case TEN   -> "10";
                case JACK  -> "J";  case QUEEN -> "Q"; case KING  -> "K";
                case ACE   -> "A";
            };
            String suitStr = switch (card.getSuit()) {
                case HEARTS   -> "♥"; case DIAMONDS -> "♦";
                case CLUBS    -> "♣"; case SPADES   -> "♠";
            };

            Color color = card.isRed() ? Color.web("#c62828") : Color.web("#1a1a1a");
            label.setText(rankStr + suitStr);
            label.setFill(color);
            suitBig.setText(suitStr);
            suitBig.setFill(color);
        }
    }

    /** A greyed-out placeholder slot */
    public static CardView placeholder() {
        Rectangle rect = new Rectangle(CARD_W, CARD_H);
        rect.setArcWidth(ARC);
        rect.setArcHeight(ARC);
        rect.setFill(Color.web("#2e7d32", 0.25));
        rect.setStroke(Color.web("#4caf50", 0.4));
        rect.setStrokeWidth(1.5);
        rect.getStrokeDashArray().addAll(6.0, 4.0);
        CardView cv = new CardView(new com.poker.model.Card(Card.Rank.ACE, Card.Suit.SPADES));
        cv.getChildren().clear();
        cv.getChildren().add(rect);
        return cv;
    }
}
