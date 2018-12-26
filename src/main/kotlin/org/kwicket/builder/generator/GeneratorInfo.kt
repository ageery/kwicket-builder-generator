package org.kwicket.builder.generator

import com.squareup.kotlinpoet.TypeVariableName

class GeneratorInfo(
    val configInterface: ClassInfo,
    val configClass: ClassInfo,
    val factoryMethod: ClassInfo,
    val includeMethod: ClassInfo,
    val tagClass: ClassInfo,
    val tagMethod: ClassInfo,
    val componentParameterName: String = "C",
    val modelParameterName: String = "T"
)

fun GeneratorInfo.toModelTypeVarName() = TypeVariableName(modelParameterName)
fun GeneratorInfo.toComponentTypeVarName() = TypeVariableName(componentParameterName)