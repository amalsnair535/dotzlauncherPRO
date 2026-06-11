plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dotz.launcherpro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dotz.launcherpro"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "5.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "store"
    productFlavors {
        create("google") {
            dimension = "store"
            applicationId = "com.dotz.launcherpro"
            versionNameSuffix = "-google"
        }
        create("indus") {
            dimension = "store"
            applicationId = "com.dotz.launcherpro"
            versionNameSuffix = "-indus"
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = "C:/Users/USER/Downloads/DOTZLAUNCHERPRO KEYSTORE/KEYSTORE"
            val keystoreFile = file(keystorePath)
            
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                keyAlias = "key0"

                // Reading passwords from project properties (gradle.properties)
                // You can add these to your global ~/.gradle/gradle.properties for security
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null && releaseSigningConfig.storePassword != null && releaseSigningConfig.keyPassword != null) {
                signingConfig = releaseSigningConfig
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
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.session)
    "googleImplementation"(libs.google.billing)
    "googleImplementation"("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.gson)
    implementation(libs.generativeai)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.ui.tooling)
}
