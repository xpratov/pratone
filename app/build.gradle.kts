plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.pratone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pratone.app"
        minSdk = 26 // needed for MediaBrowserServiceCompat/Media3 modern APIs + AEC reliability
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 / ExoPlayer — playback engine + MediaSession + auto-generated media notification
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Room — local persistence for playlists / favorites / recently played (NOT the music library
    // itself, which always lives in MediaStore — Room only stores app-specific metadata)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Coil — album art loading from content:// URIs (MediaStore artwork), avoids hand-rolled
    // bitmap decoding/caching which is easy to get wrong and leak memory on.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Optional dedicated wake-word engine. Left as a compile-time dependency because the
    // WakeWordDetector interface is implemented against it (see voice/PorcupineWakeWordDetector.kt).
    // Requires a free personal AccessKey from https://console.picovoice.ai — see README section
    // "Configuring the wake-word engine". Comment out if you don't want the extra ~2MB and don't
    // plan to use Porcupine; the app still builds and runs on the SpeechRecognizer fallback alone
    // if you also remove/stub PorcupineWakeWordDetector.kt.
    implementation("ai.picovoice:porcupine-android:3.0.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
