plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Añade KSP (puedes mezclar alias y plugin directo)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"



}




android {
    namespace = "com.sebas.tiendaropa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sebas.tiendaropa"
        minSdk = 24          // Compose recomienda 24+ (puedes dejar 29 si quieres)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // 1) Habilita Compose
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Usa la sugerida por tu IDE si difiere
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // 2) Compose
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:1.7.3")

    implementation("androidx.compose.ui:ui-tooling-preview:1.7.3")
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.3")

    // (Opcional) Navigation Compose, si luego la usamos
    // implementation("androidx.navigation:navigation-compose:2.8.1")

    // Lifecycle / coroutines (para viewModelScope y Flow.stateIn)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // 3) Room
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

// Cargar imagen del logo (URI) en Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

// Íconos Material (ya lo venías usando)
    implementation("androidx.compose.material:material-icons-extended:1.7.3")


    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
