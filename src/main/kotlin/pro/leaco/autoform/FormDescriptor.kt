package pro.leaco.autoform

import com.google.gson.*


/**
 * 前端自动表单配置项
 *
 * @property label 表单中对应字段的label值，需和 type 在同一个对象中
 * @property disabled 不可编辑
 * @property defaultValue 默认值
 * @property rules 表单校验规则
 * @property componentType 表单项目类型, 如果不填写，则系统会自动判定使用一个最合适的组件
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
)

