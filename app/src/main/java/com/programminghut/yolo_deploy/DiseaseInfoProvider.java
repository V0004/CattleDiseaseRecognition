package com.programminghut.yolo_deploy;

import android.content.Context;

public class DiseaseInfoProvider {
    public static class Info {
        public final String reasons;
        public final String firstAid;
        public final String prevention;

        public Info(String reasons, String firstAid, String prevention) {
            this.reasons = reasons;
            this.firstAid = firstAid;
            this.prevention = prevention;
        }
    }

    public static Info getInfo(Context context, String disease) {
        if (disease == null) return null;
        
        String trimmed = disease.trim();
        if (trimmed.equalsIgnoreCase("Foot and Mouth Disease")) {
            return new Info(
                    context.getString(R.string.fmd_reasons),
                    context.getString(R.string.fmd_first_aid),
                    context.getString(R.string.fmd_prevention)
            );
        } else if (trimmed.equalsIgnoreCase("Lumpy Skin Disease")) {
            return new Info(
                    context.getString(R.string.lsd_reasons),
                    context.getString(R.string.lsd_first_aid),
                    context.getString(R.string.lsd_prevention)
            );
        }
        return null;
    }
}
