package pro.leaco.autoform

/**
 * 对应的vue表单验证类型
 */
enum class FormValidType(val value: String) {

    EMPTY("any"),

    /**string	字符串类型	*/
    STRING("string"),

    /**number	number类型，自动添加 .number 修饰符	el-input*/
    NUMBER("number"),

    /**boolean	布尔类型	el-switch*/
    BOOLEAN("boolean"),

    /**regexp	正则表达式，必须是可以正确转化为 RegExp 实例的字符串	el-input*/
    REGEXP("regexp"),

    /**integer	number 类型的整数，自动添加 .number 修饰符	el-input*/
    INTEGER("integer"),

    /**float	number 类型的浮点数，自动添加 .number 修饰符	el-input*/
    FLOAT("float"),

    /**enum	枚举类型，需要和 enum, options 属性配合使用，值必须是 enum 数组中的一个	el-select*/
    ENUM("enum"),

    /**date	必须是合法的 Date 对象	el-date-picker*/
    DATE("date"),

    /**url	符合链接格式的字符串	el-input*/
    URL("url"),

    /**hex	符合哈希格式的字符串	el-input*/
    HEX("hex"),

    /**email	符合邮件格式的字符串	el-input*/
    EMAIL("email"),

    /**object	object 类型，配合 fields 和 defaultField 使用	dynamic-form-item*/
    OBJECT("object"),

    /**array	array 类型，配合 defaultField 使用	dynamic-form-item*/
    ARRAY("array"), ;

    companion object {
        fun parse(expr: String): FormValidType {
            for (value in values()) {
                if (value.value == expr) {
                    return value
                }
            }
            throw IllegalArgumentException("wrong VueFormType: $expr")
        }
    }
}
