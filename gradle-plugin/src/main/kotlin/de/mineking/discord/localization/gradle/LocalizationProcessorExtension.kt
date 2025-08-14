package de.mineking.discord.localization.gradle

import kotlin.reflect.typeOf

open class LocalizationProcessorExtension {
    var managerName: String = "de.mineking.discord.localization.DefaultLocalizationManager"
    var properties: MutableMap<String, String> = mutableMapOf()
    var locales: List<String> = listOf("en-US")
    var defaultLocale: String? = null
        get() = field ?: locales.first()
    var locationFormat: String = "text/%locale%/%name%.yaml"
    var botPackage: String = ""
}

inline fun <reified T> LocalizationProcessorExtension.declareProperty(name: String) {
    properties[name] = typeOf<T>().toString()
}

fun LocalizationProcessorExtension.declareProperty(name: String, type: String) {
    properties[name] = type
}