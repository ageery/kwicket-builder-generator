package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

//fun KClass<*>.toClassName() = ClassName(java.`package`?.name ?: "", java.simpleName)

fun ClassName.parameterizedBy(vararg typeNames: TypeName?) = typeNames.filterNotNull().let {
    if (it.isNotEmpty()) parameterizedBy(*it.toTypedArray()) else this
}


