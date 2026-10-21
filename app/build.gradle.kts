plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Fixed signing key. It is NOT in this (public) repository: CI restores it from the GitHub secrets
// AK1_KEYSTORE_BASE64 / AK1_KEYSTORE_PASSWORD into these two variables (see docs/SIGNING.md).
// Without them the build falls back to the default per-machine debug key, so forks and local builds
// keep working; such APKs just cannot be installed over an APK signed with the fixed key.
val ak1Keystore: java.io.File? = System.getenv("AK1_KEYSTORE_PATH")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }
    ?.takeIf { it.exists() }
val ak1KeystorePassword: String? = System.getenv("AK1_KEYSTORE_PASSWORD")?.takeIf { it.isNotEmpty() }
val useAk1Key = ak1Keystore != null && ak1KeystorePassword != null

android {
    namespace = "com.kichikan.ak1"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.kichikan.ak1"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.9.0"
    }
    signingConfigs {
        if (useAk1Key) {
            create("ak1") {
                storeFile = ak1Keystore
                storePassword = ak1KeystorePassword
                keyAlias = "ak1"
                keyPassword = ak1KeystorePassword
                storeType = "pkcs12"
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (useAk1Key) signingConfig = signingConfigs.getByName("ak1")
        }
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
