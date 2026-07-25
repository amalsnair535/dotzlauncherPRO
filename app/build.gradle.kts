import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dotz.launcherpro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dotz.launcherpro"
        minSdk = 24
        targetSdk = 36
        versionCode = 48
        versionName = "7.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = "C:/Users/USER/Downloads/DOTZLAUNCHERPRO KEYSTORE/KEYSTORE"
            val keystoreFile = file(keystorePath)

            // Manually load local.properties
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }

            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                keyAlias = "key0"
 
                // Read from localProperties, fallback to project properties or environment variables
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                    ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
                    ?: System.getenv("RELEASE_STORE_PASSWORD")

                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                    ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null && releaseSigningConfig.storePassword != null && releaseSigningConfig.keyPassword != null) {
                signingConfig = releaseSigningConfig
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            ndk {
                debugSymbolLevel = "FULL"
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null && releaseSigningConfig.storePassword != null && releaseSigningConfig.keyPassword != null) {
                signingConfig = releaseSigningConfig
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
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
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.session)
    implementation(libs.google.billing)
    implementation(libs.google.ads)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.gson)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.ui.tooling)
}
