package com.programminghut.yolo_deploy.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSManager {
    private TextToSpeech tts;
    private boolean isInitialized = false;

    public TTSManager(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("hi", "IN")); // Set to Hindi/English based on selection
                // Logic to set language based on app locale can be added here
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "Language not supported");
                } else {
                    isInitialized = true;
                }
            } else {
                Log.e("TTSManager", "Initialization failed");
            }
        });
    }

    public void speak(String text) {
        if (isInitialized && tts != null) {
            // Use standard locale setting from the app
            tts.setLanguage(Locale.getDefault());
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
