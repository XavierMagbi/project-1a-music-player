plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.epfl.esl.musicplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.epfl.esl.musicplayer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        // BOM (Bill of Materials) - Manages versions for core Compose libraries
        implementation(platform("androidx.compose:compose-bom:2024.01.00")) // Use a recent BOM
        androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))

        // Core Compose libraries (versions are managed by the BOM)
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-graphics")
        implementation("androidx.activity:activity-compose")

        // Wearable Data Layer
        implementation("com.google.android.gms:play-services-wearable:18.1.0")

        // --- NEW WEAR COMPOSE MATERIAL 3 LIBRARIES ---
        implementation("androidx.wear.compose:compose-material3:1.5.6")
        implementation("androidx.wear.compose:compose-foundation") // Foundation for Wear

        // Icons library
        implementation("androidx.compose.material:material-icons-extended")

        // --- TOOLING FOR PREVIEWS (DEBUG ONLY) ---
        // Remove phone-specific tooling to fix WearDevices issue
        debugImplementation("androidx.compose.ui:ui-tooling")
        // Use the correct Wear OS tooling
        debugImplementation("androidx.wear.compose:compose-ui-tooling:1.5.6")

        // Testing
        androidTestImplementation("androidx.compose.ui:ui-test-junit4")
        debugImplementation("androidx.compose.ui:ui-test-manifest")
    }




