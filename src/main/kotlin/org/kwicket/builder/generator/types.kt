package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import kotlinx.html.HTMLTag
import kotlinx.html.Tag
import kotlinx.html.visit
import org.apache.wicket.model.IModel

// generic types

internal val stringTypeName = String::class.asTypeName()
internal val nullableStringTypeName = String::class.asTypeName().copy(nullable = true)
internal val nullableBooleanTypeName = Boolean::class.asTypeName().copy(nullable = true)
internal val stringMapTypeName = Map::class.asTypeName().parameterizedBy(stringTypeName, stringTypeName)
internal val nullableStringModelTypeName =
    IModel::class.asTypeName().parameterizedBy(String::class.asTypeName()).copy(nullable = true)

// kwicket classes

internal val configurableComponentTagTypeName = ClassName("org.kwicket.builder.dsl", "ConfigurableComponentTag")
internal val factoryInvokeTypeName = ClassName("org.kwicket.builder.factory", "invoke")

// methods

internal val visitMethod = ClassName("kotlinx.html", "visit")
val qMethodTypeName = ClassName("org.kwicket.builder.queued", "q")