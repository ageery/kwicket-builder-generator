package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.html.HTMLTag
import kotlinx.html.HtmlBlockTag
import org.apache.wicket.MarkupContainer
import java.io.File

// FIXME: add kdocs for type parameters

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
        builder.addType(
            TypeSpec.interfaceBuilder(toConfigInterfaceName()).apply {
                addKdoc("${configInterfaceKdoc(generatorInfo)}\n\n")
                if (isConfigOnly) addTypeVar(ParamType.Component)
                if (modelInfo.type == TargetType.Unbounded) addTypeVar(ParamType.Model)
                props.forEach {
                    addProp(
                        propInfo = it,
                        builder = this,
                        superClassProp = false,
                        generatorType = GeneratorType.ConfigInterface
                    )
                }
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
                if (isConfigOnly) {
                    addModifiers(KModifier.OPEN)
                    addTypeVar(ParamType.Component)
                }
                if (modelInfo.type == TargetType.Unbounded) addTypeVar(ParamType.Model)
                primaryConstructor(toConfigClassConstructor())
                addSuperinterface(
                    toConfigInterfaceName().parameterizedBy(
                        if (isConfigOnly) toSuperInterfaceComponentParameter() else null,
                        toModelTypeVarName()
                    )
                )
                props.forEach {
                    addProp(
                        propInfo = it,
                        builder = this,
                        superClassProp = true,
                        generatorType = GeneratorType.ConfigClass
                    )
                }
                addConfigClassSuperClass(this)
            }.build()
        )
    }

    /**
     * Generates an HTML tag class.
     *
     * @receiver the [ConfigInfo] to convert into an HTML tag class
     */
    fun ConfigInfo.toTagClass() {
        builder.addType(
            TypeSpec.classBuilder(toTagClassName()).apply {
                addKdoc("${tagClassKdoc(generatorInfo)}\n\n")
                if (modelInfo.type == TargetType.Unbounded) addTypeVar(ParamType.Model)
                addSuperinterface(HtmlBlockTag::class.asTypeName())
                addSuperinterface(
                    superinterface = toConfigInterfaceName().parameterizedBy(toModelTypeVarName()),
                    delegate = CodeBlock.of(tagBuilderParamName)
                )
                superclass(toTagSuperClass())
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

    // FIXME: does the model parameter have to be non-null for the parameterized version?
    /**
     * Generates an HTML tag method.
     *
     * @receiver the [ConfigInfo] to convert into an HTML tag method
     */
    fun ConfigInfo.toTagMethod(isModelParameterNamed: Boolean = true) {
        builder.addFunction(
            FunSpec.builder(generatorInfo.tagMethod.toName(this)).apply {
                addKdoc("${tagMethodKdoc(generatorInfo)}\n\n")
                if (modelInfo.type == TargetType.Unbounded && isModelParameterNamed) addTypeVar(ParamType.Model)
                receiver(HTMLTag::class.asTypeName())
                val props = allProps.filter { isModelParameterNamed || it.name != "model" }
                (props + listOf(
                    idPropInfo(isNullable = true),
                    tagNamePropInfo,
                    initialAttrsPropInfo,
                    blockPropInfo
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
                        toTagClassName(), toConfigClassName(),
                        allProps.joinToString(", ") { "${it.name} = ${it.name}" }, visitMethod, blockParmName
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
    fun ConfigInfo.toIncludeMethod(parameterized: Boolean = true) {
        builder.addFunction(
            FunSpec.builder(generatorInfo.includeMethod.toName(this)).apply {
                addKdoc("${includeMethodKdoc(generatorInfo)}\n\n")
                if (modelInfo.type == TargetType.Unbounded) addTypeVar(ParamType.Model)
                receiver(MarkupContainer::class)
                returns(targetWithGeneric())
                addParam(
                    idPropInfo(),
                    kdoc = KdocOption.Method,
                    configInfo = this@toIncludeMethod,
                    generatorType = GeneratorType.IncludeMethod
                )
                allProps.forEach {
                    addParam(
                        it,
                        kdoc = KdocOption.Method,
                        configInfo = this@toIncludeMethod,
                        generatorType = GeneratorType.IncludeMethod
                    )
                }
                addParam(
                    blockPropInfo,
                    kdoc = KdocOption.Method,
                    configInfo = this@toIncludeMethod,
                    generatorType = GeneratorType.IncludeMethod
                )
                addCode(
                    CodeBlock.of(
                        """return %T(id = id, block = block, factory = { cid, config -> config.%T(cid) }, config = %L)""",
                        qMethodTypeName,
                        toClassName(generatorInfo.factoryMethod),
                        CodeBlock.of(
                            """%T(${allProps.joinToString(", ") { "${it.name} = ${it.name}" }})""", toConfigClassName()
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

    private fun ConfigInfo.toModelTypeVarName() =
        if (modelInfo.type == TargetType.Unbounded) generatorInfo.toModelTypeVarName() else null

    private fun ConfigInfo.targetWithGeneric() =
        componentInfo.target.asTypeName().parameterizedBy(toModelTypeVarName())

    private fun ConfigInfo.toConfigInterfaceName() = toClassName(generatorInfo.configInterface)
    private fun ConfigInfo.toConfigClassName() = toClassName(generatorInfo.configClass)
    private fun ConfigInfo.toTagClassName() = toClassName(generatorInfo.tagClass)

    private fun ConfigInfo.toComponentTypeVarName() =
        TypeVariableName(
            generatorInfo.componentParam.name, componentInfo.target.asClassName()
                .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) generatorInfo.toModelTypeVarName() else null)
        )

    private fun ConfigInfo.toTagSuperClass() = configurableComponentTagTypeName
        .parameterizedBy(
            toSuperInterfaceModelParameter() ?: modelInfo.target.asClassName(),
            toSuperInterfaceComponentParameter(),
            toConfigInterfaceName().parameterizedBy(toSuperInterfaceModelParameter())
        )

    private fun ConfigInfo.addConfigClassSuperClass(builder: TypeSpec.Builder) {
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
                propInfo.default?.let { defaultLambda ->
                    defaultLambda.invoke(configInfo)?.let { defaultValue ->
                        it.defaultValue(defaultValue)
                    }
                }
                if (kdoc.add) it.addKdoc("${propInfo.desc.invoke(propInfo)}${kdoc.eol}")
            }.build()
        )

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

    private fun PropInfo.toPropertySpec(configInfo: ConfigInfo, superClassProp: Boolean, generatorType: GeneratorType) =
        PropertySpec.builder(
            name = name,
            type = type(
                configInfo, TypeContext(
                    modelTypeName = generatorInfo.toModelTypeVarName(),
                    type = generatorType, isModelParameterNamed = false, generatorInfo = generatorInfo
                )
            ),
            modifiers = *listOfNotNull(if (superClassProp) KModifier.OVERRIDE else null).toTypedArray()
        )
            .mutable(mutable)
            .apply { if (superClassProp) initializer(name) }
            .build()

    private fun ConfigInfo.addProp(
        propInfo: PropInfo,
        builder: TypeSpec.Builder,
        superClassProp: Boolean,
        generatorType: GeneratorType
    ) =
        builder.addProperty(
            propInfo.toPropertySpec(
                this, superClassProp = superClassProp,
                generatorType = generatorType
            )
        )
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

    private enum class ParamType {
        Component,
        Model
    }

    private fun TypeSpec.Builder.addTypeVar(type: ParamType) = when(type) {
        ParamType.Component -> addTypeVariable(generatorInfo.toComponentTypeVarName())
            .addKdoc(generatorInfo.componentParam.toKdocValue())
        ParamType.Model -> addTypeVariable(generatorInfo.toModelTypeVarName())
            .addKdoc(generatorInfo.modelParam.toKdocValue())
    }

    private fun FunSpec.Builder.addTypeVar(type: ParamType) = when(type) {
        ParamType.Component -> addTypeVariable(generatorInfo.toComponentTypeVarName())
            .addKdoc(generatorInfo.componentParam.toKdocValue())
        ParamType.Model -> addTypeVariable(generatorInfo.toModelTypeVarName())
            .addKdoc(generatorInfo.modelParam.toKdocValue())
    }

}
