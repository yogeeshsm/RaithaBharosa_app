import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

val envProperties = Properties()
val envFile = file(".env")
if (envFile.exists()) {
    envProperties.load(FileInputStream(envFile))
}

android {
    namespace = "com.raitha.bharosa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.raitha.bharosa"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // API keys – provide values via app/.env file (see README for setup)
        buildConfigField("String", "AGMARKNET_API_KEY", "\"${envProperties.getProperty("Current Daily Price of Various Commodities from Various Markets (Mandi)") ?: ""}\"")
        buildConfigField("String", "WEATHER_API_KEY", "\"${envProperties.getProperty("WEATHER_API") ?: ""}\"")

        
        // Twilio SMS API credentials
        buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"${envProperties.getProperty("TWILIO_ACCOUNT_SID") ?: ""}\"")
        buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"${envProperties.getProperty("TWILIO_AUTH_TOKEN") ?: ""}\"")
        buildConfigField("String", "TWILIO_PHONE_NUMBER", "\"${envProperties.getProperty("TWILIO_PHONE_NUMBER") ?: ""}\"")
        
        // WhatsApp Business API credentials
        buildConfigField("String", "WHATSAPP_API_KEY", "\"${envProperties.getProperty("WHATSAPP_API_KEY") ?: ""}\"")
        buildConfigField("String", "WHATSAPP_PHONE_NUMBER_ID", "\"${envProperties.getProperty("WHATSAPP_PHONE_NUMBER_ID") ?: ""}\"")
        buildConfigField("String", "WHATSAPP_BUSINESS_ACCOUNT_ID", "\"${envProperties.getProperty("WHATSAPP_BUSINESS_ACCOUNT_ID") ?: ""}\"")
        
        // Razorpay Payment Gateway credentials
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"${envProperties.getProperty("RAZORPAY_KEY_ID") ?: ""}\"")
        buildConfigField("String", "RAZORPAY_KEY_SECRET", "\"${envProperties.getProperty("RAZORPAY_KEY_SECRET") ?: ""}\"")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Task to copy .env file to assets for runtime access (only if it exists)
tasks.register<Copy>("copyEnvToAssets") {
    if (envFile.exists()) {
        from(file(".env"))
        into(file("src/main/assets"))
    }
}

// Make the copy task run before processing assets
tasks.named("preBuild") {
    dependsOn("copyEnvToAssets")
}

dependencies {
    implementation(libs.androidx.core.ktx)
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
    implementation(libs.gson)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.auth)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    // Room (SQLite offline cache)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
