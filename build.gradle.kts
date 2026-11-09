// Root build file. Plugins are declared here with "apply false" so every
// module shares one version of each, taken from the version catalog.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
