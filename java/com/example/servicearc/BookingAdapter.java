package com.example.servicearc;

import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder>{

    List<ServiceRequest> list;
    private String currentUid;

    public BookingAdapter(List<ServiceRequest> list){
        this.list = list;
        this.currentUid = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,int position){
        ServiceRequest req = list.get(position);
        if (req == null) return;

        boolean isProviderViewing = currentUid != null && currentUid.equals(req.providerId);

        // Display Name: Show Provider Name to Customer, Customer Name to Provider
        holder.name.setText(isProviderViewing ? req.customerName : req.providerName);

        holder.address.setText(req.customerAddress);
        holder.distance.setText(String.format("%.1f km away", req.distance));
        
        String statusText = req.status != null ? req.status : "pending";
        holder.status.setText(statusText.toUpperCase());
        holder.txtPhone.setText("Mobile: " + (isProviderViewing ? req.customerPhone : "N/A"));

        // Status Colors and Messages for Customer
        if("pending".equals(req.status)){
            holder.status.setTextColor(0xFFFF9800);
            if (!isProviderViewing) holder.status.setText("SEARCHING FOR PROVIDER...");
        }else if("accepted".equals(req.status)){
            holder.status.setTextColor(0xFF2196F3);
            if (!isProviderViewing) holder.status.setText("ACCEPTED - " + req.providerName + " IS ARRIVING SOON");
        }else if("arrived".equals(req.status)){
            holder.status.setTextColor(0xFFFFC107);
            if (!isProviderViewing) holder.status.setText("PROVIDER ARRIVED - WORK WILL BE DONE SOON");
        }else if("in_progress".equals(req.status)){
            holder.status.setTextColor(0xFFFFC107);
        }else if("completed".equals(req.status)){
            holder.status.setTextColor(0xFF4CAF50);
        }

        // Logic for Active vs History tabs
        if ("completed".equals(req.status) || "cancelled".equals(req.status) || "rejected".equals(req.status)) {
            // History Tab
            holder.layoutButtons.setVisibility(View.GONE);
            holder.timer.setVisibility(View.GONE);
            holder.txtCompletionDate.setVisibility(View.VISIBLE);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.txtCompletionDate.setText("Date: " + sdf.format(new Date(req.timestamp)));
        } else {
            // Active Tab
            holder.txtCompletionDate.setVisibility(View.GONE);
            
            if (isProviderViewing) {
                holder.layoutButtons.setVisibility(View.VISIBLE);
                holder.timer.setVisibility(View.VISIBLE);
                holder.track.setVisibility(View.VISIBLE);
                holder.manage.setVisibility(View.VISIBLE);
                holder.btnComplete.setVisibility(View.VISIBLE);

                holder.track.setText("Navigate");
                holder.manage.setText("Arrived");

                if ("arrived".equals(req.status)) holder.manage.setVisibility(View.GONE);

                holder.track.setOnClickListener(v -> {
                    try {
                        Uri uri = Uri.parse("google.navigation:q=" + req.customerLat + "," + req.customerLng);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.setPackage("com.google.android.apps.maps");
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(), "Google Maps not found", Toast.LENGTH_SHORT).show();
                    }
                });

                holder.manage.setOnClickListener(v -> {
                    if (req.requestId != null) {
                        FirebaseFirestore.getInstance().collection("service_requests")
                                .document(req.requestId).update("status", "arrived");
                    }
                });

                holder.btnComplete.setOnClickListener(v -> {
                    if (req.requestId != null) {
                        FirebaseFirestore.getInstance().collection("service_requests")
                                .document(req.requestId).update("status", "completed");
                    }
                });

            } else {
                // Customer Viewing Active Bookings
                holder.layoutButtons.setVisibility(View.VISIBLE);
                holder.track.setVisibility(View.GONE);
                holder.manage.setVisibility(View.GONE);
                holder.btnComplete.setVisibility(View.VISIBLE);
                
                holder.btnComplete.setText("Cancel");
                holder.btnComplete.setBackgroundResource(R.drawable.btn_complete); // Using red-ish color for cancel
                holder.btnComplete.setOnClickListener(v -> {
                    if (req.requestId != null) {
                        FirebaseFirestore.getInstance().collection("service_requests")
                                .document(req.requestId).update("status", "cancelled")
                                .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Request Cancelled", Toast.LENGTH_SHORT).show());
                    }
                });

                // Timer only for pending
                if ("pending".equals(req.status)) {
                    holder.timer.setVisibility(View.VISIBLE);
                    long remaining = req.expireAt - System.currentTimeMillis();
                    if(remaining > 0){
                        if (holder.timerTask != null) holder.timerTask.cancel();
                        holder.timerTask = new CountDownTimer(remaining, 1000){
                            public void onTick(long millisUntilFinished){
                                long min = millisUntilFinished / 60000;
                                long sec = (millisUntilFinished % 60000) / 1000;
                                holder.timer.setText(String.format(Locale.getDefault(), "Waiting: %02d:%02d", min, sec));
                            }
                            public void onFinish(){ holder.timer.setText("Request Expired"); }
                        }.start();
                    } else { holder.timer.setText("Request Expired"); }
                } else {
                    holder.timer.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount(){
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name, address, status, distance, timer, txtPhone, txtCompletionDate;
        Button track, manage, btnComplete;
        View layoutButtons;
        CountDownTimer timerTask;

        ViewHolder(View v){
            super(v);
            name = v.findViewById(R.id.txtProvider);
            address = v.findViewById(R.id.txtAddress);
            status = v.findViewById(R.id.txtStatus);
            distance = v.findViewById(R.id.txtDistance);
            timer = v.findViewById(R.id.txtTimer);
            txtPhone = v.findViewById(R.id.txtPhone);
            txtCompletionDate = v.findViewById(R.id.txtCompletionDate);
            layoutButtons = v.findViewById(R.id.layoutButtons);
            track = v.findViewById(R.id.btnTrack);
            manage = v.findViewById(R.id.btnManage);
            btnComplete = v.findViewById(R.id.btnComplete);
        }
    }
}
