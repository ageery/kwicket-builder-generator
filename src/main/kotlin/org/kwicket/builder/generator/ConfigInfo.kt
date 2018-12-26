package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName

class ConfigInfo(
    val componentInfo: ComponentInfo,
    val modelInfo: ModelInfo = ModelInfo(),
    val isConfigOnly: Boolean = componentInfo.target.isAbstract,
    val basename: String = componentInfo.target.java.simpleName,
    val parent: ConfigInfo? = null,
    val props: List<PropInfo> = emptyList(),
    val tagInfo: TagInfo = TagInfo(),
    val configInterfaceKdoc: (GeneratorInfo) -> String = {
        "Configuration for creating a [${componentInfo.target.simpleName}] component."
    },
    val configClassKdoc: (GeneratorInfo) -> String = {
        "Implementation of [${componentInfo.target.simpleName}]."
    }
)

val ConfigInfo.allParentProps: List<PropInfo>
    get() = parent?.let { it.allParentProps + it.props } ?: emptyList()

val ConfigInfo.allProps: List<PropInfo>
    get() = props + allParentProps

fun ConfigInfo.toClassName(classInfo: ClassInfo) = ClassName(classInfo.toPackage(this), classInfo.toName(this))
