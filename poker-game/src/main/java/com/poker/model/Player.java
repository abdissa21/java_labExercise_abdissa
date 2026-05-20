package com.poker.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private final String name;
    private final boolean isHuman;
    private final List<Card> hand = new ArrayList<>();
    private int chips;
    private int currentBet;
    private boolean folded;
    private boolean allIn;

    public Player(String name, boolean isHuman, int startingChips) {
        this.name = name;
        this.isHuman = isHuman;
        this.chips = startingChips;
        this.currentBet = 0;
        this.folded = false;
        this.allIn = false;
    }

    public void receiveCard(Card card) { hand.add(card); }
    public void clearHand() { hand.clear(); }

    public List<Card> getHand() { return hand; }
    public String getName() { return name; }
    public boolean isHuman() { return isHuman; }
    public int getChips() { return chips; }
    public int getCurrentBet() { return currentBet; }
    public boolean isFolded() { return folded; }
    public boolean isAllIn() { return allIn; }

    public void setFolded(boolean folded) { this.folded = folded; }
    public void setCurrentBet(int bet) { this.currentBet = bet; }

    public int placeBet(int amount) {
        int actual = Math.min(amount, chips);
        chips -= actual;
        currentBet += actual;
        if (chips == 0) allIn = true;
        return actual;
    }

    public void collectWinnings(int amount) { chips += amount; }

    public void resetForNewRound() {
        hand.clear();
        currentBet = 0;
        folded = false;
        allIn = false;
    }
}
