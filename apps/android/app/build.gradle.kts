import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
    // A11 (ADR-014): Firebase for the dev flavor only. prod has no google-services.json and no Firebase deps.
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val local = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { local.load(it) }
}
fun localProp(key: String): String? = local.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

// Flavors (A10, ADR-014). dev: dev.* keys, falling back to the pre-A10 unprefixed keys.
// -PAPI_PUBLIC_URL=... overrides dev only (QA against a local fake server).
val devApiUrl = (project.findProperty("API_PUBLIC_URL") as String?)
    ?: localProp("dev.API_PUBLIC_URL") ?: localProp("API_PUBLIC_URL") ?: "http://127.0.0.1:8080"
val devInvite = localProp("dev.INVITE_CODE") ?: localProp("INVITE_CODE") ?: "troca-isto"
// prod: no fallback. A prod variant does not build without both keys (guard at the end of this file).
val prodApiUrl = localProp("prod.API_PUBLIC_URL")
val prodInvite = localProp("prod.INVITE_CODE")

// Release signing (A9): key.properties + nutri-release.jks at the repo root, never in git.
// Without key.properties the release is left unsigned; it never falls back to the debug key.
val keyPropsFile = rootProject.file("../../key.properties")
val keyProps = Properties()
if (keyPropsFile.exists()) {
    keyPropsFile.inputStream().use { keyProps.load(it) }
} else {
    logger.warn("key.properties not found at ${keyPropsFile.path}: release APK will be unsigned.")
}

android {
    namespace = "com.nutri.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nutri.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "ENV", "\"dev\"")
            buildConfigField("String", "API_PUBLIC_URL", "\"$devApiUrl\"")
            buildConfigField("String", "INVITE_CODE", "\"$devInvite\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "ENV", "\"prod\"")
            buildConfigField("String", "API_PUBLIC_URL", "\"${prodApiUrl.orEmpty()}\"")
            buildConfigField("String", "INVITE_CODE", "\"${prodInvite.orEmpty()}\"")
        }
    }

    signingConfigs {
        if (keyPropsFile.exists()) {
            create("release") {
                // storeFile in key.properties is relative to apps/android/ (the Gradle root).
                storeFile = rootProject.file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    val bom = platform(libs.compose.bom)
    implementation(bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.material.icons.extended)
    // @Preview in main sources: the annotation must exist in release too (A9).
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.retrofit.serialization)
    implementation(libs.coroutines)
    implementation(libs.datastore)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.security.crypto)
    ksp(libs.room.compiler)

    // A11: Firebase only in dev. prod gets NoopTelemetry (src/prod) and no Firebase classes.
    "devImplementation"(platform(libs.firebase.bom))
    "devImplementation"(libs.firebase.crashlytics)
    "devImplementation"(libs.firebase.analytics)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp)
    testImplementation(libs.serialization.json)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)
}

kapt {
    correctErrorTypes = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test> {
    testLogging {
        events("passed", "failed")
    }
}

googleServices {
    // google-services.json lives only in src/dev (gitignored). prod and a clean clone build without it.
    missingGoogleServicesStrategy = com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
}

roborazzi {
    // Regression baselines. docs/qa/android/current/ holds emulator screencaps only.
    outputDir.set(file("src/test/snapshots"))
}

// A10: a prod variant never builds against a guessed server. Only prod tasks are blocked.
val prodConfigured = prodApiUrl != null && prodInvite != null
tasks.matching { it.name.startsWith("preProd") && it.name.endsWith("Build") }.configureEach {
    doFirst {
        if (!prodConfigured) {
            throw GradleException(
                "prod flavor: defina prod.API_PUBLIC_URL e prod.INVITE_CODE em apps/android/local.properties",
            )
        }
    }
}
