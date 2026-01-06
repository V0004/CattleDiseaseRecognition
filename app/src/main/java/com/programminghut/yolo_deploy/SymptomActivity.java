package com.programminghut.yolo_deploy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SymptomActivity extends AppCompatActivity {

    // Complete list of 93 symptoms
    String[] symptoms = {
            "anorexia", "abdominal_pain", "anaemia", "abortions", "acetone", "aggression", "arthrogyposis", "ankylosis", "anxiety", "bellowing", "blood_loss",
            "blood_poisoning", "blisters", "colic", "Condemnation_of_livers", "conjunctivae", "coughing", "depression", "discomfort", "dyspnea", "dysentery",
            "diarrhoea", "dehydration", "drooling", "dull", "decreased_fertility", "diffculty_breath", "emaciation", "encephalitis", "fever", "facial_paralysis",
            "frothing_of_mouth", "frothing", "gaseous_stomach", "highly_diarrhoea", "high_pulse_rate", "high_temp", "high_proportion", "hyperaemia", "hydrocephalus",
            "isolation_from_herd", "infertility", "intermittent_fever", "jaundice", "ketosis", "loss_of_appetite", "lameness", "lack_of-coordination", "lethargy",
            "lacrimation", "milk_flakes", "milk_watery", "milk_clots", "mild_diarrhoea", "moaning", "mucosal_lesions", "milk_fever", "nausea", "nasel_discharges",
            "oedema", "pain", "painful_tongue", "pneumonia", "photo_sensitization", "quivering_lips", "reduction_milk_vields", "rapid_breathing", "rumenstasis",
            "reduced_rumination", "reduced_fertility", "reduced_fat", "reduces_feed_intake", "raised_breathing", "stomach_pain", "salivation", "stillbirths",
            "shallow_breathing", "swollen_pharyngeal", "swelling", "saliva", "swollen_tongue", "tachycardia", "torticollis", "udder_swelling", "udder_heat",
            "udder_hardeness", "udder_redness", "udder_pain", "unwillingness_to_move", "ulcers", "vomiting", "weight_loss", "weakness"
    };
    ArrayList<CheckBox> allCheckboxes = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom);

        Intent intent = getIntent();

        String classification = intent.getStringExtra("classification");
        String confidence = intent.getStringExtra("confidence");
        String modelUsed = intent.getStringExtra("modelUsed");

        Button submitButton = findViewById(R.id.submitButton);
        // Dynamically add checkboxes (2 per row)
        TableLayout container = findViewById(R.id.checkboxContainer);
        container.removeAllViews(); // Clear existing views

        for (int i = 0; i < symptoms.length; i += 2) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            // First checkbox
            CheckBox cb1 = new CheckBox(this);
            cb1.setText(symptoms[i]);
            cb1.setTextSize(14);
            row.addView(cb1);
            allCheckboxes.add(cb1);

            // Second checkbox (if it exists)
            if (i + 1 < symptoms.length) {
                CheckBox cb2 = new CheckBox(this);
                cb2.setText(symptoms[i + 1]);
                cb2.setTextSize(14);
                addCheckboxWithLimitListener(cb2);
                row.addView(cb2);
                allCheckboxes.add(cb2);
            } else {
                // Add empty space if only one checkbox in the last row
                Space space = new Space(this);
                space.setLayoutParams(new TableRow.LayoutParams(
                        0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // Even spacing
                row.addView(space);
            }

            container.addView(row);
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> selectedSymptoms = getSelectedSymptoms();

                if (selectedSymptoms.isEmpty()) {
                    Toast.makeText(SymptomActivity.this, "Please select at least one symptom", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    DiseasePredictor model = new DiseasePredictor(getApplicationContext());
                    Map<String, Float> predictions = model.predictDisease(selectedSymptoms);
                    model.close();

                    List<Map.Entry<String, Float>> sortedList = new ArrayList<>(predictions.entrySet());
                    sortedList.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

                    ArrayList<String> topDiseases = new ArrayList<>();

                    for (int i = 0; i < Math.min(3, sortedList.size()); i++) {
                        Map.Entry<String, Float> entry = sortedList.get(i);
                        topDiseases.add(entry.getKey());
                    }

                    if (sortedList.size() >= 2) {
                        float top1 = sortedList.get(0).getValue();
                        float top2 = sortedList.get(1).getValue();

                        if (Math.abs(top1 - top2) < 2.0f) {
                            Toast.makeText(SymptomActivity.this, "Top results are close. Select more symptoms for better accuracy.", Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(SymptomActivity.this, ResultActivity.class);
                            intent.putStringArrayListExtra("top_diseases", topDiseases);
                            intent.putExtra("confirm",0);
                            if (getIntent().hasExtra("classification")) {
                                intent.putExtra("classification", getIntent().getStringExtra("classification"));
                                intent.putExtra("confidence", getIntent().getStringExtra("confidence"));
                                intent.putExtra("modelUsed", getIntent().getStringExtra("modelUsed"));
                            }
                            startActivity(intent);
                        }
                        else
                        {
                            Toast.makeText(SymptomActivity.this, "Here is the top result", Toast.LENGTH_LONG).show();

                            ArrayList<String> confirmedTopDisease = new ArrayList<>();
                            confirmedTopDisease.add(sortedList.get(0).getKey());

                            Intent intent = new Intent(SymptomActivity.this, ResultActivity.class);
                            intent.putStringArrayListExtra("top_diseases", confirmedTopDisease);
                            intent.putExtra("confirm",1);
                            if (getIntent().hasExtra("classification")) {
                                intent.putExtra("classification", getIntent().getStringExtra("classification"));
                                intent.putExtra("confidence", getIntent().getStringExtra("confidence"));
                                intent.putExtra("modelUsed", getIntent().getStringExtra("modelUsed"));
                            }
                            startActivity(intent);
                        }
                    }
                    else
                    {
                        Intent intent = new Intent(SymptomActivity.this, ResultActivity.class);
                        intent.putStringArrayListExtra("top_diseases", topDiseases);
                        if (getIntent().hasExtra("classification")) {
                            intent.putExtra("classification", getIntent().getStringExtra("classification"));
                            intent.putExtra("confidence", getIntent().getStringExtra("confidence"));
                            intent.putExtra("modelUsed", getIntent().getStringExtra("modelUsed"));
                        }
                        startActivity(intent);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(SymptomActivity.this, "Error loading model", Toast.LENGTH_SHORT).show();
                }
            }

            private List<String> getSelectedSymptoms()
            {
                List<String> selected = new ArrayList<>();
                for (CheckBox cb : allCheckboxes)
                {
                    if (cb.isChecked())
                    {
                        selected.add(cb.getText().toString());
                    }
                }
                return selected;
            }
        });
    }

    private void addCheckboxWithLimitListener(CheckBox checkBox)
    {
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
        {
            int checkedCount = 0;
            for (CheckBox cb : allCheckboxes)
            {
                if (cb.isChecked()) checkedCount++;
            }

            if (checkedCount >= 10)
            {
                for (CheckBox cb : allCheckboxes)
                {
                    if (!cb.isChecked())
                    {
                        cb.setEnabled(false);
                    }
                }
                Toast.makeText(SymptomActivity.this, "Maximum 10 symptoms can be selected", Toast.LENGTH_SHORT).show();
            } else {
                for (CheckBox cb : allCheckboxes)
                {
                    cb.setEnabled(true);
                }
            }
        });
    }
}
