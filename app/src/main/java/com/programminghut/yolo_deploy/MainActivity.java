package com.programminghut.yolo_deploy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.programminghut.yolo_deploy.utils.BoundingBoxCropHelper;
import com.programminghut.yolo_deploy.utils.ImagePickerHelper;
import com.programminghut.yolo_deploy.utils.ManualCropHelper;
import com.programminghut.yolo_deploy.utils.TTSManager;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private ImageView imageView;
    private Button manualCropButton;
    
    private Bitmap currentBitmap;
    private Uri photoUri; 
    private Yolov5TFLiteDetector objectDetector;
    private DiseaseClassifier diseaseClassifier;
    private TTSManager ttsManager;
    
    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ttsManager = new TTSManager(this);

        initViews();
        initPaints();
        initModels();
        
        loadCachedImageIfPresent();
    }

    private void initViews() {
        imageView = findViewById(R.id.imageView);
        manualCropButton = findViewById(R.id.manualCropButton);
        Button classifyButton = findViewById(R.id.classifyButton);
        FloatingActionButton fabAudio = findViewById(R.id.fabAudio);

        classifyButton.setOnClickListener(v -> {
            if (currentBitmap == null) {
                showToast(getString(R.string.upload_first));
                return;
            }
            performClassification(currentBitmap);
        });

        manualCropButton.setOnClickListener(v -> {
            if (currentBitmap == null) {
                showToast(getString(R.string.upload_first));
                return;
            }
            ManualCropHelper.startManualCrop(this, currentBitmap);
        });

        fabAudio.setOnClickListener(v -> {
            String content = getString(R.string.main_title) + ". " + 
                             getString(R.string.upload_image) + ". " + 
                             getString(R.string.manual_crop) + ". " + 
                             getString(R.string.predict_disease) + ". " + 
                             getString(R.string.verify_image) + ". " + 
                             getString(R.string.autocrop_predict);
            ttsManager.speak(content);
        });
    }

    private void initPaints() {
        boxPaint.setStrokeWidth(2);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);

        textPaint.setTextSize(25);
        textPaint.setColor(Color.GREEN);
        textPaint.setStyle(Paint.Style.FILL);
    }

    private void initModels() {
        objectDetector = new Yolov5TFLiteDetector();
        objectDetector.setModelFile("yolov_8n.tflite");
        objectDetector.initialModel(this);

        try {
            diseaseClassifier = new DiseaseClassifier(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize disease classifier", e);
            showToast(getString(R.string.error_initializing_models));
        }
    }

    private void loadCachedImageIfPresent() {
        File cacheFile = new File(getApplicationContext().getCacheDir(), "final_image.jpg");
        if (cacheFile.exists()) {
            currentBitmap = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
            imageView.setImageBitmap(currentBitmap);
            manualCropButton.setVisibility(View.VISIBLE);
        }
    }

    public void showImagePicker(View view) {
        ImagePickerHelper.showImagePicker(this, uri -> {
            this.photoUri = uri;
        });
    }

    public void predict(View view) {
        if (currentBitmap == null) {
            showToast(getString(R.string.upload_first));
            return;
        }

        ArrayList<Recognition> recognitions = objectDetector.detect(currentBitmap);
        Bitmap mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        boolean cowDetected = false;

        for (Recognition recognition : recognitions) {
            if (recognition.getConfidence() > 0.4 && "cow".equalsIgnoreCase(recognition.getLabel())) {
                RectF location = recognition.getLocation();
                if (location != null) {
                    canvas.drawRect(location, boxPaint);
                    canvas.drawText(getString(R.string.cow_label) + ":" + String.format(Locale.US, "%.2f", recognition.getConfidence()), 
                            location.left, location.top, textPaint);
                }
                cowDetected = true;
            }
        }

        if (cowDetected) {
            showToast(getString(R.string.cow_detected_msg));
        } else {
            showToast(getString(R.string.no_cow_detected_msg));
        }
        imageView.setImageBitmap(mutableBitmap);
    }

    public void cropBoundingBox(View view) {
        if (currentBitmap == null) {
            showToast(getString(R.string.upload_first));
            return;
        }

        BoundingBoxCropHelper.cropCowFromBoundingBox(
                currentBitmap,
                objectDetector,
                new BoundingBoxCropHelper.CropResultCallback() {
                    @Override
                    public void onCropSuccess(Bitmap croppedBitmap) {
                        currentBitmap = croppedBitmap;
                        imageView.setImageBitmap(croppedBitmap);
                        performClassification(croppedBitmap);
                    }

                    @Override
                    public void onNoCowDetected() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.cow_not_detected_title)
                                .setMessage(R.string.is_uploaded_image_cow)
                                .setPositiveButton(R.string.yes, (dialog, which) -> performClassification(currentBitmap))
                                .setNegativeButton(R.string.no, (dialog, which) -> showImagePicker(view))
                                .setCancelable(false)
                                .show();
                    }
                }
        );
    }

    private void performClassification(Bitmap bitmap) {
        if (diseaseClassifier == null) {
            showToast(getString(R.string.classifier_not_initialized));
            return;
        }

        DiseaseClassifier.ClassificationResult result = diseaseClassifier.classify(bitmap);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("classification", result.label);
        intent.putExtra("confidence", String.format(Locale.US, "%.2f", result.confidence));
        intent.putExtra("modelUsed", result.modelUsed);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
            if (resultCode == UCrop.RESULT_ERROR && data != null) {
                Throwable cropError = UCrop.getError(data);
                if (cropError != null) showToast(getString(R.string.crop_error) + ": " + cropError.getMessage());
            }
            return;
        }

        try {
            if (requestCode == IMAGE_PICK && data != null && data.getData() != null) {
                currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Uri imageUri = (data != null && data.getData() != null) ? data.getData() : photoUri;
                if (imageUri != null) {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                } else if (data != null && data.getExtras() != null) {
                    currentBitmap = (Bitmap) data.getExtras().get("data");
                }
            } else if (requestCode == UCrop.REQUEST_CROP && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                }
            }

            if (currentBitmap != null) {
                imageView.setImageBitmap(currentBitmap);
                manualCropButton.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error processing activity result", e);
            showToast(getString(R.string.failed_load_image));
        }
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        super.onDestroy();
    }
}
