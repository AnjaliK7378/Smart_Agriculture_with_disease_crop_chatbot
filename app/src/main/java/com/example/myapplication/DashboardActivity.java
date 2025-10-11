package com.example.myapplication;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.view.MenuItem;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private static final String TAG = "AgroSenseDashboard";
    private TextView txtTemperature, txtHumidity, txtSoilMoisture, txtIrrigationStatus,
            txtFertilizerStatus, txtLastIrrigation, txtLastFertilization, txtPhValue,
            txtNitrogen, txtPhosphorus, txtPotassium, txtLastUpdated;
    private LinearLayout forecastLayout;
    private ToggleButton toggleAuto, toggleManual;
    private LinearLayout manualControlLayout;
    private Button btnMotorOn, btnMotorOff, btnRefresh;
    private RequestQueue requestQueue;
    private static final String THING_SPEAK_URL = "https://api.thingspeak.com/channels/2958025/feeds.json?api_key=I8R19U2FG4Q5O9TY&results=1";
    private static final String WEATHER_API_KEY = "0d7ff1e777e5972817cfe7a9afc86ce8";
    private static final String WEATHER_FORECAST_BASE_URL = "https://api.openweathermap.org/data/2.5/forecast?";
    private static final String CITY_NAME = "Pune,IN";
    private static final String THINGSPEAK_WRITE_URL = "https://api.thingspeak.com/update";
    private static final String WRITE_API_KEY = "C1UC1TCPKCSQ4NTN";

    private static final String ESP8266_IP = "http://10.66.240.53"; // ESP IP
    private static final String MOTOR_ENDPOINT = "/motor?state=";
    private boolean shouldPromptIrrigation = false;
    private boolean willRain = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        initializeViews();
        setupToggleButtons();
        requestQueue = Volley.newRequestQueue(this);

        fetchThingSpeakData();
        fetchWeatherForecast();
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
            fetchThingSpeakData();
            fetchWeatherForecast();
        });
    }
    private void initializeViews() {
        txtSoilMoisture = findViewById(R.id.txtSoilMoisture);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtHumidity = findViewById(R.id.txtHumidity);
        txtPhValue = findViewById(R.id.txtPhValue);
        txtIrrigationStatus = findViewById(R.id.txtIrrigationStatus);
        txtFertilizerStatus = findViewById(R.id.txtFertilizerStatus);
        txtLastIrrigation = findViewById(R.id.txtLastIrrigation);
        txtLastFertilization = findViewById(R.id.txtLastFertilization);
        txtLastUpdated = findViewById(R.id.txtLastUpdated);
        forecastLayout = findViewById(R.id.forecastLayout);

        txtNitrogen = findViewById(R.id.txtNitrogen);
        txtPhosphorus = findViewById(R.id.txtPhosphorus);
        txtPotassium = findViewById(R.id.txtPotassium);
        toggleAuto = findViewById(R.id.toggleAuto);
        toggleManual = findViewById(R.id.toggleManual);
        manualControlLayout = findViewById(R.id.manualControlLayout);
        btnMotorOn = findViewById(R.id.btnMotorOn);
        btnMotorOff = findViewById(R.id.btnMotorOff);
        btnRefresh = findViewById(R.id.btnRefresh);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.nav_dashboard) {
                    // Already on Dashboard, do nothing or re-scroll to top
                    return true;
                } else if (itemId == R.id.nav_predict) {
                    startActivity(new Intent(getApplicationContext(), DiseasePredictionActivity.class));
                    // Optional: remove transition animation
                    overridePendingTransition(0, 0);
                    finish();
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

        resetDisplayValues();
    }

    private void resetDisplayValues() {
        runOnUiThread(() -> {
            txtSoilMoisture.setText("N/A");
            txtTemperature.setText("N/A");
            txtHumidity.setText("N/A");
            txtPhValue.setText("N/A");
            txtNitrogen.setText("N/A ppm");
            txtPhosphorus.setText("N/A ppm");
            txtPotassium.setText("N/A ppm");
            txtIrrigationStatus.setText("Data Unavailable");
            txtFertilizerStatus.setText("Data Unavailable");
            txtLastUpdated.setText("N/A");
            forecastLayout.removeAllViews();
        });
    }

    private void fetchThingSpeakData() {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, THING_SPEAK_URL, null,
                response -> {
                    try {
                        Log.d(TAG, "ThingSpeak Response: " + response.toString());

                        JSONArray feeds = response.getJSONArray("feeds");
                        if (feeds.length() > 0) {
                            JSONObject latestFeed = feeds.getJSONObject(feeds.length() - 1);
                            String createdAt = latestFeed.optString("created_at", "N/A");
                            String soilMoisture = latestFeed.optString("field1", "N/A");
                            String temperature = latestFeed.optString("field2", "N/A");
                            String humidity = latestFeed.optString("field3", "N/A");
                            String ph = latestFeed.optString("field4", "N/A");
                            String n = latestFeed.optString("field5", "N/A");
                            String p = latestFeed.optString("field6", "N/A");
                            String k = latestFeed.optString("field7", "N/A");

                            Log.d(TAG, "Fetched data - Temperature: " + temperature + ", Humidity: " + humidity + ", Moisture: " + soilMoisture + ", pH: " + ph);

                            runOnUiThread(() -> updateSensorDataUI(createdAt, temperature, humidity, soilMoisture, ph, n, p, k));
                            if (toggleAuto.isChecked()) {
                                shouldPromptIrrigation = shouldPromptIrrigation(soilMoisture);
                                if (shouldPromptIrrigation && !willRain) {
                                    runOnUiThread(this::showIrrigationPrompt);
                                }
                            }

                        } else {
                            Toast.makeText(this, "No ThingSpeak feed data", Toast.LENGTH_SHORT).show();
                            resetDisplayValues();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing ThingSpeak JSON: " + e.getMessage());
                        Toast.makeText(this, "ThingSpeak data error", Toast.LENGTH_SHORT).show();
                        resetDisplayValues();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching ThingSpeak data: " + error.getMessage());
                    Toast.makeText(this, "Failed to fetch sensor data", Toast.LENGTH_SHORT).show();
                    resetDisplayValues();
                });
        requestQueue.add(request);
    }

    private void updateSensorDataUI(String createdAt, String temperature, String humidity, String moisture,
                                    String ph, String n, String p, String k) {
        txtTemperature.setText(temperature.equals("N/A") ? "N/A" : temperature + "°C");
        txtHumidity.setText(humidity.equals("N/A") ? "N/A" : humidity + "%");
        txtSoilMoisture.setText(moisture.equals("N/A") ? "N/A" : moisture + "%");
        txtPhValue.setText(ph.equals("N/A") ? "N/A" : ph);
        txtNitrogen.setText(n + " ppm");
        txtPhosphorus.setText(p + " ppm");
        txtPotassium.setText(k + " ppm");

        try {
            if (!moisture.equals("N/A")) {
                double moistureValue = Double.parseDouble(moisture);
                txtIrrigationStatus.setText(moistureValue < 30 ? "Irrigation Needed" : "Optimal");
            } else {
                txtIrrigationStatus.setText("Data Unavailable");
            }
        } catch (NumberFormatException e) {
            txtIrrigationStatus.setText("Invalid Data");
        }

        updateFertilizerStatus(n, p, k);

        String lastUpdate = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        txtLastIrrigation.setText("Last: " + lastUpdate);
        txtLastFertilization.setText("Last: " + lastUpdate);

        if (!createdAt.equals("N/A")) {
            try {
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = utcFormat.parse(createdAt);

                SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String localDate = localFormat.format(date);
                txtLastUpdated.setText(localDate);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing created_at timestamp: " + e.getMessage());
                txtLastUpdated.setText("Invalid Timestamp");
            }
        } else {
            txtLastUpdated.setText("N/A");
        }
    }

    private void updateFertilizerStatus(String n, String p, String k) {
        try {
            if (!n.equals("N/A") && !p.equals("N/A") && !k.equals("N/A")) {
                double nValue = Double.parseDouble(n);
                double pValue = Double.parseDouble(p);
                double kValue = Double.parseDouble(k);

                if (nValue < 20 || pValue < 15 || kValue < 25) {
                    txtFertilizerStatus.setText("Fertilization Needed");
                } else {
                    txtFertilizerStatus.setText("Optimal");
                }
            } else {
                txtFertilizerStatus.setText("Data Unavailable");
            }
        } catch (NumberFormatException e) {
            txtFertilizerStatus.setText("Invalid Data");
        }
    }

    private void fetchWeatherForecast() {
        String forecastUrl = WEATHER_FORECAST_BASE_URL + "q=" + CITY_NAME + "&appid=" + WEATHER_API_KEY + "&units=metric";

        JsonObjectRequest forecastRequest = new JsonObjectRequest(Request.Method.GET, forecastUrl, null,
                response -> {
                    try {
                        JSONArray list = response.getJSONArray("list");
                        forecastLayout.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(DashboardActivity.this);

                        willRain = false;

                        for (int i = 0; i < Math.min(list.length(), 3); i++) {
                            JSONObject forecast = list.getJSONObject(i);
                            JSONObject main = forecast.getJSONObject("main");
                            JSONArray weatherArray = forecast.getJSONArray("weather");
                            JSONObject weather = weatherArray.getJSONObject(0);
                            String description = weather.getString("description");
                            String temperature = String.format(Locale.getDefault(), "%.1f°C", main.getDouble("temp"));
                            String time = forecast.getString("dt_txt");

                            if (description.contains("rain")) {
                                willRain = true;
                            }

                            View forecastItemView = inflater.inflate(R.layout.item_forecast, forecastLayout, false);
                            TextView txtTime = forecastItemView.findViewById(R.id.txtTime);
                            TextView txtTemperature = forecastItemView.findViewById(R.id.txtTemperature);
                            TextView txtDescription = forecastItemView.findViewById(R.id.txtDescription);

                            txtTime.setText(time);
                            txtTemperature.setText(temperature);
                            txtDescription.setText(description);

                            forecastLayout.addView(forecastItemView);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing weather forecast JSON: " + e.getMessage());
                        Toast.makeText(DashboardActivity.this, "Weather forecast error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching weather forecast: " + error.getMessage());
                    Toast.makeText(DashboardActivity.this, "Failed to fetch weather forecast", Toast.LENGTH_SHORT).show();
                    resetDisplayValues();
                });
        requestQueue.add(forecastRequest);
    }
    private boolean shouldPromptIrrigation(String moisture) {
        try {
            if (!moisture.equals("N/A")) {
                double moistureValue = Double.parseDouble(moisture);
                return moistureValue < 30;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid moisture value");
        }
        return false;
    }
    private void showIrrigationPrompt() {
        new androidx.appcompat.app.AlertDialog.Builder(DashboardActivity.this)
                .setTitle("💧 Irrigation Needed")
                .setMessage("Soil moisture is low. Do you want to water the plant?")
                .setPositiveButton("Yes", (dialog, which) -> sendMotorCommand("on"))
                .setNegativeButton("No", null)
                .show();
    }
    private void setupToggleButtons() {
        toggleAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                toggleManual.setChecked(false);
                manualControlLayout.setVisibility(View.GONE);
                Toast.makeText(DashboardActivity.this, "Auto Mode ON", Toast.LENGTH_SHORT).show();

                //  Update field4 = 0 (auto mode)
                updateThingSpeakField(4, 0);

            } else {
                Toast.makeText(DashboardActivity.this, "Auto Mode OFF", Toast.LENGTH_SHORT).show();
            }
        });


        toggleManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                toggleAuto.setChecked(false);
                manualControlLayout.setVisibility(View.VISIBLE);
                Toast.makeText(DashboardActivity.this, "Manual Mode ON", Toast.LENGTH_SHORT).show();

                // Update field4 = 1 (manual mode)
                updateThingSpeakField(4, 1);

            } else {
                manualControlLayout.setVisibility(View.GONE);
                Toast.makeText(DashboardActivity.this, "Manual Mode OFF", Toast.LENGTH_SHORT).show();
            }
        });

        btnMotorOn.setOnClickListener(v -> {
            sendMotorCommand("on");
            updateThingSpeakField(5, 1);
        });

        btnMotorOff.setOnClickListener(v -> {
            sendMotorCommand("off");
            updateThingSpeakField(5, 0);
        });

    }

    private void updateThingSpeakField(int fieldNumber, int value) {
        String url = THINGSPEAK_WRITE_URL + "?api_key=" + WRITE_API_KEY + "&field" + fieldNumber + "=" + value;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "ThingSpeak field " + fieldNumber + " updated to " + value + ", response: " + response);
                    if ("0".equals(response.trim())) {
                        Log.e(TAG, "ThingSpeak update failed (response=0). Check API key or rate limit.");
                    }
                },
                error -> Log.e(TAG, "Failed to update ThingSpeak field " + fieldNumber + ": " + error.getMessage())
        );

        requestQueue.add(request);
    }


    private void sendMotorCommand(String state) {
        String url = ESP8266_IP + MOTOR_ENDPOINT + state;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Toast.makeText(DashboardActivity.this, "Motor turned " + state, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Motor " + state + " success: " + response);
                },
                error -> {
                    Toast.makeText(DashboardActivity.this, "Failed to turn motor " + state, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error: " + error.getMessage());
                });

        requestQueue.add(request);
    }

}