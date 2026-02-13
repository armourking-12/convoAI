// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // 👇 ADD THE VERSION HERE! (4.4.2 is the latest stable)
    id("com.google.gms.google-services") version "4.4.2" apply false
}