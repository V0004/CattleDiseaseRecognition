package com.programminghut.yolo_deploy;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import androidx.annotation.NonNull;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class DiseasePredictor {
    private final Interpreter tflite;
    private static final String MODEL_FILE = "disease_predictor.tflite";

    public DiseasePredictor(@NonNull Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 
                fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    public Map<String, Float> predictDisease(@NonNull List<String> selectedSymptoms) {
        float[] inputVector = new float[SymptomManager.SYMPTOMS.size()];
        
        for (String symptom : selectedSymptoms) {
            int index = SymptomManager.SYMPTOMS.indexOf(symptom);
            if (index != -1) {
                inputVector[index] = 1.0f;
            }
        }

        float[][] outputProbabilities = new float[1][SymptomManager.DISEASE_CLASSES.size()];
        tflite.run(inputVector, outputProbabilities);

        return getTopDiseases(outputProbabilities[0]);
    }

    private Map<String, Float> getTopDiseases(float[] probabilities) {
        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<>(
                (a, b) -> Float.compare(b.getValue(), a.getValue())
        );

        for (int i = 0; i < SymptomManager.DISEASE_CLASSES.size(); i++) {
            pq.offer(new AbstractMap.SimpleEntry<>(SymptomManager.DISEASE_CLASSES.get(i), probabilities[i]));
        }

        Map<String, Float> topDiseases = new LinkedHashMap<>();
        for (int i = 0; i < 3 && !pq.isEmpty(); i++) {
            Map.Entry<String, Float> entry = pq.poll();
            topDiseases.put(entry.getKey(), entry.getValue() * 100);
        }

        return topDiseases;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
