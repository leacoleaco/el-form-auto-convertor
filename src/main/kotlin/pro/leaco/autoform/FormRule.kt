package pro.leaco.autoform

/**
 * form validate rule
 * @property type Indicates the type of validator to use.
 * @property required The required rule property indicates that the field must exist on the source object being validated.
 * @property message display this message if validate is wrong
 * @param min A range is defined using the min and max properties. For string and array types comparison is performed against the length, for number types the number must not be less than min nor greater than max.
 * @param max A range is defined using the min and max properties. For string and array types comparison is performed against the length, for number types the number must not be less than min nor greater than max.
 * @param len To validate an exact length of a field specify the len property. For string and array types comparison is performed on the length property, for the number type this property indicates an exact match for the number, ie, it may only be strictly equal to len.
 * @param pattern The pattern rule property indicates a regular expression that the value must match to pass validation.
 * @param enum To validate a value from a list of possible values use the enum type with a enum property listing the valid values for the field, for example:
 * @param whitespace It is typical to treat required fields that only contain whitespace as errors. To add an additional test for a string that consists solely of whitespace add a whitespace property to a rule with a value of true. The rule must be a string type.
 *
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class FormRule(
    val required: Boolean = false,
    val message: String = "",
    val type: FormValidType = FormValidType.EMPTY,
    val min: Float= -1f,
    val max: Float = -1f,
    val len: Int = -1,
    val pattern: String = "",
    val enum: Array<String> = [],
    val whitespace: Boolean = false,
)
