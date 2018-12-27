package org.kwicket.builder.generator

/**
 * Whether a target is exactly specified by a class or whether it has a generic parameter.
 */
enum class TargetType {
    Exact,
    Unbounded
}