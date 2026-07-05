package com.programminghut.yolo_deploy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.programminghut.yolo_deploy.utils.TTSManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DiseaseInfo extends AppCompatActivity {

    private TextView infoTextView;
    private TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_info);

        ttsManager = new TTSManager(this);
        infoTextView = findViewById(R.id.diseaseInfoTextView);

        ArrayList<String> topDiseases = getIntent().getStringArrayListExtra("top_diseases");
        Integer confirmed = getIntent().getIntExtra("confirmed", 0);

        List<String> diseaseList = readLinesFromAsset("disease_list.txt");
        List<String> precautionsList = readLinesFromAsset("precaution_list.txt");

        StringBuilder precautionsDisplay = new StringBuilder();

        if (topDiseases != null) {
            if (confirmed == 1) {
                precautionsDisplay.append(getString(R.string.precautions_for))
                        .append(" ")
                        .append(SymptomManager.translate(topDiseases.get(0), true))
                        .append(": ");
                
                int index = findDiseaseIndex(topDiseases.get(0), diseaseList);
                if (index != -1 && index < precautionsList.size()) {
                    precautionsDisplay.append("\n\n").append(precautionsList.get(index));
                }
            } else {
                for (String disease : topDiseases) {
                    int index = findDiseaseIndex(disease, diseaseList);

                    precautionsDisplay.append("🔹 ").append(SymptomManager.translate(disease, true)).append(":\n");
                    if (index != -1 && index < precautionsList.size()) {
                        precautionsDisplay.append(precautionsList.get(index)).append("\n\n");
                    } else {
                        precautionsDisplay.append(getString(R.string.precautions_not_found)).append("\n\n");
                    }
                }
            }
        } else {
            precautionsDisplay.append(getString(R.string.no_diseases_provided));
        }

        infoTextView.setText(precautionsDisplay.toString());

        setupAudioButton();
        setupMapButton();
        setupNavigation();
    }

    private void setupAudioButton() {
        FloatingActionButton fabAudio = findViewById(R.id.fabAudio);
        if (fabAudio != null) {
            fabAudio.setOnClickListener(v -> {
                String content = infoTextView.getText().toString();
                ttsManager.speak(content);
            });
        }
    }

    private void setupMapButton() {
        FloatingActionButton fabMap = findViewById(R.id.fabMap);
        if (fabMap != null) {
            fabMap.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=cow+veterinary+hospital");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    // Fallback to web browser maps search if Google Maps app is not available
                    Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=cow+veterinary+hospital");
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                    try {
                        startActivity(webIntent);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Maps app or browser not available", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void setupNavigation() {
        ImageView backArrow = findViewById(R.id.backArrow);
        if (backArrow != null) {
            backArrow.setOnClickListener(v -> finish());
        }
    }

    private int findDiseaseIndex(String disease, List<String> diseaseList) {
        for (int i = 0; i < diseaseList.size(); i++) {
            if (diseaseList.get(i).trim().equalsIgnoreCase(disease.trim())) {
                return i;
            }
        }
        return -1;
    }

    private List<String> readLinesFromAsset(String fileName) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        super.onDestroy();
    }
}
