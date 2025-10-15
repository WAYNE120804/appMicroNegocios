// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Register the Android plugin at the project level but don't apply it here.
    // 'apply false' means it's available for sub-modules (like :app) to use.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose) apply false
}
