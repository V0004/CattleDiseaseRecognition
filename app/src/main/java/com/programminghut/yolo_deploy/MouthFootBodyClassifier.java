package com.programminghut.yolo_deploy;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;

public class MouthFootBodyClassifier extends BaseClassifier {

    private static final String MODEL_PATH = "mouth_foot_body_classifier_fixed.tflite";
    private static final int INPUT_SIZE = 224;
    private static final String[] LABELS = {"Foot/Mouth", "Body"};

    public MouthFootBodyClassifier(Context context) throws IOException {
        super(context, MODEL_PATH, INPUT_SIZE, LABELS);
    }

    @Override
    protected ImageProcessor getImageProcessor() {
        return new ImageProcessor.Builder()
                .add(new ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f))
                .build();
    }

    @Override
    public Recognition classifyImage(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = getImageProcessor().process(tensorImage);

        // This specific model seems to output [1, 1] based on previous code
        // and uses a threshold of 0.5
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, 1}, DataType.FLOAT32);
        interpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer());

        float prediction = outputBuffer.getFloatArray()[0];
        String label = prediction < 0.5f ? LABELS[0] : LABELS[1];
        float confidence = prediction < 0.5f ? 1 - prediction : prediction;

        Log.d("MFBClassifier", "Raw: " + prediction + ", Label: " + label);
        return new Recognition(prediction < 0.5f ? 0 : 1, label, confidence);
    }
    
    // Legacy support for internal Result class if needed by other components
    public static class Result {
        public final String label;
        public final float confidence;
        public Result(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
    
    public Result classify(Bitmap bitmap) {
        Recognition rec = classifyImage(bitmap);
        return new Result(rec.getLabel(), rec.getConfidence());
    }
}
