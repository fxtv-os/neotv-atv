plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neoos.neotv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neoos.app.tv.androidtv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.2.3_genericandroidtvdevice"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Suffix entfernt, damit die ID identisch bleibt
            applicationIdSuffix = "" 
        }
    }

    // Benennt APKs beim Bauen universell um
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val baseName = "${variant.applicationId}_${variant.versionName}"
            output?.outputFileName = "$baseName.apk"
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Media3 (ExoPlayer) for HLS (.m3u8) playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-ui-leanback:1.3.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.picasso:picasso:2.8")
}

// Benennt AABs beim Bauen universell um
tasks.matching { it.name.startsWith("bundle") }.all {
    doLast {
        val buildDir = layout.buildDirectory.get().asFile
        val baseBundleDir = File(buildDir, "outputs/bundle")
        val newName = "com.neoos.app.tv.androidtv_1.2.1_genericandroidtvdevice.aab"
        
        File(baseBundleDir, "release/app-release.aab").takeIf { it.exists() }?.renameTo(File(baseBundleDir, "release/$newName"))
        File(baseBundleDir, "debug/app-debug.aab").takeIf { it.exists() }?.renameTo(File(baseBundleDir, "debug/$newName"))
    }
}