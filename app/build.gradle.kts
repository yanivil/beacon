// App module. AGP 9 has built-in Kotlin, so only the Compose compiler plugin
// is applied here (no org.jetbrains.kotlin.android).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/*
 * Release signing (docs/specs/2026-11-11-play-release-design.md, section 4).
 * The repository is public, so the Play upload key and its passwords never
 * live in it: they come from four Gradle properties in the owner's
 * ~/.gradle/gradle.properties. Debug builds and unit tests don't need them.
 * A release build without them stops early and names the missing ones,
 * instead of failing later with a vaguer signing error.
 */

/** The four Gradle properties the release signing config reads. */
val uploadKeyProperties = listOf(
    "BEACON_UPLOAD_STORE_FILE",
    "BEACON_UPLOAD_STORE_PASSWORD",
    "BEACON_UPLOAD_KEY_ALIAS",
    "BEACON_UPLOAD_KEY_PASSWORD",
)

/**
 * Reads one signing property.
 *
 * @param name the Gradle property's name.
 * @return its value, or null when it is missing or blank (a blank value on the
 *   command line, such as `-PBEACON_UPLOAD_KEY_ALIAS=`, counts as missing).
 */
fun uploadKeyProperty(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }

/** The signing properties that are missing, in the order of [uploadKeyProperties]. */
val missingUploadKeyProperties: List<String> = uploadKeyProperties.filter { uploadKeyProperty(it) == null }

android {
    namespace = "dev.beacon"
    // CLAUDE.md sets compileSdk at 37 and targetSdk at 36. compileSdk 37 only
    // lets the code see Android 17's APIs, which Compose 1.12, Lifecycle 2.11
    // and Core 1.19 need; targetSdk 36 keeps how Android 16 and later treat
    // the app. Lint's reminder that targetSdk 37 exists (OldTargetApi below)
    // is silenced for that line only. Raise it when the owner changes the rule.
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.beacon"
        minSdk = 29
        //noinspection OldTargetApi
        targetSdk = 36
        // Every upload to Google Play needs a higher versionCode.
        versionCode = 3
        versionName = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Only filled in when all four properties are there; otherwise
            // checkReleaseSigning stops the release build first.
            if (missingUploadKeyProperties.isEmpty()) {
                storeFile = file(uploadKeyProperty("BEACON_UPLOAD_STORE_FILE")!!)
                storePassword = uploadKeyProperty("BEACON_UPLOAD_STORE_PASSWORD")
                keyAlias = uploadKeyProperty("BEACON_UPLOAD_KEY_ALIAS")
                keyPassword = uploadKeyProperty("BEACON_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // Shrinking stays off for 1.0.0: Beacon reads Xiaomi's Autostart
            // through a hidden Android method by reflection (Autostart.kt),
            // which shrinking could break, and the app is small.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Robolectric needs merged resources and the manifest.
            isIncludeAndroidResources = true
            all {
                // Robolectric 4.17 on JDK 21 reflects into java.base internals
                // (raw FileDescriptor access); without this export the sandbox
                // fails to start. Drop it once a Robolectric version no longer needs it.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }
}

/**
 * Stops a release build when a signing property is missing, naming each one and
 * never printing a value. Every preReleaseBuild task depends on it.
 *
 * @throws GradleException when one or more of [uploadKeyProperties] is missing or blank.
 */
val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    group = "verification"
    description = "Checks that the four release signing properties are set."
    // Captured at configuration time as a plain list, so the action holds no
    // reference to the build script.
    val missing = missingUploadKeyProperties
    doLast {
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing needs these Gradle properties in ~/.gradle/gradle.properties: " +
                    missing.joinToString(", ") +
                    ". See docs/specs/2026-11-11-play-release-design.md, section 4.",
            )
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(checkReleaseSigning) }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material.ripple)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.core.ktx)
    implementation(libs.glance.appwidget)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.glance.appwidget.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
