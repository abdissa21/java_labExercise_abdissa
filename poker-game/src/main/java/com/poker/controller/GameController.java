package com.poker.controller;

import com.poker.model.*;
import com.poker.util.HandEvaluator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameController {

    public interface GameListener {
        void onStateChanged(GameState state);
        void onMessageLogged(String message);
        void onPotUpdated(int pot);
        void onPlayerTurn(Player player);
    }

    private final Player human;
    private final Player computer;
    private final Deck deck;
    private final List<Card> communityCards = new ArrayList<>();
    private final List<GameListener> listeners = new ArrayList<>();
    private final Random rng = new Random();

    private GameState state = GameState.WAITING;
    private int pot = 0;
    private int currentBet = 0;
    private int smallBlind = 10;
    private int bigBlind = 20;
    private boolean humanIsDealer = true; // alternates each hand
    private String lastAction = "";

    public GameController() {
        human = new Player("You", true, 1000);
        computer = new Player("Computer", false, 1000);
        deck = new Deck();
    }

    public void addListener(GameListener l) { listeners.add(l); }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public Player getHuman() { return human; }
    public Player getComputer() { return computer; }
    public List<Card> getCommunityCards() { return communityCards; }
    public GameState getState() { return state; }
    public int getPot() { return pot; }
    public int getCurrentBet() { return currentBet; }
    public int getBigBlind() { return bigBlind; }

    // ── Start a new hand ───────────────────────────────────────────────────────
    public void startNewHand() {
        deck.reset();
        deck.shuffle();
        communityCards.clear();
        pot = 0;
        currentBet = 0;
        human.resetForNewRound();
        computer.resetForNewRound();

        log("─── New Hand ───");

        // Post blinds
        Player smallBlindPlayer = humanIsDealer ? computer : human;
        Player bigBlindPlayer   = humanIsDealer ? human : computer;

        int sb = smallBlindPlayer.placeBet(smallBlind);
        pot += sb;
        log(smallBlindPlayer.getName() + " posts small blind: $" + sb);

        int bb = bigBlindPlayer.placeBet(bigBlind);
        pot += bb;
        currentBet = bigBlind;
        log(bigBlindPlayer.getName() + " posts big blind: $" + bb);

        // Deal hole cards
        for (int i = 0; i < 2; i++) {
            human.receiveCard(deck.deal());
            Card c = deck.deal();
            c.setFaceUp(false);
            computer.receiveCard(c);
        }

        setState(GameState.PRE_FLOP);
        notifyPot();

        // Small blind acts first pre-flop
        schedulePlayerTurn(smallBlindPlayer);
    }

    // ── Human actions ─────────────────────────────────────────────────────────
    public void humanFold() {
        log("You fold.");
        human.setFolded(true);
        pot += 0; // no more chips go in
        awardPot(computer);
        endHand();
    }

    public void humanCheck() {
        if (currentBet > human.getCurrentBet()) {
            log("Cannot check — there is a bet to call.");
            return;
        }
        log("You check.");
        continueAfterHumanAction();
    }

    public void humanCall() {
        int needed = currentBet - human.getCurrentBet();
        if (needed <= 0) { humanCheck(); return; }
        int paid = human.placeBet(needed);
        pot += paid;
        log("You call $" + paid + ".");
        notifyPot();
        continueAfterHumanAction();
    }

    public void humanRaise(int raiseTotal) {
        int needed = raiseTotal - human.getCurrentBet();
        if (needed <= 0) return;
        int paid = human.placeBet(needed);
        pot += paid;
        currentBet = human.getCurrentBet();
        log("You raise to $" + currentBet + ".");
        notifyPot();
        continueAfterHumanAction();
    }

    // ── Flow control ──────────────────────────────────────────────────────────
    private void continueAfterHumanAction() {
        // Computer acts next
        if (!computer.isFolded() && !computer.isAllIn()) {
            computeAI();
        }
        advanceStreet();
    }

    private void computeAI() {
        int toCall = currentBet - computer.getCurrentBet();

        // Simple AI: evaluate hand strength to decide
        HandEvaluator.HandResult result = null;
        if (!communityCards.isEmpty()) {
            result = HandEvaluator.evaluate(computer.getHand(), communityCards);
        }

        int handStrength = result == null ? 0 : result.rank.getValue();
        double callRatio = pot > 0 ? (double) toCall / pot : 1.0;

        if (toCall == 0) {
            // No cost to stay — check or occasionally bet
            if (handStrength >= 3 && rng.nextInt(3) == 0) {
                int raise = bigBlind * (1 + rng.nextInt(3));
                int paid = computer.placeBet(raise);
                pot += paid;
                currentBet = computer.getCurrentBet();
                log("Computer bets $" + paid + ".");
            } else {
                log("Computer checks.");
            }
        } else if (handStrength >= 6 || (handStrength >= 3 && callRatio < 0.3)) {
            // Strong hand or good pot odds — call or raise
            if (handStrength >= 7 && rng.nextBoolean()) {
                int raise = toCall + bigBlind * (1 + rng.nextInt(4));
                int paid = computer.placeBet(Math.min(raise, computer.getChips()));
                pot += paid;
                currentBet = Math.max(currentBet, computer.getCurrentBet());
                log("Computer raises to $" + computer.getCurrentBet() + ".");
            } else {
                int paid = computer.placeBet(toCall);
                pot += paid;
                log("Computer calls $" + paid + ".");
            }
        } else if (handStrength >= 1 && callRatio < 0.5) {
            // Marginal — call if cheap
            int paid = computer.placeBet(toCall);
            pot += paid;
            log("Computer calls $" + paid + ".");
        } else {
            // Fold or bluff
            if (rng.nextInt(5) == 0) { // 20% bluff
                int paid = computer.placeBet(toCall);
                pot += paid;
                log("Computer calls $" + paid + ".");
            } else {
                log("Computer folds.");
                computer.setFolded(true);
            }
        }
        notifyPot();
    }

    private void advanceStreet() {
        if (computer.isFolded()) { awardPot(human); endHand(); return; }
        if (human.isFolded())    { awardPot(computer); endHand(); return; }

        // Equalize bets (handle edge cases)
        switch (state) {
            case PRE_FLOP  -> dealFlop();
            case FLOP      -> dealTurn();
            case TURN      -> dealRiver();
            case RIVER     -> showdown();
            default        -> {}
        }
    }

    private void dealFlop() {
        deck.deal(); // burn
        for (int i = 0; i < 3; i++) communityCards.add(deck.deal());
        currentBet = 0;
        human.setCurrentBet(0);
        computer.setCurrentBet(0);
        log("Flop: " + cardsStr(communityCards));
        setState(GameState.FLOP);
        schedulePlayerTurn(humanIsDealer ? computer : human);
    }

    private void dealTurn() {
        deck.deal(); // burn
        communityCards.add(deck.deal());
        currentBet = 0;
        human.setCurrentBet(0);
        computer.setCurrentBet(0);
        log("Turn: " + communityCards.get(3));
        setState(GameState.TURN);
        schedulePlayerTurn(humanIsDealer ? computer : human);
    }

    private void dealRiver() {
        deck.deal(); // burn
        communityCards.add(deck.deal());
        currentBet = 0;
        human.setCurrentBet(0);
        computer.setCurrentBet(0);
        log("River: " + communityCards.get(4));
        setState(GameState.RIVER);
        schedulePlayerTurn(humanIsDealer ? computer : human);
    }

    private void showdown() {
        // Reveal computer's cards
        computer.getHand().forEach(c -> c.setFaceUp(true));
        setState(GameState.SHOWDOWN);

        HandEvaluator.HandResult humanResult   = HandEvaluator.evaluate(human.getHand(), communityCards);
        HandEvaluator.HandResult computerResult = HandEvaluator.evaluate(computer.getHand(), communityCards);

        log("Your hand: " + humanResult.rank.getDisplayName());
        log("Computer's hand: " + computerResult.rank.getDisplayName());

        int cmp = humanResult.compareTo(computerResult);
        if (cmp > 0) {
            log("You win the pot of $" + pot + "! 🎉");
            awardPot(human);
        } else if (cmp < 0) {
            log("Computer wins the pot of $" + pot + ".");
            awardPot(computer);
        } else {
            log("Split pot — tie!");
            int half = pot / 2;
            human.collectWinnings(half);
            computer.collectWinnings(pot - half);
            pot = 0;
        }
        endHand();
    }

    private void awardPot(Player winner) {
        winner.collectWinnings(pot);
        pot = 0;
        notifyPot();
    }

    private void endHand() {
        setState(GameState.SHOWDOWN);
        humanIsDealer = !humanIsDealer;

        if (human.getChips() == 0) {
            log("You're out of chips. Game over!");
            setState(GameState.GAME_OVER);
        } else if (computer.getChips() == 0) {
            log("Computer is out of chips. You win the game! 🏆");
            setState(GameState.GAME_OVER);
        }
    }

    private void schedulePlayerTurn(Player p) {
        if (p.isHuman()) {
            for (GameListener l : listeners) l.onPlayerTurn(p);
        } else {
            // AI acts, then human if needed
            computeAI();
            if (state == GameState.PRE_FLOP) {
                // After AI posts and acts, human needs to act if there's a bet
                if (!computer.isFolded() && currentBet > human.getCurrentBet()) {
                    for (GameListener l : listeners) l.onPlayerTurn(human);
                } else if (!computer.isFolded()) {
                    for (GameListener l : listeners) l.onPlayerTurn(human);
                } else {
                    advanceStreet();
                }
            } else {
                // Post-flop: AI checked/bet, human responds
                if (!computer.isFolded()) {
                    for (GameListener l : listeners) l.onPlayerTurn(human);
                } else {
                    advanceStreet();
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void setState(GameState s) {
        this.state = s;
        for (GameListener l : listeners) l.onStateChanged(s);
    }

    private void notifyPot() {
        for (GameListener l : listeners) l.onPotUpdated(pot);
    }

    private void log(String msg) {
        for (GameListener l : listeners) l.onMessageLogged(msg);
    }

    private String cardsStr(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (Card c : cards) sb.append(c.getSymbol()).append(" ");
        return sb.toString().trim();
    }

    public boolean canCheck() { return currentBet <= human.getCurrentBet(); }
    public int callAmount()   { return Math.max(0, currentBet - human.getCurrentBet()); }
    public int minRaise()     { return currentBet + bigBlind; }
}
