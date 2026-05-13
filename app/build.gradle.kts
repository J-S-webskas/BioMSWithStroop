plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bioms1"
    compileSdk = 35

    buildFeatures {
        dataBinding {
            enable = true  // Correct syntax for Kotlin DSL
        }
    }

    defaultConfig {
        applicationId = "com.example.bioms1"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
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

    packagingOptions {
        resources {
            excludes += listOf(
                "**/kotlin/**",
                "**/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }

        // This is the most important fix - pickFirst for MediaPipe libraries
        pickFirsts += listOf(
            "lib/**/libc++_shared.so",
            "lib/**/libmediapipe_tasks_vision_jni.so",
            "lib/**/libmediapipe_tasks_jni.so",
            "lib/**/libmediapipe_jni.so",
            "lib/**/libtensorflowlite_jni.so",
            "lib/**/liblitert_jni.so"
        )
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.camera.view)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // MediaPipe from TOML
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.mediapipe.tasks.core)
//    implementation("com.google.mediapipe:tasks-vision:0.10.11")
//    implementation("com.google.mediapipe:tasks-core:0.10.13")
    implementation("com.google.ai.edge.litert:litert-api:1.4.1")

    implementation(libs.core)

    // MPAndroidChart from TOML
    implementation(libs.mpandroidchart)

    // CameraX from TOML
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)

    // Room Database for storing results
    implementation("androidx.room:room-runtime:2.5.2")
    annotationProcessor("androidx.room:room-compiler:2.5.2")

    implementation("com.github.anastr:speedviewlib:1.6.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")

    implementation("com.google.code.gson:gson:2.10.1")
}