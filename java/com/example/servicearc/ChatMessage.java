package com.example.servicearc;

public class ChatMessage {
    public String sender;
    public String message;
    public long time;

    public ChatMessage() {}

    public ChatMessage(String sender, String message, long time) {
        this.sender = sender;
        this.message = message;
        this.time = time;
    }
}
