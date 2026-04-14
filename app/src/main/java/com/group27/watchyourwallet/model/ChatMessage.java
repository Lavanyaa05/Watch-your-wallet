package com.group27.watchyourwallet.model;

public class ChatMessage {
    public String message;
    public boolean isUser;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }
}
