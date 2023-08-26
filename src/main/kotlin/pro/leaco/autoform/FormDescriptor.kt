package pro.leaco.autoform

import kotlin.reflect.KClass


/**
 * 前端自动表单配置项
 *
 * @property label 表单中对应字段的label值，需和 type 在同一个对象中
 * @property disabled 不可编辑
 * @property defaultValue 默认值
 * @property rules 表单校验规则
 * @property componentType 表单项目类型, 默认是AUTO. 如果填写AUTO，则系统会自动判定使用一个最合适的组件; 否则系统会使用要求的组件类型生成表单.
 *              此外，如果填写的是CUSTOM，则还需要在前端 el-form-auto 组件下编写定制的组件插槽，以实现自定义(这个时候数据不完全由后端决定)；
 *              如果找不到对应字段的插槽，则前端会显示为空
 * @property enumSourceKey 如果类型为 enum，但是后端无法提供数据的情况下， 可以从前端提供。 此时前端可以通过
 *              把所有枚举数据绑定到 el-form-auto 组件的 :enum-source 属性上， 然后在此处填写对应的 key 即可
 *              枚举数据的格式为 {key:[{label: 'xxx', value: 'xxx'}],...}
 *
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class FormDescriptor(
    val label: String = "",
    val disabled: Boolean = false,
    val defaultValue: String = "",
    val componentType: AutoFormComponentType = AutoFormComponentType.AUTO,
    val rules: Array<FormRule> = [],
    val props: Array<Property> = [],
    val options: Array<Option> = [],
    val itemsClass: KClass<*> = Any::class,
    val slotName: String = "",
    val tooltip: FormItemTooltip = FormItemTooltip(),
    val alert: FormItemAlert = FormItemAlert(),
    val labelPosition: AutoFormLabelPosition = AutoFormLabelPosition.RIGHT,
    val enumSourceKey: String = "",
    val enumComponent: AutoFormEnumComponent = AutoFormEnumComponent.SELECT,
) {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Property(val name: String, val value: String, val type: PropertyType = PropertyType.STRING)

    enum class PropertyType {
        STRING,
        INT,
        FLOAT,
        BOOLEAN,
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Option(val label: String, val value: String)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class FormItemTooltip(
        val content: String = "",
        val placement: String = "top",
        val effect: String = "dark",
        val offset: Int = 0,
        val transition: String = "",
        val showAfter: Int = 0,
        val showArrow: Boolean = true,
        val hideAfter: Int = 0,
        val autoClose: Boolean = true,
        val trigger: String = "hover",
    )

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class FormItemAlert(
        val message: String = "",
        val title: String = "",
        val type: String = "info",
        val description: String = "",
        val closable: Boolean = true,
        val closeText: String = "",
        val showIcon: Boolean = true,
        val center: Boolean = false,
        val effect: String = "light",
    )
}
