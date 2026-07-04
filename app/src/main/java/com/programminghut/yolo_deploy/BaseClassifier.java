package com.programminghut.yolo_deploy;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * A base class for TFLite classifiers to reduce boilerplate.
 */
public abstract class BaseClassifier {
    protected final Interpreter interpreter;
    protected final int inputImageSize;
    protected final String[] labels;

    public BaseClassifier(Context context, String modelPath, int inputImageSize, String[] labels) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, modelPath);
        
        // Use Interpreter.Options to ensure best compatibility and performance
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        options.setUseXNNPACK(true); // Built-in ops version 12 often relies on XNNPACK optimizations
        
        this.interpreter = new Interpreter(modelFile, options);
        this.inputImageSize = inputImageSize;
        this.labels = labels;
    }

    public Recognition classifyImage(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);

        ImageProcessor imageProcessor = getImageProcessor();
        tensorImage = imageProcessor.process(tensorImage);

        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(new int[]{1, inputImageSize, inputImageSize, 3}, DataType.FLOAT32);
        inputBuffer.loadBuffer(tensorImage.getBuffer());

        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, labels.length}, DataType.FLOAT32);
        interpreter.run(inputBuffer.getBuffer(), outputBuffer.getBuffer());

        return postProcess(outputBuffer.getFloatArray());
    }

    protected ImageProcessor getImageProcessor() {
        return new ImageProcessor.Builder()
                .add(new ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                .build();
    }

    protected Recognition postProcess(float[] outputValues) {
        int maxIndex = 0;
        float maxConfidence = outputValues[0];

        for (int i = 1; i < outputValues.length; i++) {
            if (outputValues[i] > maxConfidence) {
                maxConfidence = outputValues[i];
                maxIndex = i;
            }
        }

        return new Recognition(maxIndex, labels[maxIndex], maxConfidence);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
