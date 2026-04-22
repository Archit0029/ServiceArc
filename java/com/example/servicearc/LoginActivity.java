package com.example.servicearc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "LOGIN_DEBUG";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private CardView btnGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        CardView loginCard = findViewById(R.id.cardLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogle);

        // Animation
        if (loginCard != null) {
            loginCard.setAlpha(0f);
            loginCard.setTranslationY(100f);
            loginCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .start();
        }

        // Google Sign-In Configuration
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                // Force account chooser each login click
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> signIn());
            });
        }

        Log.d("FIREBASE_PROJECT",
                FirebaseFirestore.getInstance().getApp().getOptions().getProjectId());
    }

    // 🔥 Auto login only if session exists
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "Existing session found");
            checkUserInFirestore();
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account =
                        task.getResult(ApiException.class);

                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed", e);
                Toast.makeText(this,
                        "Google Sign-In Failed",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication successful");
                        checkUserInFirestore();
                    } else {
                        Log.e(TAG, "Authentication failed", task.getException());
                        Toast.makeText(this,
                                "Authentication Failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserInFirestore() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String role = documentSnapshot.getString("role");
                        Log.d(TAG, "User role: " + role);

                        if ("customer".equals(role)) {

                            startActivity(new Intent(this,
                                    CustomerDashboardActivity.class));

                        } else if ("provider".equals(role)) {

                            startActivity(new Intent(this,
                                    ProviderDashboardActivity.class));

                        } else {

                            startActivity(new Intent(this,
                                    PhoneEntryActivity.class));
                        }

                    } else {

                        // New user
                        startActivity(new Intent(this,
                                PhoneEntryActivity.class));
                    }

                    finish();
                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Firestore error", e);

                    Toast.makeText(this,
                            "Database Error",
                            Toast.LENGTH_SHORT).show();
                });
    }
}