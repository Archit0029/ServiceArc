package com.example.servicearc;

import com.google.firebase.firestore.DocumentId;

public class Provider {

    @DocumentId
    public String uid;
    public String name;
    public String serviceType;
    public String city;
    public double rating;
    public double latitude;
    public double longitude;
    public String address;
    public float distance;
    public boolean online;
    public String experience; // Changed to String to match Firestore and Activity
    
    // For UI timer
    public long requestExpireTime;

    public Provider(){}

}