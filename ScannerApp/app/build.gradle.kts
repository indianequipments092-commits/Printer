plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.indianequipments.usbscanner"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.indianequipments.usbscanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
    }
    buildTypes {
        release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
}
