package com.programminghut.yolo_deploy;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import androidx.annotation.NonNull;
import org.tensorflow.lite.Interpreter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class DiseasePredictor {
    private Interpreter tflite;
    private static final String MODEL_FILE = "disease_predictor.tflite"; // Place this file in your assets
    // Complete list of 93 symptoms
    private static final List<String> SYMPTOMS = Arrays.asList(
            "anorexia", "abdominal_pain", "anaemia", "abortions", "acetone", "aggression", "arthrogyposis", "ankylosis", "anxiety", "bellowing", "blood_loss",
            "blood_poisoning", "blisters", "colic", "Condemnation_of_livers", "conjunctivae", "coughing", "depression", "discomfort", "dyspnea", "dysentery",
            "diarrhoea", "dehydration", "drooling", "dull", "decreased_fertility", "diffculty_breath", "emaciation", "encephalitis", "fever", "facial_paralysis",
            "frothing_of_mouth", "frothing", "gaseous_stomach", "highly_diarrhoea", "high_pulse_rate", "high_temp", "high_proportion", "hyperaemia", "hydrocephalus",
            "isolation_from_herd", "infertility", "intermittent_fever", "jaundice", "ketosis", "loss_of_appetite", "lameness", "lack_of-coordination", "lethargy",
            "lacrimation", "milk_flakes", "milk_watery", "milk_clots", "mild_diarrhoea", "moaning", "mucosal_lesions", "milk_fever", "nausea", "nasel_discharges",
            "oedema", "pain", "painful_tongue", "pneumonia", "photo_sensitization", "quivering_lips", "reduction_milk_vields", "rapid_breathing", "rumenstasis",
            "reduced_rumination", "reduced_fertility", "reduced_fat", "reduces_feed_intake", "raised_breathing", "stomach_pain", "salivation", "stillbirths",
            "shallow_breathing", "swollen_pharyngeal", "swelling", "saliva", "swollen_tongue", "tachycardia", "torticollis", "udder_swelling", "udder_heat",
            "udder_hardeness", "udder_redness", "udder_pain", "unwillingness_to_move", "ulcers", "vomiting", "weight_loss", "weakness"
    );

    // Example disease class labels
    private static final List<String> DISEASE_CLASSES = Arrays.asList(
            "mastitis", "blackleg", "bloat", "coccidiosis", "cryptosporidiosis",
            "displaced_abomasum", "gut_worms", "listeriosis", "liver_fluke", "necrotic_enteritis",
            "peri_weaning_diarrhoea", "rift_valley_fever", "rumen_acidosis", "traumatic_reticulitis",
            "calf_diphtheria", "foot_rot", "foot_and_mouth", "ragwort_poisoning", "wooden_tongue",
            "infectious_bovine_rhinotracheitis", "acetonaemia", "fatty_liver_syndrome", "calf_pneumonia",
            "schmallen_berg_virus", "trypanosomosis", "fog_fever"
    );

    public DiseasePredictor(@NonNull Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    // Load model file from the app's files directory (ensure you copy it from assets)
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    // Takes a list of selected symptoms and returns a map of top 3 disease predictions (in percentage)
    public Map<String, Float> predictDisease(@NonNull List<String> selectedSymptoms) {
        float[] inputVector = new float[SYMPTOMS.size()];
        Arrays.fill(inputVector, 0); // Initialize all to 0

        // Mark selected symptoms as 1
        for (String symptom : selectedSymptoms) {
            int index = SYMPTOMS.indexOf(symptom);
            if (index != -1) {
                inputVector[index] = 1;
            }
        }

        // Output array for probabilities (assuming model outputs one probability per disease class)
        float[][] outputProbabilities = new float[1][DISEASE_CLASSES.size()];
        // Run inference
        tflite.run(inputVector, outputProbabilities);

        return getTopDiseases(outputProbabilities[0]);
    }

    private Map<String, Float> getTopDiseases(float[] probabilities) {
        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<>(
                (a, b) -> Float.compare(b.getValue(), a.getValue())
        );

        for (int i = 0; i < DISEASE_CLASSES.size(); i++) {
            pq.offer(new AbstractMap.SimpleEntry<>(DISEASE_CLASSES.get(i), probabilities[i]));
        }

        Map<String, Float> topDiseases = new LinkedHashMap<>();
        for (int i = 0; i < 3 && !pq.isEmpty(); i++) {
            Map.Entry<String, Float> entry = pq.poll();
            topDiseases.put(entry.getKey(), entry.getValue() * 100); // convert to percentage
        }

        return topDiseases;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
