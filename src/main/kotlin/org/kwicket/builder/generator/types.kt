package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import org.apache.wicket.model.IModel

fun stringType(nullable: Boolean = true) = String::class.asTypeName().copy(nullable = nullable)
fun booleanType(nullable: Boolean = true) = Boolean::class.asTypeName().copy(nullable = nullable)
fun stringModelType(nullable: Boolean = true) =
    IModel::class.asTypeName().parameterizedBy(String::class.asTypeName()).copy(nullable = nullable)