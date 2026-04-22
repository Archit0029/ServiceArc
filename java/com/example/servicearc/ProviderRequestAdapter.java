package com.example.servicearc;

import android.view.*;
import android.widget.*;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRequestAdapter
        extends RecyclerView.Adapter<ProviderRequestAdapter.ViewHolder> {

    List<ServiceRequest> list;

    public ProviderRequestAdapter(List<ServiceRequest> list){
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView txtCustomerName, txtAddress, txtWaitValue;
        Button btnAccept, btnReject, btnWait, btnConfirmWait;
        LinearLayout layoutWaitTime, layoutActionButtons;
        SeekBar seekWaitTime;

        public ViewHolder(View itemView){
            super(itemView);

            txtCustomerName = itemView.findViewById(R.id.txtCustomerName);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtWaitValue = itemView.findViewById(R.id.txtWaitValue);

            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnWait = itemView.findViewById(R.id.btnWait);
            btnConfirmWait = itemView.findViewById(R.id.btnConfirmWait);

            layoutWaitTime = itemView.findViewById(R.id.layoutWaitTime);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            seekWaitTime = itemView.findViewById(R.id.seekWaitTime);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType){

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_request,parent,false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,int position){

        ServiceRequest req = list.get(position);

        holder.txtCustomerName.setText(req.customerName);
        holder.txtAddress.setText("Tap accept to view location");

        // RESET UI state
        holder.layoutWaitTime.setVisibility(View.GONE);
        holder.layoutActionButtons.setVisibility(View.VISIBLE);

        holder.btnAccept.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("service_requests")
                    .document(req.requestId)
                    .update("status","accepted");
            
            sendNotification(req.customerId, "Request Accepted", req.providerName + " has accepted your service request.");
        });

        holder.btnReject.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("service_requests")
                    .document(req.requestId)
                    .update("status","rejected");

            sendNotification(req.customerId, "Request Rejected", req.providerName + " has rejected your service request.");
        });

        // WAIT BUTTON LOGIC
        holder.btnWait.setOnClickListener(v -> {
            holder.layoutWaitTime.setVisibility(View.VISIBLE);
            holder.layoutActionButtons.setVisibility(View.GONE);
        });

        holder.seekWaitTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                holder.txtWaitValue.setText(progress + " min");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        holder.btnConfirmWait.setOnClickListener(v -> {
            int waitMinutes = holder.seekWaitTime.getProgress();
            if (waitMinutes == 0) waitMinutes = 1; // Minimum 1 minute if selected 0

            int finalWaitMinutes = waitMinutes;
            FirebaseFirestore.getInstance()
                    .collection("service_requests")
                    .document(req.requestId)
                    .update("status", "waiting", "waitTime", finalWaitMinutes)
                    .addOnSuccessListener(unused -> {
                        sendNotification(req.customerId, "Provider Delayed", 
                                req.providerName + " want some time to come (" + finalWaitMinutes + " min)");
                        
                        holder.layoutWaitTime.setVisibility(View.GONE);
                        holder.layoutActionButtons.setVisibility(View.VISIBLE);
                        Toast.makeText(holder.itemView.getContext(), "Wait time sent to customer", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void sendNotification(String userId, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("notifications").add(notification);
    }

    @Override
    public int getItemCount(){
        return list.size();
    }

}
