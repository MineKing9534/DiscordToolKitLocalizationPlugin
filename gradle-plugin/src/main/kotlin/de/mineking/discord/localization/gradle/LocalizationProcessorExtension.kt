package de.mineking.discord.localization.gradle

import kotlin.reflect.typeOf

open class LocalizationProcessorExtension {
    var managerName: String = "de.mineking.discord.localization.DefaultLocalizationManager"
    var properties: MutableMap<String, String> = mutableMapOf()
    var imports: MutableMap<String, String> = mutableMapOf()
    var locales: List<String> = listOf("en-US")
    var defaultLocale: String? = null
        get() = field ?: locales.first()
    var localizationDirectory = "localization"
    var locationFormat: String = "%locale%/%name%.yaml"
    var botPackage: String = ""
}

fun LocalizationProcessorExtension.import(qualified: String, name: String = qualified.split(".").last()) {
    imports[name] = qualified
}

inline fun <reified T> LocalizationProcessorExtension.declareProperty(name: String) {
    properties[name] = typeOf<T>().toString()
}

fun LocalizationProcessorExtension.declareProperty(name: String, type: String) {
    properties[name] = type
}