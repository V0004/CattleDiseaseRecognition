package com.programminghut.yolo_deploy.utils;

import android.app.Activity;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;

public class ManualCropHelper {

    public static void startManualCrop(Activity activity, Bitmap bitmap) {

        if (bitmap == null) {
            Toast.makeText(activity, "Bitmap is null / बिटमैप खाली है", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save bitmap to cache
        File cacheFile = new File(
                activity.getCacheDir(),
                "temp_crop_" + System.currentTimeMillis() + ".jpg"
        );

        try (FileOutputStream out = new FileOutputStream(cacheFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Failed to prepare image for cropping / क्रॉपिंग के लिए छवि तैयार करने में विफल", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri sourceUri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".fileprovider",
                cacheFile
        );

        Uri destinationUri = Uri.fromFile(
                new File(activity.getCacheDir(),
                        "cropped_" + System.currentTimeMillis() + ".jpg")
        );

        UCrop.of(sourceUri, destinationUri)
                .withMaxResultSize(3000, 3000)
                .start(activity);
    }
}