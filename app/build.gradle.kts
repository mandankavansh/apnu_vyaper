plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.anvexgroup.sheharsetu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anvexgroup.sheharsetu"
        minSdk = 23
        //noinspection OldTargetApi
        targetSdk = 35
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
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Google Play Services
    implementation(libs.play.services.location)

    // Networking
    implementation("com.android.volley:volley:1.2.1")

    // Image Loading (Glide + OkHttp integration)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // UI
    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    // ⚡ Cloud Messaging – REQUIRED for push notifications
    implementation("com.google.firebase:firebase-messaging")
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}