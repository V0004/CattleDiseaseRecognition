package com.programminghut.yolo_deploy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.programminghut.yolo_deploy.utils.ImagePickerHelper;
import com.programminghut.yolo_deploy.utils.BoundingBoxCropHelper;
import com.programminghut.yolo_deploy.utils.ManualCropHelper;

import com.yalantis.ucrop.UCrop;

import android.Manifest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_MANUAL_CROP = 102;
    private Button manualCropButton;
    private static final int IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_REQUEST_CODE = 101;
    private Uri photoUri;
    ImageView imageView;
    private File photoFile;
    Bitmap bitmap;
    Yolov5TFLiteDetector yolov5TFLiteDetector;
    private DenseNet121Classifier classifierDenseNet;
    private ResNet50Classifier classifierResNet;
    Paint boxPaint = new Paint();
    Paint textPain = new Paint();
    String image_uri = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        manualCropButton = findViewById(R.id.manualCropButton);

        if (!image_uri.equals(""))
        {
            bitmap = getBitmapFromCache();
            imageView.setImageBitmap(bitmap);
            manualCropButton.setVisibility(View.VISIBLE);
        }

        Button classifyButton = findViewById(R.id.classifyButton);
        classifyButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (bitmap == null)
                {
                    Toast.makeText(MainActivity.this, "Upload an Image first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                classifyCow(bitmap); // Call the updated method with the bitmap
            }
        });

        manualCropButton.setOnClickListener(v -> {
            if (bitmap == null) {
                Toast.makeText(this, "Upload an Image first!", Toast.LENGTH_SHORT).show();
                return;
            }
            ManualCropHelper.startManualCrop(this,bitmap);
        });

        yolov5TFLiteDetector = new Yolov5TFLiteDetector();
        yolov5TFLiteDetector.setModelFile("yolov_8n.tflite");
        yolov5TFLiteDetector.initialModel(this);

        try {
            classifierDenseNet = new DenseNet121Classifier(this);
            classifierResNet = new ResNet50Classifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        boxPaint.setStrokeWidth(2);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);

        textPain.setTextSize(25);
        textPain.setColor(Color.GREEN);
        textPain.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!image_uri.equals(""))
        {
            imageView.setImageBitmap(getBitmapFromCache());
        }
    }

    public Bitmap getBitmapFromCache(){
        File cacheFile = new File(getApplicationContext().getCacheDir(), "final_image.jpg");
        Bitmap myBitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
        return myBitmap;
    }

    public void showImagePicker(View view) {
        ImagePickerHelper.showImagePicker(this, uri -> {
            photoUri = uri; // store for onActivityResult
        });
    }

    public void predict(View view)
    {
        if (bitmap == null) {
            Toast.makeText(this, "Upload an Image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Recognition> recognitions =  yolov5TFLiteDetector.detect(bitmap);
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        boolean cowDetected2 = false;

        for(Recognition recognition: recognitions)
        {
            if(recognition.getConfidence() > 0.4 && recognition.getLabelName().equalsIgnoreCase("cow"))
            {
                RectF location = recognition.getLocation();
                canvas.drawRect(location, boxPaint);
                canvas.drawText(recognition.getLabelName() + ":" + recognition.getConfidence(), location.left, location.top, textPain);
                Toast.makeText(this, "Cow Detected, Proceed with Crop & Classify", Toast.LENGTH_SHORT).show();
                cowDetected2 = true;
            }
        }

        if(!cowDetected2)
        {
            Toast.makeText(this, "No Cow Detected", Toast.LENGTH_SHORT).show();
        }
        imageView.setImageBitmap(mutableBitmap);
    }

    //Crops the detected cow from an image and sends it for classification, otherwise prompts the user if no cow is detected.
    public void cropBoundingBox(View view) {

        if (bitmap == null) {
            Toast.makeText(this, "Upload an Image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        BoundingBoxCropHelper.cropCowFromBoundingBox(
                bitmap,
                yolov5TFLiteDetector,
                new BoundingBoxCropHelper.CropResultCallback() {

                    @Override
                    public void onCropSuccess(Bitmap croppedBitmap) {
                        imageView.setImageBitmap(croppedBitmap);
                        classifyCow(croppedBitmap);
                    }

                    @Override
                    public void onNoCowDetected() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Cow not detected")
                                .setMessage("Is the Uploaded Image of Cow?")
                                .setPositiveButton("Yes",
                                        (dialog, which) -> classifyCow(bitmap))
                                .setNegativeButton("No",
                                        (dialog, which) -> showImagePicker(view))
                                .setCancelable(false)
                                .show();
                    }
                }
        );
    }

    public void classifyCow(Bitmap bitmap)
    {
        if (bitmap == null)
        {
            return;
        }

//        DenseNet121Classifier classifierDenseNet = null;
//        ResNet50Classifier classifierResNet = null;

        try
        {
            classifierDenseNet = new DenseNet121Classifier(this);
            classifierResNet = new ResNet50Classifier(this);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Recognition resultDenseNet = classifierDenseNet.classifyImage(bitmap);
        Recognition resultResNet = classifierResNet.classifyImage(bitmap);

        String labelDenseNet = resultDenseNet.getLabel();
        float confidenceDenseNet = resultDenseNet.getConfidence();

        String labelResNet = resultResNet.getLabel();
        float confidenceResNet = resultResNet.getConfidence();

        String finalLabel;
        float finalConfidence;
        String modelUsed;

        Log.d("Model Output", "DenseNet Confidence: " + confidenceDenseNet + ", ResNet Confidence: " + confidenceResNet);

        if (labelDenseNet.equals("Healthy") && labelResNet.equals("Healthy")) {
            finalLabel = "Healthy";
            finalConfidence = (confidenceDenseNet + confidenceResNet) / 2;
            modelUsed = "Both";
        } else if (!labelDenseNet.equals("Healthy") && !labelResNet.equals("Healthy"))
        {
            if( confidenceDenseNet >= 0.899f && confidenceResNet >= 0.899f)
            {
                Log.d("ModelOutput", "Both confidences >= 0.90, using Foot/Mouth Classifier...");

                try {
                    MouthFootBodyClassifier mfbClassifier = new MouthFootBodyClassifier(getAssets(), "mouth_foot_body_classifier_fixed.tflite");
                    MouthFootBodyClassifier.Result mfbResult = mfbClassifier.classify(bitmap);
                    if(mfbResult.label.equals("Foot/Mouth"))
                    {
                        finalLabel = "Foot and Mouth Disease";
                        finalConfidence = confidenceResNet;
                        modelUsed = "Resnet50 & Foot/Mouth Classifier";
                    }
                    else {
                        finalLabel = "Lumpy Skin Disease";
                        finalConfidence = confidenceDenseNet;
                        modelUsed = "DenseNet121 & Foot/Mouth Classifier";
                    }

                    Log.d("ModelOutput", "Foot/Mouth Classifier used. Final label: " + finalLabel + ", Confidence: " + finalConfidence);

                } catch (IOException e) {
                    e.printStackTrace();
                    // Fallback in case of error
                    finalLabel = "Unknown";
                    finalConfidence = 0f;
                    modelUsed = "Error";

                    Log.e("ModelOutput", "Error loading or running Foot/Mouth Classifier", e);
                }
            } else if (confidenceDenseNet >= confidenceResNet) {
                finalLabel = labelDenseNet;
                finalConfidence = confidenceDenseNet;
                modelUsed = "DenseNet121";
            } else {
                try {
                    MouthFootBodyClassifier mfbClassifier = new MouthFootBodyClassifier(getAssets(), "mouth_foot_body_classifier_fixed.tflite");
                    MouthFootBodyClassifier.Result mfbResult = mfbClassifier.classify(bitmap);
                    if(mfbResult.label.equals("Foot/Mouth"))
                    {
                        finalLabel = "Foot and Mouth Disease";
                        finalConfidence = confidenceResNet;
                        modelUsed = "Resnet50 & Foot/Mouth Classifier";
                    }
                    else {
                        finalLabel = "Lumpy Skin Disease";
                        finalConfidence = confidenceDenseNet;
                        modelUsed = "DenseNet121 & Foot/Mouth Classifier";
                    }
                    Log.d("ModelOutput", "Foot/Mouth Classifier used. Final label: " + finalLabel + ", Confidence: " + finalConfidence);

                } catch (IOException e) {
                    e.printStackTrace();
                    finalLabel = "Unknown";
                    finalConfidence = 0f;
                    modelUsed = "Error";

                    Log.e("ModelOutput", "Error loading or running Foot/Mouth Classifier", e);
                }
            }
        } else {
            if (!labelDenseNet.equals("Healthy")) {
                finalLabel = labelDenseNet;
                finalConfidence = confidenceDenseNet;
                modelUsed = "DenseNet121";
            } else {
                try {
                    MouthFootBodyClassifier mfbClassifier = new MouthFootBodyClassifier(getAssets(), "mouth_foot_body_classifier_fixed.tflite");
                    MouthFootBodyClassifier.Result mfbResult = mfbClassifier.classify(bitmap);
                    if(mfbResult.label.equals("Foot/Mouth"))
                    {
                        finalLabel = "Foot and Mouth Disease";
                        finalConfidence = confidenceResNet;
                        modelUsed = "Resnet50 & Foot/Mouth Classifier";
                    }
                    else {
                        finalLabel = "Healthy";
                        finalConfidence = confidenceDenseNet;
                        modelUsed = "DenseNet121 & Foot/Mouth Classifier";
                    }
                    Log.d("ModelOutput", "Foot/Mouth Classifier used. Final label: " + finalLabel + ", Confidence: " + finalConfidence);

                } catch (IOException e) {
                    e.printStackTrace();
                    finalLabel = "Unknown";
                    finalConfidence = 0f;
                    modelUsed = "Error";

                    Log.e("ModelOutput", "Error loading or running Foot/Mouth Classifier", e);
                }
            }
        }

        finalConfidence = Math.min(finalConfidence * 100, 100);

        // Open new activity with results
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("classification", finalLabel);
        intent.putExtra("confidence", String.format("%.2f", finalConfidence));
        intent.putExtra("modelUsed", modelUsed);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && photoUri != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if (requestCode == UCrop.REQUEST_CROP) {
                // Handle manual crop result
                final Uri resultUri = UCrop.getOutput(data);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    imageView.setImageBitmap(bitmap);
                    // You might want to hide the manual crop button now or keep it visible for further cropping
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load cropped image", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            // Handle crop error
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(this, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}