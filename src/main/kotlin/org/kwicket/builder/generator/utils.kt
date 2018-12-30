package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.kwicket.builder.generator.components.generatorInfo
import kotlin.reflect.KClass

internal fun ClassName.parameterizedBy(vararg typeNames: TypeName?) = typeNames.filterNotNull().let {
    if (it.isNotEmpty()) parameterizedBy(*it.toTypedArray()) else this
}

internal fun Map<*, *>.toLiteralMapCodeBlock() = CodeBlock.of(
    if (isEmpty()) "emptyMap()"
    else "mapOf(${entries.joinToString(",\n") { """"${it.key}" to "${it.value}"""" }})"
)

internal fun KClass<*>.modelParam(context: TypeContext) = asTypeName()
    .parameterizedBy(if (context.isModelParameterNamed) context.generatorInfo.toModelTypeVarName() else STAR)
