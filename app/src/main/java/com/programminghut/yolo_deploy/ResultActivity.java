package com.programminghut.yolo_deploy;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Get data from intent
        Intent intent = getIntent();

        TextView resultTextView = findViewById(R.id.resultTextView);
        TextView accurateTextView = findViewById(R.id.accurate);
        accurateTextView.setVisibility(View.VISIBLE);
        ImageView backArrow = findViewById(R.id.backArrow);
        Button symptomButton = findViewById(R.id.symptomButton);
        Button precautionButton = findViewById(R.id.precautionButton);
        symptomButton.setVisibility(View.VISIBLE);
        precautionButton.setVisibility(View.GONE);


        ArrayList<String> top_diseases = intent.getStringArrayListExtra("top_diseases");
        Integer confirmed = intent.getIntExtra("confirmed", 0);
        String classification = intent.getStringExtra("classification");
        String confidence = intent.getStringExtra("confidence");
        String modelUsed = intent.getStringExtra("modelUsed");

        if (top_diseases != null & classification != null) {
            StringBuilder display = new StringBuilder();

            display.append("🔍 Image-based Prediction:\n")
                    .append("Disease: ").append(classification.replace("_", " ").toUpperCase()).append("\n")
                    .append("Model: ").append(modelUsed).append("\n")
                    .append("Confidence: ").append(confidence).append("\n\n");

            display.append("🩺 Symptom-based Top Disease:\n")
                    .append(top_diseases.get(0).replace("_", " ").toUpperCase()).append("\n");

            resultTextView.setText(display.toString());
        }
        else if (top_diseases != null)
        {
            accurateTextView.setVisibility(View.GONE);
            if (!top_diseases.isEmpty() && confirmed == 0)
            {
                StringBuilder resultBuilder = new StringBuilder();
                for (String disease : top_diseases) {
                    resultBuilder.append("• ").append(disease).append("\n");
                }
                resultTextView.setText(resultBuilder.toString());
            } else if(!top_diseases.isEmpty() && confirmed == 1){
                resultTextView.setText("Prediction: " + top_diseases.get(0));
            } else {
                resultTextView.setText("No diseases found.");
            }

            precautionButton.setVisibility(View.VISIBLE);
            precautionButton.setOnClickListener(v -> {
                Intent infoIntent = new Intent(ResultActivity.this, DiseaseInfo.class);
                infoIntent.putExtra("confirmed", confirmed);
                infoIntent.putStringArrayListExtra("top_diseases", top_diseases);
                startActivity(infoIntent);
            });

            symptomButton.setText("Enter More Symptoms");
            symptomButton.setOnClickListener(v -> finish());
        } else if (classification.equals("Healthy"))
        {
            resultTextView.setText("Prediction: " + classification);
            symptomButton.setText("Enter More Symptoms");
            symptomButton.setOnClickListener(v -> finish());
        } else {
            assert classification != null;
            if(classification.equals("Foot and Mouth Disease")) {
                resultTextView.setText(
                        "Prediction:\n" + classification +
                                "\n\n 🚨 Reasons: \nVirus Spread | Direct contact | Contaminated surface" +
                                "\n 🩹 First Aid:\n Isolate cattle | Clean mouth | Provide water" +
                                "\n 🛑 Prevention: \nVaccinate regularly | Disinfect premises | Limit Movement"
                );

                if(Float.valueOf(confidence) <= 0.75f)
                {
                    Toast.makeText(ResultActivity.this, "Try to add symptoms to get more accurate results.", Toast.LENGTH_SHORT).show();
                }

                symptomButton.setOnClickListener(v ->
                {
                    Intent symptomIntent = new Intent(ResultActivity.this, SymptomActivity.class);

                    intent.putExtra("classification", classification);
                    intent.putExtra("confidence", confidence);
                    intent.putExtra("modelUsed", modelUsed);

                    startActivity(symptomIntent);
                });
            } else {
                resultTextView.setText(
                        "Prediction:\n" + classification +
                                "\n\n 🚨 Reasons: \nInsect bites | Direct contact | Contaminated food" +
                                "\n 🩹 First Aid:\n Isolate cattle | Clean wounds | Provide water" +
                                "\n 🛑 Prevention: \nVaccinate early | Use insect repellent | Quarantine New"
                );

                symptomButton.setOnClickListener(v ->
                {
                    Intent symptomIntent = new Intent(ResultActivity.this, SymptomActivity.class);
                    startActivity(symptomIntent);
                });
            }
        }

        // Handle back button
        backArrow.setOnClickListener(v -> finish()); // Close activity and go back
    }
}