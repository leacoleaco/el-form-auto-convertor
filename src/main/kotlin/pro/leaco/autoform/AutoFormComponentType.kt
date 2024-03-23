package pro.leaco.autoform

import java.time.LocalDate
import java.time.LocalDateTime

enum class AutoFormLabelPosition(val value: String) {
    TOP("top"),
    RIGHT("right"),
}

/**
 * 对应的vue表单组件类型
 */
enum class AutoFormComponentType(val value: String) {
    /**
     * 自动判定
     */
    AUTO(""),

    /**
     * 自定义类型
     *
     */
    CUSTOM("custom"),

    /**
     * 自动判定失败后的未知类型
     */
    UNKNOWN("unknown"),

    /**string	字符串类型	el-input*/
    STRING("string"),

    /**number	number类型，自动添加 .number 修饰符	el-input*/
    NUMBER("number"),

    /**boolean	布尔类型	el-switch*/
    BOOLEAN("boolean"),

    /**regexp	正则表达式，必须是可以正确转化为 RegExp 实例的字符串	el-input*/
    REGEXP("regexp"),

    /**enum	枚举类型，需要和 enum, options 属性配合使用，值必须是 enum 数组中的一个	el-select*/
    ENUM("enum"),

    /**date	必须是合法的 Date 对象	el-date-picker*/
    DATE("date"),
    YEAR("date"),
    MONTH("date"),
    WEEK("date"),

    /**date	必须是合法的 DateTime 对象	el-date-time-picker*/
    DATE_TIME("date"),

    DATE_TIME_RANGE("date"),
    DATE_RANGE("date"),

    /**date	必须是合法的 Time 对象	el-time-picker*/
    TIME("time"),
    TIME_RANGE("timeRange"),

    /**
     * 转换为滑块， 需要是数值类型
     */
    SLIDER("slider"),

    /**
     * 颜色选择
     */
    COLOR("color"),

    /**array 类型，配合 defaultField 使用	dynamic-form-item*/
    ARRAY("array"),

    /** wrap 类型，配合 field 使用 dynamic-form-item*/
    WRAP("wrap"),

    /**object 类型，配合 fields 和 defaultField 使用	dynamic-form-item*/
    OBJECT("object"),

    ;

    companion object {
        fun parse(expr: String): AutoFormComponentType {
            for (value in values()) {
                if (value.value == expr) {
                    return value
                }
            }
            throw IllegalArgumentException("wrong VueFormType: $expr")
        }

        fun tryDetectedComponentType(clazz: Class<*>): AutoFormComponentType {
            return if (clazz == String::class.java) STRING
            else if (clazz == Boolean::class.java) BOOLEAN
            else if (clazz.isPrimitive) {
                when (clazz) {
                    Int::class.java -> NUMBER
                    Short::class.java -> NUMBER
                    Long::class.java -> NUMBER
                    Float::class.java -> NUMBER
                    Double::class.java -> NUMBER
                    Char::class.java -> STRING
                    Byte::class.java -> STRING
                    else -> UNKNOWN
                }
            } else if (clazz.isArray) ARRAY
            else if (clazz == java.util.ArrayList::class.java) ARRAY
            else if (clazz == java.util.Arrays::class.java) ARRAY
            else if (clazz == List::class.java) ARRAY
            else if (clazz == Array::class.java) ARRAY
            else if (clazz == Map::class.java) OBJECT
            else if (clazz == Regex::class.java) REGEXP
            else if (clazz == LocalDate::class.java) DATE
            else if (clazz == LocalDateTime::class.java) DATE_TIME
            else if (clazz == java.lang.Integer::class.java) NUMBER
            else if (clazz == java.lang.Long::class.java) NUMBER
            else if (clazz == java.lang.Float::class.java) NUMBER
            else if (clazz == java.lang.Double::class.java) NUMBER
            else if (clazz == java.lang.String::class.java) STRING
            else if (clazz == java.lang.Character::class.java) STRING
            else if (clazz == java.lang.Byte::class.java) STRING
            else WRAP
        }
    }
}

enum class AutoFormEnumComponent(val value: String) {
    SELECT(""),
    CHECKBOX("checkbox"),
    RADIO("radio"),
    CASCADER("cascader"),
}
