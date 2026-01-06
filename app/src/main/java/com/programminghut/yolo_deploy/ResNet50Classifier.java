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
import java.util.Arrays;

public class ResNet50Classifier
{
    private Interpreter interpreter;
    private final int IMAGE_SIZE = 224; // ResNet50 expects 224x224 images
    private final String[] labels = {"Healthy", "Foot and Mouth Disease"}; // Update with correct labels

    public ResNet50Classifier(Context context) throws IOException
    {
        interpreter = new Interpreter(FileUtil.loadMappedFile(context, "foot_and_mouth_model_217.tflite"));
    }

    public Recognition classifyImage(Bitmap bitmap)
    {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        ImageProcessor imageProcessor = new ImageProcessor
                .Builder()
                .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0f, 255f))
                .build();


        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // Model expects [1, 224, 224, 3] input shape
        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(new int[]{1, IMAGE_SIZE, IMAGE_SIZE, 3}, DataType.FLOAT32);
        inputBuffer.loadBuffer(tensorImage.getBuffer());

        // Output Buffer
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
        interpreter.run(inputBuffer.getBuffer(), outputBuffer.getBuffer());

        // Get raw output values
        float[] outputValues = outputBuffer.getFloatArray();

        // Apply Softmax to normalize values
        ModelUtils utils = new ModelUtils();
        float[] probabilities = utils.softmax(outputValues);

        // Find the highest confidence score
        int maxIndex = 0;
        float maxConfidence = probabilities[0];

        for (int i = 1; i < probabilities.length; i++)
        {
            if (probabilities[i] > maxConfidence)
            {
                maxConfidence = probabilities[i];
                maxIndex = i;
            }
        }

        Log.d("Model Output", "FMD Output values: " + Arrays.toString(probabilities));

        return new Recognition(maxIndex, labels[maxIndex], maxConfidence);
    }

    public class ModelUtils
    {
        public float[] softmax(float[] logits)
        {
            float maxLogit = Float.NEGATIVE_INFINITY;
            for (float logit : logits)
            {
                if (logit > maxLogit)
                {
                    maxLogit = logit;
                }
            }

            float sumExp = 0f;
            float[] expValues = new float[logits.length];

            // Compute exponentials with numerical stability
            for (int i = 0; i < logits.length; i++)
            {
                expValues[i] = (float) Math.exp(logits[i] - maxLogit);
                sumExp += expValues[i];
            }

            // Normalize to get probabilities
            for (int i = 0; i < logits.length; i++)
            {
                expValues[i] /= sumExp;
            }

            return expValues;
        }
    }
}