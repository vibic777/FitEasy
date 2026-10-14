import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
val signing = Properties().apply {
    val source = rootProject.file("keystore.properties")
    if (source.isFile) source.inputStream().use { load(it) }
}
val releaseInfo=Properties().apply {rootProject.file("version.properties").inputStream().use {load(it)}}
android {
    namespace = "com.fiteasy.app"
    compileSdk = 37
    sourceSets.getByName("main").kotlin.directories.add("../shared/src")
    sourceSets.getByName("main").kotlin.directories.add(rootProject.layout.buildDirectory.dir("generated/buildInfo/kotlin").get().asFile.absolutePath)
    defaultConfig { applicationId = "com.vibic777.fiteasy"; minSdk = 28; targetSdk = 36; versionCode = releaseInfo.getProperty("build").toInt(); versionName = releaseInfo.getProperty("version") }
    signingConfigs {
        if (signing.getProperty("storeFile") != null) create("release") {
            storeFile = rootProject.file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
        // The debug APK carries the same test key as the release one, whatever machine builds it: a phone takes a new
        // build over the old one only when the key is the same. Without keystore.properties the key of the machine is used.
        if (signing.getProperty("storeFile") != null) getByName("debug") {
            storeFile = rootProject.file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }
    buildTypes { release {
        signingConfig = signingConfigs.findByName("release")
        // R8 as in Notio (D091): unused library code and resources out, names kept so journal stack traces stay readable.
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
tasks.matching {it.name.startsWith("compile") && it.name.contains("Kotlin")}.configureEach {dependsOn(":generateBuildInfo")}
dependencies {
    implementation("org.jmdns:jmdns:3.6.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    // QR code of the TOTP key (D124).
    implementation(libs.zxing.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
