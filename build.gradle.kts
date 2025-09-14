plugins {
    id("maven-publish")
}

val release = System.getenv("RELEASE") == "true"

version = "1.1.0"
rootProject.extra["effectiveVersion"] = System.getenv("BRANCH") ?: version

allprojects {
    group = "de.mineking"
    version = rootProject.version

    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.mineking.dev/releases")
    }

    publishing {
        repositories {
            maven {
                url = uri("https://maven.mineking.dev/" + (if (release) "releases" else "snapshots"))
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_SECRET")
                }
            }
        }

        publications.withType<MavenPublication> {
            artifactId = "DiscordToolKit-${project.name}"
            version = rootProject.extra["effectiveVersion"].toString()
        }
    }
}