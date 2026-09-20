plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.antoniocompany.financetracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.antoniocompany.financetracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

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

    buildTypes {
        release {
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