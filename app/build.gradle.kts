import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val geminiApiKey: String = run {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        FileInputStream(file).use { props.load(it) }
    }
    props.getProperty("GEMINI_API_KEY", "")
}

android {
    namespace = "com.jeyar.mindease"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.jeyar.mindease"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}