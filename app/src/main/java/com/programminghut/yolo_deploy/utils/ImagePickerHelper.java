package com.programminghut.yolo_deploy.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagePickerHelper {

    public static final int IMAGE_PICK = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 2;
    public static final int CAMERA_REQUEST_CODE = 101;

    public interface CameraCallback {
        void onCameraImageReady(Uri photoUri);
    }

    public static void showImagePicker(Activity activity, CameraCallback callback) {

        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Image From");
        builder.setItems(options, (dialog, which) -> {

            if (which == 0) {
                // Camera
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                            activity,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST_CODE
                    );

                } else {
                    openCamera(activity, callback);
                }

            } else {
                openGallery(activity);
            }
        });

        builder.show();
    }

    private static void openCamera(Activity activity, CameraCallback callback) {

        Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePhoto.resolveActivity(activity.getPackageManager()) != null) {

            File photoFile;
            try {
                photoFile = createImageFile(activity);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            Uri photoUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    photoFile
            );

            callback.onCameraImageReady(photoUri);

            takePhoto.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePhoto.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            activity.startActivityForResult(takePhoto, REQUEST_IMAGE_CAPTURE);
        }
    }

    private static void openGallery(Activity activity) {
        Intent pickGallery = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        pickGallery.setType("image/*");
        activity.startActivityForResult(pickGallery, IMAGE_PICK);
    }

    private static File createImageFile(Activity activity) throws IOException {

        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
        ).format(new Date());

        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}
