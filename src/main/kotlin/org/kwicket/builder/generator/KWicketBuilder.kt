package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.html.HTMLTag
import kotlinx.html.HtmlBlockTag
import org.apache.wicket.MarkupContainer
import java.io.File

/**
 * Code generator for Wicket component functionality.
 *
 * @property generatorInfo naming information
 * @property builder container into which the code will be generated
 */
class KWicketBuilder(val generatorInfo: GeneratorInfo, val builder: FileSpec.Builder) {

    /**
     * Generates a configuration interface.
     *
     * @receiver the [ConfigInfo] to convert into a configuration interface
     */
    fun ConfigInfo.toConfigInterface() {
        val typeContext = toTypeContext(generatorType = GeneratorType.ConfigInterface,  isModelParameterNamed = true)
        builder.addType(
            TypeSpec.interfaceBuilder(toConfigInterfaceName()).apply {
                addKdoc("${configInterfaceKdoc(generatorInfo)}\n\n")
                if (isConfigOnly) addTypeVar(builder = this, type = ParamType.Component, typeContext = typeContext)
                if (modelInfo.isUseTypeVar) addTypeVar(builder = this, type = ParamType.Model, typeContext = typeContext)
                props.forEach {
                    addProp(
                        propInfo = it,
                        builder = this,
                        superClassProp = false,
                        generatorType = GeneratorType.ConfigInterface,
                        isModelParameterNamed = true
                    )
                }
                parent?.let {
                    addSuperinterface(
                        it.toConfigInterfaceName().parameterizedBy(
                            toSuperInterfaceComponentParameter(),
                            toSuperInterfaceModelParameter(typeContext)
                        )
                    )
                }
            }.build()
        )
    }

    /**
     * Generates a configuration class.
     *
     * @receiver the [ConfigInfo] to convert into a configuration class
     */
    fun ConfigInfo.toConfigClass() {
        builder.addType(
            TypeSpec.classBuilder(toConfigClassName()).apply {
                addKdoc("${configClassKdoc(generatorInfo)}\n\n")
                val typeContext = toTypeContext(generatorType = GeneratorType.ConfigClass,  isModelParameterNamed = true)
                if (isConfigOnly) {
                    addModifiers(KModifier.OPEN)
                    addTypeVar(builder = this, type = ParamType.Component, typeContext = typeContext)
                }
                if (modelInfo.isUseTypeVar) addTypeVar(this, ParamType.Model, typeContext)
                primaryConstructor(toConfigClassConstructor())
                addSuperinterface(
                    toConfigInterfaceName().parameterizedBy(
                        if (isConfigOnly) toSuperInterfaceComponentParameter() else null,
                        toClassModelTypeVarName(isModelParameterNamed = true)
                    )
                )
                props.forEach {
                    addProp(
                        propInfo = it,
                        builder = this,
                        superClassProp = true,
                        generatorType = GeneratorType.ConfigClass,
                        isModelParameterNamed = !modelInfo.isExactlyOneType
                    )
                }
                addConfigClassSuperClass(builder = this, typeContext = typeContext)
            }.build()
        )
    }

    /**
     * Generates an HTML tag class.
     *
     * @receiver the [ConfigInfo] to convert into an HTML tag class
     */
    fun ConfigInfo.toTagClass() {
        val typeContext = toTypeContext(generatorType = GeneratorType.TagClass,  isModelParameterNamed = true)
        builder.addType(
            TypeSpec.classBuilder(toTagClassName()).apply {
                addKdoc("${tagClassKdoc(generatorInfo)}\n\n")
                if (modelInfo.isUseTypeVar) addTypeVar(builder = this, type = ParamType.Model, typeContext = typeContext)
                addSuperinterface(HtmlBlockTag::class.asTypeName())
                addSuperinterface(
                    superinterface = toConfigInterfaceName()
                        .parameterizedBy(toClassModelTypeVarName(isModelParameterNamed = true)),
                    delegate = CodeBlock.of(tagBuilderParamName)
                )
                superclass(toTagSuperClass(typeContext))
                primaryConstructor(FunSpec.constructorBuilder().apply {
                    tagClassParams.forEach {
                        addSuperclassConstructorParameter("${it.name} = ${it.name}")
                        addParam(
                            propInfo = it, kdoc = KdocOption.Constructor, configInfo = this@toTagClass,
                            generatorType = GeneratorType.TagClass
                        )
                    }
                }.build())
            }.build()
        )
    }

    /**
     * Generates an HTML tag method.
     *
     * @receiver the [ConfigInfo] to convert into an HTML tag method
     */
    fun ConfigInfo.toTagMethod(isModelParameterNamed: Boolean = true) {
        val typeContext = toTypeContext(generatorType = GeneratorType.TagMethod,  isModelParameterNamed = isModelParameterNamed)
        builder.addFunction(
            FunSpec.builder(generatorInfo.tagMethod.toName(this)).apply {
                addKdoc("${tagMethodKdoc(generatorInfo)}\n\n")
                if (modelInfo.isUseTypeVar && isModelParameterNamed)
                    // FIXME: this should use the generic type if it exists
                    addTypeVar(
                        builder = this,
                        type = ParamType.Model,
                        typeContext = toTypeContext(GeneratorType.TagMethod, isModelParameterNamed)
                    )
                receiver(HTMLTag::class.asTypeName())
                val props = allProps.filter { isModelParameterNamed || it.name != "model" }
                (props + listOf(
                    idPropInfo(isNullable = true),
                    tagNamePropInfo,
                    initialAttrsPropInfo,
                    tagBlockPropInfo
                )).forEach {
                    addParam(
                        it,
                        kdoc = KdocOption.Method,
                        configInfo = this@toTagMethod,
                        isModelParameterNamed = isModelParameterNamed,
                        generatorType = GeneratorType.TagMethod
                    )
                }
                addCode(
                    CodeBlock.of(
                        """%T(id = id, tagName = tagName, initialAttributes = initialAttributes, consumer = consumer, config = %T(%L)).%T(%L)""",
                        toTagClassName(), toConfigClassNameFromTagMethod(typeContext),
                        props.joinToString(", ") { "${it.name} = ${it.name}" }, visitMethod, blockParmName
                    )
                )
            }.build()
        )
    }

    /**
     * Generates an include method.
     *
     * @receiver the [ConfigInfo] to convert into an include method
     */
    fun ConfigInfo.toIncludeMethod(isModelParameterNamed: Boolean = true) {
        val typeContext = toTypeContext(GeneratorType.IncludeMethod, isModelParameterNamed)
        builder.addFunction(
            FunSpec.builder(generatorInfo.includeMethod.toName(this)).apply {
                addKdoc("${includeMethodKdoc(generatorInfo)}\n\n")
                if (isConfigOnly) addTypeVar(builder = this, type = ParamType.Component, typeContext = typeContext)
                if (modelInfo.isUseTypeVar && isModelParameterNamed)
                    addTypeVar(builder = this, type = ParamType.Model, typeContext = typeContext)
                receiver(MarkupContainer::class)
                returns(targetWithGeneric(isModelParameterNamed)) // FIXME: this should be parameterized with the generic type var if there is generic
                addParam(
                    idPropInfo(),
                    kdoc = KdocOption.Method,
                    configInfo = this@toIncludeMethod,
                    generatorType = GeneratorType.IncludeMethod
                )
                val props = allProps.filter { isModelParameterNamed || it.name != "model" }
                props.forEach {
                    addParam(
                        it,
                        kdoc = KdocOption.Method,
                        configInfo = this@toIncludeMethod,
                        generatorType = GeneratorType.IncludeMethod,
                        isModelParameterNamed = isModelParameterNamed
                    )
                }
                addParam(
                    includeBlockPropInfo,
                    kdoc = KdocOption.Method,
                    configInfo = this@toIncludeMethod,
                    generatorType = GeneratorType.IncludeMethod,
                    isModelParameterNamed = isModelParameterNamed
                )
                addCode(
                    CodeBlock.of(
                        """return %T(id = id, block = block, factory = { cid, config -> config.%T(cid) }, config = %L)""",
                        toClassName(generatorInfo.includeFactory), toClassName(generatorInfo.factoryMethod),
                        CodeBlock.of(
                            """%T(${props.joinToString(", ") { "${it.name} = ${it.name}" }})""",
                            toInvokeConfigClassName(typeContext)
                        )
                    )
                )
            }.build()
        )
    }

    /**
     * Writes the generated code to the [dir].
     *
     * @param dir directory in which to write the code
     */
    fun write(dir: File) = builder.build().writeTo(dir)

    /**
     * Writes the code to the [out].
     *
     * @param out [Appendable] to write the code to
     */
    fun write(out: Appendable) = builder.build().writeTo(out)


    private fun toTypeContext(generatorType: GeneratorType, isModelParameterNamed: Boolean) =
        TypeContext(
            modelTypeName = generatorInfo.toModelTypeVarName(),
            type = generatorType,
            isModelParameterNamed = isModelParameterNamed,
            generatorInfo = generatorInfo
        )

    // FIXME: I would like to delete this method
    private fun ConfigInfo.toModelTypeVarName(isModelParameterNamed: Boolean) =
        if (modelInfo.type == TargetType.Unbounded || modelInfo.genericType != null) // FIXME: this is a hack
            if (isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR
        else null

    private fun ConfigInfo.toClassModelTypeVarName(isModelParameterNamed: Boolean) =
        if (modelInfo.isUseTypeVar)
            if (isModelParameterNamed) generatorInfo.toModelTypeVarName() else STAR
        else null

    private fun ConfigInfo.targetWithGeneric(isModelParameterNamed: Boolean) =
        componentInfo.target.asTypeName().parameterizedBy(
            if (componentInfo.isTargetParameterizedByModel) toModelTypeVarName(isModelParameterNamed)
            else null
        )

    private fun ConfigInfo.toConfigInterfaceName() = toClassName(generatorInfo.configInterface)
    private fun ConfigInfo.toConfigClassName() = toClassName(generatorInfo.configClass)
    private fun ConfigInfo.toConfigClassNameFromTagMethod(typeContext: TypeContext) =
        toClassName(generatorInfo.configClass)
            .parameterizedBy(if (typeContext.isModelParameterNamed) null
            else if (modelInfo.genericType != null) (modelInfo.genericType)(typeContext) else this.modelInfo.target.invoke(typeContext))

    private fun ConfigInfo.toInvokeConfigClassName(typeContext: TypeContext) =
        toClassName(generatorInfo.configClass)
            .parameterizedBy(
                if (typeContext.isModelParameterNamed || (parent?.componentInfo?.isTargetParameterizedByModel == true))
                    null
                else if (modelInfo.type == TargetType.Exact && modelInfo.nullable && modelInfo.genericType == null)
                    modelInfo.target.invoke(typeContext)
                else null
            )

    private fun ConfigInfo.toTagClassName() = toClassName(generatorInfo.tagClass)

    private fun ConfigInfo.toComponentTypeVarName() =
        TypeVariableName(
            generatorInfo.componentParam.name, componentInfo.target.asClassName()
                .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) generatorInfo.toModelTypeVarName() else null)
        )

    private val ConfigInfo.configInterfaceTypeName: TypeName
        get() = toConfigInterfaceName().parameterizedBy(if (modelInfo.isUseTypeVar) generatorInfo.toModelTypeVarName() else null)

    private fun ConfigInfo.toTagSuperClass(typeContext: TypeContext) = toClassName(generatorInfo.baseTagClass)
        .parameterizedBy(
            toSuperInterfaceModelParameter(typeContext) ?: modelInfo.target.invoke(typeContext),
            toSuperInterfaceComponentParameter(),
            configInterfaceTypeName
        )

    private fun ConfigInfo.addConfigClassSuperClass(builder: TypeSpec.Builder, typeContext: TypeContext) {
        if (parent != null) {
            builder.superclass(
                parent.toConfigClassName().parameterizedBy(
                    toSuperInterfaceComponentParameter(),
                    toSuperInterfaceModelParameter(typeContext)
                )
            ).apply {
                allParentProps.forEach { addSuperclassConstructorParameter("${it.name} = ${it.name}") }
            }
        }
    }

    private fun FunSpec.Builder.addParam(
        propInfo: PropInfo, kdoc: KdocOption, configInfo: ConfigInfo,
        isModelParameterNamed: Boolean = true, generatorType: GeneratorType
    ): FunSpec.Builder {
        val type = propInfo.type(
            configInfo, toTypeContext(generatorType = generatorType, isModelParameterNamed = isModelParameterNamed)
        )
        return addParameter(
            ParameterSpec.builder(propInfo.name, type).also {
                propInfo.default?.let { defaultLambda ->
                    defaultLambda.invoke(configInfo, type)?.let { defaultValue ->
                        it.defaultValue(defaultValue)
                    }
                }
                if (kdoc.add) it.addKdoc("${propInfo.desc.invoke(propInfo)}${kdoc.eol}")
            }.build()
        )
    }

    private fun ConfigInfo.toConfigClassConstructor() = FunSpec.constructorBuilder().apply {
        allParentProps.map { Triple(it, KdocOption.Constructor, this@toConfigClassConstructor) }
            .plus(props.map { Triple(it, KdocOption.Constructor, this@toConfigClassConstructor) })
            .forEach { (propInfo, kdoc, configInfo) ->
                addParam(
                    propInfo = propInfo,
                    kdoc = kdoc,
                    configInfo = configInfo,
                    generatorType = GeneratorType.ConfigClass
                )
            }
    }.build()

    private fun ConfigInfo.addProp(
        propInfo: PropInfo,
        builder: TypeSpec.Builder,
        superClassProp: Boolean,
        generatorType: GeneratorType,
        isModelParameterNamed: Boolean
    ) = builder.addProperty(
        PropertySpec.builder(
            name = propInfo.name,
            type = propInfo.type(
                this,
                toTypeContext(generatorType = generatorType, isModelParameterNamed = isModelParameterNamed)
            ),
            modifiers = *listOfNotNull(if (superClassProp) KModifier.OVERRIDE else null).toTypedArray()
        )
            .mutable(propInfo.mutable)
            .apply { if (superClassProp) initializer(propInfo.name) }
            .build()
    )
        .addKdoc("@property ${propInfo.name} ${propInfo.desc.invoke(propInfo)}\n")

    private fun ConfigInfo.toSuperInterfaceComponentParameter() =
        if (isConfigOnly) generatorInfo.toComponentTypeVarName()
        else componentInfo.target.asClassName()
            .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) generatorInfo.toModelTypeVarName() else null)

    private fun ConfigInfo.toSuperInterfaceModelParameter(typeContext: TypeContext) = when {
        modelInfo.genericType != null -> modelInfo.target.invoke(typeContext)
        modelInfo.isUseTypeVar -> generatorInfo.toModelTypeVarName()
        parent?.modelInfo?.type == TargetType.Unbounded -> modelInfo.target.invoke(typeContext)
        else -> null
    }

    private enum class ParamType {
        Component,
        Model
    }

    private fun ConfigInfo.addTypeVar(builder: TypeSpec.Builder, type: ParamType, typeContext: TypeContext) = when (type) {
        ParamType.Component -> builder.addTypeVariable(
            generatorInfo.toComponentTypeVarName()
                .copy(
                    bounds = listOf(
                        componentInfo.target.asTypeName().parameterizedBy(
                            if (componentInfo.isTargetParameterizedByModel) WildcardTypeName.consumerOf(generatorInfo.toModelTypeVarName())
                            else null
                        )
                    )
                )
        )
            .addKdoc(generatorInfo.componentParam.toKdocValue())
        ParamType.Model -> builder.addTypeVariable(
            generatorInfo.toModelTypeVarName()
                .copy(bounds = listOf(/*modelInfo.target.invoke(typeContext))*/ modelInfo.derivedGenericType(typeContext)))
        )
            .addKdoc(generatorInfo.modelParam.toKdocValue())
    }

    private fun ConfigInfo.addTypeVar(builder: FunSpec.Builder, type: ParamType, typeContext: TypeContext) =
        when (type) {
            ParamType.Component -> builder.addTypeVariable(
                /*
                builder.addTypeVariable(generatorInfo.toComponentTypeVarName())
                .addKdoc(generatorInfo.componentParam.toKdocValue())
                 */
                generatorInfo.toComponentTypeVarName()
                    .copy(
                        bounds = listOf(
                            componentInfo.target.asTypeName().parameterizedBy(
                                if (componentInfo.isTargetParameterizedByModel) WildcardTypeName.consumerOf(generatorInfo.toModelTypeVarName())
                                else null
                            )
                        )
                    ))
            ParamType.Model -> builder.addTypeVariable(
                generatorInfo.toModelTypeVarName()
                    .copy(bounds = listOf(/*modelInfo.target.invoke(typeContext)*/ modelInfo.derivedGenericType(typeContext)))
            )
                .addKdoc(generatorInfo.modelParam.toKdocValue())
        }

}
