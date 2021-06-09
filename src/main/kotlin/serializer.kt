package xls2json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocalDateTimeSerializer(val format: String) :
  StdSerializer<LocalDateTime>(LocalDateTime::class.java) {
  val formatter = DateTimeFormatter.ofPattern(format)

  override fun serialize(
    localDateTime: LocalDateTime,
    gen: JsonGenerator,
    arg2: SerializerProvider
  ) {
    gen.writeString(formatter.format(localDateTime))
  }
}

class LocalTimeSerializer(val format: String) : StdSerializer<LocalTime>(LocalTime::class.java) {
  val formatter = DateTimeFormatter.ofPattern(format)

  override fun serialize(localTime: LocalTime, gen: JsonGenerator, arg2: SerializerProvider) {
    gen.writeString(formatter.format(localTime))
  }
}
