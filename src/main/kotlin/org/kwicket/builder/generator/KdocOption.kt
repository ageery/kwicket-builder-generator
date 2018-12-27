package org.kwicket.builder.generator

/**
 * Type of KDoc to add.
 *
 * @property add whether to add a KDoc
 * @property eol end-of-line to apply to the KDoc
 */
enum class KdocOption(val add: Boolean, val eol: String) {
    None(false, ""),
    Method(true, ""),
    Constructor(true, "\n")
}