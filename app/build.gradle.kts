import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "API_KEY",
            "\"${localProperties.getProperty("Gemini_api_key")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    android {
        buildFeatures {
            buildConfig = true  // Enable BuildConfig
        }
        defaultConfig {
            buildConfigField("String", "API_KEY", "\"AIzaSyDucof92Ya0AeW1V2hZClqsJ_0ZXuNWL0k \"")  // Replace with your actual key
        }
    }

}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.7.1")

    // Core dependencies
    implementation("androidx.core:core:1.7.0")
    implementation("androidx.activity:activity-ktx:1.7.2")

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.4.0")

    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // --- Chatbot & UI Dependencies (Cleaned) ---
    implementation ("com.google.android.material:material:1.12.0") // Kept newest version
    implementation ("androidx.appcompat:appcompat:1.6.1")         // Kept one copy
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4") // Kept one copy
    implementation ("androidx.recyclerview:recyclerview:1.3.2")     // Kept one copy
    implementation ("com.android.volley:volley:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")// Kept one copy
}
