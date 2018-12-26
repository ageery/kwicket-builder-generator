package org.kwicket.builder.generator

import kotlin.reflect.KClass

class ModelInfo(
    val type: TargetType = TargetType.Unbounded,
    val target: KClass<*> = Any::class,
    val nullable: Boolean = true,
    val kdoc: String? = null
)