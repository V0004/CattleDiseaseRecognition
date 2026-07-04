package com.programminghut.yolo_deploy.utils;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.programminghut.yolo_deploy.Yolov5TFLiteDetector;
import com.programminghut.yolo_deploy.Recognition;

import java.util.ArrayList;

public class BoundingBoxCropHelper {

    public interface CropResultCallback {
        void onCropSuccess(Bitmap croppedBitmap);
        void onNoCowDetected();
    }

    public static void cropCowFromBoundingBox(
            Bitmap bitmap,
            Yolov5TFLiteDetector detector,
            CropResultCallback callback
    ) {

        if (bitmap == null || detector == null) {
            callback.onNoCowDetected();
            return;
        }

        ArrayList<Recognition> recognitions = detector.detect(bitmap);

        for (Recognition recognition : recognitions) {
            // Updated to use getLabel() instead of getLabelName() to match refactored Recognition class
            if (recognition.getConfidence() > 0.4f &&
                    "cow".equalsIgnoreCase(recognition.getLabel())) {

                RectF location = recognition.getLocation();
                if (location == null) continue;

                int left = (int) Math.max(location.left, 0);
                int top = (int) Math.max(location.top, 0);
                int right = (int) Math.min(location.right, bitmap.getWidth());
                int bottom = (int) Math.min(location.bottom, bitmap.getHeight());

                if (right <= left || bottom <= top) continue;

                Bitmap croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        right - left,
                        bottom - top
                );

                callback.onCropSuccess(croppedBitmap);
                return;
            }
        }

        callback.onNoCowDetected();
    }
}
