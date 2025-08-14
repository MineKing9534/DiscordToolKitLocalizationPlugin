package de.mineking.discord.localization.processor

import com.charleskorn.kaml.*
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dagger.spi.model.getNormalizedPackageName
import de.mineking.discord.DiscordToolKit
import de.mineking.discord.localization.LocalizationFile
import net.dv8tion.jda.api.interactions.DiscordLocale
import java.io.File
import kotlin.reflect.KClass

private const val LOCALIZATION_FILE_INTERFACE = "de.mineking.discord.localization.LocalizationFile"
private const val LOCALIZATION_MANAGER_INTERFACE = "de.mineking.discord.localization.LocalizationManager"

private const val LOCALIZE_ANNOTATION = "de.mineking.discord.localization.Localize"
private const val LOCALIZATION_PATH_ANNOTATION = "de.mineking.discord.localization.LocalizationPath"
private const val LOCALE_ANNOTATION = "de.mineking.discord.localization.Locale"
private const val LOCALIZATION_PARAMETER_ANNOTATION = "de.mineking.discord.localization.LocalizationParameter"

class LocalizationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val managerName: String,
    private val properties: Map<String, TypeName>,
    private val locales: List<DiscordLocale>,
    private val defaultLocale: DiscordLocale,
    private val locationFormat: String,
    private val botPackage: String
) : SymbolProcessor {
    private val files = mutableMapOf<String, Map<String, Map<DiscordLocale, YamlNode>>>()

    private var didRun = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (didRun) return emptyList()

        val localizationFileInterface = resolver
            .getClassDeclarationByName(LOCALIZATION_FILE_INTERFACE)
            ?.asStarProjectedType()
            ?: error("Could not find marker interface 'LocalizationFile'")

        val localizationFileImplementations = resolver.getAllFiles().flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE }
            .filter { file -> localizationFileInterface in file.superTypes.map { it.resolve() } }
            .associateWith { generateFileImplementation(it) }

        generateManagerImplementation(resolver, localizationFileInterface, localizationFileImplementations)

        didRun = true

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun generateManagerImplementation(resolver: Resolver, localizationFileInterface: KSType, implementations: Map<KSClassDeclaration, Pair<String, String>>) {
        val temp = managerName.split(".")
        val className = temp.last()
        val packageName = temp.dropLast(1).joinToString(".")

        val fileSpec = FileSpec.builder(packageName, className)
        val typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(resolver.getKotlinClassByName(LOCALIZATION_MANAGER_INTERFACE)!!.toClassName())

        typeSpec.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("manager", DiscordToolKit::class.asClassName().parameterizedBy(STAR))
                .build()
        )

        typeSpec.addProperty(
            PropertySpec.builder("manager", DiscordToolKit::class.asClassName().parameterizedBy(STAR), KModifier.OVERRIDE)
                .initializer("manager")
                .build()
        )

        typeSpec.addProperty(
            PropertySpec.builder("locales", List::class.parameterizedBy(DiscordLocale::class), KModifier.OVERRIDE)
                .initializer("listOf(${locales.joinToCode { CodeBlock.of("%T.$it", DiscordLocale::class) }})")
                .build()
        )

        typeSpec.addProperty(
            PropertySpec.builder("defaultLocale", DiscordLocale::class, KModifier.OVERRIDE)
                .initializer("%T.$defaultLocale", DiscordLocale::class)
                .build()
        )

        typeSpec.addProperty(
            PropertySpec.builder("properties", ClassName("kotlin.collections", "MutableMap").parameterizedBy(String::class.asClassName(), ANY.copy(nullable = true)))
                .initializer("mutableMapOf()")
                .build()
        )

        typeSpec.addProperty(
            PropertySpec.builder("cache", ClassName("kotlin.collections", "MutableMap").parameterizedBy(KClass::class.asClassName().parameterizedBy(STAR), LocalizationFile::class.asClassName()))
                .addModifiers(KModifier.PRIVATE)
                .initializer("mutableMapOf()")
                .build()
        )

        val localizationFileTypeParameter = TypeVariableName("T", localizationFileInterface.toTypeName())
        typeSpec.addFunction(
            FunSpec.builder("read")
                .addModifiers(KModifier.OVERRIDE)
                .addTypeVariable(localizationFileTypeParameter)
                .returns(localizationFileTypeParameter)
                .addParameter("type", KClass::class.asClassName().parameterizedBy(localizationFileTypeParameter))
                .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
                .beginControlFlow("return cache.getOrPut(type)")
                    .beginControlFlow("when (type)")
                        .apply {
                            implementations.forEach { (interfaceDefinition, classDefinition) ->
                                addStatement("%T::class -> %T(this)", interfaceDefinition.toClassName(), ClassName(classDefinition.first, classDefinition.second))
                            }

                            addStatement($$"else -> error(\"Could not find LocalizationFile implementation for $type\")")
                        }
                    .endControlFlow()
                .endControlFlow()
                .addCode("as %T\n", localizationFileTypeParameter)
                .build()
        )

        fileSpec.addType(typeSpec.build())
        fileSpec.build().writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    private fun generateFileImplementation(definition: KSClassDeclaration): Pair<String, String> {
        val className = "${definition.simpleName.asString()}Impl"

        val fileSpec = FileSpec.builder(definition.getNormalizedPackageName(), className)
        val typeSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(definition.toClassName())

        val managerType = ClassName.bestGuess(managerName)

        typeSpec.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("manager", managerType)
                .build()
        )

        typeSpec.addProperty(
            PropertySpec.builder("manager", managerType, KModifier.OVERRIDE)
                .initializer("manager")
                .build()
        )

        properties.forEach { name, type ->
            typeSpec.addProperty(
                PropertySpec.builder(name, type)
                    .delegate("manager.properties")
                    .build()
            )
        }

        val entries = readLocalizationFile(definition)
        val typeData = mutableMapOf<String, List<KSValueParameter>>()

        definition.getAllFunctions()
            .filter { function -> function.hasAnnotation(LOCALIZE_ANNOTATION) }
            .forEach { function ->
                val locale = function.parameters.single { it.hasAnnotation(LOCALE_ANNOTATION) }
                val parameters = function.parameters.filter { it.hasAnnotation(LOCALIZATION_PARAMETER_ANNOTATION) }

                val name = function.getAnnotation(LOCALIZE_ANNOTATION)?.arguments?.first()?.value?.toString()?.takeIf { it.isNotBlank() }
                    ?: function.simpleName.asString().replace("(?<=[^[A-Z]])[A-Z]".toRegex(), ".$0").lowercase()

                typeData += name to parameters

                typeSpec.addFunction(
                    FunSpec.builder(function.simpleName.asString())
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameters(function.parameters.map {
                            ParameterSpec.builder(it.name!!.asString(), it.type.toTypeName())
                                .build()
                        })
                        .returns(function.returnType!!.toTypeName())
                        .addStatement("return %N(${(listOf(locale.name!!.asString()) + parameters.map { "${it.name!!.asString()} = ${it.name!!.asString()}" }).joinToString(", ")})", name.replace(".", "_"))
                        .build()
                )
            }

        val typedEntries = mutableMapOf<String, List<ParameterSpec>>()

        entries.forEach { name, values ->
            val parameters = typeData[name]?.map { ParameterSpec.builder(it.name!!.asString(), it.type.resolve().toClassName()).build() } ?: values
                .mapNotNull { (it.value as? YamlTaggedNode)?.tag }
                .toSet()
                .also { if (it.size > 1) error("Yaml defined parameter lists have to match") }
                .firstOrNull()
                ?.parseParameterListDefinition()
            ?: emptyList()

            typedEntries[name] = parameters

            val function = FunSpec.builder(name.replace(".", "_"))
                .addParameter("locale", DiscordLocale::class)
                .addParameters(parameters)
                .returns(String::class)
                .beginControlFlow("return when (locale)")
                    .apply {
                        values.entries.sortedBy { (locale) -> locale == defaultLocale }.forEach { (locale, value) ->
                            val node = (value as? YamlTaggedNode)?.innerNode ?: value
                            val content = node.yamlScalar.content

                            if (locale != defaultLocale) addStatement("%T.$locale -> ", DiscordLocale::class)
                            else addStatement("else -> ", DiscordLocale::class)

                            addStatement("\"\"\"")
                            addCode("%L\n", content)
                            addStatement("\"\"\".trimIndent()")
                        }
                    }
                .endControlFlow()
                .build()

            typeSpec.addFunction(function)
        }

        typeSpec.addFunction(
            FunSpec.builder("readString")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("name", String::class)
                .addParameter("locale", DiscordLocale::class)
                .addParameter("args", Map::class.asClassName().parameterizedBy(String::class.asClassName(), STAR))
                .returns(String::class)
                .beginControlFlow("return when (name)")
                    .apply {
                        typedEntries.forEach { name, parameters ->
                            addStatement("%S -> %N(${(listOf("locale") + parameters.map { "${it.name} = args[\"${it.name}\"] as ${it.type}" }).joinToString(", ")})", name, name.replace(".", "_"))
                        }

                        addStatement($$"else -> error(\"Localization $name not found\")")
                    }
                .endControlFlow()
                .build()
        )

        fileSpec.addType(typeSpec.build())
        fileSpec.build().writeTo(codeGenerator, Dependencies.ALL_FILES)

        return definition.getNormalizedPackageName() to className
    }

    private fun readLocalizationFile(definition: KSClassDeclaration): Map<String, Map<DiscordLocale, YamlNode>> {
        val fileName = definition.getAnnotation(LOCALIZATION_PATH_ANNOTATION)?.arguments?.first()?.value?.toString()
            ?: definition.defaultName()

        return files.getOrPut(fileName) {
            locales.flatMap { locale ->
                val content = readLocalizationFileForLocale(locale, fileName) ?: emptyMap()
                content.map { (label, value) ->
                    label to (locale to  value)
                }
            }.groupBy { (name) -> name }.mapValues { (_, entry) -> entry.associate { it.second } }
        }
    }

    private fun readLocalizationFileForLocale(locale: DiscordLocale, name: String): Map<String, YamlNode>? {
        val location = locationFormat
            .replace("%locale%", locale.locale.lowercase().replace("-", "_"))
            .replace("%name%", name.replace(".", "/"))

        val file = File(location)
        if (!file.exists()) {
            logger.warn("Localization file for '$name' in $locale not found ($location)")
            return null
        }

        return Yaml.default.parseToYamlNode(file.inputStream()).flattenYaml()
    }

    private fun KSClassDeclaration.defaultName() = qualifiedName!!.asString().removePrefix("$botPackage.").removeSuffix(simpleName.asString()) +
        simpleName.asString().replace("(?<=[^A-Z])[A-Z]".toRegex(), "_$0").lowercase()
}

private fun KSAnnotated.getAnnotation(name: String) = annotations.firstOrNull {
    val annotation = it.annotationType.resolve().declaration
    annotation.qualifiedName?.asString() == name
}

private fun KSAnnotated.hasAnnotation(name: String) = getAnnotation(name) != null

private fun String.parseParameterListDefinition() = split(",").map {
    val temp = it.split(":", limit = 2)
    ParameterSpec.builder(temp[0], temp[1].parseTypeString()).build()
}