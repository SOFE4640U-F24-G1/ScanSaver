package com.group1.scansaver.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.group1.scansaver.LoginActivity;
import com.group1.scansaver.MapActivity;
import com.group1.scansaver.R;
import com.group1.scansaver.databasehelpers.FirestoreHandler;
import com.group1.scansaver.databasehelpers.ItemsDBHandlerLocal;
import com.group1.scansaver.databinding.FragmentProfileBinding;
import com.group1.scansaver.dataobjects.Item;
import com.group1.scansaver.dataobjects.User;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ConstraintLayout profileLayout = binding.profileLayout;

        TextView firstName = binding.fName;
        TextView lastName = binding.lName;
        TextView email = binding.email;
        TextView uName = binding.uName;

        ///////////
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            final TextView textView = binding.textProfile;
            profileViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
            profileLayout.setVisibility(View.GONE);

            binding.testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent testIntent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(testIntent);
                }
            });
        } else {

            final TextView profileTextView = binding.textProfile;
            profileTextView.setText("Welcome to your profile!");
            binding.testButton.setText("Logout");
            binding.testButton.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });


            String userId = currentUser.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            if (user != null) {
                                firstName.setText(user.getFirstName());
                                lastName.setText(user.getLastName());
                                email.setText(user.getEmail());
                                uName.setText(user.getUsername());
                            }
                        } else {
                            Log.e("Firestore", "User document does not exist");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error getting User", e);
                    });

        }
        ////////////

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}