package xls2json

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import picocli.CommandLine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class CLITest {

  val classloader: ClassLoader = this.javaClass.getClassLoader()
  val app: XLS2Json = XLS2Json()
  val cmd: CommandLine = CommandLine(app)
  var sw: StringWriter? = null
  val ls = System.lineSeparator()

  @BeforeEach
  fun init() {
    sw = StringWriter()
    cmd.setOut(PrintWriter(sw))
  }

  @ParameterizedTest
  @CsvSource("sample.xls", "sample.xlsx")
  fun `list tables of samples`(fname: String) {
    val file = File(classloader.getResource(fname).getFile())

    val exitCode = cmd.execute("--list-tables", file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals("[\"Sheet1\"]$ls", sw.toString())
  }

  @Test
  fun `list tables of sampleTwoSheets`() {
    val file = File(classloader.getResource("sampleTwoSheets.xls").getFile())

    val exitCode = cmd.execute("--list-tables", file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals("[\"Sheet1\",\"Sheet2\"]$ls", sw.toString())
  }

  @Test
  fun `no argument`() {
    val exitCode = cmd.execute()
    assertEquals(0, exitCode)
    assertEquals("", sw.toString())
  }

  @Test
  fun `empty book`() {
    val file = File(classloader.getResource("empty.xls").getFile())

    val exitCode = cmd.execute(file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals("{\"Sheet1\":[]}$ls", sw.toString())
  }

  @Test
  fun `empty book pretty`() {
    val file = File(classloader.getResource("empty.xls").getFile())

    val exitCode = cmd.execute("--pretty", file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals("{$ls  \"Sheet1\" : [ ]$ls}$ls", sw.toString())
  }

  @Test
  fun `empty book pretty color`() {
    val file = File(classloader.getResource("empty.xls").getFile())

    val exitCode = cmd.execute("--pretty", "--color", file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals("{\u001b[33m$ls  \"Sheet1\"\u001b[0m : [ ]$ls}$ls", sw.toString())
  }

  @ParameterizedTest
  @CsvSource("sample.xls", "sample.xlsx")
  fun `samples extract`(fname: String) {
    val file = File(classloader.getResource(fname).getFile())

    val exitCode = cmd.execute(file.absolutePath)
    assertEquals(0, exitCode)
    assertEquals(
      "{\"Sheet1\":[[\"empty\",null],[\"String\",\"hello\",null],[\"StringNumber\",\"14.8\",null],[\"Int\",1234,null,null,null,null],[\"bool\",true,null,null,null,null],[\"bool\",false,null,null,null,null],[\"float\",23.12345,null,null,null,null],[\"datetime\",\"2021-05-18T21:19:53.040\",null,null,null,null],[\"time\",\"21:19:32.000\",null],[\"formulars\",null],[\"string\",\"hello\",null],[\"float\",-0.34678748622465627,null],[\"int\",5,null],[\"datetime\",\"2021-06-20T06:51:24.563\",null],[\"time\",\"13:37:00.000\",null],[],[],[],[null,null,null],[null,null,null],[null,null,null]]}$ls",
      sw.toString()
    )
  }
}
