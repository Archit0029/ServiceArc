package com.example.servicearc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment {

    private RecyclerView recycler;
    private EditText messageInput;
    private ImageButton sendBtn;
    private TextView txtChatTarget;
    private LinearLayout layoutNoChat, chatInputLayout;
    
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    private String activeRequestId = null;
    private String otherUserId = null;
    private String otherUserName = null;
    private ListenerRegistration chatListener, sessionListener;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        recycler = view.findViewById(R.id.chatRecycler);
        messageInput = view.findViewById(R.id.chatMessage);
        sendBtn = view.findViewById(R.id.sendBtn);
        txtChatTarget = view.findViewById(R.id.txtChatTarget);
        layoutNoChat = view.findViewById(R.id.layoutNoChat);
        chatInputLayout = view.findViewById(R.id.chatInputLayout);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(messages);
        recycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        checkActiveSession();

        sendBtn.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void checkActiveSession() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Listen for accepted/active service requests where this user is the customer
        sessionListener = db.collection("service_requests")
                .whereEqualTo("customerId", uid)
                .whereIn("status", Arrays.asList("accepted", "arrived", "in_progress"))
                .addSnapshotListener((query, error) -> {
                    if (query != null && !query.isEmpty()) {
                        DocumentSnapshot doc = query.getDocuments().get(0);
                        activeRequestId = doc.getId();
                        otherUserId = doc.getString("providerId");
                        otherUserName = doc.getString("providerName");
                        
                        showChatUI(true);
                        listenToMessages();
                    } else {
                        showChatUI(false);
                    }
                });
    }

    private void showChatUI(boolean hasActiveSession) {
        if (hasActiveSession) {
            layoutNoChat.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            chatInputLayout.setVisibility(View.VISIBLE);
            txtChatTarget.setText("Chat with " + (otherUserName != null ? otherUserName : "Provider"));
        } else {
            layoutNoChat.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            chatInputLayout.setVisibility(View.GONE);
            txtChatTarget.setText("Chat");
            
            if (chatListener != null) chatListener.remove();
            messages.clear();
            adapter.notifyDataSetChanged();
        }
    }

    private void listenToMessages() {
        if (activeRequestId == null) return;
        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("service_requests")
                .document(activeRequestId)
                .collection("messages")
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    messages.clear();
                    for (DocumentSnapshot doc : value) {
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        if (m != null) messages.add(m);
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recycler.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty() || activeRequestId == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("sender", auth.getUid());
        msg.put("message", text);
        msg.put("time", System.currentTimeMillis());

        db.collection("service_requests")
                .document(activeRequestId)
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> messageInput.setText(""))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) sessionListener.remove();
        if (chatListener != null) chatListener.remove();
    }
}
