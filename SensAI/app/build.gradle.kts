plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.andres.sensai"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.andres.sensai"
        minSdk = 26
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
<<<<<<< HEAD
<<<<<<< HEAD
    // Compose BOM (recomendado)
    implementation(platform("androidx.compose:compose-bom:2026.01.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.01"))

    // Material3 + icons (arregla Unresolved reference Icons/icons)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose (si lo usas)
    implementation("androidx.navigation:navigation-compose:2.9.0") // si ya tienes otra, mantenla

    // LocalLifecycleOwner (nuevo import) + compose runtime lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.5.3")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")

    // MediaPipe Tasks Vision (PoseLandmarker)
    implementation("com.google.mediapipe:tasks-vision:0.10.32")
=======
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
=======
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
}