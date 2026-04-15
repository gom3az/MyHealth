plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.huawei.agconnect)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.gomaa.healthy"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.gomaa.healthy"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    // HMS Core
    implementation(libs.agconnect.core)
    // TODO: Uncomment these when Health Kit is enabled in AppGallery Connect console (Phase 1)
    // Note: Health Kit SDK requires:
    // 1. Health Service Kit card enabled in AppGallery Connect
    // 2. Read permissions approved for DT_CONTINUOUS_STEPS_DELTA, DT_CONTINUOUS_HEART_RATE_STATISTICS
    // 3. Account Kit enabled for Huawei ID Sign-In
    // implementation(libs.huawei.health)  // Cloud-based health data SDK
    // implementation(libs.huawei.account) // Account Kit for OAuth

    // Wear Engine - Keep for now (can be removed after Health Kit migration complete)
    implementation(libs.wearengine)
    // DataStore Preferences
    implementation(libs.datastore.preferences)
    // Security - Encrypted SharedPreferences for OAuth tokens
    implementation(libs.security.crypto)
    // Health Connect
    implementation(libs.health.connect.client)
    // Paging3
    implementation(libs.paging.runtime.ktx)
    implementation(libs.paging.compose)
    // WorkManager for background sync
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}