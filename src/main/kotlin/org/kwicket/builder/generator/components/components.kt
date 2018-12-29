package org.kwicket.builder.generator.components

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.apache.wicket.Component
import org.apache.wicket.Page
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink
import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.behavior.Behavior
import org.apache.wicket.devutils.debugbar.DebugBar
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.basic.MultiLineLabel
import org.apache.wicket.markup.html.form.Button
import org.apache.wicket.markup.html.form.CheckBox
import org.apache.wicket.markup.html.form.FormComponent
import org.apache.wicket.markup.html.form.TextField
import org.apache.wicket.markup.html.link.BookmarkablePageLink
import org.apache.wicket.markup.html.link.Link
import org.apache.wicket.markup.html.link.PopupSettings
import org.apache.wicket.model.IModel
import org.apache.wicket.request.mapper.parameter.PageParameters
import org.kwicket.builder.generator.*
import kotlin.reflect.KClass

val componentConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Component::class),
    modelInfo = ModelInfo(),
    props = listOf(
        PropInfo(
            name = "model",
            type = {
                IModel::class.asTypeName()
                    .parameterizedBy(
                        if (modelInfo.type == TargetType.Exact)
                            modelInfo.target.asTypeName()
                        else it.modelTypeName
                    ).copy(nullable = this.modelInfo.nullable && (!it.type.isMethod))
            },
            desc = { "model for the component" }
        ),
        PropInfo(
            name = "markupId",
            type = { nullableStringTypeName },
            desc = { "optional unique id to use in the associated markup" }
        ),
        PropInfo(
            name = "outputMarkupId",
            type = { nullableBooleanTypeName },
            desc = { "whether to include an id for the component in the markup" }),
        PropInfo(
            name = "outputMarkupPlaceholderTag",
            type = { nullableBooleanTypeName },
            desc = { "whether to include a placeholder tag for the component in the markup when the component is not visible" }
        ),
        PropInfo(
            name = "isVisible",
            type = { nullableBooleanTypeName },
            desc = { "whether the component is initially visible" }
        ),
        PropInfo(
            name = "isEnabled",
            type = { nullableBooleanTypeName },
            desc = { "whether the component is initially enabled" }
        ),
        PropInfo(
            name = "isVisibilityAllowed",
            type = { nullableBooleanTypeName },
            desc = { "whether the component is initially allowed to be visible" }
        ),
        PropInfo(
            name = "escapeModelStrings",
            type = { nullableBooleanTypeName },
            desc = { "whether the component model strings should be escaped" }
        ),
        PropInfo(
            name = "renderBodyOnly",
            type = { nullableBooleanTypeName },
            desc = { "whether to only render the body of the component" }
        ),
        PropInfo(
            name = "behavior",
            type = { Behavior::class.asTypeName().copy(nullable = true) },
            desc = { "[Behavior] to add to the component" }
        ),
        PropInfo(
            name = "behaviors",
            type = { List::class.parameterizedBy(Behavior::class).copy(nullable = true) },
            desc = { "list of [Behavior]s to add to the component" }
        ),
        PropInfo(
            name = "statelessHint",
            type = { ClassName("org.kwicket.builder.config", "StatelessHint").copy(nullable = false) },
            default = { CodeBlock.of("StatelessHint.Default") },
            desc = { "type of stateless hint for the component" }
        ),
        PropInfo(
            name = "onConfig",
            type = {
                LambdaTypeName.get(
                    returnType = Unit::class.asTypeName(),
                    receiver = if (isConfigOnly) TypeVariableName(it.generatorInfo.componentParam.name) else componentInfo.target.asClassName()
                        .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) it.modelTypeName else null)
                ).copy(nullable = this.modelInfo.nullable)
            },
            desc = { "optional lambda to execute in the onConfigure lifecycle method" }
        )
    )
)

val webMarkupContainerConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = WebMarkupContainer::class),
    parent = componentConfig
)

val labelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Label::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "span")
)

val debugBarConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = DebugBar::class),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "isInitiallyExpanded",
            type = { nullableBooleanTypeName },
            desc = { "whether the debug is initially expanded" },
            default = { CodeBlock.of("true") }
        )
    )
)

val multiLineLabelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = MultiLineLabel::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "span")
)

val formComponentConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = FormComponent::class, isTargetParameterizedByModel = true),
    isConfigOnly = true,
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "label",
            type = { nullableStringModelTypeName },
            desc = { "label associated with the form component" }
        )
    )
)

val textFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = TextField::class, isTargetParameterizedByModel = true),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "text"))
)

val abstractButtonConfig = ConfigInfo(
    basename = "AbstractButton",
    componentInfo = ComponentInfo(target = Button::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = String::class),
    tagInfo = TagInfo(tagName = "button"),
    isConfigOnly = true,
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "defaultFormProcessing",
            type = { nullableBooleanTypeName },
            desc = { "whether the button submits the data" }
        )
    )
)

val buttonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Button::class),
    parent = abstractButtonConfig,
    modelInfo = ModelInfo(type = TargetType.Exact, target = String::class)
)

val checkBoxConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = CheckBox::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = Boolean::class, nullable = false),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "checkbox"))
)

val abstractLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Link::class, isTargetParameterizedByModel = true),
    parent = componentConfig,
    isConfigOnly = true,
    props = listOf(
        PropInfo(
            name = "popupSettings",
            type = { PopupSettings::class.asTypeName() },
            desc = { "specifies how the link opens" }
        )
    ),
    tagInfo = TagInfo(tagName = "a")
)

val ajaxLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxLink::class),
    parent = abstractLinkConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "ajax click handler" }
        )
    )
)

val bookmarkablePageLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = BookmarkablePageLink::class),
    parent = abstractLinkConfig,
    props = listOf(
        PropInfo(
            name = "pageClass",
            desc = { "class of the page the link references" },
            type = { KClass::class.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Page::class.asTypeName())) }
        ),
        PropInfo(
            name = "pageParams",
            type = { PageParameters::class.asTypeName() },
            desc = { "parameters to pass to the link" }
        )
    )
)

val ajaxFallbackLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxFallbackLink::class),
    parent = abstractLinkConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName().copy(nullable = true)),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "click handler lambda" }
        )
    )
)

val generatorInfo = GeneratorInfo(
    configInterface = ClassInfo(toPackage = { "org.kwicket.builder.config" }, toName = { "I${basename}Config" }),
    configClass = ClassInfo(toPackage = { "org.kwicket.builder.config" }, toName = { "${basename}Config" }),
    factoryMethod = ClassInfo(toPackage = { "org.kwicket.builder.factory" }, toName = { "invoke" }),
    includeMethod = ClassInfo(toPackage = { "org.kwicket.builder.queued" }, toName = { basename.decapitalize() }),
    tagClass = ClassInfo(toPackage = { "org.kwicket.builder.dsl.tag" }, toName = { "${basename}Tag" }),
    tagMethod = ClassInfo(toPackage = { "org.kwicket.builder.dsl.tag" }, toName = { basename.decapitalize() })
)

val allComponents = listOf(
    componentConfig,
    labelConfig,
    multiLineLabelConfig,
    debugBarConfig,
    webMarkupContainerConfig,
    abstractButtonConfig,
    buttonConfig,
    formComponentConfig,
    textFieldConfig,
    ajaxLinkConfig,
    checkBoxConfig,
    abstractLinkConfig,
    bookmarkablePageLinkConfig,
    ajaxFallbackLinkConfig
)