pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.1.21"
        id("org.jetbrains.kotlin.android") version "2.1.21"
        id("com.android.application") version "8.11.1"
        id("com.google.devtools.ksp") version "2.1.21-2.0.2"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "budget-tracker"

include(":parser")
include(":ledger")

// The Android app needs an Android SDK. Only include it where one is configured,
// so the parser can still be built and tested anywhere with just a JDK.
if (System.getenv("ANDROID_HOME") != null || file("local.properties").exists()) {
    include(":app")
}
