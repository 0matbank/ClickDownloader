plugins {
    id("com.android.application")
}

android {
    namespace = "com.clickdownloader.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.clickdownloader.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

/*
 * youtubedl-android pulls libraries that may transitively bring old
 * kotlin-stdlib-jdk7 / kotlin-stdlib-jdk8 artifacts.
 *
 * Kotlin 1.8+ merged those JDK-specific classes into kotlin-stdlib.
 * Keeping old 1.7.x jdk7/jdk8 jars together with kotlin-stdlib 1.8.x
 * causes Android's checkDebugDuplicateClasses task to fail.
 *
 * Exclude the obsolete split artifacts globally and use one stdlib.
 */
configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")

    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core:1.15.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")

    val youtubedlAndroid = "0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:$youtubedlAndroid")
}
