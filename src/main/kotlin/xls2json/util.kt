package xls2json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate
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
    var escape: String? = null

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
        if (escape != null) {
            // When writing the first value of an array, the
            // (WriterBased)JsonGenerator will first call this
            // method. Hence we need to add the color code.
            g.writeRaw(escape)
        }
    }

    override fun writeArrayValueSeparator(g: JsonGenerator) {
        g.writeRaw(_separators.getArrayValueSeparator())
        if (arrayLevel > 1) {
            g.writeRaw(" ")
        } else {
            _arrayIndenter.writeIndentation(g, _nesting)
        }
        if (escape != null) {
            // When writing the subsequent value of an array, the
            // (WriterBased)JsonGenerator will first call this
            // method. Hence we need to add the color code.
            g.writeRaw(escape)
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

class Highlighter(val gen: JsonGenerator) : JsonGeneratorDelegate(gen) {
    val esc = "\u001b["
    val reset = "${esc}0m"
    val black = "${esc}30m"
    val red = "${esc}31m"
    val green = "${esc}32m"
    val yellow = "${esc}33m"
    val blue = "${esc}34m"
    val magenta = "${esc}35m"
    val cyan = "${esc}36m"
    val white = "${esc}37m"

    val fieldToColor =
        mapOf(
            "sheetname" to yellow,
            "string" to red,
            "null" to magenta,
            "boolean" to green,
            "int" to cyan,
            "float" to blue
        )

    fun setEscape(escapeName: String) {
        val pp = gen.prettyPrinter
        if (pp != null && pp is PrettyPrinter) {
            pp.escape = fieldToColor[escapeName]
        }
    }

    fun unsetEscape() {
        val pp = gen.prettyPrinter
        if (pp != null && pp is PrettyPrinter) {
            pp.escape = null
        }
    }

    override fun writeFieldName(value: String) {
        super.writeRaw(fieldToColor["sheetname"])
        super.writeFieldName(value)
        super.writeRaw(reset)
    }
    override fun writeString(value: String) {
        setEscape("string")
        super.writeString(value)
        unsetEscape()
        super.writeRaw(reset)
    }
    override fun writeNull() {
        setEscape("null")
        super.writeNull()
        unsetEscape()
        super.writeRaw(reset)
    }
    override fun writeBoolean(value: Boolean) {
        setEscape("boolean")
        super.writeBoolean(value)
        unsetEscape()
        super.writeRaw(reset)
    }
    override fun writeNumber(value: Long) {
        setEscape("int")
        super.writeNumber(value)
        unsetEscape()
        super.writeRaw(reset)
    }
    override fun writeNumber(value: Double) {
        setEscape("float")
        super.writeNumber(value)
        unsetEscape()
        super.writeRaw(reset)
    }
    override fun writeNumber(value: Float) {
        setEscape("float")
        super.writeNumber(value)
        unsetEscape()
        super.writeRaw(reset)
    }
}
