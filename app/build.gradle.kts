plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.v3d3.counters"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.v3d3.counters"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
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
    // Core Compose and Wear OS Compose dependencies
    implementation(platform(libs.compose.bom)) // Provides consistent versions for Compose libraries
    implementation(libs.ui) // Core UI elements and modifiers
    implementation(libs.ui.graphics) // Graphics primitives for Compose
    implementation(libs.ui.tooling.preview) // For Compose UI previews
    implementation(libs.compose.material) // Wear OS specific Material Design components
    implementation(libs.compose.foundation) // Foundation components, including HorizontalPager
    implementation(libs.activity.compose) // For ComponentActivity and setContent

    // Material Icons Extended for icons like Add, Minimize, Refresh
    implementation(libs.material.icons.extended)

    // Gson for JSON serialization/deserialization of Counter objects
    implementation(libs.gson)

    // Wear OS Tooling Preview for device previews
    implementation(libs.wear.tooling.preview)

    // Test dependencies (typically kept for unit and UI testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}