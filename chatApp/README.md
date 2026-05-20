# Simple JavaFX Chat App

Multi-client chat with a threaded server, SQLite persistence, and separated layers:

| Layer | Package | Role |
|-------|---------|------|
| Model | `com.chatapp.model` | Message data |
| Database | `com.chatapp.database` | Save/load messages (SQLite) |
| Logic | `com.chatapp.logic` | Protocol parsing and formatting |
| Server | `com.chatapp.server` | Accept clients (one thread each), broadcast |
| Client logic | `com.chatapp.client.ChatClientLogic` | Sockets and protocol (no UI) |
| Client UI | `com.chatapp.client.ChatClientApp` | JavaFX interface |

## Requirements

- Java 17+
- Maven

## Run

**Terminal 1 — start the server:**

```bash
cd c:\Users\Hp\Desktop\chatApp
mvn -q exec:java@server
```

**Terminal 2+ — start a client (run once per user):**

```bash
mvn -q javafx:run
```

Or from your IDE: run `ChatServer.main` then `ChatClientApp.main`.

1. Enter host (`localhost`), port (`5000`), and a username.
2. Click **Connect**.
3. Open more client windows with different usernames to chat.
4. Messages are stored in `chat.db` in the project folder and reload on connect.

## Protocol (simple text lines)

- `LOGIN username`
- `CHAT message text`
- `HISTORY` — server replies with stored messages
- Server pushes `MSG`, `JOIN`, `LEAVE` to all clients
