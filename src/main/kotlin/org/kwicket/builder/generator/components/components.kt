package org.kwicket.builder.generator.components

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.apache.wicket.Component
import org.apache.wicket.Page
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink
import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.ajax.markup.html.form.AjaxButton
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink
import org.apache.wicket.behavior.Behavior
import org.apache.wicket.devutils.debugbar.DebugBar
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel
import org.apache.wicket.extensions.markup.html.form.datetime.*
import org.apache.wicket.extensions.markup.html.form.select.Select
import org.apache.wicket.extensions.markup.html.tabs.ITab
import org.apache.wicket.feedback.IFeedbackMessageFilter
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.basic.MultiLineLabel
import org.apache.wicket.markup.html.form.*
import org.apache.wicket.markup.html.form.upload.FileUpload
import org.apache.wicket.markup.html.form.upload.FileUploadField
import org.apache.wicket.markup.html.image.Image
import org.apache.wicket.markup.html.image.InlineImage
import org.apache.wicket.markup.html.image.Picture
import org.apache.wicket.markup.html.image.Source
import org.apache.wicket.markup.html.link.*
import org.apache.wicket.markup.html.list.ListItem
import org.apache.wicket.markup.html.list.ListView
import org.apache.wicket.markup.html.media.MediaComponent
import org.apache.wicket.markup.html.media.audio.Audio
import org.apache.wicket.markup.html.media.video.Video
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.markup.repeater.RepeatingView
import org.apache.wicket.model.IModel
import org.apache.wicket.request.mapper.parameter.PageParameters
import org.apache.wicket.request.resource.IResource
import org.apache.wicket.request.resource.PackageResourceReference
import org.apache.wicket.request.resource.ResourceReference
import org.apache.wicket.util.lang.Bytes
import org.apache.wicket.validation.IValidator
import org.kwicket.builder.generator.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.FormatStyle
import kotlin.reflect.KClass

/*
 Migration notes:
 - AbstractColumnConfig -- not sure what to do with this as it isn't a component...
 - DataTable -- this has an extra type var for the sort
 - CheckGroup -- this could be a weird one <- ** do this next ** // can't do this one -- almost need a superclass & then this will be the model -- I think this needs one more
 - FileUpload -- this could be weird too -- it's a collection
 */

/**
 * kWicket package and class info.
 */
val generatorInfo = GeneratorInfo(
    configInterface = ClassInfo(toPackage = { "org.kwicket.builder.config" }, toName = { "I${basename}Config" }),
    configClass = ClassInfo(toPackage = { "org.kwicket.builder.config" }, toName = { "${basename}Config" }),
    factoryMethod = ClassInfo(toPackage = { "org.kwicket.builder.factory" }, toName = { "invoke" }),
    includeMethod = ClassInfo(toPackage = { "org.kwicket.builder.include.dsl" }, toName = { basename.decapitalize() }),
    tagClass = ClassInfo(toPackage = { "org.kwicket.builder.tag.dsl" }, toName = { "${basename}Tag" }),
    tagMethod = ClassInfo(toPackage = { "org.kwicket.builder.tag.dsl" }, toName = { basename.decapitalize() }),
    baseTagClass = ClassInfo(toPackage = { "org.kwicket.builder.tag" }, toName = { "ConfigurableComponentTag" }),
    includeFactory = ClassInfo(toPackage = { "org.kwicket.builder.include" }, toName = { "q" })
)

/**
 * [Component] config def.
 */
val componentConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Component::class),
    modelInfo = ModelInfo(),
    props = listOf(
        PropInfo(
            name = "model",
            type = {
                IModel::class.asTypeName()
                    .parameterizedBy(
                        if (modelInfo.isExactlyOneType || modelInfo.genericType != null) modelInfo.target.invoke(it)
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
                    receiver =
                    if (isConfigOnly) TypeVariableName(it.generatorInfo.componentParam.name)
                    else componentInfo.target.asClassName().parameterizedBy(
                        if (componentInfo.isTargetParameterizedByModel)
                            if (it.isModelParameterNamed) it.modelTypeName else STAR
                        else null
                    )
                ).copy(nullable = true)
            },
            desc = { "optional lambda to execute in the onConfigure lifecycle method" }
        )
    )
)

/**
 * Abstract [Form] config def.
 */
val abstractFormConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Form::class),
    basename = "AbstractForm",
    isConfigOnly = true,
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "isMultiPart",
            type = { nullableBooleanTypeName },
            desc = { "whether the form is a multi-part submission" }
        ),
        PropInfo(
            name = "maxSize",
            type = { Bytes::class.asTypeName().nullable() },
            desc = { "maximum bytes the form can submit" }
        ),
        PropInfo(
            name = "fileMaxSize",
            type = { Bytes::class.asTypeName().nullable() },
            desc = { "maximum bytes a single file in the form submission can be" }
        ),
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver =
                    if (isConfigOnly)
                        it.generatorInfo.toComponentTypeVarName()
                    else
                        componentInfo.target.asClassName().parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "submit handler lambda" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver =
                    if (isConfigOnly)
                        it.generatorInfo.toComponentTypeVarName()
                    else
                        componentInfo.target.asClassName().parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "error handler lambda" }
        )
    )
)

/**
 * [Form] config def.
 */
val formConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Form::class),
    parent = abstractFormConfig
)

/**
 * [StatelessForm] config def.
 */
val statelessFormConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = StatelessForm::class),
    parent = abstractFormConfig
)

/**
 * [WebMarkupContainer] config def.
 */
val webMarkupContainerConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = WebMarkupContainer::class),
    parent = componentConfig
)

/**
 * [Label] config def.
 */
val labelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Label::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "span")
)

/**
 * [DebugBar] config def.
 */
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

/**
 * [MultiLineLabel] config def.
 */
val multiLineLabelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = MultiLineLabel::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "span")
)

/**
 * [FormComponent] config def.
 */
val formComponentConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = FormComponent::class, isTargetParameterizedByModel = true),
    isConfigOnly = true,
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "isRequired",
            type = { nullableBooleanTypeName },
            desc = { "whether the form component is required" }
        ),
        PropInfo(
            name = "label",
            type = { nullableStringModelTypeName },
            desc = { "label associated with the form component" }
        ),
        PropInfo(
            name = "validators",
            type = {
                List::class.asTypeName().parameterizedBy(
                    IValidator::class.asTypeName()
                        .parameterizedBy(
                            when {
                                modelInfo.isExactlyOneType -> modelInfo.target.invoke(it)
                                it.isModelParameterNamed -> it.modelTypeName
                                modelInfo.type == TargetType.Exact -> modelInfo.target.invoke(it)
                                it.modelTypeName == STAR -> Any::class.asTypeName().nullable()
                                else -> Any::class.asTypeName().nullable()
                            }
                        )
                ).nullable()
            },
            desc = { "validator for the form component" }
        )
    )
)

/**
 * [TextArea] config def.
 */
val textAreaConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = TextArea::class),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "textarea")
)

/**
 * [TextField] config def.
 */
val textFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = TextField::class, isTargetParameterizedByModel = true),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "text")),
    props = listOf(
        PropInfo(
            name = "type",
            type = {
                Class::class.asTypeName()
                    .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else Any::class.asTypeName().nullable())
                    .nullable()
            },
            desc = { "type of the input" }
        )
    )
)

/**
 * [PasswordTextField] config def.
 */
val passwordTextFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = PasswordTextField::class),
    modelInfo = ModelInfo(target = { String::class.asClassName().nullable() }, nullable = true, type = TargetType.Exact),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "password"))
)

///**
// * [TextField] config def.
// */
//val requiredTextFieldConfig = ConfigInfo(
//    componentInfo = ComponentInfo(target = RequiredTextField::class, isTargetParameterizedByModel = true),
//    parent = formComponentConfig,
//    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "text")),
//    props = listOf(
//        PropInfo(
//            name = "type",
//            type = {
//                Class::class.asTypeName()
//                    .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else Any::class.asTypeName().nullable())
//                    .nullable()
//            },
//            desc = { "type of the input" }
//        )
//    )
//)

/**
 * [AutoCompleteTextField] config def.
 */
// FIXME: it would be nice to put all of the properties from the settings object here
val autoCompleteTextFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AutoCompleteTextField::class, isTargetParameterizedByModel = true),
    parent = formComponentConfig,
    isConfigOnly = false,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "text")),
    props = listOf(
        PropInfo(
            name = "type",
            type = {
                Class::class.asTypeName()
                    .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else Any::class.asTypeName().nullable())
                    .nullable()
            },
            desc = { "type of the input" }
        ),
        PropInfo(
            name = "renderer",
            type = {
                IAutoCompleteRenderer::class.asTypeName()
                    .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else Any::class.asTypeName().nullable())
                    .nullable()
            },
            desc = { "how to render the autocomplete choices" }
        ),
        PropInfo(
            name = "settings",
            type = { AutoCompleteSettings::class.asTypeName().nullable() },
            desc = { "settings for the autocomplete" }
        ),
        PropInfo(
            name = "choices",
            type = {
                LambdaTypeName.get(
                    receiver = AutoCompleteTextField::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else Any::class.asTypeName().nullable())
                        .nullable(),
                    parameters = *arrayOf(String::class.asTypeName().nullable()),
                    returnType = Sequence::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else Any::class.asTypeName().nullable())
                        .nullable()
                ).copy(nullable = true)
            },
            desc = { "settings for the autocomplete" }
        )
    )
)

/**
 * Abstract [Button] config def.
 */
val abstractButtonConfig = ConfigInfo(
    basename = "AbstractButton",
    componentInfo = ComponentInfo(target = Button::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { nullableStringTypeName }),
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

/**
 * [AjaxButton] config def.
 */
val ajaxButtonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxButton::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = { nullableStringTypeName }
    ),
    parent = abstractButtonConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "form",
            type = { Form::class.asTypeName().parameterizedBy(STAR).nullable() },
            desc = { "form the button is associated with" }
        ),
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxButton::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "submit handler lambda" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxButton::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "error handler lambda" }
        )
    )
)

/**
 * [IndicatingAjaxButton] config def.
 */
val indicatingAjaxButtonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = IndicatingAjaxButton::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = { nullableStringTypeName }
    ),
    parent = abstractButtonConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "form",
            type = { Form::class.asTypeName().parameterizedBy(STAR).nullable() },
            desc = { "form the button is associated with" }
        ),
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxButton::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "submit handler lambda" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxButton::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "error handler lambda" }
        )
    )
)

/**
 * [AjaxFallbackButton] config def.
 */
val ajaxFallbackButtonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxFallbackButton::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = { nullableStringTypeName }
    ),
    parent = abstractButtonConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "form",
            type = { Form::class.asTypeName().parameterizedBy(STAR).nullable() },
            desc = { "form the button is associated with" }
        ),
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxFallbackButton::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName().copy(nullable = true)),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "submit handler lambda" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxFallbackButton::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName().copy(nullable = true)),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "error handler lambda" }
        )
    )
)

/**
 * [AjaxTabbedPanel] config def.
 */
val ajaxTabbedPanelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxTabbedPanel::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = { Int::class.asTypeName() },
        genericType = { ITab::class.asTypeName() }
    ),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "tabs",
            type = {
                List::class.asTypeName()
                    .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else ITab::class.asTypeName())
            },
            desc = { "tabs in the tab panel" }
        )
    )
)

/**
 * [Button] config def.
 */
val buttonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Button::class),
    parent = abstractButtonConfig,
    modelInfo = ModelInfo(type = TargetType.Exact, target = { nullableStringTypeName }),
    props = listOf(
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver = Button::class.asTypeName(),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "submit handler lambda" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver = Button::class.asTypeName(),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "error handler lambda" }
        )
    )
)

/**
 * [CheckBox] config def.
 */
val checkBoxConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = CheckBox::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { Boolean::class.asTypeName() }, nullable = false),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "checkbox"))
)

/**
 * [AjaxCheckBox] config def.
 */
val ajaxCheckBoxConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxCheckBox::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { Boolean::class.asTypeName() }, nullable = false),
    parent = formComponentConfig,
    isConfigOnly = false,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "checkbox")),
    props = listOf(
        PropInfo(
            name = "onUpdate",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxCheckBox::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "lambda handler when the checkbox is toggled" }
        )
    )
)

/**
 * Abstract [Link] config def.
 */
val abstractLinkConfig = ConfigInfo(
    basename = "AbstractLink",
    componentInfo = ComponentInfo(target = Link::class),
    parent = componentConfig,
    isConfigOnly = true,
    props = listOf(
        PropInfo(
            name = "popupSettings",
            type = { PopupSettings::class.asTypeName().nullable() },
            desc = { "specifies how the link opens" }
        ),
        PropInfo(
            name = "autoEnable",
            type = { nullableBooleanTypeName },
            desc = { "whether link should automatically enable/disable based on current page" }
        )
    ),
    tagInfo = TagInfo(tagName = "a")
)

/**
 * [ExternalLink] config def.
 */
val externalLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = ExternalLink::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { nullableStringTypeName }),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "popupSettings",
            type = { PopupSettings::class.asTypeName().nullable() },
            desc = { "specifies how the link opens" }
        ),
        PropInfo(
            name = "label",
            type = {
                IModel::class.asTypeName().parameterizedBy(STAR).copy(nullable = true)
            },
            desc = { "text for the link" }
        )
    )
)

///**
// * [FileUploadField] config def.
// */
//val fileUploadFieldConfig = ConfigInfo(
//    componentInfo = ComponentInfo(target = FileUploadField::class),
//    modelInfo = ModelInfo(type = TargetType.Exact, target = Boolean::class, nullable = true),
//    parent = formComponentConfig,
//    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "checkbox"))
//)

/**
 * [Link] config def.
 */
val linkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Link::class),
    parent = abstractLinkConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = Link::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "ajax click handler" }
        )
    )
)

/**
 * [AjaxLink] config def.
 */
val ajaxLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxLink::class),
    parent = componentConfig,
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
        ),
        PropInfo(
            name = "updateAjaxAttrs",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestAttributes::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "updates the ajax attributes" }
        )
    )
)

/**
 * [IndicatingAjaxLink] config def.
 */
val indicatingAjaxLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = IndicatingAjaxLink::class),
    parent = componentConfig,
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
        ),
        PropInfo(
            name = "updateAjaxAttrs",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestAttributes::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "updates the ajax attributes" }
        )
    )
)

/**
 * [BookmarkablePageLink] config def.
 */
val bookmarkablePageLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = BookmarkablePageLink::class),
    parent = abstractLinkConfig,
    props = listOf(
        PropInfo(
            name = "page",
            desc = { "class of the page the link references" },
            type = {
                KClass::class.asTypeName().parameterizedBy(WildcardTypeName.producerOf(Page::class.asTypeName()))
                    .nullable()
            }
        ),
        PropInfo(
            name = "params",
            type = { PageParameters::class.asTypeName().nullable() },
            desc = { "parameters to pass to the link" }
        )
    )
)

/**
 * [AjaxFallbackLink] config def.
 */
val ajaxFallbackLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxFallbackLink::class),
    parent = abstractLinkConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxFallbackLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName().copy(nullable = true)),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "click handler lambda" }
        ),
        PropInfo(
            name = "updateAjaxAttrs",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxFallbackLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestAttributes::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "updates the ajax attributes" }
        )
    )
)

/**
 * [IndicatingAjaxFallbackLink] config def.
 */
val indicatingAjaxFallbackLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = IndicatingAjaxFallbackLink::class),
    parent = abstractLinkConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxFallbackLink::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName().copy(nullable = true)),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "click handler lambda" }
        )
    )
)

/**
 * [AjaxSubmitLink] config def.
 */
val ajaxSubmitLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxSubmitLink::class),
    parent = componentConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxSubmitLink::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "submit handler lambda" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver = AjaxSubmitLink::class.asTypeName(),
                    parameters = * arrayOf(AjaxRequestTarget::class.asTypeName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "error handler lambda" }
        ),
        PropInfo(
            name = "form",
            type = { Form::class.asTypeName().parameterizedBy(STAR).copy(nullable = true) },
            desc = { "form the link submits" }
        )
    )
)

/**
 * [Check] config def.
 */
val checkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Check::class),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "group",
            type = { CheckGroup::class.modelParam(it) },
            desc = { "check group the check is associated with" }
        )
    )
)

// FIXME: to make this work
// 1) extend from component not formcomponent
// 2) need a way to say: generic in T, but the model is Collection<T>
// currently, it generates T : Collection<T>
// FIXME: one more field in ModelInfo -- something like another type -- Unbounded but modeled?
// or something like: generic restriction -- and have it usually default to target
/**
 * [CheckGroup] config def.
 */
val checkGroupConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = CheckGroup::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = {
            Collection::class.asClassName().parameterizedBy(if (isModelParameterNamed) modelTypeName else STAR)
        },
        genericType = {
            /*this.modelTypeName*/ /*if (isModelParameterNamed) modelTypeName else*/ /*if (type == GeneratorType.IncludeMethod) modelTypeName else*/ Any::class.asTypeName()
            .nullable()
        }),
    //parent = formComponentConfig,
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "span")
)

/**
 * [DropDownChoice] config def.
 */
val dropDownChoiceConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = DropDownChoice::class),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "choices",
            type = {
                IModel::class.asClassName()
                    .parameterizedBy(
                        WildcardTypeName.producerOf(
                            List::class.asTypeName()
                                .parameterizedBy(if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR)
                        )
                    ).nullable()
            },
            desc = { "choices for the drop down" }
        ),
        PropInfo(
            name = "choiceRenderer",
            type = {
                IChoiceRenderer::class.asClassName().parameterizedBy(
                    WildcardTypeName.consumerOf(
                        if (it.isModelParameterNamed)
                            it.generatorInfo.toModelTypeVarName()
                        else Any::class.asTypeName().nullable()
                    )
                ).nullable()
            },
            desc = { "how to render the drop down choices" }
        )
    )
)

/**
 * [FeedbackPanel] config def.
 */
val feedbackPanelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = FeedbackPanel::class),
    modelInfo = ModelInfo(type = TargetType.Unbounded),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "filter",
            type = { IFeedbackMessageFilter::class.asClassName().copy(nullable = true) },
            desc = { "filter for the messages to be displayed in the feedback panel" }
        )
    )
)

/**
 * [FileUploadField] config def.
 */
val fileUploadFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = FileUploadField::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = { mutableListTypeName.parameterizedBy(FileUpload::class.asTypeName()) }
    ),
    parent = formComponentConfig
)

/**
 * Abstract [Image] config def.
 */
val abstractImageConfig = ConfigInfo(
    basename = "AbstractImage",
    componentInfo = ComponentInfo(target = Image::class),
    parent = componentConfig,
    isConfigOnly = true,
    props = listOf(
        PropInfo(
            name = "resRef",
            type = { ResourceReference::class.asTypeName().copy(nullable = true) },
            desc = { "resource reference of the image" }
        ),
        PropInfo(
            name = "resParams",
            type = { PageParameters::class.asTypeName().copy(nullable = true) },
            desc = { "parameters to add to the image link" }
        ),
        PropInfo(
            name = "resRefs",
            type = {
                List::class.asTypeName().parameterizedBy(ResourceReference::class.asTypeName()).copy(nullable = true)
            },
            desc = { "resource references of the image" }
        ),
        PropInfo(
            name = "imageResource",
            type = { IResource::class.asTypeName().copy(nullable = true) },
            desc = { "resource of the image" }
        ),
        PropInfo(
            name = "imageResources",
            type = {
                List::class.asTypeName().parameterizedBy(IResource::class.asTypeName()).copy(nullable = true)
            },
            desc = { "resources of the image" }
        ),
        PropInfo(
            name = "xValues",
            type = { List::class.asTypeName().parameterizedBy(String::class.asTypeName()).copy(nullable = true) },
            desc = { "x values for image" }
        ),
        PropInfo(
            name = "sizes",
            type = { List::class.asTypeName().parameterizedBy(String::class.asTypeName()).copy(nullable = true) },
            desc = { "sizes of the image" }
        )
    )
)

/**
 * [Image] config def.
 */
val imageConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Image::class),
    parent = abstractImageConfig
)

/**
 * [Source] config def.
 */
val sourceConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Source::class),
    parent = abstractImageConfig,
    props = listOf(
        PropInfo(
            name = "media",
            type = { String::class.asTypeName().copy(nullable = true) },
            desc = { "media type of the image" }
        ),
        PropInfo(
            name = "crossOrigin",
            type = { Image.Cors::class.asTypeName().copy(nullable = true) },
            desc = { "CORS type of the image" }
        )
    )
)

/**
 * [InlineImage] config def.
 */
val inlineImageConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = InlineImage::class),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "resRef",
            type = { PackageResourceReference::class.asTypeName().copy(nullable = true) },
            desc = { "resource reference of the image" }
        )
    )
)

/**
 * [ListView] config def.
 */
val listViewConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = ListView::class),
    modelInfo = ModelInfo(type = TargetType.Unbounded, nullable = true, target = {
        List::class.asTypeName().parameterizedBy(if (isModelParameterNamed) modelTypeName else STAR)
    },
        genericType = { Any::class.asTypeName().nullable() }),
    parent = componentConfig,
    isConfigOnly = false,
    props = listOf(
        PropInfo(
            name = "populateItem",
            type = {
                LambdaTypeName.get(
                    receiver = ListItem::class.asTypeName()
                        .parameterizedBy(if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            },
            desc = { "how to populate every iteration" }
        )
    )
)

/**
 * [LocalDateTextField] config def.
 */
val localDateTextFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = LocalDateTextField::class),
    modelInfo = ModelInfo(
        target = { LocalDate::class.asClassName().nullable() },
        type = TargetType.Exact
    ),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "formatPattern",
            type = { nullableStringTypeName },
            desc = { "how to format the date" }
        ),
        PropInfo(
            name = "parsePattern",
            type = { nullableStringTypeName },
            desc = { "how to parse the date" }
        ),
        PropInfo(
            name = "dateStyle",
            type = { FormatStyle::class.asTypeName().copy(nullable = true) },
            desc = { "how to parse the date" }
        )
    )
)

/**
 * [LocalDateTimeField] config def.
 */
val localDateTimeFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = LocalDateTimeField::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { LocalDateTime::class.asClassName().nullable() }),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "toLocalDate",
            type = {
                LambdaTypeName.get(
                    returnType = LocalDate::class.asTypeName(),
                    parameters = *arrayOf(LocalDateTime::class.asTypeName())
                ).nullable()
            },
            desc = { "how to extract a LocalDate object from a LocalDateTime object" }
        ),
        PropInfo(
            name = "toLocalTime",
            type = {
                LambdaTypeName.get(
                    returnType = LocalTime::class.asTypeName(),
                    parameters = *arrayOf(LocalDateTime::class.asTypeName())
                ).nullable()
            },
            desc = { "how to extract a LocalDate object from a LocalDateTime object" }
        ),
        PropInfo(
            name = "defaultTime",
            type = { LambdaTypeName.get(returnType = LocalTime::class.asTypeName()).nullable() },
            desc = { "how to create a default LocalTime object" }
        )
    )
)

/**
 * [LocalDateTimeTextField] config def.
 */
val localDateTimeTextFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = LocalDateTimeTextField::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { LocalDateTime::class.asClassName().nullable() }),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "dateTimePattern",
            type = { nullableStringTypeName },
            desc = { "how to parse the datetime" }
        ),
        PropInfo(
            name = "dateStyle",
            type = { FormatStyle::class.asTypeName().copy(nullable = true) },
            desc = { "how to format the date portion of the datetime" }
        ),
        PropInfo(
            name = "timeStyle",
            type = { FormatStyle::class.asTypeName().copy(nullable = true) },
            desc = { "how to format the time portion of the datetime" }
        )
    )
)

/**
 * [MediaComponent] config def.
 */
val mediaComponentConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = MediaComponent::class),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "resRef",
            type = { ResourceReference::class.asTypeName().nullable() },
            desc = { "reference to the media resource" }
        ),
        PropInfo(
            name = "url",
            type = { nullableStringTypeName },
            desc = { "url to the media" }
        ),
        PropInfo(
            name = "pageParams",
            type = { PageParameters::class.asTypeName().copy(nullable = true) },
            desc = { "parameters for the page" }
        ),
        PropInfo(
            name = "isMuted",
            type = { nullableBooleanTypeName },
            desc = { "whether the media is muted" }
        ),
        PropInfo(
            name = "hasControls",
            type = { nullableBooleanTypeName },
            desc = { "whether the media has controls" }
        ),
        PropInfo(
            name = "preload",
            type = { MediaComponent.Preload::class.asTypeName().nullable() },
            desc = { "how the media is preloaded" }
        ),
        PropInfo(
            name = "isAutoPlay",
            type = { nullableBooleanTypeName },
            desc = { "whether the media will start automatically" }
        ),
        PropInfo(
            name = "isLooping",
            type = { nullableBooleanTypeName },
            desc = { "whether the media should start over when it finishes" }
        ),
        PropInfo(
            name = "startTime",
            type = { nullableStringTypeName },
            desc = { "where in the media to start playing" }
        ),
        PropInfo(
            name = "endTime",
            type = { nullableStringTypeName },
            desc = { "where in the media to stop playing" }
        ),
        PropInfo(
            name = "mediaGroup",
            type = { nullableStringTypeName },
            desc = { "name of the group the media is part of" }
        ),
        PropInfo(
            name = "crossOrigin",
            type = { MediaComponent.Cors::class.asTypeName().nullable() },
            desc = { "CORS type" }
        ),
        PropInfo(
            name = "type",
            type = { nullableStringTypeName },
            desc = { "type of the media" }
        )
    )
)

/**
 * [Audio] config def.
 */
val audioConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Audio::class),
    parent = mediaComponentConfig,
    tagInfo = TagInfo(tagName = "audio")
)

/**
 * [Video] config def.
 */
val videoConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Video::class),
    parent = mediaComponentConfig,
    tagInfo = TagInfo(tagName = "video"),
    props = listOf(
        PropInfo(
            name = "width",
            type = { Int::class.asTypeName().nullable() },
            desc = { "width of the video playback" }
        ),
        PropInfo(
            name = "height",
            type = { Int::class.asTypeName().nullable() },
            desc = { "width of the video playback" }
        ),
        PropInfo(
            name = "poster",
            type = { ResourceReference::class.asTypeName().nullable() },
            desc = { "reference to the resource for an image representing the video" }
        )
    )
)

/**
 * [Picture] config def.
 */
val pictureConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Picture::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "picture")
)

/**
 * [RadioChoice] config def.
 */
val radioChoiceConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = RadioChoice::class),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "choices",
            type = {
                IModel::class.asClassName()
                    .parameterizedBy(
                        WildcardTypeName.producerOf(
                            List::class.asTypeName()
                                .parameterizedBy(if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR)
                        )
                    )
            },
            desc = { "radio choice options" }
        ),
        PropInfo(
            name = "choiceRenderer",
            type = {
                IChoiceRenderer::class.asClassName().parameterizedBy(
                    WildcardTypeName.consumerOf(
                        if (it.isModelParameterNamed)
                            it.generatorInfo.toModelTypeVarName()
                        else Any::class.asTypeName().nullable()
                    )
                )
            },
            desc = { "how to render the radio choices" }
        )
    )
)

/**
 * [Radio] config def.
 */
val radioConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Radio::class),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "label",
            type = { nullableStringModelTypeName },
            desc = { "label for the radio button" }
        ),
        PropInfo(
            name = "group",
            type = {
                RadioGroup::class.asTypeName()
                    .parameterizedBy(if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR)
                    .nullable()
            },
            desc = { "radio group the radio button belongs to" }
        )
    ),
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "radio"))
)

/**
 * [RadioGroup] config def.
 */
val radioGroupConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = RadioGroup::class),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "span")
)

/**
 * [RepeatingView] config def.
 */
val repeatingViewConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = RepeatingView::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "div")
)

/**
 * [Select] config def.
 */
val selectConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Select::class),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "select")
)

/**
 * [StatelessLink] config def.
 */
val statelessLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = StatelessLink::class),
    isConfigOnly = false,
    parent = abstractLinkConfig,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = StatelessLink::class.asTypeName().parameterizedBy(if (it.isModelParameterNamed) it.generatorInfo.toModelTypeVarName() else STAR),
                    returnType = Unit::class.asTypeName()
                ).nullable()
            },
            desc = { "link click handler lambda" }
        )
    )
)

/**
 * [SubmitLink] config def.
 */
val submitLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = SubmitLink::class),
    isConfigOnly = false,
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "form",
            type = { Form::class.asTypeName().parameterizedBy(STAR) },
            desc = { "form the link is submitting" }
        ),
        PropInfo(
            name = "onSubmit",
            type = {
                LambdaTypeName.get(
                    receiver = SubmitLink::class.asTypeName(),
                    returnType = Unit::class.asTypeName()
                )
            },
            desc = { "lambda called when the form is submitted without errors" }
        ),
        PropInfo(
            name = "onError",
            type = {
                LambdaTypeName.get(
                    receiver = SubmitLink::class.asTypeName(),
                    returnType = Unit::class.asTypeName()
                )
            },
            desc = { "lambda called when the form has validation errors" }
        )
    )
)

/**
 * [TimeField] config def.
 */
val timeFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = TimeField::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { LocalTime::class.asClassName().nullable() }),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "use12HourFormat",
            type = { nullableBooleanTypeName },
            desc = { "whether the time is displayed and parsed in a 12-hour format" }
        )
    )
)

/**
 * [ZonedDateTimeField] config def.
 */
val zonedDateTimeFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = ZonedDateTimeField::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = { ZonedDateTime::class.asClassName().nullable() }),
    parent = formComponentConfig,
    props = listOf(
        PropInfo(
            name = "toLocalDate",
            type = {
                LambdaTypeName.get(
                    returnType = LocalDate::class.asTypeName(),
                    parameters = *arrayOf(ZonedDateTime::class.asTypeName())
                ).nullable()
            },
            desc = { "how to extract a LocalDate object from a LocalDateTime object" }
        ),
        PropInfo(
            name = "toLocalTime",
            type = {
                LambdaTypeName.get(
                    returnType = LocalTime::class.asTypeName(),
                    parameters = *arrayOf(ZonedDateTime::class.asTypeName())
                ).nullable()
            },
            desc = { "how to extract a LocalDate object from a LocalDateTime object" }
        ),
        PropInfo(
            name = "defaultTime",
            type = { LambdaTypeName.get(returnType = LocalTime::class.asTypeName()).nullable() },
            desc = { "how to create a default LocalTime object" }
        )
    )
)

/**
 * [List] of all of the component config defs.
 */
val allComponents = listOf(
    componentConfig,
    ajaxTabbedPanelConfig,
    labelConfig,
    multiLineLabelConfig,
    debugBarConfig,
    webMarkupContainerConfig,
    abstractButtonConfig,
    buttonConfig,
    ajaxButtonConfig,
    indicatingAjaxButtonConfig,
    ajaxFallbackButtonConfig,
    formComponentConfig,
    textAreaConfig,
    textFieldConfig,
    passwordTextFieldConfig,
//    requiredTextFieldConfig,
    autoCompleteTextFieldConfig,
    ajaxLinkConfig,
    indicatingAjaxLinkConfig,
    checkBoxConfig,
    ajaxCheckBoxConfig,
    abstractLinkConfig,
    externalLinkConfig,
    linkConfig,
    bookmarkablePageLinkConfig,
    ajaxFallbackLinkConfig,
    indicatingAjaxFallbackLinkConfig,
    ajaxSubmitLinkConfig,
    checkConfig,
    dropDownChoiceConfig,
    feedbackPanelConfig,
    abstractImageConfig,
    imageConfig,
    sourceConfig,
    inlineImageConfig,
    //listViewConfig,
    localDateTextFieldConfig,
    localDateTimeTextFieldConfig,
    localDateTimeFieldConfig,
    pictureConfig,
    radioChoiceConfig,
    radioConfig,
    radioGroupConfig,
    selectConfig,
    statelessLinkConfig,
    submitLinkConfig,
    abstractFormConfig,
    formConfig,
    statelessFormConfig,
    timeFieldConfig,
    zonedDateTimeFieldConfig,
    mediaComponentConfig,
    audioConfig,
    videoConfig,
    fileUploadFieldConfig,
    checkGroupConfig,
    repeatingViewConfig
)
