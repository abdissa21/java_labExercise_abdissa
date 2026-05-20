# ♠ Texas Hold'em Poker — JavaFX

A 1-vs-1 Texas Hold'em poker game built with JavaFX. Play against a simple AI computer opponent with full hand evaluation, betting rounds, and pot management.

---

## 📁 Project Structure

```
poker-game/
├── pom.xml                          ← Maven build file
└── src/main/java/com/poker/
    ├── model/
    │   ├── Card.java                ← Card (rank, suit, face-up state)
    │   ├── Deck.java                ← 52-card deck with shuffle/deal
    │   ├── Player.java              ← Player state (chips, hand, bets)
    │   └── GameState.java           ← Enum: PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN
    ├── controller/
    │   └── GameController.java      ← Game logic, AI, betting, pot management
    ├── util/
    │   └── HandEvaluator.java       ← Full 5-card hand evaluation (all 9 hand types)
    └── view/
        ├── PokerApp.java            ← JavaFX Application & main UI
        └── CardView.java            ← Card rendering component
```

---

## ✅ Prerequisites

| Requirement | Version  | Check with          |
|-------------|----------|---------------------|
| Java JDK    | 17+      | `java -version`     |
| Maven       | 3.8+     | `mvn -version`      |

You do **not** need to install JavaFX separately — Maven downloads it automatically.

---

## 🚀 How to Run

### Option 1 — Maven JavaFX Plugin (recommended during development)

```bash
cd poker-game
mvn clean javafx:run
```

### Option 2 — Build a fat JAR and run it

```bash
cd poker-game
mvn clean package -q
java -jar target/poker-game-1.0.0.jar
```

> **macOS note:** If you see a blank window on macOS, add `-Dprism.order=sw` to the java command.
> ```bash
> java -Dprism.order=sw -jar target/poker-game-1.0.0.jar
> ```

---

## 🎮 How to Play

### Game Flow (Texas Hold'em)
1. Click **New Hand ▶** to deal a new hand
2. Blinds are posted automatically (Small: $10, Big: $20)
3. Each player receives 2 hole cards
4. Betting rounds: **Pre-Flop → Flop (3 cards) → Turn (1 card) → River (1 card)**
5. **Showdown** — best 5-card hand from 7 cards wins

### Your Actions
| Button  | When Available | What it does |
|---------|---------------|--------------|
| **Fold**  | Always        | Give up your hand, opponent wins the pot |
| **Check** | No bet to you | Pass without betting |
| **Call**  | Bet pending   | Match the current bet |
| **Raise** | Always        | Increase the bet (use the slider to set amount) |

### Hand Rankings (lowest → highest)
```
High Card < One Pair < Two Pair < Three of a Kind < Straight
< Flush < Full House < Four of a Kind < Straight Flush < Royal Flush
```

### AI Behaviour
The computer uses a simple strategy:
- Evaluates its hand strength against community cards
- Calls, raises, or folds based on pot odds and hand rank
- Occasionally bluffs (20% chance when hand is weak)

---

## 🛠 Troubleshooting

**"No main manifest attribute"** → use `mvn javafx:run` instead of `java -jar`

**"UnsupportedClassVersionError"** → you need Java 17+. Run `java -version` to check.

**Maven not found** → install from https://maven.apache.org/install.html
or use the included wrapper: `./mvnw clean javafx:run`

**Window appears but is blank** → try `java -Dprism.order=sw -jar target/poker-game-1.0.0.jar`

---

## 📦 Dependencies (auto-downloaded by Maven)

- `org.openjfx:javafx-controls:21.0.2`
- `org.openjfx:javafx-fxml:21.0.2`
