package pro.leaco.autoform

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
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
        check(dataClazz.constructors.any { it.parameterCount == 0 }) { "Class '$dataClazz' must have one constructor method with no parameters." }
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
        optionSources: Map<String, List<LabelValue>> = emptyMap(),
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
        optionSources: Map<String, List<LabelValue>> = emptyMap(),
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
            "props" to descriptor.props.associate { p ->
                p.name to p.value.let {
                    when (p.type) {
                        FormDescriptor.PropertyType.STRING -> it
                        FormDescriptor.PropertyType.INT -> it.toInt()
                        FormDescriptor.PropertyType.FLOAT -> it.toFloat()
                        FormDescriptor.PropertyType.BOOLEAN -> it.toBoolean()
                    }
                }
            }.ifEmpty { null },
            "options" to if (descriptor.optionSource.isNotBlank())
                optionSources[descriptor.optionSource]?.map { p ->
                    mapOf(
                        "label" to p.label,
                        "value" to p.value,
                    )
                }
            else descriptor.options.map { p ->
                mapOf(
                    "label" to p.label,
                    "value" to p.value,
                )
            }.ifEmpty { null },
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
                if (it.message.isBlank()) return@let null
                return@let mapOf(
                    "message" to it.message,
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


}
