package com.example.myapplication;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class NPKPredictor {
    private static final String MODEL_NAME = "npk_model.tflite";
    private Interpreter interpreter;

    // Constructor that takes Context as a parameter
    public NPKPredictor(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd(MODEL_NAME).getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = context.getAssets().openFd(MODEL_NAME).getStartOffset();
        long declaredLength = context.getAssets().openFd(MODEL_NAME).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] predictNPK(float[] inputFeatures) {
        float[][] input = new float[1][inputFeatures.length];
        input[0] = inputFeatures;

        float[][] output = new float[1][3];  // N, P, K values
        interpreter.run(input, output);

        return output[0];
    }
}