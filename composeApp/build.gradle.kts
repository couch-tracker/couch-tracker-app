import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android)

            // There is no androidUnitTest target
            implementation(libs.kotest.runner.junit5)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.datatest)
        }
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

android {
    namespace = "io.github.couchtracker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "io.github.couchtracker"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    testOptions.unitTests {
        all {
            it.useJUnitPlatform()
        }
    }

    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

sqldelight {
    databases {
        create("User") {
            packageName.set("io.github.couchtracker.db.user")
            srcDirs.setFrom("commonMain/sqldelight/user")
        }
        create("Tmdb") {
            packageName.set("io.github.couchtracker.db.tmdb")
            srcDirs.setFrom("commonMain/sqldelight/tmdb")
        }
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.register("detektAll") {
    group = "verification"
    dependsOn += "detektAndroidRelease"
    dependsOn += "detektAndroidDebugAndroidTest"
    dependsOn += "detektAndroidReleaseUnitTest"
}

