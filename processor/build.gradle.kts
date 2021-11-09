plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31")
    implementation(project(":annotation"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.31-1.0.0")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("com.squareup:kotlinpoet-ksp:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.5.31")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.5")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.5")
}
