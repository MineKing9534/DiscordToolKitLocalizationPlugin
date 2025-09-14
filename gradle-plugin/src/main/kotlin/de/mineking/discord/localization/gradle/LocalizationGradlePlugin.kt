package de.mineking.discord.localization.gradle

import com.google.devtools.ksp.gradle.KspExtension
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
            project.extensions.configure(KspExtension::class.java) {
                arg("dtk_managerName", extension.managerName)
                arg("dtk_properties", extension.properties.map { (name, type) -> "$name:$type" }.joinToString(","))
                arg("dtk_imports", extension.imports.map { (name, qualified) -> "$name:$qualified" }.joinToString(","))
                arg("dtk_locales", extension.locales.joinToString(","))
                arg("dtk_defaultLocale", extension.defaultLocale!!)
                arg("dtk_locationFormat", extension.locationFormat)
                arg("dtk_botPackage", extension.botPackage)
            }
        }

        val pluginVersion = this@LocalizationGradlePlugin::class.java.`package`.implementationVersion
        project.dependencies.add("ksp", "de.mineking:DiscordToolKit-processor:$pluginVersion")
    }
}