package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderChatActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EditText message;
    private Button send;
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setupBottomNavigation();

        recycler = findViewById(R.id.chatRecycler);
        message = findViewById(R.id.chatMessage);
        send = findViewById(R.id.sendBtn);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messages);
        recycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadMessages();

        send.setOnClickListener(v -> sendMessage());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(R.menu.provider_bottom_menu);
        bottomNav.setSelectedItemId(R.id.nav_provider_chat);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_provider_home) {
                startActivity(new Intent(this, ProviderDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_provider_bookings) {
                startActivity(new Intent(this, ProviderBookingsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_provider_chat) {
                return true;
            } else if (id == R.id.nav_provider_profile) {
                startActivity(new Intent(this, ProviderProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void sendMessage(){
        String text = message.getText().toString();
        if(text.isEmpty()) return;

        Map<String,Object> msg = new HashMap<>();
        msg.put("sender", FirebaseAuth.getInstance().getUid());
        msg.put("message", text);
        msg.put("time", System.currentTimeMillis());

        db.collection("chats").add(msg);
        message.setText("");
    }

    private void loadMessages(){
        db.collection("chats")
                .orderBy("time")
                .addSnapshotListener((value,error)->{
                    if (value == null) return;
                    messages.clear();
                    for(DocumentSnapshot doc : value){
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        messages.add(m);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
