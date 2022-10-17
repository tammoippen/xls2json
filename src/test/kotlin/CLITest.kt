package xls2json

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import picocli.CommandLine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CLITest {

    val classloader: ClassLoader = this.javaClass.getClassLoader()
    val app: XLS2Json = XLS2Json()
    val cmd: CommandLine = CommandLine(app)
    var sw: StringWriter? = null
    var swErr: StringWriter? = null
    val cwd = Paths.get("").toAbsolutePath().toString()
    val ls = System.lineSeparator()
    val fs = File.separator
    val datefmt = "yyyy-MM-dd'T'HH"
    var now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(datefmt))

    @BeforeEach
    fun init() {
        sw = StringWriter()
        swErr = StringWriter()
        cmd.setOut(PrintWriter(sw))
        cmd.setErr(PrintWriter(swErr))
        now = LocalDateTime.now().format(DateTimeFormatter.ofPattern(datefmt))
    }

    @Test
    fun `file not found`() {
        val file = File("xxx.xls")

        val exitCode = cmd.execute(file.absolutePath)
        assertEquals(1, exitCode)
        assertEquals("", sw.toString())
        val errStr = swErr.toString()
        assertTrue(errStr.startsWith("Error: java.io.FileNotFoundException"))
        assertTrue(errStr.endsWith("xxx.xls$ls"))
    }

    @Test
    fun `file not found with stack trace`() {
        val file = File("xxx.xls")

        val exitCode = cmd.execute("--verbose", file.absolutePath)
        assertEquals(1, exitCode)
        assertEquals("", sw.toString())
        val errStr = swErr.toString().split(ls)
        assertEquals("Transforming `$cwd${fs}xxx.xls` ...", errStr[0])
        assertTrue(errStr[1].startsWith("java.io.FileNotFoundException"))
        assertTrue(errStr[1].endsWith("xxx.xls"))
        assertTrue(
            "at org.apache.poi.ss.usermodel.WorkbookFactory.create(WorkbookFactory.java:" in errStr[2]
        )
        assertTrue(errStr.size > 3)
    }

    @Test
    fun `empty file`(@TempDir tempDir: Path) {
        val file = Files.createFile(tempDir.resolve("xxx.xls"))

        val exitCode = cmd.execute(file.toFile().absolutePath)
        assertEquals(1, exitCode)
        assertEquals("", sw.toString())
        val errStr = swErr.toString()
        assertTrue(errStr.startsWith("Error: org.apache.poi.EmptyFileException: The supplied file "))
        assertTrue(errStr.endsWith("xxx.xls' was empty (zero bytes long)$ls"))
    }

    @Test
    fun `no such sheet`() {
        val file = File(classloader.getResource("sample.xls").getFile())

        val exitCode = cmd.execute("--table=xxx", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals("{\"xxx\":null}$ls", sw.toString())
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
        assertEquals("", swErr.toString())
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

    @Test
    fun `verbose output`() {
        val file = File(classloader.getResource("empty.xls").getFile())

        val exitCode = cmd.execute("--verbose", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals("{\"Sheet1\":[]}$ls", sw.toString())
        assertEquals(
            "Transforming `$cwd${fs}build${fs}resources${fs}test${fs}empty.xls` ...$ls",
            swErr.toString()
        )
    }

    @Test
    fun `memory output`() {
        val file = File(classloader.getResource("empty.xls").getFile())

        val exitCode = cmd.execute("--memory", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals("{\"Sheet1\":[]}$ls", sw.toString())
        val errStrs = swErr.toString().split(ls)
        assertEquals(5, errStrs.size)
        assertTrue(errStrs[0].startsWith("Memory next wbk"))
        assertTrue(errStrs[1].startsWith("Memory wbk loaded:"))
        assertTrue(errStrs[2].startsWith("Memory sheets"))
        assertTrue(errStrs[3].startsWith("Memory done"))
        assertEquals("", errStrs[4])
    }

    @ParameterizedTest
    @CsvSource("sample.xls", "sample.xlsx")
    fun `samples extract`(fname: String) {
        val file = File(classloader.getResource(fname).getFile())

        // use datefmt, such that current time can be compared
        val exitCode = cmd.execute("-D", datefmt, file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals(
            "{\"Sheet1\":[[\"empty\",null]," +
                "[\"String\",\"hello\",null]," +
                "[\"StringNumber\",\"14.8\",null]," +
                "[\"Int\",1234,null,null,null,null]," +
                "[\"bool\",true,null,null,null,null]," +
                "[\"bool\",false,null,null,null,null]," +
                "[\"float\",23.12345,null,null,null,null]," +
                "[\"datetime\",\"2021-05-18T21\",null,null,null,null]," +
                "[\"time\",\"21:19:32.000\",null]," +
                "[\"formulars\",null]," +
                "[\"string\",\"hello\",null]," +
                "[\"float\",-0.34678748622465627,null]," +
                "[\"int\",5,null]," +
                "[\"datetime\",\"$now\",null]," +
                "[\"time\",\"13:37:00.000\",null]," +
                "[]," +
                "[]," +
                "[]," +
                "[null,null,null]," +
                "[null,null,null]," +
                "[null,null,null]]}$ls",
            sw.toString()
        )
    }

    @ParameterizedTest
    @CsvSource("sample.xls", "sample.xlsx")
    fun `samples extract stripped`(fname: String) {
        val file = File(classloader.getResource(fname).getFile())

        // use datefmt, such that current time can be compared
        val exitCode = cmd.execute("-D", datefmt, "--strip", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals(
            "{\"Sheet1\":[[\"empty\"]," +
                "[\"String\",\"hello\"]," +
                "[\"StringNumber\",\"14.8\"]," +
                "[\"Int\",1234]," +
                "[\"bool\",true]," +
                "[\"bool\",false]," +
                "[\"float\",23.12345]," +
                "[\"datetime\",\"2021-05-18T21\"]," +
                "[\"time\",\"21:19:32.000\"]," +
                "[\"formulars\"]," +
                "[\"string\",\"hello\"]," +
                "[\"float\",-0.34678748622465627]," +
                "[\"int\",5]," +
                "[\"datetime\",\"$now\"]," +
                "[\"time\",\"13:37:00.000\"]" +
                "]}$ls",
            sw.toString()
        )
    }

    @ParameterizedTest
    @CsvSource("sample.xls", "sample.xlsx")
    fun `samples extract pretty`(fname: String) {
        val file = File(classloader.getResource(fname).getFile())

        // use datefmt, such that current time can be compared
        val exitCode = cmd.execute("-D", datefmt, "--pretty", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals(
            "{$ls" +
                "  \"Sheet1\" : [$ls" +
                "    [ \"empty\", null ],$ls" +
                "    [ \"String\", \"hello\", null ],$ls" +
                "    [ \"StringNumber\", \"14.8\", null ],$ls" +
                "    [ \"Int\", 1234, null, null, null, null ],$ls" +
                "    [ \"bool\", true, null, null, null, null ],$ls" +
                "    [ \"bool\", false, null, null, null, null ],$ls" +
                "    [ \"float\", 23.12345, null, null, null, null ],$ls" +
                "    [ \"datetime\", \"2021-05-18T21\", null, null, null, null ],$ls" +
                "    [ \"time\", \"21:19:32.000\", null ],$ls" +
                "    [ \"formulars\", null ],$ls" +
                "    [ \"string\", \"hello\", null ],$ls" +
                "    [ \"float\", -0.34678748622465627, null ],$ls" +
                "    [ \"int\", 5, null ],$ls" +
                "    [ \"datetime\", \"$now\", null ],$ls" +
                "    [ \"time\", \"13:37:00.000\", null ],$ls" +
                "    [ ],$ls" +
                "    [ ],$ls" +
                "    [ ],$ls" +
                "    [ null, null, null ],$ls" +
                "    [ null, null, null ],$ls" +
                "    [ null, null, null ]$ls" +
                "  ]$ls" +
                "}$ls",
            sw.toString()
        )
    }

    @ParameterizedTest
    @CsvSource("sample.xls", "sample.xlsx")
    fun `samples extract pretty strip`(fname: String) {
        val file = File(classloader.getResource(fname).getFile())

        // use datefmt, such that current time can be compared
        val exitCode = cmd.execute("-D", datefmt, "--pretty", "-s", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals(
            "{$ls" +
                "  \"Sheet1\" : [$ls" +
                "    [ \"empty\" ],$ls" +
                "    [ \"String\", \"hello\" ],$ls" +
                "    [ \"StringNumber\", \"14.8\" ],$ls" +
                "    [ \"Int\", 1234 ],$ls" +
                "    [ \"bool\", true ],$ls" +
                "    [ \"bool\", false ],$ls" +
                "    [ \"float\", 23.12345 ],$ls" +
                "    [ \"datetime\", \"2021-05-18T21\" ],$ls" +
                "    [ \"time\", \"21:19:32.000\" ],$ls" +
                "    [ \"formulars\" ],$ls" +
                "    [ \"string\", \"hello\" ],$ls" +
                "    [ \"float\", -0.34678748622465627 ],$ls" +
                "    [ \"int\", 5 ],$ls" +
                "    [ \"datetime\", \"$now\" ],$ls" +
                "    [ \"time\", \"13:37:00.000\" ]$ls" +
                "  ]$ls" +
                "}$ls",
            sw.toString()
        )
    }

    @ParameterizedTest
    @CsvSource("sample.xls", "sample.xlsx")
    fun `samples extract pretty strip color`(fname: String) {
        val file = File(classloader.getResource(fname).getFile())

        // use datefmt, such that current time can be compared
        // force color
        val exitCode = cmd.execute("-D", datefmt, "--pretty", "-s", "--color", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals(
            "{\u001b[33m$ls" +
                "  \"Sheet1\"\u001b[0m : [$ls" +
                "    [ \u001b[31m\"empty\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"String\"\u001b[0m, \u001b[31m\"hello\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"StringNumber\"\u001b[0m, \u001b[31m\"14.8\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"Int\"\u001b[0m, \u001b[36m1234\u001b[0m ],$ls" +
                "    [ \u001b[31m\"bool\"\u001b[0m, \u001b[32mtrue\u001b[0m ],$ls" +
                "    [ \u001b[31m\"bool\"\u001b[0m, \u001b[32mfalse\u001b[0m ],$ls" +
                "    [ \u001b[31m\"float\"\u001b[0m, \u001b[34m23.12345\u001b[0m ],$ls" +
                "    [ \u001b[31m\"datetime\"\u001b[0m, \u001b[31m\"2021-05-18T21\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"time\"\u001b[0m, \u001b[31m\"21:19:32.000\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"formulars\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"string\"\u001b[0m, \u001b[31m\"hello\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"float\"\u001b[0m, \u001b[34m-0.34678748622465627\u001b[0m ],$ls" +
                "    [ \u001b[31m\"int\"\u001b[0m, \u001b[36m5\u001b[0m ],$ls" +
                "    [ \u001b[31m\"datetime\"\u001b[0m, \u001b[31m\"$now\"\u001b[0m ],$ls" +
                "    [ \u001b[31m\"time\"\u001b[0m, \u001b[31m\"13:37:00.000\"\u001b[0m ]$ls" +
                "  ]$ls" +
                "}$ls",
            sw.toString()
        )
    }

    @Test
    fun `sample two tables extract pretty`() {
        val file = File(classloader.getResource("sampleTwoSheets.xls").getFile())

        // use datefmt, such that current time can be compared
        val exitCode = cmd.execute("-D", datefmt, "--pretty", file.absolutePath)
        assertEquals(0, exitCode)
        assertEquals(
            "{$ls" +
                "  \"Sheet1\" : [$ls" +
                "    [ \"empty\", null ],$ls" +
                "    [ \"String\", \"hello\", null ],$ls" +
                "    [ \"StringNumber\", \"14.8\", null ],$ls" +
                "    [ \"Int\", 1234, null, null, null, null ],$ls" +
                "    [ \"bool\", true, null, null, null, null ],$ls" +
                "    [ \"bool\", false, null, null, null, null ],$ls" +
                "    [ \"float\", 23.12345, null, null, null, null ],$ls" +
                "    [ \"datetime\", \"2021-05-18T21\", null, null, null, null ],$ls" +
                "    [ \"time\", \"21:19:32.000\", null ],$ls" +
                "    [ \"formulars\", null ],$ls" +
                "    [ \"string\", \"hello\", null ],$ls" +
                "    [ \"float\", -0.34678748622465627, null ],$ls" +
                "    [ \"int\", 5, null ],$ls" +
                "    [ \"datetime\", \"$now\", null ],$ls" +
                "    [ \"time\", \"13:37:00.000\", null ],$ls" +
                "    [ ],$ls" +
                "    [ ],$ls" +
                "    [ ],$ls" +
                "    [ null, null, null ],$ls" +
                "    [ null, null, null ],$ls" +
                "    [ null, null, null ]$ls" +
                "  ],$ls" +
                "  \"Sheet2\" : [$ls" +
                "    [ \"empty\", null ],$ls" +
                "    [ \"String\", \"hello\", null ],$ls" +
                "    [ \"StringNumber\", \"14.8\", null ],$ls" +
                "    [ \"Int\", 1234, null, null, null, null ],$ls" +
                "    [ \"bool\", true, null, null, null, null ],$ls" +
                "    [ \"bool\", false, null, null, null, null ],$ls" +
                "    [ \"float\", 23.12345, null, null, null, null ],$ls" +
                "    [ \"datetime\", \"2021-05-18T21\", null, null, null, null ],$ls" +
                "    [ \"time\", \"21:19:32.000\", null ],$ls" +
                "    [ \"formulars\", null ],$ls" +
                "    [ \"string\", \"hello\", null ],$ls" +
                "    [ \"float\", -0.34678748622465627, null ],$ls" +
                "    [ \"int\", 5, null ],$ls" +
                "    [ \"datetime\", \"$now\", null ],$ls" +
                "    [ \"time\", \"13:37:00.000\", null ],$ls" +
                "    [ ],$ls" +
                "    [ ],$ls" +
                "    [ ],$ls" +
                "    [ null, null, null ],$ls" +
                "    [ null, null, null ],$ls" +
                "    [ null, null, null ]$ls" +
                "  ]$ls" +
                "}$ls",
            sw.toString()
        )
    }
}
