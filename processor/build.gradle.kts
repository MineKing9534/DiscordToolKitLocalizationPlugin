plugins {
    kotlin("jvm") version "2.3.20"
}

dependencies {
    implementation("de.mineking:DiscordToolKit:1.5.1")
    implementation("net.dv8tion:JDA:6.4.1")
    implementation("com.charleskorn.kaml:kaml:0.104.0")

    implementation("com.squareup:kotlinpoet-ksp:2.3.0")
    implementation("com.google.dagger:dagger-compiler:2.59.2")
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        from(components["java"])
    }
}

kotlin {
    jvmToolchain(21)
}