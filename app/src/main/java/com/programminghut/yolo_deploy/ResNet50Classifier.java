package com.programminghut.yolo_deploy;

import android.content.Context;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import java.io.IOException;

public class ResNet50Classifier extends BaseClassifier {

    private static final String MODEL_PATH = "foot_and_mouth_model_217.tflite";
    private static final int INPUT_SIZE = 224;
    private static final String[] LABELS = {"Healthy", "Foot and Mouth Disease"};

    public ResNet50Classifier(Context context) throws IOException {
        super(context, MODEL_PATH, INPUT_SIZE, LABELS);
    }

    @Override
    protected ImageProcessor getImageProcessor() {
        return new ImageProcessor.Builder()
                .add(new ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f)) // ResNet often requires normalization
                .build();
    }

    @Override
    protected Recognition postProcess(float[] outputValues) {
        float[] probabilities = softmax(outputValues);
        int maxIndex = 0;
        float maxConfidence = probabilities[0];

        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxConfidence) {
                maxConfidence = probabilities[i];
                maxIndex = i;
            }
        }
        return new Recognition(maxIndex, labels[maxIndex], maxConfidence);
    }

    private float[] softmax(float[] logits) {
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logits) maxLogit = Math.max(maxLogit, logit);

        float sumExp = 0f;
        float[] expValues = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            expValues[i] = (float) Math.exp(logits[i] - maxLogit);
            sumExp += expValues[i];
        }
        for (int i = 0; i < logits.length; i++) expValues[i] /= sumExp;
        return expValues;
    }
}
