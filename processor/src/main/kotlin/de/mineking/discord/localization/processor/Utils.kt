package de.mineking.discord.localization.processor

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun YamlNode.flattenYaml(prefix: String = ""): Map<String, YamlNode> = when (this) {
    is YamlMap -> {
        entries.flatMap { (key, value) ->
            val fullKey = if (prefix.isEmpty()) key.content else "$prefix.${key.content}"
            value.flattenYaml(fullKey).entries
        }.associate { it.key to it.value }
    }
    else -> mapOf(prefix to this)
}

private val kotlinPrimitives = mapOf(
    "Int" to INT,
    "Long" to LONG,
    "Short" to SHORT,
    "Byte" to BYTE,
    "Float" to FLOAT,
    "Double" to DOUBLE,
    "Boolean" to BOOLEAN,
    "Char" to CHAR,
    "String" to STRING,
    "Any" to ANY,
    "Unit" to UNIT
)

fun String.parseTypeString(): TypeName {
    val trimmed = trim()

    if (trimmed.endsWith("?")) return trimmed.dropLast(1).parseTypeString().copy(nullable = true)
    if (trimmed == "*") return STAR

    val genericStart = trimmed.indexOf("<")
    if (genericStart != -1 && trimmed.endsWith(">")) {
        val baseName = trimmed.substring(0, genericStart).trim()
        val genericPart = trimmed.substring(genericStart + 1, trimmed.length - 1)
        val typeArguments = splitTopLevelGenerics(genericPart).map { it.parseTypeString() }

        return ClassName.bestGuess(baseName).parameterizedBy(*typeArguments.toTypedArray())
    }

    return kotlinPrimitives[trimmed] ?: ClassName.bestGuess(trimmed)
}

private fun splitTopLevelGenerics(generics: String): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    var depth = 0

    for (c in generics) {
        when (c) {
            '<' -> {
                depth++
                buffer.append(c)
            }
            '>' -> {
                depth--
                buffer.append(c)
            }
            ',' -> {
                if (depth == 0) {
                    result += buffer.toString().trim()
                    buffer.clear()
                } else buffer.append(c)
            }
            else -> buffer.append(c)
        }
    }

    if (buffer.isNotEmpty()) result += buffer.toString().trim()
    return result
}