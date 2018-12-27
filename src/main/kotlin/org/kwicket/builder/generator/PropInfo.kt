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
    val default: CodeBlock? = CodeBlock.of(nullDefault),
    val desc: PropInfo.() -> String = { "info about $name" }
)

// FIXME: other things we would like to be able to pass to the type lambda:
// - GeneratorInfo
// - type of what is being generated -- config interface, config class, include method, tag class, tag method
// - whether the model parameter is named -- Boolean
// - type of the model

enum class GeneratorType {
    ConfigInterface,
    ConfigClass,
    IncludeMethod,
    TagClass,
    TagMethod
}

class TypeContext(
    val generatorInfo: GeneratorInfo,
    val type: GeneratorType,
    val isModelParameterNamed: Boolean = true,
    val modelTypeName: TypeName
)