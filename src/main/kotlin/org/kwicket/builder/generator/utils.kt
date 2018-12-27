package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

fun ClassName.parameterizedBy(vararg typeNames: TypeName?) = typeNames.filterNotNull().let {
    if (it.isNotEmpty()) parameterizedBy(*it.toTypedArray()) else this
}

fun Map<*, *>.toLiteralMapCodeBlock() = CodeBlock.of(
    if (isEmpty()) "emptyMap()"
    else "mapOf(${entries.joinToString(",\n") { """"${it.key}" to "${it.value}"""" }})"
)

