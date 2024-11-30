package com.group1.scansaver.api;

import android.util.Log;

import com.group1.scansaver.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UPCApiRequest {

    public interface UPCApiResponseCallback {
        void onSuccess(String barcode, String title, String msrp, String stores, String imgUrl);
        void onError(String error);
    }

    public void fetchProductDetails(String barcode, UPCApiResponseCallback callback) {

        String API_URL = "https://api.upcdatabase.org/product/";

        API_URL = API_URL + barcode;

        Log.e("API","API URL: "+API_URL);

        OkHttpClient client = new OkHttpClient();

        // NOT REAL KEY, KEY MUST GO IN SAFE SPACE
        String API_KEY = BuildConfig.API_KEY; // API KEY

        Request request = new Request.Builder()
                .url(API_URL+"?apikey="+API_KEY)
                //.addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.e("API", "Response Body: " + responseBody);
                    parseResponse(responseBody, callback);
                } else {
                    String errorBody = response.body().string();
                    callback.onError("Request failed with code: " + response.code() + ", body: " + errorBody);
                    Log.e("API", "FAILURE-API: " + response.code() + ", Body: " + errorBody);
                    //CAN HANDLE INVALID UPCS HERE
                }
                //LOGGING BELOW
            } catch (IOException e) {
                Log.e("API", "IOException occurred: " + e.getMessage());
                callback.onError("IOException: " + e.getMessage());
            } catch (Exception e) {
                Log.e("API", "Unexpected Exception: " + e.getMessage());
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }

    private void parseResponse(String responseBody, UPCApiResponseCallback callback) {
        try {
            if (!responseBody.trim().startsWith("{")) {
                int jsonStart = responseBody.indexOf("{");
                if (jsonStart != -1) {
                    responseBody = responseBody.substring(jsonStart);
                } else {
                    throw new JSONException("No valid JSON found in response.");
                }
            }

            JSONObject jsonResponse = new JSONObject(responseBody);

            String barcode = jsonResponse.optString("barcode", "N/A");
            String title = jsonResponse.optString("title", "N/A");
            String msrp = jsonResponse.optString("msrp", "N/A");
            String store = "";
            String imageURL = "";

            if(!jsonResponse.optString("metadata").isEmpty()){
               try{
                   JSONObject meta = new JSONObject(jsonResponse.optString("metadata"));
                   store = meta.optString("stores","N/A");
                   imageURL = meta.optString("images","N/A");
               }catch(Exception e){
                 // If needed do something here
               }

            }

            Log.e("API", "store value:"+store);
            Log.e("API", "url value:"+imageURL);

            callback.onSuccess(barcode, title, msrp, store, imageURL);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onError("Failed to parse response: " + e.getMessage());
        }
    }

    public List<double[]> getStoreLocations(String storeName, double userLat, double userLong) {
        List<double[]> results = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();

        double radiusKm = 20.0;

        Log.e("API", "UserLat: "+userLat+" UserLong: "+userLong);
        // Calculate the bounding box
        double[] boundingBox = calculateBoundingBox(userLat, userLong, radiusKm);
        String viewbox = boundingBox[0] + "," + boundingBox[1] + "," + boundingBox[2] + "," + boundingBox[3];

        String bounded = "1"; // Limit results to the specified bounding box
        String countryCode = "ca"; // Canada

        String url = "https://nominatim.openstreetmap.org/search?q=" + storeName +
                "&format=json&addressdetails=1&viewbox=" + viewbox + "&bounded=" + bounded +
                "&lat=" + userLat + "&lon=" + userLong +
                "&countrycodes=" + countryCode;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "ScanSaver/1.0 (juliano1@live.ca)")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String jsonData = response.body().string();
                Log.e("API Response", jsonData);
                JSONArray jsonArray = new JSONArray(jsonData);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject location = jsonArray.getJSONObject(i);
                    double latitude = location.getDouble("lat");
                    double longitude = location.getDouble("lon");

                    results.add(new double[]{latitude, longitude});
                }
            } else {
                Log.e("API", "Failed API call");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }


    private double[] calculateBoundingBox(double lat, double lon, double radiusKm) {
        final double earthRadiusKm = 6371.1370;

        double latOffset = Math.toDegrees(radiusKm / earthRadiusKm);
        double lonOffset = Math.toDegrees(radiusKm / (earthRadiusKm * Math.cos(Math.toRadians(lat))));

        double minLat = lat - latOffset;
        double maxLat = lat + latOffset;
        double minLon = lon - lonOffset;
        double maxLon = lon + lonOffset;

        return new double[]{minLat, maxLat, minLon, maxLon};
    }

}

