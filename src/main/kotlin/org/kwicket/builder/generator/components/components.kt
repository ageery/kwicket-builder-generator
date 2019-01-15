package org.kwicket.builder.generator.components

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.apache.wicket.Component
import org.apache.wicket.Page
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink
import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink
import org.apache.wicket.behavior.Behavior
import org.apache.wicket.devutils.debugbar.DebugBar
import org.apache.wicket.extensions.markup.html.form.datetime.LocalDateTextField
import org.apache.wicket.extensions.markup.html.form.datetime.LocalDateTimeField
import org.apache.wicket.extensions.markup.html.form.datetime.LocalDateTimeTextField
import org.apache.wicket.extensions.markup.html.form.select.Select
import org.apache.wicket.feedback.IFeedbackMessageFilter
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.basic.MultiLineLabel
import org.apache.wicket.markup.html.form.*
import org.apache.wicket.markup.html.image.Image
import org.apache.wicket.markup.html.image.InlineImage
import org.apache.wicket.markup.html.image.Picture
import org.apache.wicket.markup.html.image.Source
import org.apache.wicket.markup.html.link.BookmarkablePageLink
import org.apache.wicket.markup.html.link.Link
import org.apache.wicket.markup.html.link.PopupSettings
import org.apache.wicket.markup.html.link.StatelessLink
import org.apache.wicket.markup.html.list.ListItem
import org.apache.wicket.markup.html.list.ListView
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.model.IModel
import org.apache.wicket.request.mapper.parameter.PageParameters
import org.apache.wicket.request.resource.IResource
import org.apache.wicket.request.resource.PackageResourceReference
import org.apache.wicket.request.resource.ResourceReference
import org.apache.wicket.validation.IValidator
import org.kwicket.builder.generator.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.FormatStyle
import kotlin.reflect.KClass

/**
 * kWicket generator information.
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

val componentConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Component::class),
    modelInfo = ModelInfo(),
    props = listOf(
        PropInfo(
            name = "model",
            type = {
                IModel::class.asTypeName()
                    .parameterizedBy(
                        if (modelInfo.isExactlyOneType)
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
            name = "validator",
            type = { IValidator::class.asTypeName()
                .parameterizedBy(
                    when {
                        modelInfo.isExactlyOneType -> modelInfo.target.asTypeName() // FIXME: I don't understand why this is necessary
                        it.isModelParameterNamed -> it.modelTypeName
                        modelInfo.type == TargetType.Exact -> modelInfo.target.asTypeName().copy(nullable = modelInfo.nullable)
                        it.modelTypeName == STAR -> Any::class.asTypeName().nullable()
                        else -> null
                    }
                ).nullable() },
            desc = { "validator for the form component" }
        ),
        PropInfo(
            name = "validators",
            type = { List::class.asTypeName().parameterizedBy(IValidator::class.asTypeName()
                .parameterizedBy(
                    when {
                        modelInfo.isExactlyOneType -> modelInfo.target.asTypeName() // FIXME: I don't understand why this is necessary
                        it.isModelParameterNamed -> it.modelTypeName
                        modelInfo.type == TargetType.Exact -> modelInfo.target.asTypeName().copy(nullable = modelInfo.nullable)
                        it.modelTypeName == STAR -> Any::class.asTypeName().nullable()
                        else -> null
                    }
                )).nullable() },
            desc = { "validator for the form component" }
        )
        /*
        IModel::class.asTypeName()
                    .parameterizedBy(
                        if (modelInfo.type == TargetType.Exact)
                            modelInfo.target.asTypeName()
                        else it.modelTypeName
         */
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

val ajaxFallbackButtonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxFallbackButton::class),
    modelInfo = ModelInfo(
        type = TargetType.Exact,
        target = String::class
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

val buttonConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Button::class),
    parent = abstractButtonConfig,
    modelInfo = ModelInfo(type = TargetType.Exact, target = String::class),
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

val checkBoxConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = CheckBox::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = Boolean::class, nullable = false),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "checkbox"))
)

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
        )
    ),
    tagInfo = TagInfo(tagName = "a")
)

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

val ajaxSubmitLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = AjaxSubmitLink::class),
    parent = abstractLinkConfig,
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

val dropDownConfig = ConfigInfo(
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
                    )
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
                )
            },
            desc = { "how to render the drop down choices" }
        )
    )
)

//val emailLinkConfig = ConfigInfo(
//    componentInfo = ComponentInfo(target = Emai::class),
//    parent = abstractLinkConfig,
//    props = listOf(
//        PropInfo(
//            name = "label",
//            type = { IModel::class.asTypeName().parameterizedBy(STAR).copy(nullable = true) },
//            desc = { "label for the link" }
//        )
//    )
//)

val feedbackPanelConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = FeedbackPanel::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = Unit::class, nullable = false),
    parent = componentConfig,
    props = listOf(
        PropInfo(
            name = "filter",
            type = { IFeedbackMessageFilter::class.asClassName().copy(nullable = true) },
            desc = { "filter for the messages to be displayed in the feedback panel" }
        )
    )
)

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

val imageConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Image::class),
    parent = abstractImageConfig
)

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

val listViewConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = ListView::class),
    parent = componentConfig,
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

val localDateTextFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = LocalDateTextField::class),
    modelInfo = ModelInfo(target = LocalDate::class, nullable = true, type = TargetType.Exact), // generate a T parameter if the target is nullable
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

val localDateTimeFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = LocalDateTimeField::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = LocalDateTime::class, nullable = true),
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

val localDateTimeTextFieldConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = LocalDateTimeTextField::class),
    modelInfo = ModelInfo(type = TargetType.Exact, target = LocalDateTime::class, nullable = true),
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

val pictureConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Picture::class),
    parent = componentConfig,
    tagInfo = TagInfo(tagName = "picture")
)

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
            type = { RadioGroup::class.asTypeName().parameterizedBy(it.modelTypeName).nullable() },
            desc = { "radio group the radio button belongs to" }
        )
    ),
    tagInfo = TagInfo(tagName = "input", attrs = mapOf("type" to "radio"))
)

val radioGroupConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = RadioGroup::class),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "span")
)

val selectConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = Select::class),
    parent = formComponentConfig,
    tagInfo = TagInfo(tagName = "select")
)

val statelessLinkConfig = ConfigInfo(
    componentInfo = ComponentInfo(target = StatelessLink::class),
    isConfigOnly = false,
    parent = abstractLinkConfig,
    props = listOf(
        PropInfo(
            name = "onClick",
            type = {
                LambdaTypeName.get(
                    receiver = StatelessLink::class.asTypeName().parameterizedBy(it.modelTypeName),
                    returnType = Unit::class.asTypeName()
                )
            },
            desc = { "link click handler lambda" }
        )
    )
)

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

val allComponents = listOf(
    componentConfig,
    labelConfig,
    multiLineLabelConfig,
    debugBarConfig,
    webMarkupContainerConfig,
    abstractButtonConfig,
    buttonConfig,
    ajaxFallbackButtonConfig,
    formComponentConfig,
    textFieldConfig,
    ajaxLinkConfig,
    checkBoxConfig,
    abstractLinkConfig,
    linkConfig,
    bookmarkablePageLinkConfig,
    ajaxFallbackLinkConfig,
    ajaxSubmitLinkConfig,
    checkConfig,
    dropDownConfig,
    //emailLinkConfig,
    feedbackPanelConfig,
    abstractImageConfig,
    imageConfig,
    sourceConfig,
    inlineImageConfig,
    listViewConfig,
    localDateTextFieldConfig,
    localDateTimeTextFieldConfig,
    localDateTimeFieldConfig,
    pictureConfig,
    radioChoiceConfig,
    radioConfig,
    radioGroupConfig,
    selectConfig,
    statelessLinkConfig,
    submitLinkConfig
)