plugins {
    `kotlin-dsl`
    `java-gradle-plugin`

    id("com.google.devtools.ksp") version "2.3.6" apply false
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6")
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        create("dtk-localization-plugin") {
            id = "de.mineking.discord.localization"
            implementationClass = "de.mineking.discord.localization.gradle.LocalizationGradlePlugin"
        }
    }
}

tasks.jar {
    manifest {
        attributes["Implementation-Version"] = rootProject.extra["effectiveVersion"]
    }
}
