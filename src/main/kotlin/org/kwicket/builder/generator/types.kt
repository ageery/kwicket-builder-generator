package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.html.TagConsumer
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

// props

internal val blockPropInfo = PropInfo(
    name = "block",
    mutable = true,
    desc = { "optional block to execute to configure the component" },
    default = { CodeBlock.of(nullDefault) },
    type = {
        LambdaTypeName.get(
            receiver = toClassName(it.generatorInfo.configInterface)
                .parameterizedBy(if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR),
            returnType = Unit::class.asTypeName()
        ).copy(nullable = true)
    }
)

internal val tagNamePropInfo = PropInfo(
    name = "tagName",
    type = { stringTypeName },
    default = { CodeBlock.of(""""$defaultTagName"""") },
    mutable = false,
    desc = { "Name of the HTML tag" }
)

internal val initialAttrsPropInfo  = PropInfo(
    name = "initialAttributes", type = { stringMapTypeName },
    default = { defaultTagAttrs.toLiteralMapCodeBlock() },
    mutable = false,
    desc = { "Tag attributes" }
)

internal val configPropInfo = PropInfo(
    name = "config",
    default = null,
    type = {
        // FIXME: pull this out
        toClassName(it.generatorInfo.configInterface).parameterizedBy(it.generatorInfo.toModelTypeVarName())
    }
)

internal val factoryPropInfo = PropInfo(
    name = "factory",
    default = { CodeBlock.of("{ cid, c -> c.%T(cid) }", factoryInvokeTypeName) },
    type = {
        LambdaTypeName.get(
            returnType = componentInfo.target.asTypeName().parameterizedBy(
                if (modelInfo.type == TargetType.Unbounded) it.generatorInfo.toModelTypeVarName() else null
            ),
            parameters = *arrayOf(
                stringTypeName,
                toClassName(it.generatorInfo.configInterface).parameterizedBy(it.generatorInfo.toModelTypeVarName())
            )
        )
    }
)

internal fun idPropInfo(isNullable: Boolean = false) = PropInfo(
    name = "id", type = { nullableStringTypeName },
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