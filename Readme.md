# DiscordToolKitLocalizationPlugin

This repository contains a gradle plugin and a ksp processor to create `LocalizationFile` implementations for DiscordToolKit projects at compile time.

## Installation

To add the plugin to your project, you first have to add the required repositories to your build:

Add this in your `settings.gradle.kts`:

```kt
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.mineking.dev/snapshots")
    }
}
```

And in your `build.gradle.kts`:

```kt
repositories {
    maven("https://maven.mineking.dev/snapshots")
}
```

Now, to actually add the plugin to your project, add this to your `build.gradle.kts`:

```kt
plugins {
    id("de.mineking.discord.localization") version "1.0.0"
} 
```

## Configuration

When to plugin is added to your project, you can now configure it:

```kt 
discordLocalization {
    locales = listOf("en-US", "de") //The locales you support
    defaultLocale = "en-US" //The default locale (Localization for unknown locales will efault to the values for this locale)

    locationFormat = "$projectDir/localization/%locale%/%name%.yaml" //The path where to find your localization files
    botPackage = "com.example.bot" //A package prefix to remove from file paths (Optional)
}
```

## Defining Localizations

To actually define the localized values, you can create yaml files in the location specified in `locationFormat` (Take a look at the examples in [DiscordToolKit](https://github.com/MineKing9534/DiscordToolKit)).

The string values in your yaml files will be put in string literals in the generated implementations. This means, that you can use kotlin string templating to add variables or even code to the localization.