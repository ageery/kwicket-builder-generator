package org.kwicket.builder.generator

import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

/**
 * Configuration information about a component for the purposes of generating code.
 *
 * @property componentInfo information about the type of component
 * @property modelInfo information about the model of the component
 * @property isConfigOnly whether the configuration corresponds to a single, concrete component
 * @property basename name which will form the basis for the various pieces of generated code
 * @property parent config info this config info is based on
 * @property props properties of the configuration
 * @property tagInfo default tag name and attributes for the tag portion of the generation
 * @property configInterfaceKdoc lambda for generating a kdoc for the configuration interface
 * @property configClassKdoc lambda for generating a kdoc for the configuration class
 * @property tagClassKdoc lambda for generating a kdoc for the tag class
 * @property tagMethodKdoc lambda for generating a kdoc for the tag method
 * @property includeMethodKdoc lambda for generating a kdoc for the include method
 */
class ConfigInfo(
    val componentInfo: ComponentInfo,
    val modelInfo: ModelInfo = ModelInfo(),
    val isConfigOnly: Boolean = componentInfo.target.isAbstract,
    val basename: String = componentInfo.target.java.simpleName,
    val parent: ConfigInfo? = null,
    val props: List<PropInfo> = emptyList(),
    val tagInfo: TagInfo? = null,
    val configInterfaceKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Configuration for creating a [${componentInfo.target.simpleName}] component."
    },
    val configClassKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Implementation of [${it.configInterface.toName.invoke(this)}]."
    },
    val tagClassKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "HTML tag class corresponding to the [${componentInfo.target.simpleName}] component."
    },
    val tagMethodKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Adds the [${componentInfo.target.simpleName}] component to the HTML."
    },
    val includeMethodKdoc: ConfigInfo.(GeneratorInfo) -> String = {
        "Creates and includes the [${componentInfo.target.simpleName}] component in a [MarkupContainer]."
    }
)

/**
 * Default tag name for the config.
 *
 * If no tag name is defined for the config, finds the last tag name defined in an ancestor, falling back to "div".
 */
val ConfigInfo.defaultTagName: String
     get() = tagInfo?.tagName ?: parent?.defaultTagName ?: "div"

/**
 * Default tag attributes for the config.
 *
 * If no tag attributes are defined for the config, finds the last tag attributes defined in an ancestor, falling back
 * to an empty map.
 */
val ConfigInfo.defaultTagAttrs: Map<String, String>
    get() = tagInfo?.attrs ?: parent?.defaultTagAttrs ?: emptyMap()

/**
 * All of the ancestor properties for the config.
 */
val ConfigInfo.allParentProps: List<PropInfo>
    get() = parent?.let { it.allParentProps + it.props } ?: emptyList()

/**
 * All of the properties, starting with the properties directly defined for the config and including all of the
 * ancestor properties.
 */
val ConfigInfo.allProps: List<PropInfo>
    get() = props + allParentProps

/**
 * Creates a [ClassName] object from a [ClassInfo] object.
 *
 * @receiver configuration information
 * @param classInfo class info to convert to a [ClassName]
 * @return [ClassName] corresponding to the [ClassInfo]
 */
fun ConfigInfo.toClassName(classInfo: ClassInfo) = ClassName(classInfo.toPackage(this), classInfo.toName(this))

/**
 * Information about the component the configuration is for.
 *
 * @property target class of the component
 * @property isTargetParameterizedByModel whether the target component is parameterized by the model
 */
class ComponentInfo(
    val target: KClass<*>,
    val isTargetParameterizedByModel: Boolean = !target.typeParameters.isNullOrEmpty()
)

/**
 * Information about the model of the component.
 *
 * @property type the type of the model -- unbounded or restricted
 * @property target class of the model
 * @property nullable whether the object of the model is nullable
 */
class ModelInfo(
    val type: TargetType = TargetType.Unbounded,
    val target: KClass<*> = Any::class,
    val nullable: Boolean = true
)

/**
 * Whether the [ModelInfo] uses the type variable.
 */
val ModelInfo.isUseTypeVar: Boolean
    get() = type == TargetType.Unbounded || nullable

/**
 * Whether the [ModelInfo] specifies exactly one type.
 */
val ModelInfo.isExactlyOneType: Boolean
    get() = type == TargetType.Exact && (!nullable)