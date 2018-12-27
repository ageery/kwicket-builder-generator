package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName

class ConfigInfo(
    val componentInfo: ComponentInfo,
    val modelInfo: ModelInfo = ModelInfo(),
    val isConfigOnly: Boolean = componentInfo.target.isAbstract,
    val basename: String = componentInfo.target.java.simpleName,
    val parent: ConfigInfo? = null,
    val props: List<PropInfo> = emptyList(),
    val tagInfo: TagInfo? = null,
    val configInterfaceKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Configuration for creating a [${componentInfo.target.simpleName}] component."
    },
    val configClassKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Implementation of [${it.configInterface.toName.invoke(this)}]."
    },
    val tagClassKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "HTML tag class corresponding to the [${componentInfo.target.simpleName}] component."
    },
    val tagMethodKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Adds the [${componentInfo.target.simpleName}] component to the HTML."
    },
    val includeMethodKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Creates and includes the [${componentInfo.target.simpleName}] component in a [MarkupContainer]."
    }
)

val ConfigInfo.defaultTagName: String
     get() = tagInfo?.tagName ?: parent?.defaultTagName ?: "div"

val ConfigInfo.defaultTagAttrs: Map<String, String>
    get() = tagInfo?.attrs ?: parent?.defaultTagAttrs ?: emptyMap()

val ConfigInfo.allParentProps: List<PropInfo>
    get() = parent?.let { it.allParentProps + it.props } ?: emptyList()

val ConfigInfo.allProps: List<PropInfo>
    get() = props + allParentProps

fun ConfigInfo.toClassName(classInfo: ClassInfo) = ClassName(classInfo.toPackage(this), classInfo.toName(this))
