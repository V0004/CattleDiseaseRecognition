package com.programminghut.yolo_deploy;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.programminghut.yolo_deploy.utils.TTSManager;

/**
 * This activity allows the user to choose between Image detection and Symptom-based prediction.
 * It uses Material Cards for a modern UI feel.
 */
public class ChoiceActivity extends AppCompatActivity {

    private TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        ttsManager = new TTSManager(this);

        // UI Components - Matching the new activity_choice.xml IDs
        MaterialCardView cardImage = findViewById(R.id.cardImage);
        MaterialCardView cardSymptoms = findViewById(R.id.cardSymptoms);
        FloatingActionButton fabAudio = findViewById(R.id.fabAudio);

        // Entrance Animations
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        if (cardImage != null) cardImage.startAnimation(slideUp);
        if (cardSymptoms != null) cardSymptoms.startAnimation(slideUp);

        // Audio Assistance logic
        if (fabAudio != null) {
            fabAudio.setOnClickListener(v -> {
                String content = getString(R.string.choice_prompt) + ". " +
                        getString(R.string.image) + ": " + getString(R.string.image_desc) + ". " +
                        getString(R.string.symptoms) + ": " + getString(R.string.symptoms_desc);
                ttsManager.speak(content);
            });
        }

        // Navigate to Camera/Image Detection
        if (cardImage != null) {
            cardImage.setOnClickListener(v -> {
                startActivity(new Intent(ChoiceActivity.this, MainActivity.class));
            });
        }

        // Navigate to Symptom-based Check
        if (cardSymptoms != null) {
            cardSymptoms.setOnClickListener(v -> {
                startActivity(new Intent(ChoiceActivity.this, SymptomActivity.class));
            });
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
