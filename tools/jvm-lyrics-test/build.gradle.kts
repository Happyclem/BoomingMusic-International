plugins {
    kotlin("jvm") version "2.3.21"
}

repositories {
    mavenCentral()
}

// Path to the app's main sources, relative to this standalone project.
val appMain = "../../app/src/main/java"

sourceSets {
    main {
        kotlin {
            // Local JVM shims for the few Android/Compose symbols the parser files touch.
            srcDir("src/main/kotlin")
            // The REAL parser + model sources, compiled as-is (no copies).
            srcDir(appMain)
            // Keep the compilation unit tiny and self-contained: only the lyrics model + parsers.
            include(
                "com/mardous/booming/data/model/lyrics/SyncedLyrics.kt",
                "com/mardous/booming/data/model/lyrics/LyricsActor.kt",
                "com/mardous/booming/data/LyricsParser.kt",
                "com/mardous/booming/data/local/lyrics/lrc/**",
                "com/mardous/booming/data/local/lyrics/ttml/**",
                "com/mardous/booming/core/model/lyrics/TranslationFilter.kt",
                // shims (matched within src/main/kotlin)
                "**/_shims/**"
            )
        }
    }
    test {
        kotlin {
            srcDir("src/test/kotlin")
        }
    }
}

dependencies {
    // Real XmlPullParser implementation on the JVM (the parser uses org.xmlpull.v1.*).
    implementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

kotlin {
    jvmToolchain(21)
}
