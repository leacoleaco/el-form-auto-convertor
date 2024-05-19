package pro.leaco.autoform

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType
import kotlin.reflect.jvm.kotlinProperty

object AutoFormDescriptorConverter {

    val GSON: Gson = GsonBuilder()
        .registerTypeAdapter(AutoFormComponentType::class.java, VueFormTypeAdapter)
        .create()

    object VueFormTypeAdapter : TypeAdapter<AutoFormComponentType>() {
        override fun write(w: JsonWriter, data: AutoFormComponentType) {
            w.value(data.value)
        }

        override fun read(r: JsonReader): AutoFormComponentType {
            return AutoFormComponentType.parse(r.nextString())
        }
    }

    /**
     * convert json object to Data
     * @param DATA the data's type
     * @param dataClazz the data's class
     * @param config the data's refer json
     * @return data
     */
    fun <DATA> convert(dataClazz: Class<DATA>, config: JsonObject): DATA {
        val reflectInfos = readReflectInfo(dataClazz)
        return convert(dataClazz, config, reflectInfos)
    }

    /**
     * convert json object to Data
     * @param DATA the data's type
     * @param dataClazz the data's class
     * @param config the data's refer json
     * @param reflectInfoMap reflect info of DATA
     * @return data
     */
    fun <DATA> convert(dataClazz: Class<DATA>, config: JsonObject, reflectInfoMap: Map<String, ReflectInfo>): DATA {
        val data = dataClazz.constructors.first { it.parameterCount == 0 }.newInstance() as DATA
        reflectInfoMap.forEach { (fieldName, info) ->
            val value = convertToValueByDescriptor(config.get(fieldName), info)

            //反射注入值
            info.field.trySetAccessible()
            val isMarkedNullable = info.field.kotlinProperty?.returnType?.isMarkedNullable ?: false
            if (isMarkedNullable) {
                //属性可null,直接设置
                info.field.set(data, value)
            } else {
                //属性不可null, 非空才设置
                if (value != null) {
                    info.field.set(data, value)
                }
            }
        }
        return data
    }


    private fun convertToValueByDescriptor(
        ele: JsonElement?,
        reflectInfo: ReflectInfo
    ): Any? {
        val clazz = reflectInfo.field.type
        if (ele == null || ele is JsonNull) {
            return null
        }
        return GSON.fromJson(ele, clazz)
    }


    /**
     * read descriptor form a class
     *
     * @param DATA
     * @param dataClazz
     * @return
     */
    fun <DATA> readReflectInfo(dataClazz: Class<DATA>): Map<String, ReflectInfo> {
        if (dataClazz.isEnum) {
            return emptyMap()
        }

        if (dataClazz != List::class.java && dataClazz != Array::class.java) {
            check(dataClazz.constructors.any { it.parameterCount == 0 }) { "Class '$dataClazz' must have one constructor method with no parameters." }
        }
        val instance = dataClazz.constructors.first { it.parameterCount == 0 }.newInstance()
        val associate = dataClazz.declaredFields.map { field ->
            val descriptor = field.getAnnotation(FormDescriptor::class.java) ?: return@map null
            val defaultValue: Any? =
                if (field.trySetAccessible()) {
                    field.get(instance)
                } else {
                    null
                }
            field.name to ReflectInfo(descriptor, field, defaultValue)
        }.filterNotNull().toMap()
        return associate
    }

    /**
     * convert reflect info to descriptors
     *
     * @param reflectInfoMap
     * @param defaultValueReplaceMarkMap you can put some mark value here to replace the default value
     *          e.g.    we set  ("[animal]","dog"),   then if any default value contain "[animal]", it will be replaced by "dog"
     *          the defaultValue:"the [animal]" will be replaced by "the dog"
     * @return
     */
    fun convertToDescriptors(
        reflectInfoMap: Map<String, ReflectInfo>,
        defaultValueReplaceMarkMap: Map<String, String> = emptyMap(),
        optionSources: Map<String, () -> List<LabelValue>> = emptyMap(),
    ): Map<String, Any?> {
        return reflectInfoMap.asSequence()
            .map { it.key to convertToDescriptors(it.value, defaultValueReplaceMarkMap, optionSources) }
            .toMap()
    }

    fun readClazzDescriptors(dataClazz: Class<*>): Map<String, Any?> {
        return convertToDescriptors(readReflectInfo(dataClazz))
    }


    /**
     * convert reflect info to descriptors
     *
     * @param reflectInfo
     * @param defaultValueReplaceMarkMap you can put some mark value here to replace the default value
     *          e.g.    we set  ("[animal]","dog"),   then if any default value contain "[animal]", it will be replaced by "dog"
     *          the defaultValue:"the [animal]" will be replaced by "the dog"
     * @return
     */
    fun convertToDescriptors(
        reflectInfo: ReflectInfo,
        defaultValueReplaceMarkMap: Map<String, String> = emptyMap(),
        optionSources: Map<String, () -> List<LabelValue>> = emptyMap(),
    ): Map<String, Any?> {
        val descriptor = reflectInfo.descriptor

        val componentType =
            if (descriptor.componentType != AutoFormComponentType.AUTO) descriptor.componentType else AutoFormComponentType.tryDetectedComponentType(
                reflectInfo.field.type
            )

        val itemDescriptor = if (componentType == AutoFormComponentType.ARRAY) {
            val itemClazz = descriptor.itemsClass
            val itemReflectInfo = readReflectInfo(itemClazz.java)
            val itemDesc = convertToDescriptors(itemReflectInfo)
            mapOf(
                "type" to AutoFormComponentType.OBJECT.value,
                "fields" to itemDesc
            )
        } else null

        val wrapFieldDescriptor = if (componentType == AutoFormComponentType.WRAP) {
            val wrapClass = reflectInfo.field.type
            val itemReflectInfo = readReflectInfo(wrapClass)
            convertToDescriptors(itemReflectInfo)
        } else null

        var defaultValue =
            if (componentType !== AutoFormComponentType.WRAP)
                descriptor.defaultValue.ifEmpty { reflectInfo.defaultValue }
            else null

        if (defaultValue is String && defaultValueReplaceMarkMap.isNotEmpty()) {
            defaultValueReplaceMarkMap.forEach { (mark, value) ->
                defaultValue = (defaultValue as String).replace(mark, value)
            }
        }

        val props = buildProps(componentType, descriptor.props)

        val options = buildOptions(componentType, reflectInfo, optionSources)

        val isEnumMultiple =
            componentType == AutoFormComponentType.ENUM && (
                    reflectInfo.field.type.isArray || reflectInfo.field.type.isAssignableFrom(List::class.java))

        return mapOf(
            "type" to componentType.value,
            "label" to descriptor.label,
            "disabled" to if (descriptor.disabled) true else null,
            "defaultValue" to defaultValue,
            "rules" to descriptor.rules.map { r ->
                if (r.min >= 0f || r.max >= 0f) {
                    check(r.type != FormValidType.EMPTY) { "写了 min, max 就必须设置类型" }
                }
                if (r.whitespace) {
                    check(r.type != FormValidType.STRING) { "写了 whitespace 就必须设置类型为 STRING" }
                }

                arrayOf<Pair<String, Any?>>(
                    "type" to
                            if (r.type != FormValidType.EMPTY) r.type.value
                            else null,
                    "required" to if (r.required) true else null,
                    "message" to r.message.ifEmpty {
                        if (r.required) "该字段为必填项" else null
                    },
                    "min" to if (r.min >= 0) r.min else null,
                    "max" to if (r.max >= 0) r.max else null,
                    "len" to if (r.len >= 0) r.len else null,
                    "pattern" to r.pattern.ifEmpty { null },
                    "enum" to if (r.enum.isNotEmpty()) r.enum else null,
                    "whitespace" to if (r.whitespace) true else null,
                ).filter { it.second != null }.toMap()
            }.ifEmpty { null },
            "props" to props,
            "options" to options,
            "multiple" to isEnumMultiple,
            "dependOnProp" to descriptor.dependOnProp.ifBlank { null },
            "dependOnPropRevert" to if (descriptor.dependOnPropRevert) "1" else null,
            "slotName" to descriptor.slotName.ifBlank { null },
            "tooltip" to descriptor.tooltip.let {
                if (it.content.isBlank()) return@let null
                return@let mapOf(
                    "content" to it.content,
                    "placement" to it.placement,
                    "effect" to it.effect,
                    "offset" to it.offset,
                    "transition" to it.transition,
                    "showAfter" to it.showAfter,
                    "showArrow" to it.showArrow,
                    "hideAfter" to it.hideAfter,
                    "autoClose" to it.autoClose,
                    "trigger" to it.trigger,
                )
            },
            "alert" to descriptor.alert.let {
                if (it.message.isBlank() && it.messageSlot.isBlank()) return@let null
                return@let mapOf(
                    "message" to it.message.ifBlank { null },
                    "messageSlot" to it.messageSlot.ifBlank { null },
                    "props" to applyProps(it.props, mutableMapOf<String, Any>()),
                    "type" to it.type,
                    "showIcon" to it.showIcon,
                    "closable" to it.closable,
                    "center" to it.center,
                    "effect" to it.effect,
                    "title" to it.title,
                    "description" to it.description,
                    "closeText" to it.closeText,
                )
            },
            "enumSourceKey" to descriptor.enumSourceKey.ifBlank { null },
            "enumComponent" to descriptor.enumComponent.value.ifBlank { null },
            "labelPosition" to descriptor.labelPosition.value.ifBlank { null },
            "itemDescriptor" to itemDescriptor?.ifEmpty { null },
            "fields" to wrapFieldDescriptor?.ifEmpty { null },
        )
    }

    private fun buildOptions(
        componentType: AutoFormComponentType,
        reflectInfo: ReflectInfo,
        optionSources: Map<String, () -> List<LabelValue>>
    ): List<Map<String, Any?>>? {

        // key-value pair
        val r = mutableMapOf<String, Any?>()

        if (componentType == AutoFormComponentType.ENUM) {
            //auto get enum options
            val field = reflectInfo.field
            val clazz = field.type
            if (clazz.isEnum) {
                clazz.enumConstants?.forEach {
                    val enumName = it.toString()
                    val f = clazz.getField(enumName)
                    val l = f.getAnnotation(FormDescriptor.OptionLabel::class.java)
                    r[l?.label ?: enumName] = enumName
                }
            } else if (clazz.isArray || clazz.isAssignableFrom(List::class.java)) {
                val genericType = field.genericType
                if (genericType is ParameterizedType) {
                    val actualTypeArguments = genericType.actualTypeArguments
                    if (actualTypeArguments.isNotEmpty() && actualTypeArguments[0] is Class<*>) {
                        val aClass = actualTypeArguments[0] as Class<*>
                        if (aClass.isEnum) {
                            aClass.enumConstants?.forEach {
                                val enumName = it.toString()
                                val f = aClass.getField(enumName)
                                val l = f.getAnnotation(FormDescriptor.OptionLabel::class.java)
                                r[l?.label ?: enumName] = enumName
                            }
                        }
                    }
                }
            }
        }

        val descriptor = reflectInfo.descriptor
        //重写或者补充选项
        val customOpts = (if (descriptor.optionSource.isNotBlank())
            optionSources[descriptor.optionSource]?.invoke()?.map { p -> p.label to p.value }
        else descriptor.options.map { p -> p.label to p.value }.ifEmpty { null })

        customOpts?.forEach {
            r[it.first] = it.second
        }

        return r.map { (k, v) ->
            mapOf(
                "label" to k,
                "value" to v,
            )
        }
    }

    private fun buildProps(
        componentType: AutoFormComponentType,
        properties: Array<FormDescriptor.Property>
    ): MutableMap<String, Any> {
        val props = mutableMapOf<String, Any>()

        when (componentType) {


            AutoFormComponentType.YEAR -> {
                props["type"] = "year"
            }

            AutoFormComponentType.MONTH -> {
                props["type"] = "month"
            }

            AutoFormComponentType.DATE -> {
                props["type"] = "date"
                props["format"] = "YYYY-MM-DD"
                props["value-format"] = "YYYY-MM-DD"
                props["date-format"] = "YYYY-MM-DD"
            }

            AutoFormComponentType.WEEK -> {
                props["type"] = "week"
            }

            AutoFormComponentType.DATE_TIME -> {
                props["type"] = "datetime"
                props["format"] = "YYYY-MM-DD HH:mm:ss"
                props["date-format"] = "YYYY-MM-DD HH:mm:ss"
                props["value-format"] = "YYYY-MM-DD HH:mm:ss"
                props["time"] = "HH:mm:ss"
            }

            AutoFormComponentType.DATE_RANGE -> {
                props["type"] = "daterange"
                props["format"] = "YYYY-MM-DD"
                props["value-format"] = "YYYY-MM-DD"
                props["date-format"] = "YYYY-MM-DD"
            }

            AutoFormComponentType.DATE_TIME_RANGE -> {
                props["type"] = "datetimerange"
                props["format"] = "YYYY-MM-DD HH:mm:ss"
                props["value-format"] = "YYYY-MM-DD HH:mm:ss"
                props["date-format"] = "YYYY-MM-DD"
                props["time"] = "HH:mm:ss"
            }

            AutoFormComponentType.TIME -> {
                props["type"] = "time"
                props["format"] = "HH:mm:ss"
                props["value-format"] = "HH:mm:ss"
                props["date-format"] = "HH:mm:ss"
            }

            AutoFormComponentType.TIME_RANGE -> {
                props["type"] = "timeRange"
                props["format"] = "HH:mm:ss"
                props["value-format"] = "HH:mm:ss"
                props["date-format"] = "HH:mm:ss"
            }

            else -> {}
        }


        return applyProps(properties, props)
    }

    private fun applyProps(
        propertys: Array<FormDescriptor.Property>,
        props: MutableMap<String, Any>
    ): MutableMap<String, Any> {
        propertys.associate { p ->
            p.name to p.value.let {
                when (p.type) {
                    FormDescriptor.PropertyType.STRING -> it
                    FormDescriptor.PropertyType.INT -> it.toInt()
                    FormDescriptor.PropertyType.FLOAT -> it.toFloat()
                    FormDescriptor.PropertyType.BOOLEAN -> it.toBoolean()
                }
            }
        }.ifEmpty { null }?.forEach { (k, v) -> props[k] = v }
        return props
    }


}
