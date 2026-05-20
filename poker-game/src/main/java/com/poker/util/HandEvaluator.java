package com.poker.util;

import com.poker.model.Card;
import java.util.*;

public class HandEvaluator {

    public enum HandRank {
        HIGH_CARD(0, "High Card"),
        ONE_PAIR(1, "One Pair"),
        TWO_PAIR(2, "Two Pair"),
        THREE_OF_A_KIND(3, "Three of a Kind"),
        STRAIGHT(4, "Straight"),
        FLUSH(5, "Flush"),
        FULL_HOUSE(6, "Full House"),
        FOUR_OF_A_KIND(7, "Four of a Kind"),
        STRAIGHT_FLUSH(8, "Straight Flush"),
        ROYAL_FLUSH(9, "Royal Flush");

        private final int value;
        private final String displayName;

        HandRank(int value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public int getValue() { return value; }
        public String getDisplayName() { return displayName; }
    }

    public static class HandResult implements Comparable<HandResult> {
        public final HandRank rank;
        public final List<Integer> tiebreakers;
        public final String description;

        public HandResult(HandRank rank, List<Integer> tiebreakers, String description) {
            this.rank = rank;
            this.tiebreakers = tiebreakers;
            this.description = description;
        }

        @Override
        public int compareTo(HandResult other) {
            int rankCmp = Integer.compare(this.rank.getValue(), other.rank.getValue());
            if (rankCmp != 0) return rankCmp;
            for (int i = 0; i < Math.min(this.tiebreakers.size(), other.tiebreakers.size()); i++) {
                int cmp = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
                if (cmp != 0) return cmp;
            }
            return 0;
        }
    }

    public static HandResult evaluate(List<Card> holeCards, List<Card> communityCards) {
        List<Card> all = new ArrayList<>(holeCards);
        all.addAll(communityCards);

        // Generate all 5-card combinations from up to 7 cards
        List<List<Card>> combos = getCombinations(all, 5);
        HandResult best = null;
        for (List<Card> combo : combos) {
            HandResult result = evaluateFive(combo);
            if (best == null || result.compareTo(best) > 0) best = result;
        }
        return best;
    }

    private static HandResult evaluateFive(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> b.getRank().getValue() - a.getRank().getValue());

        boolean flush = isFlush(sorted);
        boolean straight = isStraight(sorted);
        Map<Integer, Integer> counts = getCounts(sorted);
        List<Integer> vals = getOrderedValues(counts);

        if (flush && straight) {
            int high = sorted.get(0).getRank().getValue();
            if (high == 14) return new HandResult(HandRank.ROYAL_FLUSH, List.of(high), "Royal Flush");
            return new HandResult(HandRank.STRAIGHT_FLUSH, List.of(high), "Straight Flush");
        }
        if (vals.get(0) == 4) {
            int quad = getByCount(counts, 4);
            int kicker = getByCount(counts, 1);
            return new HandResult(HandRank.FOUR_OF_A_KIND, List.of(quad, kicker), "Four of a Kind");
        }
        if (vals.get(0) == 3 && vals.get(1) == 2) {
            int trips = getByCount(counts, 3);
            int pair = getByCount(counts, 2);
            return new HandResult(HandRank.FULL_HOUSE, List.of(trips, pair), "Full House");
        }
        if (flush) {
            List<Integer> tb = sorted.stream().map(c -> c.getRank().getValue()).toList();
            return new HandResult(HandRank.FLUSH, tb, "Flush");
        }
        if (straight) {
            int high = sorted.get(0).getRank().getValue();
            return new HandResult(HandRank.STRAIGHT, List.of(high), "Straight");
        }
        if (vals.get(0) == 3) {
            int trips = getByCount(counts, 3);
            List<Integer> kickers = getByCountMultiple(counts, 1);
            List<Integer> tb = new ArrayList<>();
            tb.add(trips);
            tb.addAll(kickers);
            return new HandResult(HandRank.THREE_OF_A_KIND, tb, "Three of a Kind");
        }
        if (vals.get(0) == 2 && vals.get(1) == 2) {
            List<Integer> pairs = getByCountMultiple(counts, 2);
            int kicker = getByCount(counts, 1);
            List<Integer> tb = new ArrayList<>(pairs);
            tb.add(kicker);
            return new HandResult(HandRank.TWO_PAIR, tb, "Two Pair");
        }
        if (vals.get(0) == 2) {
            int pair = getByCount(counts, 2);
            List<Integer> kickers = getByCountMultiple(counts, 1);
            List<Integer> tb = new ArrayList<>();
            tb.add(pair);
            tb.addAll(kickers);
            return new HandResult(HandRank.ONE_PAIR, tb, "One Pair");
        }
        List<Integer> tb = sorted.stream().map(c -> c.getRank().getValue()).toList();
        return new HandResult(HandRank.HIGH_CARD, tb, "High Card");
    }

    private static boolean isFlush(List<Card> cards) {
        Card.Suit s = cards.get(0).getSuit();
        return cards.stream().allMatch(c -> c.getSuit() == s);
    }

    private static boolean isStraight(List<Card> sorted) {
        List<Integer> vals = sorted.stream().map(c -> c.getRank().getValue()).toList();
        // Check ace-low straight
        if (vals.equals(List.of(14, 5, 4, 3, 2))) return true;
        for (int i = 0; i < 4; i++)
            if (vals.get(i) - vals.get(i + 1) != 1) return false;
        return true;
    }

    private static Map<Integer, Integer> getCounts(List<Card> cards) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Card c : cards) map.merge(c.getRank().getValue(), 1, Integer::sum);
        return map;
    }

    private static List<Integer> getOrderedValues(Map<Integer, Integer> counts) {
        List<Integer> vals = new ArrayList<>(counts.values());
        vals.sort(Collections.reverseOrder());
        return vals;
    }

    private static int getByCount(Map<Integer, Integer> counts, int count) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() == count)
                .mapToInt(Map.Entry::getKey).max().orElse(0);
    }

    private static List<Integer> getByCountMultiple(Map<Integer, Integer> counts, int count) {
        return counts.entrySet().stream()
                .filter(e -> e.getValue() == count)
                .map(Map.Entry::getKey)
                .sorted(Collections.reverseOrder())
                .toList();
    }

    private static List<List<Card>> getCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        combine(cards, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static void combine(List<Card> cards, int k, int start, List<Card> current, List<List<Card>> result) {
        if (current.size() == k) { result.add(new ArrayList<>(current)); return; }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            combine(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
