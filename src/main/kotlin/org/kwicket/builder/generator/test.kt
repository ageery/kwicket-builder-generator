package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.apache.wicket.Component
import org.apache.wicket.behavior.Behavior
import org.apache.wicket.devutils.debugbar.DebugBar
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.basic.MultiLineLabel
import org.apache.wicket.markup.html.form.Button
import org.apache.wicket.markup.html.form.FormComponent
import org.apache.wicket.markup.html.form.TextField
import org.apache.wicket.model.IModel
import org.kwicket.builder.generator.components.allComponents
import org.kwicket.builder.generator.components.generatorInfo


fun main() {

    KWicketBuilder(generatorInfo = generatorInfo, builder = FileSpec.builder("com.andrew", "andrew")).apply {
        allComponents.forEach {
            it.toConfigInterface()
            it.toConfigClass()
            if (!it.isConfigOnly) {
                it.toTagClass()
                it.toTagMethod(true)
                it.toTagMethod(false)
                it.toIncludeMethod(true)
                it.toIncludeMethod(false)
            }
        }
    }.write(System.out)

}
