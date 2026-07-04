package com.programminghut.yolo_deploy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Yolov5TFLiteDetector {
    private static final String TAG = "Yolov5TFLiteDetector";
    private final Size INPUT_SIZE = new Size(320, 320);
    private final int[] OUTPUT_SIZE = new int[]{1, 6300, 85};
    private final boolean IS_INT8 = false;
    private final float DETECT_THRESHOLD = 0.25f;
    private final float IOU_THRESHOLD = 0.45f;
    private final float IOU_CLASS_DUPLICATED_THRESHOLD = 0.7f;
    private final String LABEL_FILE = "coco_label.txt";

    private String modelFile;
    private Interpreter tflite;
    private List<String> associatedLabels;
    private final Interpreter.Options options = new Interpreter.Options();

    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }

    public void initialModel(Context context) {
        try {
            ByteBuffer tfliteModel = FileUtil.loadMappedFile(context, modelFile);
            tflite = new Interpreter(tfliteModel, options);
            associatedLabels = FileUtil.loadLabels(context, LABEL_FILE);
            Log.i(TAG, "Model and labels loaded successfully: " + modelFile);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model or label: ", e);
            Toast.makeText(context, "Load model error / मॉडल लोड करने में त्रुटि: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public ArrayList<Recognition> detect(Bitmap bitmap) {
        if (tflite == null) return new ArrayList<>();

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_SIZE.getHeight(), INPUT_SIZE.getWidth(), ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, 255))
                .build();

        TensorImage inputImage = new TensorImage(IS_INT8 ? DataType.UINT8 : DataType.FLOAT32);
        inputImage.load(bitmap);
        inputImage = imageProcessor.process(inputImage);

        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, IS_INT8 ? DataType.UINT8 : DataType.FLOAT32);
        tflite.run(inputImage.getBuffer(), outputBuffer.getBuffer());

        float[] recognitionArray = outputBuffer.getFloatArray();
        ArrayList<Recognition> allRecognitions = new ArrayList<>();

        for (int i = 0; i < OUTPUT_SIZE[1]; i++) {
            int gridStride = i * OUTPUT_SIZE[2];
            float confidence = recognitionArray[gridStride + 4];
            
            if (confidence > DETECT_THRESHOLD) {
                float x = recognitionArray[gridStride] * bitmapWidth;
                float y = recognitionArray[gridStride + 1] * bitmapHeight;
                float w = recognitionArray[gridStride + 2] * bitmapWidth;
                float h = recognitionArray[gridStride + 3] * bitmapHeight;
                
                int xmin = (int) Math.max(0, x - w / 2.0);
                int ymin = (int) Math.max(0, y - h / 2.0);
                int xmax = (int) Math.min(bitmapWidth, x + w / 2.0);
                int ymax = (int) Math.min(bitmapHeight, y + h / 2.0);
                
                int labelId = 0;
                float maxLabelScore = 0f;

                for (int j = 5; j < OUTPUT_SIZE[2]; j++) {
                    if (recognitionArray[gridStride + j] > maxLabelScore) {
                        maxLabelScore = recognitionArray[gridStride + j];
                        labelId = j - 5;
                    }
                }

                allRecognitions.add(new Recognition(
                        labelId,
                        associatedLabels.get(labelId),
                        confidence,
                        new RectF(xmin, ymin, xmax, ymax)));
            }
        }

        return nmsFilterBoxDuplication(nms(allRecognitions));
    }

    private ArrayList<Recognition> nms(ArrayList<Recognition> allRecognitions) {
        ArrayList<Recognition> nmsRecognitions = new ArrayList<>();
        if (associatedLabels == null) return nmsRecognitions;

        for (int i = 0; i < associatedLabels.size(); i++) {
            PriorityQueue<Recognition> pq = new PriorityQueue<>(100, (l, r) -> Float.compare(r.getConfidence(), l.getConfidence()));

            for (Recognition r : allRecognitions) {
                if (r.getId() == i) pq.add(r);
            }

            while (!pq.isEmpty()) {
                Recognition max = pq.poll();
                nmsRecognitions.add(max);
                pq.removeIf(detection -> boxIou(max.getLocation(), detection.getLocation()) >= IOU_THRESHOLD);
            }
        }
        return nmsRecognitions;
    }

    private ArrayList<Recognition> nmsFilterBoxDuplication(ArrayList<Recognition> allRecognitions) {
        ArrayList<Recognition> nmsRecognitions = new ArrayList<>();
        PriorityQueue<Recognition> pq = new PriorityQueue<>(100, (l, r) -> Float.compare(r.getConfidence(), l.getConfidence()));
        pq.addAll(allRecognitions);

        while (!pq.isEmpty()) {
            Recognition max = pq.poll();
            nmsRecognitions.add(max);
            pq.removeIf(detection -> boxIou(max.getLocation(), detection.getLocation()) >= IOU_CLASS_DUPLICATED_THRESHOLD);
        }
        return nmsRecognitions;
    }

    private float boxIou(RectF a, RectF b) {
        if (a == null || b == null) return 0;
        float intersection = boxIntersection(a, b);
        float union = (a.width() * a.height()) + (b.width() * b.height()) - intersection;
        return union <= 0 ? 0 : intersection / union;
    }

    private float boxIntersection(RectF a, RectF b) {
        float w = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left));
        float h = Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));
        return w * h;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}
