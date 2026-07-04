package com.programminghut.yolo_deploy;

import android.graphics.RectF;
import androidx.annotation.NonNull;

/**
 * A standard class to represent a detection or classification result.
 */
public class Recognition {
    private final int id;
    private String label;
    private final float confidence;
    private RectF location;

    public Recognition(int id, String label, float confidence) {
        this(id, label, confidence, null);
    }

    public Recognition(int id, String label, float confidence, RectF location) {
        this.id = id;
        this.label = label;
        this.confidence = confidence;
        this.location = location;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public float getConfidence() { return confidence; }
    public RectF getLocation() { return location == null ? null : new RectF(location); }

    @NonNull
    @Override
    public String toString() {
        return String.format("%s (%.1f%%)", label, confidence * 100.0f);
    }
}
