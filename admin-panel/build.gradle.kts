@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            @Suppress("DEPRECATION")
            implementation(compose.runtime)
            @Suppress("DEPRECATION")
            implementation(compose.foundation)
            @Suppress("DEPRECATION")
            implementation(compose.material3)
            @Suppress("DEPRECATION")
            implementation(compose.ui)
            implementation(ktorLibs.client.core)
            implementation(ktorLibs.client.contentNegotiation)
            implementation(ktorLibs.client.logging)
            implementation(ktorLibs.serialization.kotlinx.json)
        }
        wasmJsMain.dependencies {
            implementation(ktorLibs.client.js)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(ktorLibs.client.cio)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.evandhardspace.movie.adminpanel.MainKt"
    }
}
