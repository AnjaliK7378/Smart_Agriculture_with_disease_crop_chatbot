package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class CropRecommendationActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private EditText inputN, inputP, inputK, inputPh, inputLocation;
    private Spinner spinnerSeason;
    private Button btnFindCrops;
    private CardView cardRecommendationResult;
    private TextView txtRecommendedCrop, txtRecommendationReason, txtOtherOptions;

    private Retrofit retrofit;
    private CropApi cropApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_recommendation);

        initializeViews();
        setupSpinner();
        setupRetrofit();
        setupBottomNavigation();
        setupButtonListener();
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        inputN = findViewById(R.id.inputN);
        inputP = findViewById(R.id.inputP);
        inputK = findViewById(R.id.inputK);
        inputPh = findViewById(R.id.inputPh);
        inputLocation = findViewById(R.id.inputLocation);
        spinnerSeason = findViewById(R.id.spinnerSeason);
        btnFindCrops = findViewById(R.id.btnFindCrops);
        cardRecommendationResult = findViewById(R.id.cardRecommendationResult);
        txtRecommendedCrop = findViewById(R.id.txtRecommendedCrop);
        txtRecommendationReason = findViewById(R.id.txtRecommendationReason);
        txtOtherOptions = findViewById(R.id.txtOtherOptions);
        cardRecommendationResult.setVisibility(View.GONE);
    }

    private void setupSpinner() {
        String[] seasons = {"Select Season", "Kharif (Monsoon)", "Rabi (Winter)", "Zaid (Summer)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, seasons);
        spinnerSeason.setAdapter(adapter);
    }

    private void setupRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl("http://xxx.xxx.xxx.xxx:xxxx/") // CHANGE TO YOUR PC/RPi IP
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        cropApi = retrofit.create(CropApi.class);
    }

    private void setupButtonListener() {
        btnFindCrops.setOnClickListener(v -> {
            if (validateInputs()) {
                predictCrop();
            }
        });
    }

    private boolean validateInputs() {
        if (inputN.getText().toString().trim().isEmpty() ||
                inputP.getText().toString().trim().isEmpty() ||
                inputK.getText().toString().trim().isEmpty() ||
                inputPh.getText().toString().trim().isEmpty() ||
                inputLocation.getText().toString().trim().isEmpty() ||
                spinnerSeason.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void predictCrop() {
        double N = Double.parseDouble(inputN.getText().toString().trim());
        double P = Double.parseDouble(inputP.getText().toString().trim());
        double K = Double.parseDouble(inputK.getText().toString().trim());
        double ph = Double.parseDouble(inputPh.getText().toString().trim());
        String location = inputLocation.getText().toString().trim();
        String seasonFull = spinnerSeason.getSelectedItem().toString();
        String season = seasonFull.contains("Kharif") ? "Kharif" :
                seasonFull.contains("Rabi") ? "Rabi" :
                        seasonFull.contains("Zaid") ? "Zaid" : "Unknown";

        CropRequest request = new CropRequest(N, P, K, ph, location, season);

        btnFindCrops.setEnabled(false);
        Toast.makeText(this, "Predicting...", Toast.LENGTH_SHORT).show();

        cropApi.predictCrop(request).enqueue(new Callback<CropResponse>() {
            @Override
            public void onResponse(Call<CropResponse> call, Response<CropResponse> response) {
                btnFindCrops.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    displayRecommendation(response.body());
                } else {
                    Toast.makeText(CropRecommendationActivity.this, "Server error. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CropResponse> call, Throwable t) {
                btnFindCrops.setEnabled(true);
                Toast.makeText(CropRecommendationActivity.this, "Check internet or server IP", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayRecommendation(CropResponse result) {
        cardRecommendationResult.setVisibility(View.VISIBLE);
        txtRecommendedCrop.setText("Crop: " + result.recommended_crop);
        txtRecommendationReason.setText("Reason: " + result.reason);

        if (result.alternatives != null && result.alternatives.length > 0) {
            txtOtherOptions.setText("Other options: " + String.join(", ", Arrays.asList(result.alternatives)));
        } else {
            txtOtherOptions.setText("Other options: None");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_crops);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_predict) {
                startActivity(new Intent(this, DiseasePredictionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_crops) {
                return true;
            } else if (itemId == R.id.nav_chat) {
                startActivity(new Intent(this, ChatbotActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    // === Retrofit Models ===
    public static class CropRequest {
        public double N, P, K, ph;
        public String location, season;

        public CropRequest(double N, double P, double K, double ph, String location, String season) {
            this.N = N; this.P = P; this.K = K; this.ph = ph;
            this.location = location; this.season = season;
        }
    }

    public static class CropResponse {
        public String recommended_crop;
        public String reason;
        public String[] alternatives;
    }

    public interface CropApi {
        @POST("/predict_crop")
        Call<CropResponse> predictCrop(@Body CropRequest request);
    }
}
