plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.miqueias.valecorrida"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.miqueias.valecorrida"
        minSdk = 26
        targetSdk = 34
        // No GitHub Actions o número da versão = número da execução (v1, v2, v3...)
        val numero = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = numero
        versionName = "1.$numero"
    }

    // Chave fixa: assim cada APK novo instala por cima do anterior
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
