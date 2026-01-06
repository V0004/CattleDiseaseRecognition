// ChoiceActivity.java
package com.programminghut.yolo_deploy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class activity_choice extends AppCompatActivity
{
    Button btnImage, btnSymptoms, btnBoth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);

        btnImage = findViewById(R.id.btnImage);
        btnSymptoms = findViewById(R.id.btnSymptoms);

        btnImage.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        btnSymptoms.setOnClickListener(v -> startActivity(new Intent(this, SymptomActivity.class)));
    }
}
