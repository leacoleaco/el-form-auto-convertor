package pro.leaco.autoform.typeadapter

import pro.leaco.autoform.AutoFormComponentType
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

object VueFormTypeAdapter : TypeAdapter<AutoFormComponentType>() {
    override fun write(w: JsonWriter, data: AutoFormComponentType) {
        w.value(data.value)
    }

    override fun read(r: JsonReader): AutoFormComponentType {
        return AutoFormComponentType.parse(r.nextString())
    }
}
