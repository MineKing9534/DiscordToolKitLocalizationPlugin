package de.mineking.discord.localization.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LocalizationGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "discordLocalization",
            LocalizationProcessorExtension::class.java
        )

        project.plugins.apply("com.google.devtools.ksp")

        project.afterEvaluate {
            val directory = project.layout.projectDirectory.dir(extension.localizationDirectory)

            project.extensions.configure(KspExtension::class.java) {
                arg("dtk_managerName", extension.managerName)
                arg("dtk_properties", extension.properties.map { (name, type) -> "$name:$type" }.joinToString(","))
                arg("dtk_imports", extension.imports.map { (name, qualified) -> "$name:$qualified" }.joinToString(","))
                arg("dtk_locales", extension.locales.joinToString(","))
                arg("dtk_defaultLocale", extension.defaultLocale!!)
                arg("dtk_locationFormat", "$directory/${extension.locationFormat}")
                arg("dtk_botPackage", extension.botPackage)


                arg("dtk_yamlHash", directory.asFileTree
                    .filter { it.isFile }
                    .joinToString(";") { "${it.name}:${it.lastModified()}" }
                    .hashCode()
                    .toString()
                )
            }

            project.tasks.withType(KspTask::class.java).configureEach {
                inputs.dir(directory).withPropertyName("localizationDirectory")
            }
        }

        val pluginVersion = this@LocalizationGradlePlugin::class.java.`package`.implementationVersion
        project.dependencies.add("ksp", "de.mineking:DiscordToolKit-processor:$pluginVersion")
    }
}