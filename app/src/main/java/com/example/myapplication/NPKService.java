package com.example.myapplication;

import android.content.Context;

public class NPKService {

    // Callback interface to handle prediction results
    public interface PredictionCallback {
        void onPredictionReceived(float n, float p, float k);
        void onError(String error); // Optional error callback
    }

    // Method to predict NPK using TensorFlow Lite model
    public void predictNPK(Context context, float[] inputFeatures, PredictionCallback callback) {
        try {
            NPKPredictor predictor = new NPKPredictor(context);
            float[] results = predictor.predictNPK(inputFeatures);

            if (results != null && results.length == 3) {
                callback.onPredictionReceived(results[0], results[1], results[2]);
            } else {
                callback.onError("Invalid prediction result");
            }

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Prediction failed: " + e.getMessage());
        }
    }
}