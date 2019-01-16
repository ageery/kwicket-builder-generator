package org.kwicket.builder.generator

/**
 * Type of KDoc to add.
 *
 * @property add whether to add a KDoc
 * @property eol end-of-line to apply to the KDoc
 */
enum class KdocOption(val add: Boolean, val eol: String) {
    /**
     * No kdoc should be generated.
     */
    None(false, ""),
    /**
     * Generate kdoc for a method.
     */
    Method(true, ""),
    /**
     * Generate kdoc for a constructor.
     */
    Constructor(true, "\n")
}