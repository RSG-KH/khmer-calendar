import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release 0.1.0 depends only on Kotlin stdlib, supplied by AGP's Kotlin plugin.
// GitHub release assets have no POM endpoint; resolve this one module as an artifact.
val calendarEngineVerification = configurations.create("calendarEngineVerification") {
    isCanBeConsumed = false
    isTransitive = false
}
val verifyCalendarEngine = tasks.register("verifyCalendarEngine") {
    val archive = objects.fileCollection().from(calendarEngineVerification)
    val expectedSha256 = "d1a1a2968a6358043387fe9c1b8993b8d16595e96aad48f27e236e6555b4d509"
    inputs.files(archive)
    inputs.property("sha256", expectedSha256)
    doLast {
        val actual = MessageDigest.getInstance("SHA-256").digest(archive.singleFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        check(actual == expectedSha256) { "Khmer Calendar Engine release checksum mismatch" }
    }
}
tasks.named("preBuild") { dependsOn(verifyCalendarEngine) }

android {
    namespace = "com.rsgkh.calendar"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rsgkh.calendar"
        minSdk = 31
        targetSdk = 37
        versionCode = 17
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets {
        getByName("test").kotlin.directories.add("src/sharedTest/java")
        getByName("androidTest").kotlin.directories.add("src/sharedTest/java")
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("calendar.screenshots", layout.buildDirectory.dir("reports/screenshots").get().asFile.absolutePath)
}

dependencies {
    implementation(libs.calendar.engine)
    add(calendarEngineVerification.name, libs.calendar.engine)
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
