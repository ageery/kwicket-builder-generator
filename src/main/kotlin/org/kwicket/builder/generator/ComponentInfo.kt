package org.kwicket.builder.generator

import kotlin.reflect.KClass

class ComponentInfo(
    val target: KClass<*>,
    val isTargetParameterizedByModel: Boolean = !target.typeParameters.isNullOrEmpty(),
    val kdoc: String? = null
)