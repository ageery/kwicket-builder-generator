package org.kwicket.builder.generator

/**
 * Whether a target is exactly specified by a class or whether it has a generic parameter.
 */
enum class TargetType {
    /**
     * The target type of a model is an exact class.
     */
    Exact,
    /**
     * The target type of a model can be any class.
     */
    Unbounded
}