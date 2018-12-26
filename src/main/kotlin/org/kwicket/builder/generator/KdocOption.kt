package org.kwicket.builder.generator

enum class KdocOption(val add: Boolean, val eol: String) {
    None(false, ""),
    Method(true, ""),
    Constructor(true, "\n")
}