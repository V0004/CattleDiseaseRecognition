package com.programminghut.yolo_deploy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.programminghut.yolo_deploy.utils.TTSManager;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    
    private TextView resultTextView, accurateTextView;
    private Button symptomButton, precautionButton;
    private TTSManager ttsManager;
    private ImageView backArrow;
    private MaterialCardView cardResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ttsManager = new TTSManager(this);

        initViews();
        handleIntentData();
        setupAudioButton();
        
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        cardResult.startAnimation(slideUp);
    }

    private void initViews() {
        resultTextView = findViewById(R.id.resultTextView);
        accurateTextView = findViewById(R.id.accurate);
        symptomButton = findViewById(R.id.symptomButton);
        precautionButton = findViewById(R.id.precautionButton);
        backArrow = findViewById(R.id.backArrow);
        cardResult = findViewById(R.id.cardResult);
        
        backArrow.setOnClickListener(v -> finish());
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        ArrayList<String> topDiseases = intent.getStringArrayListExtra("top_diseases");
        String classification = intent.getStringExtra("classification");
        String confidence = intent.getStringExtra("confidence");
        String modelUsed = intent.getStringExtra("modelUsed");
        int confirmed = intent.getIntExtra("confirmed", 0);

        if (topDiseases != null && classification != null) {
            displayCombinedResults(classification, modelUsed, confidence, topDiseases);
        } else if (topDiseases != null) {
            displaySymptomResults(topDiseases, confirmed);
        } else if (classification != null) {
            displayImageResults(classification, confidence, modelUsed);
        }
    }

    private void setupAudioButton() {
        FloatingActionButton fabAudio = findViewById(R.id.fabAudio);
        fabAudio.setOnClickListener(v -> ttsManager.speak(resultTextView.getText().toString()));
    }

    @SuppressLint("SetTextI18n")
    private void displayCombinedResults(String label, String model, String conf, ArrayList<String> symptoms) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 ").append(getString(R.string.image_based_prediction)).append(":\n")
          .append(getString(R.string.disease)).append(": ").append(SymptomManager.translate(label, true).toUpperCase()).append("\n")
          .append(getString(R.string.model)).append(": ").append(model).append("\n")
          .append(getString(R.string.confidence)).append(": ").append(conf).append("\n\n")
          .append("🩺 ").append(getString(R.string.symptom_based_top_disease)).append(":\n")
          .append(SymptomManager.translate(symptoms.get(0), true).toUpperCase());
        
        resultTextView.setText(sb.toString());
        symptomButton.setVisibility(View.GONE); // Already verified
        accurateTextView.setVisibility(View.GONE);
    }

    private void displaySymptomResults(ArrayList<String> diseases, int confirmed) {
        accurateTextView.setVisibility(View.GONE);
        precautionButton.setVisibility(View.VISIBLE);
        
        if (confirmed == 1 && !diseases.isEmpty()) {
            resultTextView.setText(getString(R.string.prediction) + ": " + SymptomManager.translate(diseases.get(0), true));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.results)).append(":\n\n");
            for (String d : diseases) {
                sb.append("• ").append(SymptomManager.translate(d, true)).append("\n");
            }
            resultTextView.setText(sb.toString());
        }

        precautionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DiseaseInfo.class);
            intent.putExtra("confirmed", confirmed);
            intent.putStringArrayListExtra("top_diseases", diseases);
            startActivity(intent);
        });
        
        symptomButton.setText(R.string.enter_more_symptoms);
        symptomButton.setOnClickListener(v -> finish());
    }

    private void displayImageResults(String label, String conf, String model) {
        String translatedLabel = SymptomManager.translate(label, true);
        if ("Healthy".equalsIgnoreCase(label)) {
            resultTextView.setText(getString(R.string.prediction) + ": " + translatedLabel);
            symptomButton.setText(R.string.enter_symptoms_to_verify);
        } else {
            DiseaseInfoProvider.Info info = DiseaseInfoProvider.getInfo(this, label);
            if (info != null) {
                resultTextView.setText(String.format("%s:\n%s\n\n 🚨 %s:\n%s\n\n 🩹 %s:\n%s\n\n 🛑 %s:\n%s",
                        getString(R.string.prediction), translatedLabel, 
                        getString(R.string.reasons), info.reasons, 
                        getString(R.string.first_aid), info.firstAid, 
                        getString(R.string.prevention), info.prevention));
            } else {
                resultTextView.setText(getString(R.string.prediction) + ": " + translatedLabel);
            }

            try {
                if (conf != null && Float.parseFloat(conf.replace("%", "").trim()) <= 75.0f) {
                    Toast.makeText(this, R.string.try_adding_symptoms, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) { }
        }
        
        symptomButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SymptomActivity.class);
            intent.putExtra("classification", label);
            intent.putExtra("confidence", conf);
            intent.putExtra("modelUsed", model);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) ttsManager.shutdown();
        super.onDestroy();
    }
}
