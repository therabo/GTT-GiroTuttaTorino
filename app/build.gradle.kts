import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseStoreFile = providers.environmentVariable("GTT_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("GTT_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("GTT_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("GTT_RELEASE_KEY_PASSWORD").orNull
val gttHceAid = providers.gradleProperty("gtt.hce.aid").get().trim().uppercase(Locale.US)

require(gttHceAid.matches(Regex("[0-9A-F]{10,32}"))) {
    "gtt.hce.aid must contain a 5 to 16 byte hexadecimal NFC application identifier"
}

android {
    namespace = "it.girotuttatorino.gtt"
    compileSdk = 35

    signingConfigs {
        if (
            listOf(
                releaseStoreFile,
                releaseStorePassword,
                releaseKeyAlias,
                releaseKeyPassword,
            ).all { !it.isNullOrBlank() }
        ) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    defaultConfig {
        applicationId = "it.girotuttatorino.gtt"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GTT_HCE_AID", "\"$gttHceAid\"")
        resValue("string", "gtt_hce_aid", gttHceAid)
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // AAPT requires adaptive-icon resources to keep the v26 qualifier.
        disable += "ObsoleteSdkInt"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    //noinspection GradleDependency -- latest release compatible with compileSdk 35 / AGP 8.7.
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
