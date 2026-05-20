package com.poker.model;

public class Card {
    public enum Suit { HEARTS, DIAMONDS, CLUBS, SPADES }
    public enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8),
        NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);

        private final int value;
        Rank(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    private final Rank rank;
    private final Suit suit;
    private boolean faceUp;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.faceUp = true;
    }

    public Rank getRank() { return rank; }
    public Suit getSuit() { return suit; }
    public boolean isFaceUp() { return faceUp; }
    public void setFaceUp(boolean faceUp) { this.faceUp = faceUp; }

    public String getSymbol() {
        String r = switch (rank) {
            case TWO -> "2"; case THREE -> "3"; case FOUR -> "4";
            case FIVE -> "5"; case SIX -> "6"; case SEVEN -> "7";
            case EIGHT -> "8"; case NINE -> "9"; case TEN -> "10";
            case JACK -> "J"; case QUEEN -> "Q"; case KING -> "K"; case ACE -> "A";
        };
        String s = switch (suit) {
            case HEARTS -> "♥"; case DIAMONDS -> "♦";
            case CLUBS -> "♣"; case SPADES -> "♠";
        };
        return r + s;
    }

    public boolean isRed() {
        return suit == Suit.HEARTS || suit == Suit.DIAMONDS;
    }

    @Override
    public String toString() { return getSymbol(); }
}
