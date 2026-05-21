plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.example.shiftalarm"
    compileSdk = 35 // Рекомендую 35

    defaultConfig {
        applicationId = "com.example.shiftalarm"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.9.2"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // ViewModel для Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Дополнительно: расширенные иконки Material (если используются)
    implementation("androidx.compose.material:material-icons-extended")

    // Для работы с корутинами (если ещё нет)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}