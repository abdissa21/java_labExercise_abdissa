package com.chatapp.logic;

import com.chatapp.model.Message;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat business rules and protocol parsing. No UI or direct socket/DB code.
 */
public class ChatLogic {

    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_CHAT = "CHAT";
    public static final String CMD_HISTORY = "HISTORY";
    public static final String CMD_OK = "OK";
    public static final String CMD_ERR = "ERR";
    public static final String CMD_MSG = "MSG";
    public static final String CMD_JOIN = "JOIN";
    public static final String CMD_LEAVE = "LEAVE";

    public String buildLogin(String username) {
        return CMD_LOGIN + " " + username.trim();
    }

    public String buildChat(String text) {
        return CMD_CHAT + " " + text.trim();
    }

    public String buildHistoryRequest() {
        return CMD_HISTORY;
    }

    public ParsedCommand parseIncoming(String line) {
        if (line == null || line.isBlank()) {
            return ParsedCommand.invalid("Empty line");
        }
        int space = line.indexOf(' ');
        String cmd = space < 0 ? line.trim() : line.substring(0, space).trim();
        String payload = space < 0 ? "" : line.substring(space + 1).trim();

        return switch (cmd.toUpperCase()) {
            case CMD_LOGIN -> ParsedCommand.login(payload);
            case CMD_CHAT -> ParsedCommand.chat(payload);
            case CMD_HISTORY -> ParsedCommand.history();
            case CMD_OK -> ParsedCommand.ok(payload);
            case CMD_ERR -> ParsedCommand.error(payload);
            case CMD_MSG -> ParsedCommand.message(payload);
            case CMD_JOIN -> ParsedCommand.join(payload);
            case CMD_LEAVE -> ParsedCommand.leave(payload);
            default -> ParsedCommand.invalid("Unknown command: " + cmd);
        };
    }

    public String encodeHistory(List<Message> messages) {
        if (messages.isEmpty()) {
            return CMD_OK + " HISTORY ";
        }
        String body = messages.stream()
                .map(Message::encode)
                .collect(Collectors.joining("\n"));
        return CMD_OK + " HISTORY " + body;
    }

    public String encodeBroadcast(Message message) {
        return CMD_MSG + " " + message.encode();
    }

    public String encodeJoin(String username) {
        return CMD_JOIN + " " + username;
    }

    public String encodeLeave(String username) {
        return CMD_LEAVE + " " + username;
    }

    public String encodeError(String reason) {
        return CMD_ERR + " " + reason;
    }

    public String encodeOk(String info) {
        return CMD_OK + " " + info;
    }

    public record ParsedCommand(Type type, String payload, String error) {
        public enum Type { LOGIN, CHAT, HISTORY, OK, ERR, MSG, JOIN, LEAVE, INVALID }

        public static ParsedCommand login(String user) {
            if (user == null || user.isBlank()) {
                return new ParsedCommand(Type.INVALID, "", "Username required");
            }
            return new ParsedCommand(Type.LOGIN, user, null);
        }

        public static ParsedCommand chat(String text) {
            if (text == null || text.isBlank()) {
                return new ParsedCommand(Type.INVALID, "", "Message cannot be empty");
            }
            return new ParsedCommand(Type.CHAT, text, null);
        }

        public static ParsedCommand history() {
            return new ParsedCommand(Type.HISTORY, "", null);
        }

        public static ParsedCommand ok(String payload) {
            return new ParsedCommand(Type.OK, payload, null);
        }

        public static ParsedCommand error(String payload) {
            return new ParsedCommand(Type.ERR, payload, null);
        }

        public static ParsedCommand message(String payload) {
            return new ParsedCommand(Type.MSG, payload, null);
        }

        public static ParsedCommand join(String user) {
            return new ParsedCommand(Type.JOIN, user, null);
        }

        public static ParsedCommand leave(String user) {
            return new ParsedCommand(Type.LEAVE, user, null);
        }

        public static ParsedCommand invalid(String error) {
            return new ParsedCommand(Type.INVALID, "", error);
        }
    }
}
