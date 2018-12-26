package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.html.HtmlBlockTag
import kotlinx.html.TagConsumer
import java.io.File


class KWicketBuilder(val generatorInfo: GeneratorInfo, val builder: FileSpec.Builder) {

    fun ConfigInfo.toConfigInterface() = builder.addType(
        TypeSpec.interfaceBuilder(toConfigInterfaceName()).apply {
            addKdoc("${configInterfaceKdoc.invoke(generatorInfo)}\n\n")
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
            if (isConfigOnly) {
                addModifiers(KModifier.OPEN)
                addTypeVariable(toComponentTypeVarName())
            }
            addKdoc("${configClassKdoc.invoke(generatorInfo)}\n\n")
            primaryConstructor(toConstructor())
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
                    addParam(propInfo = it, kdoc = KdocOption.Constructor, configInfo = this@toTagClass)
                }
            }.build())
        }.build()
    )

    fun ConfigInfo.toTagMethod(parameterized: Boolean = true) = builder.addFunction(
        FunSpec.builder(generatorInfo.tagMethod.toName(this)).apply {
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(generatorInfo.toModelTypeVarName())
            receiver(htmlTagTypeName)
            (allProps + listOf(
                idProp(isNullable = true),
                toTagNamePropInfo(),
                toInitialParamsPropInfo(),
                toTagBlockPropInfo()
            )).forEach {
                addParam(
                    it,
                    kdoc = KdocOption.Method,
                    configInfo = this@toTagMethod
                )
            }
            addCode(toTagMethodBody(propInfoList = allProps))
        }.build()
    )

    fun write(dir: File) = builder.build().writeTo(dir)
    fun write(out: Appendable) = builder.build().writeTo(out)

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
            propInfoList.joinToString(separator = ", ") { "${it.name} = ${it.name}" }, visitMethod, blockParmName
        )

    private fun ConfigInfo.toTagBlockPropInfo() = PropInfo(
        name = "block",
        default = emptyLambda,
        type = {
            LambdaTypeName.get(
                returnType = Unit::class.asTypeName(),
                receiver = toTagClassName().parameterizedBy(toModelTypeVarName())
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
        default = CodeBlock.of(""""${tagInfo.defaultTagName}""""), mutable = false, desc = { "Name of the HTML tag" }
    )

    private fun toInitialParamsPropInfo() = PropInfo(
        name = "initialAttributes", type = { stringMapTypeName },
        default = CodeBlock.of("emptyMap()"), mutable = false, desc = { "Tag attributes" }
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

    private fun FunSpec.Builder.addParam(propInfo: PropInfo, kdoc: KdocOption, configInfo: ConfigInfo) =
        addParameter(
            ParameterSpec.builder(propInfo.name, propInfo.type(configInfo, generatorInfo.toModelTypeVarName())).also {
                propInfo.default?.let { defaultValue -> it.defaultValue(defaultValue) }
                if (kdoc.add) it.addKdoc("${propInfo.desc.invoke(propInfo)}${kdoc.eol}")
            }.build()
        )

    private fun ConfigInfo.toConstructor() = FunSpec.constructorBuilder().apply {
        allParentProps.map { Triple(it, KdocOption.Constructor, this@toConstructor) }
            .plus(props.map { Triple(it, KdocOption.Constructor, this@toConstructor) })
            .forEach { (propInfo, kdoc, configInfo) ->
                addParam(propInfo = propInfo, kdoc = kdoc, configInfo = configInfo)
            }
    }.build()

    private fun PropInfo.toPropertySpec(configInfo: ConfigInfo, superClassProp: Boolean) = PropertySpec.builder(
        name = name,
        type = type(configInfo, generatorInfo.toModelTypeVarName()),
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
