package com.programminghut.yolo_deploy;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;

public class DiseaseClassifier {
    private DenseNet121Classifier classifierDenseNet;
    private ResNet50Classifier classifierResNet;
    private MouthFootBodyClassifier mfbClassifier;
    private final Context context;

    public DiseaseClassifier(Context context) throws IOException {
        this.context = context;
        // Attempt to initialize core classifiers
        this.classifierDenseNet = new DenseNet121Classifier(context);
        this.classifierResNet = new ResNet50Classifier(context);
    }

    public static class ClassificationResult {
        public final String label;
        public final float confidence;
        public final String modelUsed;

        public ClassificationResult(String label, float confidence, String modelUsed) {
            this.label = label;
            this.confidence = confidence;
            this.modelUsed = modelUsed;
        }
    }

    public ClassificationResult classify(Bitmap bitmap) {
        if (classifierDenseNet == null || classifierResNet == null) {
            return new ClassificationResult("Error: Models not loaded", 0f, "None");
        }

        Recognition resultDenseNet = classifierDenseNet.classifyImage(bitmap);
        Recognition resultResNet = classifierResNet.classifyImage(bitmap);

        String labelDenseNet = resultDenseNet.getLabel();
        float confidenceDenseNet = resultDenseNet.getConfidence();

        String labelResNet = resultResNet.getLabel();
        float confidenceResNet = resultResNet.getConfidence();

        String finalLabel;
        float finalConfidence;
        String modelUsed;

        Log.d("DiseaseClassifier", "DenseNet: " + labelDenseNet + " (" + confidenceDenseNet + "), ResNet: " + labelResNet + " (" + confidenceResNet + ")");

        if ("Healthy".equals(labelDenseNet) && "Healthy".equals(labelResNet)) {
            finalLabel = "Healthy";
            finalConfidence = (confidenceDenseNet + confidenceResNet) / 2;
            modelUsed = "Both (DenseNet121 & ResNet50)";
        } else if (!"Healthy".equals(labelDenseNet) && !"Healthy".equals(labelResNet)) {
            if (confidenceDenseNet >= 0.899f && confidenceResNet >= 0.899f) {
                return runMfbClassifier(bitmap, confidenceResNet, confidenceDenseNet, null);
            } else if (confidenceDenseNet >= confidenceResNet) {
                finalLabel = labelDenseNet;
                finalConfidence = confidenceDenseNet;
                modelUsed = "DenseNet121";
            } else {
                return runMfbClassifier(bitmap, confidenceResNet, confidenceDenseNet, null);
            }
        } else {
            if (!"Healthy".equals(labelDenseNet)) {
                finalLabel = labelDenseNet;
                finalConfidence = confidenceDenseNet;
                modelUsed = "DenseNet121";
            } else {
                return runMfbClassifier(bitmap, confidenceResNet, confidenceDenseNet, "Healthy");
            }
        }

        return new ClassificationResult(finalLabel, Math.min(finalConfidence * 100, 100), modelUsed);
    }

    private ClassificationResult runMfbClassifier(Bitmap bitmap, float resNetConf, float denseNetConf, String healthyFallback) {
        try {
            if (mfbClassifier == null) {
                mfbClassifier = new MouthFootBodyClassifier(context);
            }
            Recognition mfbResult = mfbClassifier.classifyImage(bitmap);
            if ("Foot/Mouth".equals(mfbResult.getLabel())) {
                return new ClassificationResult("Foot and Mouth Disease", resNetConf, "ResNet50 & Foot/Mouth Classifier");
            } else {
                String label = (healthyFallback != null) ? healthyFallback : "Lumpy Skin Disease";
                return new ClassificationResult(label, denseNetConf, "DenseNet121 & Foot/Mouth Classifier");
            }
        } catch (Exception e) {
            Log.e("DiseaseClassifier", "Error running MFB classifier", e);
            String label = (healthyFallback != null) ? healthyFallback : "Unknown (MFB Load Error)";
            return new ClassificationResult(label, denseNetConf, "Error Loading Auxiliary Model");
        }
    }
}
