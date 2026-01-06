package com.programminghut.yolo_deploy;

import android.graphics.RectF;

public class Recognition {

    private Integer labelId;
    private String labelName;
    private Float labelScore;
    private Float confidence;
    private RectF location;

    public Recognition(final int labelId, final String labelName, final Float labelScore, final Float confidence, final RectF location)
    {
        this.labelId = labelId;
        this.labelScore = labelScore;
        this.labelName = labelName;
        this.confidence = confidence;
        this.location = location;
    }

    public Recognition(int labelId, String labelName, float confidence)
    {
        this(labelId, labelName, confidence, confidence, null);  // Calls main constructor
    }

    public Integer getLabelId() {
        return labelId;
    }

    public String getLabelName() {
        return labelName;
    }

    public float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }


    @Override
    public String toString()
    {
        String resultString = "";

        resultString += labelId + " ";

        if (labelName != null) {
            resultString += labelName + " ";
        }

        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f);
        }

        if (location != null) {
            resultString += location + " ";
        }

        return resultString.trim();
    }

    public String getLabel() {
        return labelName;
    }
}

