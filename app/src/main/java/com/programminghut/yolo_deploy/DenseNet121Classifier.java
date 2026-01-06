package com.programminghut.yolo_deploy;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;

public class DenseNet121Classifier {
    private Interpreter interpreter;
    private final int IMAGE_SIZE = 256;
    private final String[] labels = {"Lumpy Skin Disease", "Healthy"};

    public DenseNet121Classifier(Context context) throws IOException
    {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, "densenet121_model.tflite");
        interpreter = new Interpreter(modelFile);
    }

    public Recognition classifyImage(Bitmap bitmap)
    {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        ImageProcessor imageProcessor = new ImageProcessor
                .Builder()
                .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        //bitmap to tensorImage.
        tensorImage.load(bitmap);

        //Changing the size.
        tensorImage = imageProcessor.process(tensorImage);

        // Model expects [1, 256, 256, 3] input shape
        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(new int[]{1, IMAGE_SIZE, IMAGE_SIZE, 3}, DataType.FLOAT32);
        inputBuffer.loadBuffer(tensorImage.getBuffer());

        // Output Buffer
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
        interpreter.run(inputBuffer.getBuffer(), outputBuffer.getBuffer());

        // Get output values
        float[] outputValues = outputBuffer.getFloatArray();
        int maxIndex = 0;
        float maxConfidence = outputValues[0];

        for (int i = 1; i < outputValues.length; i++)
        {
            if (outputValues[i] > maxConfidence)
            {
                maxConfidence = outputValues[i];
                maxIndex = i;
            }
        }

        Log.d("Model Output", "LSD Output values: " + Arrays.toString(outputValues));

        return new Recognition(maxIndex, labels[maxIndex], maxConfidence);
    }
}