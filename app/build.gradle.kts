plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // 👇 FIXED: Added specific version to force the plugin to run
    id("com.google.gms.google-services")

    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

android {
    namespace = "com.duddleTech.convoAI"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duddleTech.convoAI"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
    }

    // ✅ Ensure this path is correct: src/main/cpp/CMakeLists.txt
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    // Core & UI
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Activity
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Networking (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Firebase (Using BOM for version management)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Updated to newer BOM
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-config")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Google AdMob (Ads)
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Google Play Billing (Subscriptions)
    implementation("com.android.billingclient:billing:6.2.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil:2.6.0")
}

// 🔥 KEEPS APP STABLE ON API 35
configurations.all {
    resolutionStrategy {
        force("androidx.activity:activity:1.9.3")
        force("androidx.activity:activity-ktx:1.9.3")
        force("androidx.activity:activity-compose:1.9.3")
    }
}