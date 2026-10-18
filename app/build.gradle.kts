plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Dirección base de la API propia (función `api-v1` de Supabase, ver `backend/README.md`). Es pública: no es ninguna clave.
// Para probar contra el backend local (con `npm start` en `backend/`) desde el emulador o un móvil por USB:
//   adb reverse tcp:54321 tcp:54321
//   ./gradlew installDebug -PapiBaseUrl=http://127.0.0.1:54321/functions/v1/api-v1/
// (con `adb reverse` la app llega al ordenador por 127.0.0.1; la dirección 10.0.2.2 del emulador se queda sin respuesta por la red Wi-Fi
// virtual). Solo la versión de depuración admite HTTP sin cifrar, y únicamente hacia el ordenador (`src/debug/res/xml`).
val apiBaseUrl = "https://rttqdgrrprzgdxdlhaeq.supabase.co/functions/v1/api-v1/"
val localApiBaseUrl = "http://127.0.0.1:54321/functions/v1/api-v1/"

android {
    namespace = "com.cmarcillovargas.turismovalencia"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cmarcillovargas.turismovalencia"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        debug {
            providers.gradleProperty("apiBaseUrl").orNull?.let { buildConfigField("String", "API_BASE_URL", "\"$it\"") }
        }
        release {
            optimization {
                enable = true
                // R8 solo procesa estos paquetes; lo de fuera (la app, ZXing) se deja intacto. Toda librería de Kotlin que
                // use la biblioteca estándar tiene que estar dentro: si no, R8 fija miembros de `kotlin.text` por lo que
                // ella referencia y reubica solo parte de `StringsKt` (IllegalAccessError al abrir la app). Al añadir
                // otra (Coil…), incluirla aquí; probar con `tools/verify/verify_release.py`.
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**", "okhttp3.**", "okio.**", "retrofit2.**")
            }
        }
        // Lo mismo que `release` (R8 incluido) pero firmada con la clave de depuración y apuntando siempre al backend local, al que
        // llega por HTTP como la de depuración (`src/debug`, ver `sourceSets`). Sirve para probar el código minificado —reservas
        // incluidas— sin escribir nada en la nube (`tools/verify/verify_release.py`). No se publica; `-PapiBaseUrl=…` le da otra dirección.
        create("releaseCheck") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("apiBaseUrl").orNull ?: localApiBaseUrl}\"")
        }
    }
    sourceSets {
        getByName("releaseCheck") {
            manifest.srcFile("src/debug/AndroidManifest.xml")
            res.srcDir("src/debug/res")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true // java.time en minSdk 24
    }
    buildFeatures {
        buildConfig = true // API_BASE_URL
        compose = true
    }
    bundle {
        // La app cambia de idioma por su cuenta (idioma por app, `data/AppLanguage.kt`): si el App Bundle separara los idiomas en APK
        // aparte, un idioma que el móvil no tiene instalado no se podría elegir. Todos van dentro (unos pocos KB).
        language {
            enableSplit = false
        }
    }
}

room {
    // Esquemas exportados (versión a versión) para escribir y probar las migraciones.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    // Idioma por app (`AppCompatDelegate.setApplicationLocales`): en Android 13+ lo guarda el sistema y en versiones anteriores lo hace
    // AppCompat (por eso `MainActivity` es una `AppCompatActivity` con un tema AppCompat). Ver `data/AppLanguage.kt`.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    // También fija la 1.8 de `kotlinx-serialization-core` en todos los classpaths: `room-testing` la necesita para leer los
    // esquemas en `MigrationTest` (con la 1.7.3 de Navigation y Lifecycle falla con AbstractMethodError).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.osmdroid.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.zxing.core)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}