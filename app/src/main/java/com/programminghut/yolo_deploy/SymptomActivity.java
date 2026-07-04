package com.programminghut.yolo_deploy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.programminghut.yolo_deploy.utils.TTSManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SymptomActivity extends AppCompatActivity {

    private final List<CheckBox> allCheckboxes = new ArrayList<>();
    private static final int MAX_SYMPTOMS = 10;
    private TTSManager ttsManager;
    private TableLayout checkboxContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom);

        ttsManager = new TTSManager(this);
        checkboxContainer = findViewById(R.id.checkboxContainer);

        setupSymptomGrid();
        setupSubmitButton();
        setupAudioButton();
        setupNavigation();
        setupSearch();
        
        // Entrance animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);
        checkboxContainer.startAnimation(fadeIn);
    }

    private void setupSymptomGrid() {
        checkboxContainer.removeAllViews();
        allCheckboxes.clear();

        List<String> symptoms = SymptomManager.SYMPTOMS;
        for (int i = 0; i < symptoms.size(); i += 2) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            row.addView(createSymptomCheckBox(symptoms.get(i)));

            if (i + 1 < symptoms.size()) {
                row.addView(createSymptomCheckBox(symptoms.get(i + 1)));
            }
            checkboxContainer.addView(row);
        }
    }

    private CheckBox createSymptomCheckBox(String symptomName) {
        CheckBox cb = new CheckBox(this);
        cb.setText(SymptomManager.formatSymptomName(symptomName));
        cb.setTag(symptomName);
        cb.setTextSize(14);
        cb.setPadding(8, 24, 8, 24);
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> handleSelectionLimit());
        allCheckboxes.add(cb);
        return cb;
    }

    private void handleSelectionLimit() {
        long checkedCount = allCheckboxes.stream().filter(CheckBox::isChecked).count();
        if (checkedCount >= MAX_SYMPTOMS) {
            allCheckboxes.forEach(cb -> {
                if (!cb.isChecked()) cb.setEnabled(false);
            });
            Toast.makeText(this, getString(R.string.max_symptoms_reached, MAX_SYMPTOMS), Toast.LENGTH_SHORT).show();
        } else {
            allCheckboxes.forEach(cb -> cb.setEnabled(true));
        }
    }

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSymptoms(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterSymptoms(String query) {
        String filter = query.toLowerCase().trim();
        checkboxContainer.removeAllViews();
        ArrayList<CheckBox> visibleCheckboxes = new ArrayList<>();
        
        for (CheckBox cb : allCheckboxes) {
            if (cb.getText().toString().toLowerCase().contains(filter)) {
                visibleCheckboxes.add(cb);
            }
        }

        for (int i = 0; i < visibleCheckboxes.size(); i += 2) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            
            CheckBox cb1 = visibleCheckboxes.get(i);
            if (cb1.getParent() != null) ((TableRow)cb1.getParent()).removeView(cb1);
            row.addView(cb1);

            if (i + 1 < visibleCheckboxes.size()) {
                CheckBox cb2 = visibleCheckboxes.get(i + 1);
                if (cb2.getParent() != null) ((TableRow)cb2.getParent()).removeView(cb2);
                row.addView(cb2);
            }
            checkboxContainer.addView(row);
        }
    }

    private void setupSubmitButton() {
        findViewById(R.id.submitButton).setOnClickListener(v -> {
            List<String> selected = getSelectedSymptoms();
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.select_one_symptom), Toast.LENGTH_SHORT).show();
                return;
            }
            performPrediction(selected);
        });
    }

    private void setupAudioButton() {
        FloatingActionButton fabAudio = findViewById(R.id.fabAudio);
        fabAudio.setOnClickListener(v -> {
            String content = getString(R.string.enter_symptoms) + ". " + 
                             getString(R.string.max_symptoms_reached, MAX_SYMPTOMS) + ". " +
                             getString(R.string.submit);
            ttsManager.speak(content);
        });
    }

    private void setupNavigation() {
        ImageView backArrow = findViewById(R.id.backArrow);
        if (backArrow != null) {
            backArrow.setOnClickListener(v -> finish());
        }
    }

    private List<String> getSelectedSymptoms() {
        List<String> selected = new ArrayList<>();
        for (CheckBox cb : allCheckboxes) {
            if (cb.isChecked()) selected.add((String) cb.getTag());
        }
        return selected;
    }

    private void performPrediction(List<String> selectedSymptoms) {
        try {
            DiseasePredictor predictor = new DiseasePredictor(this);
            Map<String, Float> predictions = predictor.predictDisease(selectedSymptoms);
            predictor.close();
            List<Map.Entry<String, Float>> sorted = new ArrayList<>(predictions.entrySet());
            sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            navigateToResults(sorted);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_prediction_model), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToResults(List<Map.Entry<String, Float>> results) {
        ArrayList<String> topDiseases = new ArrayList<>();
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            topDiseases.add(results.get(i).getKey());
        }

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putStringArrayListExtra("top_diseases", topDiseases);
        intent.putExtra("confirmed", (results.size() >= 2 && results.get(0).getValue() - results.get(1).getValue() > 2.0f) ? 1 : 0);
        
        if (getIntent().hasExtra("classification")) {
            intent.putExtra("classification", getIntent().getStringExtra("classification"));
            intent.putExtra("confidence", getIntent().getStringExtra("confidence"));
            intent.putExtra("modelUsed", getIntent().getStringExtra("modelUsed"));
        }
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) ttsManager.shutdown();
        super.onDestroy();
    }
}
