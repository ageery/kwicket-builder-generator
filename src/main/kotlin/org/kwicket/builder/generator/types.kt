package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import kotlinx.html.HTMLTag
import kotlinx.html.Tag
import kotlinx.html.visit
import org.apache.wicket.model.IModel

internal val stringTypeName = String::class.asTypeName()
internal val nullableStringTypeName = String::class.asTypeName().copy(nullable = true)
internal val stringMapTypeName = Map::class.asTypeName().parameterizedBy(stringTypeName, stringTypeName)

// FIXME: ultimately, we'll just want to depend on this library
internal val configurableComponentTagTypeName = ClassName("org.kwicket.builder.dsl", "ConfigurableComponentTag")
internal val factoryInvokeTypeName = ClassName("org.kwicket.builder.factory", "invoke")
internal val htmlTagTypeName = HTMLTag::class.asTypeName()

// FIXME: get rid of the functions and replace with vals
fun stringType(nullable: Boolean = true) = String::class.asTypeName().copy(nullable = nullable)
fun booleanType(nullable: Boolean = true) = Boolean::class.asTypeName().copy(nullable = nullable)
fun stringModelType(nullable: Boolean = true) =
    IModel::class.asTypeName().parameterizedBy(String::class.asTypeName()).copy(nullable = nullable)

val visitMethod = ClassName("kotlinx.html", "visit") // FIXME: can we reference this?
//val x = Tag::visit