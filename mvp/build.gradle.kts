plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
 id("com.google.gms.google-services") version "4.4.2"
}
android {
 namespace = "com.couplereels.mvp"
 compileSdk = 35
 defaultConfig { applicationId = "com.couplereels.mvp"; minSdk = 26; targetSdk = 35; versionCode = 6; versionName = "0.6-private-projections"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 testOptions { unitTests.isIncludeAndroidResources = true }
 buildFeatures { compose = true; buildConfig = true }
 sourceSets { getByName("main") { java.srcDir("../app/src/main/java"); res.srcDir("../app/src/main/res") }; getByName("test").java.srcDir("../app/src/test/java") }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
// Missing client config must not prevent the local Fake build.
tasks.matching { it.name.contains("GoogleServices") }.configureEach { enabled = file("google-services.json").exists() }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.02.00"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
 implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
 implementation("com.google.firebase:firebase-auth")
 implementation("com.google.firebase:firebase-firestore")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
 androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.14.1")
 testImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}
