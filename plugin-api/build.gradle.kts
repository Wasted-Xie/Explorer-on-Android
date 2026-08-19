plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.explorercore.plugin"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        // For AIDL
        // We don't need applicationId for a library
    }

    buildFeatures {
        aidl = true
    }

    // Enable view binding if needed (though we're using Compose mostly)
    // buildFeatures {
    //     viewBinding = true
    // }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.annotation:annotation:1.6.0")
    // For Parcelable support in AIDL if needed
    // implementation("androidx.annotation:annotation:1.6.0")
}