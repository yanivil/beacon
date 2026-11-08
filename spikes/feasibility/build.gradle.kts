/*
 * Step 1 test app, not the Beacon app. It deliberately has no library dependencies: a result here
 * shows what Android itself allows on the test phone, not what some library worked around.
 * Compose and Glance arrive with the real app in Step 2.
 */
plugins {
    id("com.android.application") version "9.5.1"
}

android {
    namespace = "dev.beacon.spike"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.beacon.spike"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-spike"
    }
}
