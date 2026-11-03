plugins {
    id("com.android.application")
}

android {
    namespace = "$packagename"
    compileSdk = 35

    defaultConfig {
        applicationId = "$packagename"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Solo librerías locales o nulas para evitar descargas
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
