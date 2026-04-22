package com.example.servicearc;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    List<ServiceRequest> requestList;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public RequestAdapter(List<ServiceRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_request, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ServiceRequest request = requestList.get(position);

        holder.txtName.setText(
                request.customerName != null ? request.customerName : "Unknown Customer"
        );

        holder.txtAddress.setText(
                request.customerAddress != null ? request.customerAddress : "Address not available"
        );

        holder.txtPhone.setText(
                request.customerPhone != null ? request.customerPhone : "No phone"
        );

        holder.txtDistance.setText(
                request.distance > 0 ?
                        String.format("%.1f km away", request.distance) :
                        "Distance unknown"
        );

        // ------------------------
        // CHECK IF REQUEST EXPIRED
        // ------------------------
        long currentTime = System.currentTimeMillis();

        if(request.expireAt != 0 && currentTime > request.expireAt){

            db.collection("service_requests")
                    .document(request.requestId)
                    .update("status","expired");

            holder.btnAccept.setEnabled(false);
            holder.btnReject.setEnabled(false);

            holder.txtDistance.setText("Request expired");

            return;
        }

        // ------------------------
        // ACCEPT REQUEST
        // ------------------------
        holder.btnAccept.setOnClickListener(v -> {

            db.collection("service_requests")
                    .document(request.requestId)
                    .update("status", "accepted")
                    .addOnSuccessListener(unused -> {

                        Context context = v.getContext();

                        if(request.customerLat == 0 || request.customerLng == 0){

                            Toast.makeText(context,
                                    "Customer location not available",
                                    Toast.LENGTH_LONG).show();

                            return;
                        }

                        Toast.makeText(context,
                                "Request accepted",
                                Toast.LENGTH_SHORT).show();

                        Intent intent =
                                new Intent(context, ProviderMapActivity.class);

                        intent.putExtra("customerLat", request.customerLat);
                        intent.putExtra("customerLng", request.customerLng);

                        intent.putExtra("customerName", request.customerName);
                        intent.putExtra("customerPhone", request.customerPhone);
                        intent.putExtra("customerAddress", request.customerAddress);

                        context.startActivity(intent);
                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(v.getContext(),
                                "Failed to accept request",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        // ------------------------
        // REJECT REQUEST
        // ------------------------
        holder.btnReject.setOnClickListener(v -> {

            db.collection("service_requests")
                    .document(request.requestId)
                    .update("status", "rejected")
                    .addOnSuccessListener(unused -> {

                        Toast.makeText(v.getContext(),
                                "Request rejected",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(v.getContext(),
                                "Failed to reject request",
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    // ------------------------
    // VIEW HOLDER
    // ------------------------
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtName;
        TextView txtAddress;
        TextView txtPhone;
        TextView txtDistance;

        Button btnAccept;
        Button btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtCustomerName);
            txtAddress = itemView.findViewById(R.id.txtCustomerAddress);
            txtPhone = itemView.findViewById(R.id.txtCustomerPhone);
            txtDistance = itemView.findViewById(R.id.txtDistance);

            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}