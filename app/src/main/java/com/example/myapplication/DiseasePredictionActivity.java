package com.example.myapplication;

// --- ADDED IMPORTS ---
import com.example.myapplication.BuildConfig; // For the secure API key
import android.widget.ProgressBar;             // For the loading indicator
import android.view.View;                       // For View.VISIBLE/GONE
// --------------------

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DiseasePredictionActivity extends AppCompatActivity {

    private static final String TAG = "DiseasePredictionActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    // --- API Configuration ---

    // 1. --- SECURE API KEY ---
    private static final String API_KEY = BuildConfig.API_KEY;

    // 2. --- CORRECTED URL ---

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
    private RequestQueue requestQueue;

    private BottomNavigationView bottomNavigationView;
    private Button btnUploadPhoto;
    private CardView cardResult;
    private TextView txtPredictionHistory, txtDiseaseName, txtTreatment;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_prediction);

        requestQueue = Volley.newRequestQueue(this);

        initializeViews();
        setupBottomNavigation();
        setupButtons();
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        cardResult = findViewById(R.id.cardResult);
        txtPredictionHistory = findViewById(R.id.txtPredictionHistory);
        txtDiseaseName = findViewById(R.id.txtDiseaseName);
        txtTreatment = findViewById(R.id.txtTreatment);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupButtons() {
        btnUploadPhoto.setOnClickListener(v -> {
            openImageChooser();
        });

        txtPredictionHistory.setOnClickListener(v -> {
            Toast.makeText(this, "Launching Prediction History screen...", Toast.LENGTH_SHORT).show();
            // Implement intent to launch history screen here
        });
    }

    // --- Image Selection Logic ---

    private void openImageChooser() {
        cardResult.setVisibility(View.GONE); // Clear previous results

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Plant Image"), PICK_IMAGE_REQUEST);
    }

    private String getCurrentLanguage() {
        String lang = getIntent().getStringExtra("lang");
        return lang != null ? lang : "en";
    }

    private String translateIfNeeded(String text) {
        String lang = getCurrentLanguage();
        if ("hi".equals(lang) || "mr".equals(lang)) {
            // Simple keyword-based translation (expand as needed)
            text = text.replace("Disease:", "रोग:")
                    .replace("Treatment:", "उपचार:")
                    .replace("leaf", "पत्ता")
                    .replace("blight", "झुलसा")
                    .replace("healthy", "स्वस्थ");
            if ("mr".equals(lang)) {
                text = text.replace("रोग", "आजार")
                        .replace("उपचार", "इलाज");
            }
        }
        return text;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                String base64Image = bitmapToBase64(bitmap);

                if (base64Image != null) {
                    // Show progress bar and disable button
                    progressBar.setVisibility(View.VISIBLE);
                    btnUploadPhoto.setEnabled(false);
                    predictDisease(base64Image);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to load image: " + e.getMessage());
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // 3. --- CORRECTED BASE64 ENCODING ---
        // This fixes the "Base64 decoding failed" (400) error.
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    // --- Gemini Prediction Logic ---

    private void predictDisease(String base64Image) {

        String diagnosisPrompt = "Analyze this image of a plant part. Identify the disease, and provide a clear, concise treatment plan and prevention steps for a farmer. Start your response with 'Disease: [DISEASE NAME]' on the first line.";

        try {
            JSONObject textPart = new JSONObject().put("text", diagnosisPrompt);
            JSONObject imagePart = new JSONObject()
                    .put("inlineData", new JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", base64Image));

            JSONArray parts = new JSONArray().put(textPart).put(imagePart);
            JSONObject contents = new JSONObject().put("parts", parts);
            JSONObject payload = new JSONObject().put("contents", new JSONArray().put(contents));

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GEMINI_API_URL, payload,
                    response -> {
                        // Hide progress and re-enable button
                        progressBar.setVisibility(View.GONE);
                        btnUploadPhoto.setEnabled(true);

                        try {
                            String geminiResponse = extractGeminiResponse(response);
                            displayPredictionResult(geminiResponse);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing Gemini JSON response: " + e.getMessage());
                            displayError("API response parsing failed:\n" + e.getMessage());
                        }
                    },
                    error -> {
                        // Hide progress and re-enable button
                        progressBar.setVisibility(View.GONE);
                        btnUploadPhoto.setEnabled(true);

                        String errorMsg = (error.networkResponse != null && error.networkResponse.data != null)
                                ? new String(error.networkResponse.data) : "Unknown Network Error. Check connection or API Key.";
                        Log.e(TAG, "Gemini API Error: " + errorMsg);
                        displayError(errorMsg);
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            requestQueue.add(request);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON payload: " + e.getMessage());
            displayError("Internal error formatting request: " + e.getMessage());
        }
    }

    private String extractGeminiResponse(JSONObject response) throws JSONException {
        JSONArray candidates = response.getJSONArray("candidates");
        if (candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text");
            }
        }
        return "";
    }

    private void displayPredictionResult(String result) {
        if (!result.isEmpty()) {
            String diseaseName = "N/A - See below";
            String treatment = result;

            if (result.startsWith("Disease:")) {
                String[] lines = result.split("\n", 2);
                diseaseName = lines[0].replace("Disease:", "").trim();
                treatment = lines.length > 1 ? lines[1].trim() : "No specific treatment steps provided.";
            } else {
                treatment = result;
            }

            txtDiseaseName.setText("Disease: " + diseaseName);
            txtTreatment.setText(treatment);
            cardResult.setVisibility(View.VISIBLE);
        } else {
            displayError("AI returned an empty diagnosis.");
        }
    }

    private void displayError(String message) {
        txtDiseaseName.setText("Error");
        txtTreatment.setText("Prediction Failed: " + message);
        cardResult.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Prediction Failed.", Toast.LENGTH_LONG).show();
    }

    // --- Bottom Navigation Logic (Remains Unchanged) ---

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_predict);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashboard) {
                    startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_predict) {
                    return true;
                } else if (itemId == R.id.nav_crops) {
                    startActivity(new Intent(getApplicationContext(), CropRecommendationActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_chat) {
                    startActivity(new Intent(getApplicationContext(), ChatbotActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }
}
