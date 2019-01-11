package org.kwicket.builder.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

/**
 * Information about a component property.
 *
 * @property name name of the property
 * @property type lambda for deriving the type of the property
 * @property mutable whether the property is mutable
 * @property default default value for the property
 * @property desc lambda for deriving a description of the property
 */
data class PropInfo(
    val name: String,
    val type: ConfigInfo.(TypeContext) -> TypeName,
    val mutable: Boolean = true,
    val default: (ConfigInfo.(TypeName) -> CodeBlock?)? = { if (it.isNullable) CodeBlock.of(nullDefault) else null },
    val desc: PropInfo.() -> String = { "info about $name" }
)

/**
 * Type of code being generated.
 */
enum class GeneratorType {
    ConfigInterface,
    ConfigClass,
    IncludeMethod,
    TagClass,
    TagMethod
}

val GeneratorType.isMethod: Boolean
    get() = this == GeneratorType.IncludeMethod || this == GeneratorType.TagMethod

/**
 * Context info about the type being generated.
 *
 * @property generatorInfo naming information
 * @property type type of code being generated
 * @property isModelParameterNamed whether the model parameter is named
 * @property modelTypeName name of the generic parameter of the model
 */
class TypeContext(
    val generatorInfo: GeneratorInfo,
    val type: GeneratorType,
    val isModelParameterNamed: Boolean = true,
    val modelTypeName: TypeName
)