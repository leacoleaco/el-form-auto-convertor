package pro.leaco.autoform

import java.lang.reflect.Field

data class ReflectInfo(
    val descriptor: FormDescriptor,
    val field: Field,
    val defaultValue: Any?
)
