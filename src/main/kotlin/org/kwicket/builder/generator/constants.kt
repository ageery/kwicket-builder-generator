package org.kwicket.builder.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.TypeName

internal const val nullDefault = "null"
internal const val tagBuilderParamName = "config"
internal const val blockParmName = "block"
internal val emptyLambda = CodeBlock.of("{}")
