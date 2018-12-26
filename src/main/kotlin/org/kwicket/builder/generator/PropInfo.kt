package org.kwicket.builder.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

data class PropInfo(
    val name: String,
    val type: ConfigInfo.(TypeName) -> TypeName,
    val mutable: Boolean = true,
    val default: CodeBlock? = CodeBlock.of(nullDefault),
    val desc: PropInfo.() -> String = { "info about $name" }
)