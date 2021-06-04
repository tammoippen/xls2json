package xls2json

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import picocli.CommandLine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class CLITest {
  @ParameterizedTest
  @CsvSource("sample.xls", "sample.xlsx")
  fun `list tables of samples`(fname: String) {
    val classloader = this.javaClass.getClassLoader()
    val file = File(classloader.getResource(fname).getFile())

    val app = XLS2Json()
    val cmd = CommandLine(app)
    val sw = StringWriter()
    cmd.setOut(PrintWriter(sw))

    val exitCode = cmd.execute("--list-tables", file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals("[\"Sheet1\"]" + System.lineSeparator(), sw.toString())
  }
}
