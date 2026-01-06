package com.programminghut.yolo_deploy;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class MouthFootBodyClassifier {
    private static final int IMAGE_SIZE = 224;
    private static final int NUM_CHANNELS = 3;
    private final Interpreter interpreter;
    public MouthFootBodyClassifier(AssetManager assetManager, String modelPath) throws IOException {
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath));
    }
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private ByteBuffer preprocessImage(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * IMAGE_SIZE * IMAGE_SIZE * NUM_CHANNELS * 4); // 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder());

        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int pixel = resized.getPixel(x, y);
                inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
                inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
                inputBuffer.putFloat((pixel & 0xFF) / 255.0f);         // B
            }
        }

        inputBuffer.rewind();
        return inputBuffer;
    }


    public Result classify(Bitmap bitmap) {
        float[][][][] input = preprocessImageTo4DFloatArray(bitmap); // Shape: [1][224][224][3]
        float[][] output = new float[1][1];

        interpreter.run(input, output); // Run inference

        float prediction = output[0][0];
        String label = prediction < 0.5f ? "Foot/Mouth" : "Body";
        float confidence = prediction < 0.5f ? 1 - prediction : prediction;

        Log.d("Model Output", "Raw prediction: " + prediction + ", Label: " + label + ", Confidence: " + confidence);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        Log.d("Input Shape", Arrays.toString(inputShape)); // Should be [1, 224, 224, 3]

        return new Result(label, confidence);
    }

    private float[][][][] preprocessImageTo4DFloatArray(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        float[][][][] input = new float[1][IMAGE_SIZE][IMAGE_SIZE][NUM_CHANNELS]; // NHWC

        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int pixel = resized.getPixel(x, y);

                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;         // B
            }
        }
        return input;
    }


    public static class Result {
        public final String label;
        public final float confidence;

        public Result(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}
