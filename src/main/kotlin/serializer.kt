package xls2json

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocalDateTimeSerializer(val format: String) : JsonSerializer<LocalDateTime> {
  val formatter = DateTimeFormatter.ofPattern(format)

  override fun serialize(
    localDateTime: LocalDateTime,
    srcType: Type,
    context: JsonSerializationContext
  ): JsonElement {
    return JsonPrimitive(formatter.format(localDateTime))
  }
}

class LocalTimeSerializer(val format: String) : JsonSerializer<LocalTime> {
  val formatter = DateTimeFormatter.ofPattern(format)

  override fun serialize(
    localTime: LocalTime,
    srcType: Type,
    context: JsonSerializationContext
  ): JsonElement {
    return JsonPrimitive(formatter.format(localTime))
  }
}
