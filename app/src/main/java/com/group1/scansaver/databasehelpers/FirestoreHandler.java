package com.group1.scansaver.databasehelpers;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.group1.scansaver.SignUpActivity;
import com.group1.scansaver.dataobjects.Item;

import java.util.concurrent.CompletableFuture;

public class FirestoreHandler {

    private FirebaseFirestore firestore;

    public FirestoreHandler(){
        firestore = FirebaseFirestore.getInstance();
    }

    public void insertItemIntoFirestore(Item item) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            firestore.collection("items")
                    .document(String.valueOf(item.getUPC()))
                    .set(item)
                    .addOnSuccessListener(aVoid -> {
                        Log.e("FIRE", "Storing Item Success");
                        firestore.collection("items")
                                .document(String.valueOf(item.getUPC()))
                                .update("userId", userId)
                                .addOnSuccessListener(aVoid1 -> {
                                    Log.e("FIRE", "UserId added to the item.");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FIRE", "Failed to add userId to the item", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIRE", "Storing Item FAILURE", e);
                    });
        } else {
            Log.w("FIRE", "No user is currently signed in.");
        }
    }

    public void deleteItemFromFirestore(String upc) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            firestore.collection("items")
                    .document(upc)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.e("FIRE", "Item successfully deleted.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIRE", "Failed to delete item", e);
                    });
        } else {
            Log.w("FIRE", "No user is currently signed in.");
        }
    }

    public boolean doesItemExist(String upc) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            firestore.collection("items")
                    .document(upc)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            future.complete(true);
                        } else {
                            future.complete(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIRE", "Error checking item existence", e);
                        future.complete(false);
                    });

            try {
                return future.get();
            } catch (Exception e) {
                Log.e("FIRE", "Error waiting for item check", e);
                return false;
            }
        } else {
            Log.w("FIRE", "No user is currently signed in.");
            return false;
        }
    }

}
