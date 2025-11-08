package com.example.myapplication;
public class Message {
    public static final int TYPE_USER = 1;
    public static final int TYPE_BOT = 2;

    private String message;
    private int type;

    public Message(String message, int type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {  // ← Must be getMessage()
        return message;
    }

    public int getType() {        // ← Must be getType()
        return type;
    }
}