package com.programminghut.yolo_deploy;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.programminghut.yolo_deploy.utils.TTSManager;

import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    private TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_welcome);

        ttsManager = new TTSManager(this);

        // UI Components for Animation
        MaterialCardView cardQuote = findViewById(R.id.cardQuote);
        ImageView imageViewCow = findViewById(R.id.imageViewCow);
        TextView textViewTitle = findViewById(R.id.textViewTitle);
        Button btnGetStarted = findViewById(R.id.btn_get_started);
        Button btnKnowMore = findViewById(R.id.knowmore);
        FloatingActionButton fabAudio = findViewById(R.id.fabAudio);
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.languageToggleGroup);

        // Load Animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        
        fadeIn.setDuration(1000);
        slideUp.setDuration(800);

        // Apply Animations
        cardQuote.startAnimation(fadeIn);
        imageViewCow.startAnimation(slideUp);
        textViewTitle.startAnimation(slideUp);
        btnGetStarted.startAnimation(slideUp);
        btnKnowMore.startAnimation(slideUp);

        // Language Toggle Setup
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("hi")) {
            toggleGroup.check(R.id.btnHindi);
        } else {
            toggleGroup.check(R.id.btnEnglish);
        }

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String selectedLang = (checkedId == R.id.btnHindi) ? "hi" : "en";
                if (!Locale.getDefault().getLanguage().equals(selectedLang)) {
                    setLocale(selectedLang);
                }
            }
        });

        // TTS setup
        fabAudio.setOnClickListener(v -> {
            String content = getString(R.string.quote) + ". " + 
                             getString(R.string.quote_source) + ". " + 
                             getString(R.string.welcome_title) + ". " + 
                             getString(R.string.get_started) + ". " + 
                             getString(R.string.how_to_use);
            ttsManager.speak(content);
        });

        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, ChoiceActivity.class);
            startActivity(intent);
            finish();
        });

        btnKnowMore.setOnClickListener(v -> {
            // Help/Usage logic
        });
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        recreate();
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        super.onDestroy();
    }
}
