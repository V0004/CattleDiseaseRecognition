plugins {
    id("com.android.application")
}

android {
    namespace = "com.programminghut.yolo_deploy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.programminghut.yolo_deploy"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        mlModelBinding = false 
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Ensure 2.17.0 native libraries are prioritized
            pickFirsts += "**/libtensorflowlite_jni.so"
            pickFirsts += "**/libtensorflowlite_gpu_jni.so"
        }
    }

    // Force 2.17.0 globally to ensure version 12 opcodes are supported
    configurations.all {
        resolutionStrategy {
            force("org.tensorflow:tensorflow-lite:2.17.0")
            force("org.tensorflow:tensorflow-lite-gpu:2.17.0")
            force("org.tensorflow:tensorflow-lite-support:0.5.0")
            force("org.tensorflow:tensorflow-lite-metadata:0.5.0")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    
    implementation("com.github.yalantis:ucrop:2.2.6")
    
    // Core TFLite 2.17.0
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.5.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
