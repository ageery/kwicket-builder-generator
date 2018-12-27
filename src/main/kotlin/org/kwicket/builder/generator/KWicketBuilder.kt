package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.html.HTMLTag
import kotlinx.html.HtmlBlockTag
import kotlinx.html.TagConsumer
import org.apache.wicket.MarkupContainer
import java.io.File

// FIXME: we want to be able to create both parameterized and unparameterized versions of the include and tag methods
// FIXME: add kdocs for type parameters
// FIXME: onConfig is missing generic parameter for TextField

class KWicketBuilder(val generatorInfo: GeneratorInfo, val builder: FileSpec.Builder) {

    fun ConfigInfo.toConfigInterface() = builder.addType(
        TypeSpec.interfaceBuilder(toConfigInterfaceName()).apply {
            addKdoc("${configInterfaceKdoc(generatorInfo)}\n\n")
            if (isConfigOnly) addTypeVariable(toComponentTypeVarName())
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(generatorInfo.toModelTypeVarName())
            props.forEach { addProp(propInfo = it, builder = this, superClassProp = false) }
            parent?.let {
                addSuperinterface(
                    it.toConfigInterfaceName().parameterizedBy(
                        toSuperInterfaceComponentParameter(),
                        toSuperInterfaceModelParameter()
                    )
                )
            }
        }.build()
    )

    fun ConfigInfo.toConfigClass() = builder.addType(
        TypeSpec.classBuilder(toConfigClassName()).apply {
            addKdoc("${configClassKdoc(generatorInfo)}\n\n")
            if (isConfigOnly) {
                addModifiers(KModifier.OPEN)
                addTypeVariable(toComponentTypeVarName())
            }
            primaryConstructor(toConfigClassConstructor())
            addSuperinterface(
                toConfigInterfaceName().parameterizedBy(
                    if (isConfigOnly) toSuperInterfaceComponentParameter() else null,
                    toModelTypeVarName()
                )
            )
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(generatorInfo.toModelTypeVarName())
            props.forEach { addProp(propInfo = it, builder = this, superClassProp = true) }
            addSuperClass(this)
        }.build()
    )

    fun ConfigInfo.toTagClass() = builder.addType(
        TypeSpec.classBuilder(toTagClassName()).apply {
            addKdoc("${tagClassKdoc(generatorInfo)}\n\n")
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(generatorInfo.toModelTypeVarName())
            addSuperinterface(HtmlBlockTag::class.asTypeName())
            addSuperinterface(
                superinterface = toConfigInterfaceName().parameterizedBy(toModelTypeVarName()),
                delegate = CodeBlock.of(tagBuilderParamName)
            )
            superclass(toTagSuperClass())
            primaryConstructor(FunSpec.constructorBuilder().apply {
                tagClassParams().forEach {
                    addSuperclassConstructorParameter("${it.name} = ${it.name}")
                    addParam(propInfo = it, kdoc = KdocOption.Constructor, configInfo = this@toTagClass,
                        generatorType = GeneratorType.TagClass)
                }
            }.build())
        }.build()
    )

    // FIXME: does the model parameter have to be non-null for the parameterized version?
    fun ConfigInfo.toTagMethod(isModelParameterNamed: Boolean = true) = builder.addFunction(
        FunSpec.builder(generatorInfo.tagMethod.toName(this)).apply {
            addKdoc("${tagMethodKdoc(generatorInfo)}\n\n")
            if (modelInfo.type == TargetType.Unbounded && isModelParameterNamed)
                addTypeVariable(generatorInfo.toModelTypeVarName()) // FIXME: pull this out and also add kdoc
            receiver(HTMLTag::class.asTypeName())
            val props = allProps.filter { isModelParameterNamed || it.name != "model" }
            (props + listOf(
                idProp(isNullable = true),
                toTagNamePropInfo(),
                toInitialParamsPropInfo(),
                toTagBlockPropInfo(isModelParameterNamed = isModelParameterNamed)
            )).forEach {
                addParam(
                    it,
                    kdoc = KdocOption.Method,
                    configInfo = this@toTagMethod,
                    isModelParameterNamed = isModelParameterNamed,
                    generatorType = GeneratorType.TagMethod
                )
            }
            addCode(toTagMethodBody(propInfoList = allProps))
        }.build()
    )

    fun ConfigInfo.toIncludeMethod(parameterized: Boolean = true) = builder.addFunction(
        FunSpec.builder(generatorInfo.includeMethod.toName(this)).apply {
            addKdoc("${includeMethodKdoc(generatorInfo)}\n\n")
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(generatorInfo.toModelTypeVarName())
            receiver(MarkupContainer::class)
            returns(targetWithGeneric())
            addParam(idProp(), kdoc = KdocOption.Method, configInfo = this@toIncludeMethod,
                generatorType = GeneratorType.IncludeMethod)
            allProps.forEach {
                addParam(
                    it,
                    kdoc = KdocOption.Method,
                    configInfo = this@toIncludeMethod,
                    generatorType = GeneratorType.IncludeMethod
                )
            }
            addParam(
                toIncludeBlockPropInfo(),
                kdoc = KdocOption.Method,
                configInfo = this@toIncludeMethod,
                generatorType = GeneratorType.IncludeMethod
            )
            addCode(toIncludeMethodBody(propInfoList = allProps))
        }.build()
    )

    fun write(dir: File) = builder.build().writeTo(dir)
    fun write(out: Appendable) = builder.build().writeTo(out)

    private fun ConfigInfo.toBuilderClassCreation(propInfoList: List<PropInfo>) =
        CodeBlock.of(
            """%T(${propInfoList.map { "${it.name} = ${it.name}" }.joinToString(", ")})""",
            toConfigClassName()
        )

    private fun ConfigInfo.toIncludeMethodBody(propInfoList: List<PropInfo>) = CodeBlock.of(
        """return %T(id = id, block = block, factory = { cid, config -> config.%T(cid) }, config = %L)""",
        qMethodTypeName, toClassName(generatorInfo.factoryMethod), toBuilderClassCreation(propInfoList)
    )

    private fun ConfigInfo.toIncludeBlockPropInfo() =
        PropInfo(
            name = "block", mutable = true, desc = { "optional block to execute to configure the component" },
            default = CodeBlock.of(nullDefault), type = {
                LambdaTypeName.get(
                    receiver = toConfigInterfaceName().parameterizedBy(toModelTypeVarName()),
                    returnType = Unit::class.asTypeName()
                ).copy(nullable = true)
            }
        )

    private fun ConfigInfo.toModelTypeVarName() =
        if (modelInfo.type == TargetType.Unbounded) generatorInfo.toModelTypeVarName() else null

    private fun ConfigInfo.targetWithGeneric() =
        componentInfo.target.asTypeName().parameterizedBy(toModelTypeVarName())

    private fun ConfigInfo.toConfigInterfaceName() = toClassName(generatorInfo.configInterface)
    private fun ConfigInfo.toConfigClassName() = toClassName(generatorInfo.configClass)
    private fun ConfigInfo.toTagClassName() = toClassName(generatorInfo.tagClass)
    private fun ConfigInfo.toComponentTypeVarName() =
        TypeVariableName(
            generatorInfo.componentParameterName, componentInfo.target.asClassName()
                .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) generatorInfo.toModelTypeVarName() else null)
        )

    private fun ConfigInfo.toTagMethodBody(propInfoList: List<PropInfo>): CodeBlock =
        CodeBlock.of(
            """%T(id = id, tagName = tagName, initialAttributes = initialAttributes, consumer = consumer, config = %T(%L)).%T(%L)""",
            toTagClassName(), toConfigClassName(),
            propInfoList.joinToString(", ") { "${it.name} = ${it.name}" }, visitMethod, blockParmName
        )

    private fun ConfigInfo.toTagBlockPropInfo(isModelParameterNamed: Boolean) = PropInfo(
        name = "block",
        default = emptyLambda,
        type = {
            LambdaTypeName.get(
                returnType = Unit::class.asTypeName(),
                receiver = toTagClassName().parameterizedBy(if (isModelParameterNamed) toModelTypeVarName() else STAR)
            )
        },
        desc = { "Tag configuration block" },
        mutable = false
    )

    private fun idProp(isNullable: Boolean = false) = PropInfo(
        name = "id", type = { nullableStringTypeName },
        default = if (isNullable) CodeBlock.of(nullDefault) else null, mutable = false, desc = { "Wicket component id" }
    )

    private fun ConfigInfo.toTagNamePropInfo() = PropInfo(
        name = "tagName", type = { stringTypeName },
        default = CodeBlock.of(""""$defaultTagName""""), mutable = false, desc = { "Name of the HTML tag" }
    )

    private fun ConfigInfo.toInitialParamsPropInfo() = PropInfo(
        name = "initialAttributes", type = { stringMapTypeName },
        default = defaultTagAttrs.toLiteralMapCodeBlock(), mutable = false, desc = { "Tag attributes" }
    )

    private val consumerPropInfo = PropInfo(
        name = "consumer", default = null, type = {
            TagConsumer::class.asTypeName()
                .parameterizedBy(ClassName("", "*"))
        }
    )

    private fun toConfigPropInfo() = PropInfo(
        name = "config", default = null, type = {
            toConfigInterfaceName().parameterizedBy(generatorInfo.toModelTypeVarName())
        }
    )

    private fun ConfigInfo.tagClassParams() = listOf(
        idProp(isNullable = true),
        toTagNamePropInfo(),
        toInitialParamsPropInfo(),
        consumerPropInfo,
        toConfigPropInfo(),
        toFactoryPropInfo()
    )

    private fun toFactoryPropInfo() = PropInfo(
        name = "factory",
        default = CodeBlock.of("{ cid, c -> c.%T(cid) }", factoryInvokeTypeName),
        type = {
            LambdaTypeName.get(
                returnType = targetWithGeneric(),
                parameters = *arrayOf(
                    stringTypeName,
                    toConfigInterfaceName().parameterizedBy(toModelTypeVarName())
                )
            )
        }
    )

    private fun ConfigInfo.toTagSuperClass() = configurableComponentTagTypeName
        .parameterizedBy(
            toSuperInterfaceModelParameter() ?: modelInfo.target.asClassName(),
            toSuperInterfaceComponentParameter(),
            toConfigInterfaceName().parameterizedBy(toSuperInterfaceModelParameter())
        )

    private fun ConfigInfo.addSuperClass(builder: TypeSpec.Builder) {
        if (parent != null) {
            builder.superclass(
                parent.toConfigClassName().parameterizedBy(
                    toSuperInterfaceComponentParameter(),
                    toSuperInterfaceModelParameter()
                )
            ).apply {
                allProps.forEach { addSuperclassConstructorParameter("${it.name} = ${it.name}") }
            }
        }
    }

    private fun FunSpec.Builder.addParam(
        propInfo: PropInfo, kdoc: KdocOption, configInfo: ConfigInfo,
        isModelParameterNamed: Boolean = true, generatorType: GeneratorType
    ) =
        addParameter(
            ParameterSpec.builder(
                propInfo.name, propInfo.type(
                    configInfo, TypeContext(
                        generatorInfo = generatorInfo,
                        isModelParameterNamed = isModelParameterNamed,
                        modelTypeName = if (isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR,
                        type = generatorType
                    )
                )
            ).also {
                propInfo.default?.let { defaultValue -> it.defaultValue(defaultValue) }
                if (kdoc.add) it.addKdoc("${propInfo.desc.invoke(propInfo)}${kdoc.eol}")
            }.build()
        )

    private fun ConfigInfo.toConfigClassConstructor() = FunSpec.constructorBuilder().apply {
        allParentProps.map { Triple(it, KdocOption.Constructor, this@toConfigClassConstructor) }
            .plus(props.map { Triple(it, KdocOption.Constructor, this@toConfigClassConstructor) })
            .forEach { (propInfo, kdoc, configInfo) ->
                addParam(propInfo = propInfo, kdoc = kdoc, configInfo = configInfo, generatorType = GeneratorType.ConfigClass)
            }
    }.build()

    // FIXME: TypeContext needs work
    private fun PropInfo.toPropertySpec(configInfo: ConfigInfo, superClassProp: Boolean) = PropertySpec.builder(
        name = name,
        type = type(
            configInfo, TypeContext(
                modelTypeName = generatorInfo.toModelTypeVarName(),
                type = GeneratorType.ConfigClass, isModelParameterNamed = false, generatorInfo = generatorInfo
            )
        ),
        modifiers = *listOfNotNull(if (superClassProp) KModifier.OVERRIDE else null).toTypedArray()
    )
        .mutable(mutable)
        .apply { if (superClassProp) initializer(name) }
        .build()

    private fun ConfigInfo.addProp(propInfo: PropInfo, builder: TypeSpec.Builder, superClassProp: Boolean) =
        builder.addProperty(propInfo.toPropertySpec(this, superClassProp = superClassProp))
            .addKdoc("@property ${propInfo.name} ${propInfo.desc.invoke(propInfo)}\n")

    private fun ConfigInfo.toSuperInterfaceComponentParameter() =
        if (isConfigOnly) generatorInfo.toComponentTypeVarName()
        else componentInfo.target.asClassName()
            .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) generatorInfo.toModelTypeVarName() else null)

    private fun ConfigInfo.toSuperInterfaceModelParameter() = when {
        modelInfo.type == TargetType.Unbounded -> generatorInfo.toModelTypeVarName()
        parent?.modelInfo?.type == TargetType.Unbounded -> modelInfo.target.asClassName()
        else -> null
    }

}
