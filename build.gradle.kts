plugins {
    kotlin("jvm") version "1.5.31" apply false
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.5.31"))
    }
}

allprojects {
    group = "org.moskvax.ordinal"
    version = "0.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            @Suppress("SuspiciousCollectionReassignment")
            freeCompilerArgs += listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview",
                "-Xopt-in=com.google.devtools.ksp.KspExperimental"
            )
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
            jvmTarget = "1.8"
        }
    }
}
