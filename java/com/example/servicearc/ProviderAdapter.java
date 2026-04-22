package com.example.servicearc;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Provider provider);
    }

    private List<Provider> providerList;
    private OnBookClickListener listener;
    private Map<String, CountDownTimer> timers = new HashMap<>();

    public ProviderAdapter(List<Provider> providerList,
                           OnBookClickListener listener) {
        this.providerList = providerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Provider provider = providerList.get(position);

        holder.txtName.setText(provider.name != null ? provider.name : "Provider");
        holder.txtService.setText(provider.serviceType != null ? provider.serviceType : "Service");
        holder.txtAddress.setText(provider.address != null ? provider.address : "Address not available");
        
        String ratingStr = String.format(Locale.getDefault(), "⭐ %.1f", provider.rating);
        holder.txtRating.setText(ratingStr);

        // Populate more info section
        holder.txtInfoExperience.setText("Experience: " + provider.experience + " years");
        holder.txtInfoDetails.setText("Name & Service: " + provider.name + " | " + provider.serviceType);
        
        // Toggle more info visibility
        holder.layoutMoreInfo.setVisibility(View.GONE);
        holder.btnMoreInfo.setOnClickListener(v -> {
            if (holder.layoutMoreInfo.getVisibility() == View.VISIBLE) {
                holder.layoutMoreInfo.setVisibility(View.GONE);
                holder.btnMoreInfo.setText("More info");
            } else {
                holder.layoutMoreInfo.setVisibility(View.VISIBLE);
                holder.btnMoreInfo.setText("Less info");
            }
        });

        // Cancel existing timer for this holder if any (for recycling safety)
        if (timers.containsKey(provider.uid)) {
            timers.get(provider.uid).cancel();
            timers.remove(provider.uid);
        }

        long currentTime = System.currentTimeMillis();
        if (provider.requestExpireTime > currentTime) {
            startTimer(holder, provider);
        } else {
            resetBookingButton(holder, provider);
        }

        holder.btnBook.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(provider);
                provider.requestExpireTime = System.currentTimeMillis() + 300000; // 5 mins
                startTimer(holder, provider);
            }
        });

        holder.btnCancel.setOnClickListener(v -> {
            cancelBooking(holder, provider);
        });
    }

    private void startTimer(ViewHolder holder, Provider provider) {
        long timeLeft = provider.requestExpireTime - System.currentTimeMillis();
        
        if (timeLeft <= 0) {
            resetBookingButton(holder, provider);
            return;
        }

        holder.btnBook.setEnabled(false);
        holder.btnBook.setBackgroundColor(Color.parseColor("#FF5722")); // Orange
        holder.btnCancel.setVisibility(View.VISIBLE);

        CountDownTimer timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                holder.btnBook.setText(String.format("Waiting... %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                provider.requestExpireTime = 0;
                resetBookingButton(holder, provider);
                timers.remove(provider.uid);
            }
        }.start();

        timers.put(provider.uid, timer);
    }

    private void cancelBooking(ViewHolder holder, Provider provider) {
        if (timers.containsKey(provider.uid)) {
            timers.get(provider.uid).cancel();
            timers.remove(provider.uid);
        }
        provider.requestExpireTime = 0;
        resetBookingButton(holder, provider);
        
        // Optionally update Firestore to cancel the request
        FirebaseFirestore.getInstance().collection("service_requests")
                .whereEqualTo("providerId", provider.uid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });

        Toast.makeText(holder.itemView.getContext(), "Booking Cancelled", Toast.LENGTH_SHORT).show();
    }

    private void resetBookingButton(ViewHolder holder, Provider provider) {
        holder.btnCancel.setVisibility(View.GONE);
        if (provider.online) {
            holder.btnBook.setText("Book Service");
            holder.btnBook.setEnabled(true);
            holder.btnBook.setBackgroundColor(Color.parseColor("#3C4BFF"));
        } else {
            holder.btnBook.setText("Service is Not available");
            holder.btnBook.setEnabled(false);
            holder.btnBook.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return providerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtService, txtAddress, txtRating;
        TextView btnMoreInfo, txtInfoExperience, txtInfoDetails;
        LinearLayout layoutMoreInfo;
        Button btnBook, btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtProviderName);
            txtService = itemView.findViewById(R.id.txtServiceType);
            txtAddress = itemView.findViewById(R.id.txtProviderAddress);
            txtRating = itemView.findViewById(R.id.txtProviderRating);
            btnMoreInfo = itemView.findViewById(R.id.btnMoreInfo);
            txtInfoExperience = itemView.findViewById(R.id.txtInfoExperience);
            txtInfoDetails = itemView.findViewById(R.id.txtInfoDetails);
            layoutMoreInfo = itemView.findViewById(R.id.layoutMoreInfo);
            btnBook = itemView.findViewById(R.id.btnBookService);
            btnCancel = itemView.findViewById(R.id.btnCancelService);
        }
    }
}