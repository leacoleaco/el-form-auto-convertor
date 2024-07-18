package pro.leaco.autoform

import kotlin.reflect.KClass


/**
 * 前端自动表单配置项
 *
 * @property label 表单中对应字段的label值，需和 type 在同一个对象中
 * @property disabled 不可编辑
 * @property defaultValue 默认值
 * @property rules 表单校验规则
 * @property optionSource 如果Option的值希望后期动态提供， 可以在这里设置后期提供的map对应的key参数 see: [pro.leaco.autoform.AutoFormDescriptorConverter.convertToDescriptors(pro.leaco.autoform.ReflectInfo, java.util.Map<java.lang.String,java.lang.String>, java.util.Map<java.lang.String,? extends java.util.List<pro.leaco.autoform.LabelValue>>)]
 * @property componentType 表单项目类型, 默认是AUTO. 如果填写AUTO，则系统会自动判定使用一个最合适的组件; 否则系统会使用要求的组件类型生成表单.
 *              此外，如果填写的是CUSTOM，则还需要在前端 el-form-auto 组件下编写定制的组件插槽，以实现自定义(这个时候数据不完全由后端决定)；
 *              如果找不到对应字段的插槽，则前端会显示为空
 * @property enumSourceKey 如果类型为 enum，但是后端无法提供数据的情况下， 可以从前端提供。 此时前端可以通过
 *              把所有枚举数据绑定到 el-form-auto 组件的 :enum-source 属性上， 然后在此处填写对应的 key 即可
 *              枚举数据的格式为 {key:[{label: 'xxx', value: 'xxx'}],...}
 *
 * @property dependOnProp 当前字段的显示依赖的其它字段， 当其它字段值为true或者存在时, 本字段才显示
 * @property dependOnPropRevert 反转 dependOnProp 依赖的值
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
    val optionSource: String = "",
    val itemsClass: KClass<*> = Any::class,
    val slotName: String = "",
    val tooltip: FormItemTooltip = FormItemTooltip(),
    val alert: FormItemAlert = FormItemAlert(),
    val labelPosition: AutoFormLabelPosition = AutoFormLabelPosition.RIGHT,
    val enumSourceKey: String = "",
    val enumComponent: AutoFormEnumComponent = AutoFormEnumComponent.SELECT,
    val dependOnProp: String = "",
    val dependOnPropRevert: Boolean = false,
) {

    @Retention(AnnotationRetention.RUNTIME)
    annotation class Property(val name: String, val value: String, val type: PropertyType = PropertyType.STRING)

    enum class PropertyType {
        STRING,
        INT,
        FLOAT,
        BOOLEAN,
    }

    @Retention(AnnotationRetention.RUNTIME)
    annotation class Option(val label: String, val value: String)

    /**
     * put this on enum class
     * framework will auto detected it
     */
    @Retention(AnnotationRetention.RUNTIME)
    annotation class OptionLabel(val label: String)

    @Retention(AnnotationRetention.RUNTIME)
    annotation class FormItemTooltip(
        /**
         * 显示的内容，也可被 slot#content 覆盖
         */
        val content: String = "",
        /**
         * Tooltip 组件出现的位置
         */
        val placement: String = "top",
        /**
         * Tooltip 主题，内置了 dark / light 两种
         */
        val effect: String = "dark",
        /**
         * 出现位置的偏移量
         */
        val offset: Int = 0,
        /**
         * 动画名称
         */
        val transition: String = "",
        /**
         * 在触发后多久显示内容，单位毫秒
         */
        val showAfter: Int = 0,
        /**
         * tooltip 的内容是否有箭头
         */
        val showArrow: Boolean = true,
        /**
         * 延迟关闭，单位毫秒
         */
        val hideAfter: Int = 0,
        /**
         * tooltip 出现后自动隐藏延时，单位毫秒
         */
        val autoClose: Int = 0,
        /**
         * 如何触发显示 Tooltip
         * 'hover' | 'click' | 'focus' | 'contextmenu'
         */
        val trigger: String = "hover",
    )

    @Retention(AnnotationRetention.RUNTIME)
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
        // 使用slot的方式自定义alert的内容
        val messageSlot: String = "",
        // 使用slot的方式自定义alert的内容时可以传入的props
        val props: Array<Property> = [],
    )


    @Retention(AnnotationRetention.RUNTIME)
    annotation class TipDataSource(
        // 数据关键词
        val key: String,
        // 数据源地址, 该地址返回数据默认格式需要为: {code:0,data:'xxxxxx'} ,那么这里就可以取得返回的值 xxx 进行展示
        val url: String,
    )
}
