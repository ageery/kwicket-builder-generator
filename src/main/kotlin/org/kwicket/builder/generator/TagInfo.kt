package org.kwicket.builder.generator

/**
 * Information about an HTML tag associated with config info.
 *
 * @property tagName name of the tag
 * @property attrs attributes to add to the tag
 */
class TagInfo(
    val tagName: String? = null,
    val attrs: Map<String, String>? = null
)