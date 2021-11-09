plugins {
    id("com.google.devtools.ksp") version "1.5.31-1.0.0"
    kotlin("jvm")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":annotation"))
    ksp(project(":processor"))
}
