import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.serialization)
}

// https://chatgpt.com/share/684d3051-f120-8002-ba0a-8c89b592c28fF
apply(plugin = "kotlin-parcelize")

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
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        allWarningsAsErrors = true
    }

    dependencies {
        detektPlugins(libs.detekt.formatting)

        // Kotlin stuff
        implementation(libs.kotlinx.datetime)
        implementation(libs.serialization.core)
        implementation(libs.serialization.json)

        // Compose
        implementation(compose.foundation)
        implementation(compose.material)
        implementation(compose.material3)
        implementation(compose.materialIconsExtended)
        implementation(compose.runtime)
        implementation(compose.ui)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.compose.paging)
        debugImplementation(libs.compose.ui.tooling)

        // Navigation
        implementation(libs.navigation.compose)
        implementation(libs.navigation.ui)
        debugImplementation(libs.navigation.testing)

        // Ktor
        implementation(libs.ktor.client.okttp)

        // Database stuff
        implementation(libs.requery.android)
        implementation(libs.sqldelight.android)
        implementation(libs.sqldelight.coroutines)
        implementation(libs.sqldelight.primitive.adapters)
        implementation(libs.sqldelight.sqlite.dialect)

        // Koin (dependency injection)
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.koin.androidx.compose)
        implementation(libs.koin.compose)
        implementation(libs.koin.core)

        // Coil (image library)
        implementation(libs.coil.compose)
        implementation(libs.coil.ktor)

        // Other
        implementation(libs.androidx.documentfile)
        implementation(libs.byteSize)
        implementation(libs.palette)
        implementation(libs.preference)
        implementation(libs.tmdb.api)
    }

    // Test dependencies
    dependencies {
        // Kotest
        testImplementation(libs.kotest.assertions.core)
        testImplementation(libs.kotest.extensions.koin)
        testImplementation(libs.kotest.framework.datatest)
        testImplementation(libs.kotest.property)
        testImplementation(libs.kotest.runner.junit5)

        testImplementation(libs.mockk.android)
        testImplementation(libs.koin.test)
        testImplementation(libs.sqldelight.jvm)
    }
}

android {
    namespace = "io.github.couchtracker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

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
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

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
}

sqldelight {
    val dialect = libs.sqldelight.sqlite.dialect.get().toString()
    databases {
        // Profile database schema. This is what we expose to users
        create("ProfileData") {
            packageName.set("io.github.couchtracker.db.profile")
            srcDirs.setFrom("src/main/sqldelight/profile")
            dialect(dialect)
        }
        // Internal app database. Contains app-only data, like list of profiles
        create("AppData") {
            packageName.set("io.github.couchtracker.db.app")
            srcDirs.setFrom("src/main/sqldelight/app")
            dialect(dialect)
        }
        // Internal DB containing TMDB cache
        create("TmdbCache") {
            packageName.set("io.github.couchtracker.db.tmdbCache")
            srcDirs.setFrom("src/main/sqldelight/tmdbCache")
            dialect(dialect)
        }
    }
}

tasks.register<SqlDelightTmdbCacheDefinitionsGenerator>("generateSqldelightTmdbCacheDefinitions")

tasks.configureEach {
    if (name.startsWith("generate") && name.endsWith("TmdbCacheInterface")) {
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
    allprojects {
        this@register.dependsOn(tasks.withType<Detekt>())
    }
}

tasks.withType<Detekt>().configureEach {
    exclude {
        // Exclude auto generated files
        it.file.relativeTo(projectDir).startsWith("build")
    }
}
