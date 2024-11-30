import java.util.Properties
import java.io.FileInputStream

val apikeyPropertiesFile = rootProject.file("apikey.properties")
val apikeyProperties = Properties().apply {
    load(FileInputStream(apikeyPropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}



android {
    namespace = "com.group1.scansaver"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.group1.scansaver"
        minSdk = 26
        targetSdk = 34
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {

        buildConfigField(
            "String",
            "API_KEY",
            "\"${apikeyProperties["API_KEY"]}\""
        )

    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.legacy.support.v4)
    implementation(libs.activity)
    implementation(libs.camera.core)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //implementation("org.maplibre.gl:android-sdk:9.5.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation ("com.google.guava:guava:31.1-android")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")

    implementation (libs.androidx.camera.core.v150)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle.v150)
    implementation (libs.camera.view.v150)

}