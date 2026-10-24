plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.biomsAfterWirelessConnectedVersion"
    compileSdk = 35

    buildFeatures {
        dataBinding {
            viewBinding = true
            enable = true  // Correct syntax for Kotlin DSL
        }
    }

    defaultConfig {
        applicationId = "com.example.biomsAfterWirelessConnectedVersion"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
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

        jniLibs {
//            useLegacyPackaging = true
            pickFirsts += setOf(
                "lib/arm64-v8a/libc++_shared.so",
                "lib/armeabi-v7a/libc++_shared.so",
                "lib/x86/libc++_shared.so",
                "lib/x86_64/libc++_shared.so",
                "lib/arm64-v8a/libmediapipe_tasks_vision_jni.so",
                "lib/armeabi-v7a/libmediapipe_tasks_vision_jni.so",
                "lib/x86/libmediapipe_tasks_vision_jni.so",
                "lib/x86_64/libmediapipe_tasks_vision_jni.so"
            )
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-analytics")
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
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.tbuonomo:dotsindicator:4.3")
    implementation("com.airbnb.android:lottie:6.0.0")
}