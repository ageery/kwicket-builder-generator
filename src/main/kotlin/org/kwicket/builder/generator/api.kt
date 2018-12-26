package org.kwicket.builder.generator

import com.squareup.kotlinpoet.*
import java.io.File
import kotlin.reflect.KClass

enum class KdocOption(val add: Boolean, val eol: String) {
    None(false, ""),
    Method(true, ""),
    Constructor(true, "\n")
}

class ClassInfo(val toPackage: ConfigInfo.() -> String, val toName: ConfigInfo.() -> String)

fun ConfigInfo.toClassName(classInfo: ClassInfo) = ClassName(classInfo.toPackage(this), classInfo.toName(this))

class GeneratorInfo(
    val configInterface: ClassInfo,
    val configClass: ClassInfo,
    val factoryMethod: ClassInfo,
    val includeMethod: ClassInfo,
    val tagClass: ClassInfo,
    val tagMethod: ClassInfo,
    val componentParameterName: String = "C",
    val modelParameterName: String = "T"
)

val ConfigInfo.allParentProps: List<PropInfo>
    get() = parent?.let { it.allParentProps + it.props } ?: emptyList()

fun ConfigInfo.toConfigInterfaceName(generatorInfo: GeneratorInfo) = toClassName(generatorInfo.configInterface)
fun ConfigInfo.toConfigClassName(generatorInfo: GeneratorInfo) = toClassName(generatorInfo.configClass)
fun ConfigInfo.toFactoryMethodName(generatorInfo: GeneratorInfo) = toClassName(generatorInfo.factoryMethod)
fun ConfigInfo.toIncludeMethodName(generatorInfo: GeneratorInfo) = toClassName(generatorInfo.includeMethod)
fun ConfigInfo.toTagClassName(generatorInfo: GeneratorInfo) = toClassName(generatorInfo.tagClass)
fun ConfigInfo.toTagMethodName(generatorInfo: GeneratorInfo) = toClassName(generatorInfo.tagMethod)
fun ConfigInfo.toModelTypeVarName(generatorInfo: GeneratorInfo) = TypeVariableName(generatorInfo.modelParameterName)
fun ConfigInfo.toComponentTypeVarName(generatorInfo: GeneratorInfo) =
    TypeVariableName(
        generatorInfo.componentParameterName, componentInfo.target.asClassName()
            .parameterizedBy(if (componentInfo.isTargetParameterizedByModel) generatorInfo.toModelTypeVarName() else null)
    )

val ConfigInfo.allProps: List<PropInfo>
    get() = props + allParentProps

fun GeneratorInfo.toModelTypeVarName() = TypeVariableName(modelParameterName)
fun GeneratorInfo.toComponentTypeVarName() = TypeVariableName(componentParameterName)

enum class TargetType {
    Exact,
    Unbounded
}

class ModelInfo(
    val type: TargetType = TargetType.Unbounded,
    val target: KClass<*> = Any::class,
    val nullable: Boolean = true,
    val kdoc: String? = null
)

class ComponentInfo(
    val target: KClass<*>,
    val isTargetParameterizedByModel: Boolean = !target.typeParameters.isNullOrEmpty(),
    val kdoc: String? = null
)

fun ModelInfo.toParamTypeName(modelParamTypeName: ParameterizedTypeName) = when (type) {
    TargetType.Unbounded -> modelParamTypeName
    TargetType.Exact -> target.asClassName()
}

//fun ComponentInfo.toCompParamTypeName(modelParamTypeName: ParameterizedTypeName) = when (isTargetParameterizedByModel) {
//    true -> target.asClassName().parameterizedBy(modelParamTypeName)
//    false -> target.asClassName()
//}

data class PropInfo(
    val name: String,
    val type: ConfigInfo.(TypeName) -> TypeName,
    val mutable: Boolean = true,
    val default: CodeBlock? = CodeBlock.of(nullDefault),
    val desc: PropInfo.() -> String = { "info about $name" }
)

class TagInfo(
    val defaultTagName: String = "div",
    val defaultAttrs: Map<String, String> = emptyMap()
)

class ConfigInfo(
    val componentInfo: ComponentInfo,
    val modelInfo: ModelInfo = ModelInfo(),
    val isConfigOnly: Boolean = componentInfo.target.isAbstract,
    val basename: String = componentInfo.target.java.simpleName,
    val parent: ConfigInfo? = null,
    val props: List<PropInfo> = emptyList(),
    val tagInfo: TagInfo = TagInfo(),
    val configInterfaceKdoc: (GeneratorInfo) -> String = {
        "Configuration for creating a [${componentInfo.target.simpleName}] component."
    },
    val configClassKdoc: (GeneratorInfo) -> String = {
        "Implementation of [${componentInfo.target.simpleName}]."
    }
)

class KWicketBuilder(val generatorInfo: GeneratorInfo, val builder: FileSpec.Builder) {

    fun ConfigInfo.toBuilderInterface() = builder.addType(
        TypeSpec.interfaceBuilder(toConfigInterfaceName(generatorInfo)).apply {
            addKdoc("${configInterfaceKdoc.invoke(generatorInfo)}\n\n")
            if (isConfigOnly) addTypeVariable(toComponentTypeVarName(generatorInfo))
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(toModelTypeVarName(generatorInfo))
            props.forEach { addProp(propInfo = it, configInfo = this@toBuilderInterface, superClassProp = false) }
            parent?.let {
                addSuperinterface(
                    it.toConfigInterfaceName(generatorInfo)
                        .parameterizedBy(toSuperInterfaceComponentParameter(), toSuperInterfaceModelParameter())
                )
            }
        }.build()
    )

    fun ConfigInfo.toBuilderClass() = builder.addType(
        TypeSpec.classBuilder(toConfigClassName(generatorInfo)).apply {
            if (isConfigOnly) {
                addModifiers(KModifier.OPEN)
                addTypeVariable(toComponentTypeVarName(generatorInfo))
            }
            addKdoc("${configClassKdoc.invoke(generatorInfo)}\n\n")
            primaryConstructor(toConstructor())
            addSuperinterface(
                toConfigInterfaceName(generatorInfo).parameterizedBy(
                    if (isConfigOnly) toSuperInterfaceComponentParameter() else null,
                    if (modelInfo.type == TargetType.Unbounded) toSuperInterfaceModelParameter() else null
                )
            )
            if (modelInfo.type == TargetType.Unbounded) addTypeVariable(toModelTypeVarName(generatorInfo))
            props.forEach { addProp(propInfo = it, configInfo = this@toBuilderClass, superClassProp = true) }
            addSuperClass(this)
        }.build()
    )

    fun write(dir: File) = builder.build().writeTo(dir)
    fun write(out: Appendable) = builder.build().writeTo(out)

    private fun ConfigInfo.addSuperClass(builder: TypeSpec.Builder) {
        if (parent != null) {
            builder.superclass(
                parent.toConfigClassName(generatorInfo).parameterizedBy(
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
                if (kdoc.add) it.addKdoc("${propInfo.desc}${kdoc.eol}")
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

    private fun TypeSpec.Builder.addProp(propInfo: PropInfo, configInfo: ConfigInfo, superClassProp: Boolean) =
        addProperty(propInfo.toPropertySpec(configInfo, superClassProp = superClassProp))
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
