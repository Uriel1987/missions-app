plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    // TODO: change this to something unique to you before your first install —
    // reusing a package/applicationId from an older project causes
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE errors on real devices.
    namespace = "com.ourhouse.missions"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ourhouse.missions"
        minSdk = 26
        targetSdk = 34
        // release.ps1 passes these (versionCode = git commit count); local builds fall back to 1.
        versionCode = (project.findProperty("appVersionCode") as String? ?: "1").toInt()
        versionName = project.findProperty("appVersionName") as String? ?: "1.0"
    }

    // Shared release keystore, configured in ~/.gradle/gradle.properties (RELEASE_STORE_FILE etc.,
    // never committed). Without those, release builds come out unsigned.
    val hasReleaseKey = project.hasProperty("RELEASE_STORE_FILE")
    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.activity:activity-compose:1.9.0")
    // Firebase/Play Services pull in an old Fragment transitively; release lint requires >= 1.3.0
    // for the ActivityResult API used in MainActivity.
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Local storage for "which household am I in" session state
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Local notifications: periodic background check + "is the app on screen?"
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-process:2.8.2")

    // Coroutines <-> Firestore listener bridging
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
