package com.group1.scansaver.ui.home;

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
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group1.scansaver.LoginActivity;
import com.group1.scansaver.databinding.FragmentHomeBinding;
import com.group1.scansaver.MapActivity;
import com.group1.scansaver.R;
import com.group1.scansaver.databasehelpers.ItemsDBHandlerLocal;

import com.group1.scansaver.dataobjects.Item;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView homeText = binding.textHome;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null){
            homeText.setText("Sign in To View Home Page Favourites");
            LinearLayout itemLayout = binding.scrollerInner;
            itemLayout.removeAllViews();
            //WHAT YOU WANT USER TO SEE WHEN SIGNED OUT GOES HERE
        }else{
            homeText.setText("Favourite Items"); //WHAT YOU WANT USER TO SEE WHEN SIGNED IN GOES HERE
            populateItemCards(inflater);
        }


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

        ItemsDBHandlerLocal database = new ItemsDBHandlerLocal(getContext());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            List<Item> itemList = database.getAllFavorites();

            for (Item item : itemList) {
                View itemCard = inflater.inflate(R.layout.item_card, itemLayout, false);

                TextView itemName = itemCard.findViewById(R.id.itemName);
                TextView itemLowestPrice = itemCard.findViewById(R.id.itemPrice);
                TextView itemUPC = itemCard.findViewById(R.id.upcCode);
                ImageView itemIcon = itemCard.findViewById(R.id.imageView);
                Button itemMapButton = itemCard.findViewById(R.id.viewOnMap);
                Button deleteItemButton = itemCard.findViewById(R.id.deleteButton);
                CheckBox favIcon = itemCard.findViewById(R.id.star);

                favIcon.setChecked(true);
                favIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!isChecked) {
                        favIcon.setChecked(true);
                    }
                });

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


                itemMapButton.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), MapActivity.class); // START MAP ACTIVITY SEND GEO DATA TO IT
                    intent.putExtra("store_name", storeName);
                    startActivity(intent);
                });

                deleteItemButton.setOnClickListener(v -> {
                    database.removeFavorite(item.getUPC());
                    itemLayout.removeView(itemCard);
                });


                itemLayout.addView(itemCard);
            }
        }
    }
}
