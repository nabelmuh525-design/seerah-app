plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.seerah.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.seerah.encyclopedia"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "3.0.0-alpha.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val envStoreFile = System.getenv("SEERAH_KEYSTORE_PATH")
    val envStorePass = System.getenv("SEERAH_STORE_PASSWORD")
    val envKeyAlias = System.getenv("SEERAH_KEY_ALIAS")
    val envKeyPass = System.getenv("SEERAH_KEY_PASSWORD")
    val hasReleaseSigning = !envStoreFile.isNullOrBlank() &&
        !envStorePass.isNullOrBlank() &&
        !envKeyAlias.isNullOrBlank() &&
        !envKeyPass.isNullOrBlank() &&
        file(envStoreFile).exists()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(envStoreFile!!)
                storePassword = envStorePass
                keyAlias = envKeyAlias
                keyPassword = envKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    implementation(project(":core"))
    implementation(project(":database"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":feature-home"))
    implementation(project(":feature-timeline"))
    implementation(project(":feature-atlas"))
    implementation(project(":feature-battles"))
    implementation(project(":feature-people"))
    implementation(project(":feature-places"))
    implementation(project(":feature-library"))
    implementation(project(":feature-search"))
    implementation(project(":feature-quran"))
    implementation(project(":feature-hadith"))
    implementation(project(":feature-settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
