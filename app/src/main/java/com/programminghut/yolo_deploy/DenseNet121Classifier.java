package com.programminghut.yolo_deploy;

import android.content.Context;
import java.io.IOException;

public class DenseNet121Classifier extends BaseClassifier {
    
    private static final String MODEL_PATH = "densenet121_model.tflite";
    private static final int INPUT_SIZE = 256;
    private static final String[] LABELS = {"Lumpy Skin Disease", "Healthy"};

    public DenseNet121Classifier(Context context) throws IOException {
        super(context, MODEL_PATH, INPUT_SIZE, LABELS);
    }
}
