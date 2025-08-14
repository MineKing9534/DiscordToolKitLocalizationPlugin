package de.mineking.discord.localization.processor

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import net.dv8tion.jda.api.interactions.DiscordLocale

class LocalizationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = LocalizationProcessor(
        environment.codeGenerator,
        environment.logger,
        environment.options["dtk_managerName"]!!,
        environment.options["dtk_properties"]!!.split(",").filter { it.isNotBlank() }.associate {
            val temp = it.split(":", limit = 2)
            temp[0] to temp[1].parseTypeString()
        },
        environment.options["dtk_locales"]!!.split(",").map { DiscordLocale.from(it) },
        DiscordLocale.from(environment.options["dtk_defaultLocale"]!!),
        environment.options["dtk_locationFormat"]!!,
        environment.options["dtk_botPackage"]!!
    )
}