package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather?";
    private static final String API_KEY = "0d7ff1e777e5972817cfe7a9afc86ce8";

    private final RequestQueue requestQueue;

    public WeatherService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public void getCurrentWeather(String city, WeatherCallback callback) {
        String url = BASE_URL + "q=" + city + "&appid=" + API_KEY + "&units=metric";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
                        JSONObject wind = response.getJSONObject("wind");

                        WeatherData weatherData = new WeatherData(
                                city,
                                main.getDouble("temp"),
                                main.getInt("humidity"),
                                wind.getDouble("speed"),
                                weather.getString("description"),
                                weather.getString("icon")
                        );

                        callback.onSuccess(weatherData);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing weather data", e);
                        callback.onError("Error parsing weather data");
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching weather data", error);
                    callback.onError(error.getMessage());
                }
        );

        requestQueue.add(request);
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String error);
    }

    public static class WeatherData {
        public final String city;
        public final double temperature;
        public final int humidity;
        public final double windSpeed;
        public final String condition;
        public final String iconCode;

        public WeatherData(String city, double temperature, int humidity,
                           double windSpeed, String condition, String iconCode) {
            this.city = city;
            this.temperature = temperature;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.condition = condition;
            this.iconCode = iconCode;
        }
    }
}