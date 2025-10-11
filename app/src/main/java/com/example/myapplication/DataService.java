package com.example.myapplication;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataService {
    private static final String THINGSPEAK_CHANNEL_ID = "2958025";
    private static final String THINGSPEAK_READ_API_KEY = "I8R19U2FG4Q5O9TY";

    private static final int FIELD_SOIL_MOISTURE = 1;
    private static final int FIELD_TEMPERATURE = 2;
    private static final int FIELD_HUMIDITY = 3;
    private static final int FIELD_RAIN_STATUS = 4;
    private static final int FIELD_PUMP_STATUS = 5;
    private static final int FIELD_WEATHER_DESCRIPTION = 6;
    private static final int FIELD_PH_LEVEL = 7;
    private static final int FIELD_NPK_LEVEL = 8;

    private static final String THINGSPEAK_URL =
            "https://api.thingspeak.com/channels/" + THINGSPEAK_CHANNEL_ID +
                    "/feeds.json?api_key=" + THINGSPEAK_READ_API_KEY + "&results=1";

    public interface DataCallback {
        void onDataReceived(SoilData data);
        void onError(String message);
    }

    public static void fetchLatestData(Context context, DataCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, THINGSPEAK_URL, null,
                response -> {
                    try {
                        if (response.has("error")) {
                            handleThingSpeakError(response, callback);
                        } else {
                            JSONArray feeds = response.getJSONArray("feeds");
                            if (feeds.length() > 0) {
                                JSONObject latest = feeds.getJSONObject(0);

                                SoilData data = new SoilData(
                                        latest.optString("field" + FIELD_SOIL_MOISTURE),
                                        latest.optString("field" + FIELD_TEMPERATURE),
                                        latest.optString("field" + FIELD_HUMIDITY),
                                        latest.optString("field" + FIELD_RAIN_STATUS),
                                        latest.optString("field" + FIELD_PUMP_STATUS),
                                        latest.optString("field" + FIELD_WEATHER_DESCRIPTION),
                                        latest.optString("field" + FIELD_PH_LEVEL),
                                        latest.optString("field" + FIELD_NPK_LEVEL)
                                );

                                callback.onDataReceived(data);
                            } else {
                                callback.onError("No data available");
                            }
                        }
                    } catch (Exception e) {
                        callback.onError("Unexpected error: " + e.getMessage());
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        callback.onError("HTTP " + error.networkResponse.statusCode);
                    } else {
                        callback.onError("Network error: " + error.getMessage());
                    }
                }
        );

        queue.add(request);
    }

    private static void handleThingSpeakError(JSONObject response, DataCallback callback) {
        try {
            if (response.has("error")) {
                String error = response.getString("error");
                callback.onError("ThingSpeak error: " + error);
            }
        } catch (JSONException e) {
            callback.onError("Error parsing error message");
        }
    }

    public static class SoilData {
        public String soilMoisture;
        public String temperature;
        public String humidity;
        public String rainStatus;
        public String pumpStatus;
        public String weatherDescription;
        public String phLevel;
        public String npkLevel;

        public SoilData(String soilMoisture, String temperature, String humidity,
                        String rainStatus, String pumpStatus, String weatherDescription,
                        String phLevel, String npkLevel) {
            this.soilMoisture = soilMoisture;
            this.temperature = temperature;
            this.humidity = humidity;
            this.rainStatus = rainStatus;
            this.pumpStatus = pumpStatus;
            this.weatherDescription = weatherDescription;
            this.phLevel = phLevel;
            this.npkLevel = npkLevel;
        }
    }
}