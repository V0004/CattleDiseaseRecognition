package com.programminghut.yolo_deploy;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DiseaseInfo extends AppCompatActivity {

    TextView infoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_info); // Make sure your layout file matches this

        infoTextView = findViewById(R.id.diseaseInfoTextView);

        ArrayList<String> topDiseases = getIntent().getStringArrayListExtra("top_diseases");
        Integer confirmed = getIntent().getIntExtra("confirmed", 0);

        List<String> diseaseList = readLinesFromAsset("disease_list.txt");
        List<String> precautionsList = readLinesFromAsset("precaution_list.txt");

        StringBuilder precautionsDisplay = new StringBuilder();

        if (topDiseases != null) {
            if (confirmed == 1)
            {
                precautionsDisplay.append("Precautions for ").append(topDiseases.get(0).replace("_", " ").toUpperCase()).append(": ");
            }
            else
            {
                for (String disease : topDiseases) {
                    int index = -1;
                    for (int i = 0; i < diseaseList.size(); i++) {
                        if (diseaseList.get(i).trim().equalsIgnoreCase(disease.trim())) {
                            index = i;
                            break;
                        }
                    }

                    if (index != -1 && index < precautionsList.size()) {
                        precautionsDisplay.append("🔹 ").append(disease.replace("_", " ").toUpperCase()).append(":\n")
                                .append(precautionsList.get(index)).append("\n\n");
                    } else {
                        precautionsDisplay.append("🔹 ").append(disease).append(": Precautions not found.\n\n");
                    }
                }
            }
        } else {
            precautionsDisplay.append("No diseases provided.");
        }

        infoTextView.setText(precautionsDisplay.toString());
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
}
