plugins {
    kotlin("jvm") version "2.2.0"
}

dependencies {
    implementation("de.mineking:DiscordToolKit:TESTING")
    implementation("com.github.freya022:JDA:1be8478")
    implementation("com.charleskorn.kaml:kaml:0.85.0")

    implementation("com.squareup:kotlinpoet-ksp:2.2.0")
    implementation("com.google.dagger:dagger-compiler:2.51.1")
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        from(components["java"])
    }
}

kotlin {
    jvmToolchain(21)
}