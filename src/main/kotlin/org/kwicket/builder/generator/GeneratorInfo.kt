package org.kwicket.builder.generator

import com.squareup.kotlinpoet.TypeVariableName

/**
 * Specifies how to derive the names of classes and packages from a [ConfigInfo] object for the various generated
 * pieces of code.
 *
 * @property configInterface how to derive the config interface package and class name from a [ConfigInfo] object
 * @property configClass how to derive the config class package and class name from a [ConfigInfo] object
 * @property factoryMethod how to derive the factory package and method name from a [ConfigInfo] object
 * @property includeMethod how to derive the include package and method name from a [ConfigInfo] object
 * @property tagClass how to derive the tag class package and class name from a [ConfigInfo] object
 * @property tagMethod how to derive the tag package and method name from a [ConfigInfo] object
 * @property componentParam parameter info for the component parameter
 * @property modelParam parameter info for the model parameter
 * @property baseTagClass how to derive the class kWicket tags are supposed to extend from
 * @property includeFactory how to derive the method used to include a component in a parent
 */
class GeneratorInfo(
    val configInterface: ClassInfo,
    val configClass: ClassInfo,
    val factoryMethod: ClassInfo,
    val includeMethod: ClassInfo,
    val tagClass: ClassInfo,
    val tagMethod: ClassInfo,
    val baseTagClass: ClassInfo,
    val includeFactory: ClassInfo,
    val componentParam: ParamInfo = ParamInfo(name = "C", kdoc = "type of the Wicket component"),
    val modelParam: ParamInfo = ParamInfo(name = "T", kdoc = "type of the component model")
)

/**
 * Returns a [TypeVariableName] for the model.
 *
 * @receiver [GeneratorInfo] to use for determining the model parameter name
 * @return a [TypeVariableName] for the model
 */
fun GeneratorInfo.toModelTypeVarName() = TypeVariableName(modelParam.name)

/**
 * Returns a [TypeVariableName] for the component.
 *
 * @receiver [GeneratorInfo] to use for determining the component parameter name
 * @return a [TypeVariableName] for the component
 */
fun GeneratorInfo.toComponentTypeVarName() = TypeVariableName(componentParam.name)

/**
 * Specifies how to derive a package and class from a [ConfigInfo] object.
 *
 * @property toPackage lambda for generating a package name from a [ConfigInfo] object
 * @property toName lambda for generating a class name from a [ConfigInfo] object
 */
class ClassInfo(val toPackage: ConfigInfo.() -> String, val toName: ConfigInfo.() -> String)

/**
 * Specifies the name and kdoc for a type of parameter.
 *
 * @property name name of the parameter
 * @property kdoc KDoc for the parameter
 */
class ParamInfo(val name: String, val kdoc: String)

/**
 * Returns a kdoc line for the [ParamInfo].
 *
 * @receiver [ParamInfo] to create a kdoc for
 * @return kdoc line for the [ParamInfo]
 */
fun ParamInfo.toKdocValue() = "@param $name $kdoc\n"