plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ivip.cinemaeliteplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ivip.cinemaeliteplayer"
        minSdk = 24
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        dataBinding = true
        viewBinding = true  // ← ESTA LINHA ESTAVA FALTANDO!
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === DEPENDÊNCIAS CORE ===
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // === MATERIAL DESIGN 3 ===
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    // === SPLASH SCREEN API ===
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.material3.android)

    // === MEDIA3 EXOPLAYER (PREMIUM) ===
    val media3Version = "1.2.0"
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)

    // === NETWORKING ===
    val okhttpVersion = "4.12.0"
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    val retrofitVersion = "2.9.0"
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.adapter.rxjava3)

    // === COROUTINES ===
    val coroutinesVersion = "1.7.3"
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // === BROADCAST MANAGER ===
    implementation(libs.androidx.localbroadcastmanager)

    // === GSON PARA JSON ===
    implementation(libs.gson)

    // === TRABALHO EM BACKGROUND ===
    implementation(libs.androidx.work.runtime.ktx)

    // === PERMISSIONS ===
    implementation(libs.androidx.activity.compose.v182)

    // === NAVEGAÇÃO ===
    val navVersion = "2.7.6"
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // === PREFERENCES ===
    implementation(libs.androidx.preference.ktx)

    // === SECURITY (OPCIONAL) ===
    implementation(libs.androidx.security.crypto)

    // === ROOM DATABASE (PARA CACHE FUTURO) ===
    val roomVersion = "2.6.1"
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // === DESUGARING (JAVA 8+) ===
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // === TESTES ===
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)

    // === LEAK CANARY (DEBUG) ===
    debugImplementation(libs.leakcanary.android)

    // === LOGGING (DEBUG) ===
    debugImplementation(libs.timber)
}