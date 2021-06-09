package xls2json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter
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

class PrettyPrinter : MinimalPrettyPrinter() {
  val indenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE
  var nesting = 0
  var arrayLevel = 0

  override fun writeStartObject(g: JsonGenerator) {
    g.writeRaw('{')
    if (!indenter.isInline()) {
      ++nesting
    }
  }

  override fun beforeObjectEntries(g: JsonGenerator) {
    indenter.writeIndentation(g, nesting)
  }

  override fun writeObjectFieldValueSeparator(g: JsonGenerator) {
    g.writeRaw(_separators.getObjectFieldValueSeparator())
    g.writeRaw(" ")
  }

  override fun writeObjectEntrySeparator(g: JsonGenerator) {
    indenter.writeIndentation(g, nesting)
  }

  override fun writeEndObject(g: JsonGenerator, nrOfEntries: Int) {
    if (!indenter.isInline()) {
      --nesting
    }
    if (nrOfEntries > 0) {
      indenter.writeIndentation(g, nesting)
    } else {
      g.writeRaw(" ")
    }
    g.writeRaw("}")
  }

  override fun writeStartArray(g: JsonGenerator) {
    if (!indenter.isInline()) {
      ++nesting
    }
    ++arrayLevel

    g.writeRaw("[")
  }

  override fun beforeArrayValues(g: JsonGenerator) {
    if (arrayLevel > 1) {
      g.writeRaw(" ")
    } else {
      indenter.writeIndentation(g, nesting)
    }
  }

  override fun writeArrayValueSeparator(g: JsonGenerator) {
    g.writeRaw(_separators.getArrayValueSeparator())
    if (arrayLevel > 1) {
      g.writeRaw(" ")
    } else {
      indenter.writeIndentation(g, nesting)
    }
  }

  override fun writeEndArray(g: JsonGenerator, nrOfValues: Int) {
    if (!indenter.isInline()) {
      --nesting
    }
    if (nrOfValues > 0 && arrayLevel <= 1) {
      indenter.writeIndentation(g, nesting)
    } else {
      g.writeRaw(" ")
    }
    --arrayLevel
    g.writeRaw("]")
  }
}
