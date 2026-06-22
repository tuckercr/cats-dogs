// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.devtoolsKsp) apply false
    alias(libs.plugins.daggerHiltAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics.gradle) apply false
}