import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.serialization)
}

buildConfig {
    forClass("io.github.couchtracker.tmdb", "TmdbConfig") {
        buildConfigField<String>(
            name = "API_KEY",
            value = provider {
                properties["COUCH_TRACKER_TMDB_API_KEY"]?.toString() ?: error("You must provide a Tmdb API key")
            },
        )
    }
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
            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.sqldelight.android)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.async)
            implementation(libs.sqldelight.primitive.adapters)
            implementation(libs.sqldelight.sqlite.dialect)
            implementation(libs.requery.android)
            implementation(libs.navigation.fragment)
            implementation(libs.navigation.ui)
            implementation(libs.navigation.compose)
            implementation(libs.palette)
            implementation(libs.koin.androidx.compose)

            // There is no androidUnitTest target
            implementation(libs.kotest.runner.junit5)
            implementation(libs.mockk.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.tmdb.api)
            implementation(libs.serialization.core)
            implementation(libs.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.coil)
            implementation(libs.coil.compose)
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
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"

            // FIX FOR COMPILATION FAILURE
            // SEE https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-debug/
            excludes += "META-INF/licenses/ASM"
            pickFirsts += "win32-x86-64/attach_hotspot_windows.dll"
            pickFirsts += "win32-x86/attach_hotspot_windows.dll"
            // END FIX
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // TODO remove this
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
        debugImplementation(libs.navigation.testing)
    }
}

sqldelight {
    val dialect = libs.sqldelight.sqlite.dialect.get().toString()
    databases {
        // User database schema. This is what we expose to users
        create("UserData") {
            packageName.set("io.github.couchtracker.db.user")
            srcDirs.setFrom("src/androidMain/sqldelight/user")
            dialect(dialect)
        }
        // Internal app database. Contains app-only data, like list of users
        create("AppData") {
            packageName.set("io.github.couchtracker.db.app")
            srcDirs.setFrom("src/androidMain/sqldelight/app")
            dialect(dialect)
        }
        // Internal DB containing TMDB cache
        create("TmdbCache") {
            packageName.set("io.github.couchtracker.db.tmdbCache")
            srcDirs.setFrom("src/androidMain/sqldelight/tmdbCache")
            generateAsync.set(true)
            dialect(dialect)
        }
    }
}

tasks.register<SqlDelightTmdbCacheDefinitionsGenerator>("generateSqldelightTmdbCacheDefinitions")

tasks.whenTaskAdded {
    if (this.name == "generateCommonMainTmdbCacheInterface") {
        dependsOn += "generateSqldelightTmdbCacheDefinitions"
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
