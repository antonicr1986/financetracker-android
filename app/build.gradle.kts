plugins {
    alias(libs.plugins.android.application)
}

/*
 * Version y firma llegan del workflow de Release como variables de entorno.
 * En local no existen: se compila la version 1.0 y la release sale sin
 * firmar, que es lo correcto, porque la clave no debe estar en el repo.
 *
 * VERSION_NAME sale de la etiqueta (v1.2.3 -> "1.2.3") y VERSION_CODE se
 * deriva de ella, porque Android exige que crezca en cada actualizacion.
 */
val releaseVersionName: String = System.getenv("VERSION_NAME") ?: "1.0"
val releaseVersionCode: Int = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.antoniocompany.financetracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.antoniocompany.financetracker"
        minSdk = 24
        targetSdk = 37
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // La URL de la API se fija al compilar, igual que NEXT_PUBLIC_API_URL en
        // la web. Asi manana se puede apuntar una compilacion de depuracion a
        // localhost sin tocar el codigo.
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://financetracker-api-cpctbta0gddddge5.belgiumcentral-01.azurewebsites.net/\""
        )
    }

    signingConfigs {
        // Solo existe si el workflow ha dejado el keystore en disco.
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ViewBinding genera una clase por layout, asi que las vistas se leen por
    // su id y con su tipo, sin findViewById ni casts.
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}