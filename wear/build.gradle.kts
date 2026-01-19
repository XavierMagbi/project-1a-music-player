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
        // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))

        // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.activity:activity-compose")

        // Wear Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

        // Wear Compose UI
    implementation("androidx.wear.compose:compose-material3:1.5.6")
    implementation("androidx.wear.compose:compose-foundation:1.5.6")

        // Icons
    implementation("androidx.compose.material:material-icons-extended")

        // Preview tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.wear:wear-tooling-preview:1.0.0")

        // Testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.core:core-splashscreen:1.0.1")
    }





