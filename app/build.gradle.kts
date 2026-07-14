plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.extremecoffee.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.extremecoffee.myapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.2.6"
        manifestPlaceholders["MAPS_API_KEY"] =
            (project.findProperty("MAPS_API_KEY") as? String) ?: ""
    }

    signingConfigs {
        create("release") {
            // Firma usata dalla CI se sono presenti le variabili d'ambiente.
            // In Android Studio puoi invece firmare con la procedura "Generate Signed App Bundle".
            val storePath = System.getenv("KEYSTORE_PATH")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Forza androidx.fragment a una versione recente: la 1.1.0 arriva come dipendenza
    // transitiva (es. play-services-maps/places) ed è segnalata obsoleta da Google Play.
    // Gradle risolve la più alta -> upgrade sicuro, nessun uso diretto di Fragment nel codice.
    implementation("androidx.fragment:fragment:1.8.5")

    // Google Maps + Places
    implementation("com.google.maps.android:maps-compose:6.1.2")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // Firebase Firestore (backend realtime tra telefoni)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    // await() sulle Task (Places + Firestore)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Recap settimanale via notifica anche ad app chiusa
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Accesso anonimo: serve a mettere in sicurezza Firestore con le regole
    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Fotocamera (Selfie Coffee)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    // Fornisce com.google.common.util.concurrent.ListenableFuture usata da CameraX
    implementation("com.google.guava:guava:33.3.1-android")
}

// Il plugin si applica solo se esiste il file di config (così compila anche col segnaposto)
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
