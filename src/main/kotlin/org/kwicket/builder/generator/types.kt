package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.html.TagConsumer
import org.apache.wicket.model.IModel
import org.kwicket.builder.generator.components.generatorInfo

// generic types

internal val stringTypeName = String::class.asTypeName()
internal val nullableStringTypeName = String::class.asTypeName().copy(nullable = true)
internal val nullableBooleanTypeName = Boolean::class.asTypeName().copy(nullable = true)
internal val stringMapTypeName = Map::class.asTypeName().parameterizedBy(stringTypeName, stringTypeName)
internal val nullableStringModelTypeName =
    IModel::class.asTypeName().parameterizedBy(String::class.asTypeName()).copy(nullable = true)
internal val visitMethod = ClassName("kotlinx.html", "visit")
internal val mutableListTypeName = ClassName("kotlin.collections", "MutableList")

// props

internal val includeBlockPropInfo = PropInfo(
    name = "block",
    mutable = true,
    desc = { "optional block to execute to configure the component" },
    default = { CodeBlock.of(nullDefault) },
    type = {
        LambdaTypeName.get(
            receiver = toClassName(it.generatorInfo.configInterface)
                .parameterizedBy(
                    if (isConfigOnly) it.generatorInfo.toComponentTypeVarName() else null,
                    if (modelInfo.isExactlyOneType) null
                    else if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR
                ),
            returnType = Unit::class.asTypeName()
        ).copy(nullable = true)
    }
)

internal val tagBlockPropInfo = PropInfo(
    name = "block",
    mutable = true,
    desc = { "optional block to execute to configure the component" },
    default = { emptyLambda },
    type = {
        LambdaTypeName.get(
            receiver = toClassName(it.generatorInfo.tagClass)
                .parameterizedBy(
                    if (modelInfo.isExactlyOneType) null
                    else if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR
                ),
            returnType = Unit::class.asTypeName()
        )
    }
)

internal val tagNamePropInfo = PropInfo(
    name = "tagName",
    type = { stringTypeName },
    default = { CodeBlock.of(""""$defaultTagName"""") },
    mutable = false,
    desc = { "Name of the HTML tag" }
)

internal val initialAttrsPropInfo = PropInfo(
    name = "initialAttributes", type = { stringMapTypeName },
    default = { defaultTagAttrs.toLiteralMapCodeBlock() },
    mutable = false,
    desc = { "Tag attributes" }
)

internal val configPropInfo = PropInfo(
    name = "config",
    default = null,
    type = { toClassName(it.generatorInfo.configInterface)
            // FIXME: we should pull the if-stmt out into some sort of function
        .parameterizedBy(if (modelInfo.isUseTypeVar) it.generatorInfo.toModelTypeVarName() else null) }
)

internal val factoryPropInfo = PropInfo(
    name = "factory",
    default = { CodeBlock.of("{ cid, c -> c.%T(cid) }", toClassName(generatorInfo.factoryMethod)) },
    type = {
        LambdaTypeName.get(
            returnType = componentInfo.target.asTypeName().parameterizedBy(
                //if (modelInfo.type == TargetType.Unbounded) it.generatorInfo.toModelTypeVarName() else null
                if (componentInfo.isTargetParameterizedByModel) it.generatorInfo.toModelTypeVarName() else null
            ),
            parameters = *arrayOf(
                stringTypeName,
                toClassName(it.generatorInfo.configInterface).parameterizedBy(if (modelInfo.isUseTypeVar) it.generatorInfo.toModelTypeVarName() else null)
            )
        )
    }
)

internal fun idPropInfo(isNullable: Boolean = false) = PropInfo(
    name = "id", type = { String::class.asTypeName().copy(nullable = isNullable) },
    default = { if (isNullable) CodeBlock.of(nullDefault) else null },
    mutable = false,
    desc = { "Wicket component id" }
)

internal val consumerPropInfo = PropInfo(
    name = "consumer",
    default = null,
    type = { TagConsumer::class.asTypeName().parameterizedBy(STAR) }
)

internal val tagClassParams = listOf(
    idPropInfo(isNullable = true),
    tagNamePropInfo,
    initialAttrsPropInfo,
    consumerPropInfo,
    configPropInfo,
    factoryPropInfo
)
