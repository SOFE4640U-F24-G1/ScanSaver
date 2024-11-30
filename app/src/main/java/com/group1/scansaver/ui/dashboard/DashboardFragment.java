package com.group1.scansaver.ui.dashboard;

import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.group1.scansaver.databinding.FragmentDashboardBinding;

import com.group1.scansaver.AddItemActivity;

import com.group1.scansaver.dataobjects.Item;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private Button addItemButton;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final TextView textView = binding.textDashboard;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            textView.setText("Login to Retrive Scanned History");
            LinearLayout itemLayout = binding.scrollerInner;
            itemLayout.removeAllViews();
            addItemButton = root.findViewById(R.id.addNewItemButton);
            addItemButton.setVisibility(View.GONE);
        } else {

            dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

            addItemButton = root.findViewById(R.id.addNewItemButton);
            addItemButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), AddItemActivity.class);
                intent.putExtra("SCANNED_BARCODE", "");
                startActivity(intent);
            });

            populateItemCards(inflater);
        }


        ///////

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void populateItemCards(@NonNull LayoutInflater inflater) {
        LinearLayout itemLayout = binding.scrollerInner;
        itemLayout.removeAllViews();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("items")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Item item = document.toObject(Item.class);

                            if (item != null) {
                                View itemCard = inflater.inflate(R.layout.item_card, itemLayout, false);

                                TextView itemName = itemCard.findViewById(R.id.itemName);
                                TextView itemLowestPrice = itemCard.findViewById(R.id.itemPrice);
                                TextView itemUPC = itemCard.findViewById(R.id.upcCode);
                                ImageView itemIcon = itemCard.findViewById(R.id.imageView);
                                Button itemMapButton = itemCard.findViewById(R.id.viewOnMap);
                                Button deleteItemButton = itemCard.findViewById(R.id.deleteButton);
                                CheckBox favIcon = itemCard.findViewById(R.id.star);

                                itemName.setText(item.getNAME());
                                itemLowestPrice.setText("$" + item.getPRICE());
                                itemUPC.setText(item.getUPC());

                                String imgURL = item.getITEM_IMAGEURL();
                                String storeName = item.getITEM_LOCATION();

                                if(imgURL != null){
                                    if(!imgURL.isEmpty()|| imgURL.compareTo("N/A") != 0){

                                        executorService.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(item.getITEM_IMAGEURL()).getContent());
                                                    requireActivity().runOnUiThread(() -> itemIcon.setImageBitmap(bitmap));
                                                } catch (MalformedURLException e) {
                                                    Log.e("IMGURL",e.getMessage());
                                                } catch (IOException e) {
                                                    Log.e("IMGURL",e.getMessage());
                                                }
                                            }

                                        });
                                    }
                                }else{Log.e("IMGURL","NULL URL");}

                                try(ItemsDBHandlerLocal dbHelper = new ItemsDBHandlerLocal(getContext())) {
                                    boolean isFavorite = dbHelper.isFavorite(item.getUPC());
                                    favIcon.setChecked(isFavorite);

                                    favIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                        if (isChecked) {
                                            dbHelper.addFavorite(item);
                                            Log.d("Favorites", "Item added to favorites");
                                        } else {
                                            dbHelper.removeFavorite(item.getUPC());
                                            Log.d("Favorites", "Item removed from favorites");
                                        }
                                    });
                                }catch (Exception e){

                                }


                                itemMapButton.setOnClickListener(v -> {

                                    Intent intent = new Intent(getActivity(), MapActivity.class); // START MAP ACTIVITY SEND GEO DATA TO IT
                                    intent.putExtra("store_name", storeName);
                                    startActivity(intent);
                                });

                                deleteItemButton.setOnClickListener(v ->{
                                    FirestoreHandler firestoreHandler = new FirestoreHandler();
                                    String upc = item.getUPC();

                                    firestoreHandler.deleteItemFromFirestore(upc);

                                    itemLayout.removeView(itemCard);
                                    Toast.makeText(getContext(), "Item deleted successfully", Toast.LENGTH_SHORT).show();
                                });

                                itemLayout.addView(itemCard);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error getting items", e);
                    });
        } else {
            Log.w("Auth", "No user is currently signed in.");
        }
    }
}