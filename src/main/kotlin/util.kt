package xls2json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
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

class PrettyPrinter : DefaultPrettyPrinter() {
  var arrayLevel = 0

  init {
    _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE
  }

  override fun createInstance(): PrettyPrinter {
    val pp = PrettyPrinter()
    pp.arrayLevel = arrayLevel
    return pp
  }

  override fun writeStartArray(g: JsonGenerator) {
    super.writeStartArray(g)
    ++arrayLevel
  }

  override fun beforeArrayValues(g: JsonGenerator) {
    if (arrayLevel > 1) {
      g.writeRaw(" ")
    } else {
      _arrayIndenter.writeIndentation(g, _nesting)
    }
  }

  override fun writeArrayValueSeparator(g: JsonGenerator) {
    g.writeRaw(_separators.getArrayValueSeparator())
    if (arrayLevel > 1) {
      g.writeRaw(" ")
    } else {
      _arrayIndenter.writeIndentation(g, _nesting)
    }
  }

  override fun writeEndArray(g: JsonGenerator, nrOfValues: Int) {
    if (!_arrayIndenter.isInline()) {
      --_nesting
    }
    if (nrOfValues > 0 && arrayLevel <= 1) {
      _arrayIndenter.writeIndentation(g, _nesting)
    } else {
      g.writeRaw(" ")
    }
    g.writeRaw("]")
    --arrayLevel
  }
}
