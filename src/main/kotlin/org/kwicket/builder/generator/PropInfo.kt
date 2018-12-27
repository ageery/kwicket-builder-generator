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
    val type: ConfigInfo.(TypeName) -> TypeName,
    val mutable: Boolean = true,
    val default: CodeBlock? = CodeBlock.of(nullDefault),
    val desc: PropInfo.() -> String = { "info about $name" }
)