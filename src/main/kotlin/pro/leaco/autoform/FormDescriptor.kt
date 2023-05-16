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
) {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Property(val name: String, val value: String)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Option(val label: String, val value: String)
}
