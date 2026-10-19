plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Version comes from the tag in CI (v1.2.3 -> 1.2.3), default for local/PR builds.
val appVersion: String = (System.getenv("APP_VERSION") ?: "0.1.0").removePrefix("v")
val versionParts = appVersion.split("-")[0].split(".").map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
val appVersionCode = versionParts[0] * 10000 + versionParts[1] * 100 + versionParts[2]
val keystorePassword: String? = System.getenv("ANDROID_KEYSTORE_PASSWORD")

android {
    namespace = "com.groovex.livelooper.control"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.groovex.livelooper.control"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
        buildConfigField("String", "UPDATE_REPO", "\"thegroovex/Livelooper-control\"")
    }

    signingConfigs {
        create("release") {
            // PKCS12 keystore is committed (AES-256, strong random password); the password lives only in the
            // GitHub secret ANDROID_KEYSTORE_PASSWORD. Without it release builds stay unsigned.
            storeFile = rootProject.file("signing/release.p12")
            storeType = "pkcs12"
            storePassword = keystorePassword
            keyAlias = "livelooper-control"
            keyPassword = keystorePassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePassword != null) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        warningsAsErrors = false
        abortOnError = true
        disable += setOf("OldTargetApi", "GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
