package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName

class ClassInfo(val toPackage: ConfigInfo.() -> String, val toName: ConfigInfo.() -> String)