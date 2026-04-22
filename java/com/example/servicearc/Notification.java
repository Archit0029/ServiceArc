package com.example.servicearc;

import com.google.firebase.firestore.DocumentId;

public class Notification {
    @DocumentId
    public String id;
    public String title;
    public String message;
    public long timestamp;
    public String userId;

    public Notification() {}

    public Notification(String title, String message, String userId) {
        this.title = title;
        this.message = message;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }
}
