package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    RecyclerView recycler;
    EditText message;
    ImageButton send;
    List<ChatMessage> messages = new ArrayList<>();
    ChatAdapter adapter;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Remove BottomNavigationView logic if it's handled by CustomerDashboardActivity's ViewPager
        // But for now, let's just fix the crash by changing Button to ImageButton

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
                        if (m != null) messages.add(m);
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recycler.scrollToPosition(messages.size() - 1);
                    }
                });
    }
}
