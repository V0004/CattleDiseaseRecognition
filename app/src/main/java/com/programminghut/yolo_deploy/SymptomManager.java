package com.programminghut.yolo_deploy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SymptomManager {
    public static final List<String> SYMPTOMS = Arrays.asList(
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
    );

    public static final List<String> DISEASE_CLASSES = Arrays.asList(
            "mastitis", "blackleg", "bloat", "coccidiosis", "cryptosporidiosis",
            "displaced_abomasum", "gut_worms", "listeriosis", "liver_fluke", "necrotic_enteritis",
            "peri_weaning_diarrhoea", "rift_valley_fever", "rumen_acidosis", "traumatic_reticulitis",
            "calf_diphtheria", "foot_rot", "foot_and_mouth", "ragwort_poisoning", "wooden_tongue",
            "infectious_bovine_rhinotracheitis", "acetonaemia", "fatty_liver_syndrome", "calf_pneumonia",
            "schmallen_berg_virus", "trypanosomosis", "fog_fever"
    );

    private static final Map<String, String> TRANSLATIONS_HI = new HashMap<>();
    static {
        // Symptoms Hindi
        TRANSLATIONS_HI.put("anorexia", "भूख न लगना");
        TRANSLATIONS_HI.put("abdominal_pain", "पेट दर्द");
        TRANSLATIONS_HI.put("anaemia", "खून की कमी");
        TRANSLATIONS_HI.put("fever", "बुखार");
        TRANSLATIONS_HI.put("diarrhoea", "दस्त");
        TRANSLATIONS_HI.put("dehydration", "निर्जलीकरण");
        TRANSLATIONS_HI.put("coughing", "खांसी");
        TRANSLATIONS_HI.put("weakness", "कमजोरी");
        TRANSLATIONS_HI.put("lameness", "लंगड़ापन");
        
        // Diseases Hindi
        TRANSLATIONS_HI.put("mastitis", "थनैला रोग");
        TRANSLATIONS_HI.put("foot_and_mouth", "खुरपका और मुँहपका रोग");
        TRANSLATIONS_HI.put("blackleg", "लंगड़ा बुखार");
        TRANSLATIONS_HI.put("bloat", "आफरा");
        TRANSLATIONS_HI.put("healthy", "स्वस्थ");
        TRANSLATIONS_HI.put("foot and mouth disease", "खुरपका और मुँहपका रोग");
        TRANSLATIONS_HI.put("lumpy skin disease", "लम्पी त्वचा रोग");
    }

    public static String formatSymptomName(String symptom) {
        return translate(symptom, true);
    }

    public static String translate(String key, boolean capitalize) {
        if (key == null) return "";
        
        boolean isHindi = Locale.getDefault().getLanguage().equals("hi");
        
        if (isHindi) {
            String hindi = TRANSLATIONS_HI.get(key.toLowerCase());
            if (hindi != null) return hindi;
        }

        // Default to English
        String eng = key.replace("_", " ");
        if (capitalize) {
            eng = eng.substring(0, 1).toUpperCase() + eng.substring(1);
        }
        return eng;
    }
}
