package pro.leaco.autoform.typeadapter

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TimeType {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    class LocalDateTimeTypeAdapter(
    ) : TypeAdapter<LocalDateTime>() {
        override fun write(writer: JsonWriter, data: LocalDateTime?) {
            if (data == null) {
                writer.nullValue()
            } else {
                writer.value(fmt.format(data))
            }
        }

        override fun read(reader: JsonReader): LocalDateTime? {
            val element = JsonParser.parseReader(reader)
            if (element.isJsonNull) {
                return null
            } else if (element.isJsonObject) {
                //适配 bson 内的时间格式
                return Instant.parse(element.asJsonObject.get("\$date").asString).atZone(ZoneOffset.UTC)
                    .toLocalDateTime()
            }
            return LocalDateTime.parse(element.asString, fmt)
        }
    }

    class LocalDateTypeAdapter(
    ) : TypeAdapter<LocalDate>() {
        override fun write(writer: JsonWriter, data: LocalDate?) {
            if (data == null) {
                writer.nullValue()
            } else {
                writer.value(fmt.format(data))
            }
        }

        override fun read(reader: JsonReader): LocalDate? {
            val s = reader.nextString()
            if (s.isNullOrBlank()) {
                return null
            }
            return LocalDate.parse(s, fmt)
        }
    }

    class LocalTimeTypeAdapter(
    ) : TypeAdapter<LocalTime>() {
        override fun write(writer: JsonWriter, time: LocalTime?) {
            if (time == null) {
                writer.nullValue()
            } else {
                writer.value(fmt.format(time))
            }
        }

        override fun read(reader: JsonReader): LocalTime? {
            val s = reader.nextString()
            if (s.isNullOrBlank())
                return null
            return LocalTime.parse(s, fmt)
        }
    }


}
