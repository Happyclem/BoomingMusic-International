// Standalone JVM project used ONLY to run the lyrics parser logic on the local machine
// (the main app is an Android module that needs the Android SDK to even configure).
//
// It compiles the REAL parser + model sources from the app, plus a handful of tiny JVM shims
// for the few Android/Compose symbols those files reference. It is intentionally NOT part of the
// app build (`:app` does not include it), so it can never affect the shipped APK.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "jvm-lyrics-test"
