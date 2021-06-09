package xls2json

// import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
// import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import picocli.CommandLine.Spec
import java.io.File
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun memory(event: String, err: PrintWriter) {
  val rt = Runtime.getRuntime()
  val mem = (rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0
  err.println("Memory %-10s: %.3f MB".format(event, mem))
}

@Command(
  name = "xls2json",
  header = ["Open an xls(x|m) file and transform to json.", ""],
  mixinStandardHelpOptions = true,
  version = ["xls2json $buildInfoVersion"],
  description =
  [
    "",
    "  If no `--table`s are provided, then all",
    "  tables will be extracted.",
    "",
    "  All files will be processed one by one,",
    "  each outputting on line of json.",
    "",
  ],
  footer =
  [
    "",
    "By Tammo Ippen <tammo.ippen@posteo.de>",
    "Issues: https://github.com/tammoippen/xls2json/issues",
    "",
  ],
  sortOptions = false
)
class XLS2Json : Callable<Int> {
  @Spec lateinit var spec: CommandSpec

  @Option(names = ["-m", "--memory"], description = ["Show memory usage information."], order = 1)
  var showMemory = false

  @Option(names = ["-v", "--verbose"], description = ["Show more information."], order = 1)
  var verbose = false

  @Option(names = ["--pretty"], description = ["Pretty print the JSON."], order = 1)
  var pretty = false

  @Option(names = ["-l", "--list-tables"], description = ["List all tables."], order = 2)
  var list_tables = false

  @Option(names = ["-t", "--table"], description = ["Specify the tables to transform"], order = 3)
  var tables = listOf<String>()

  @Option(
    names = ["-p", "--password"],
    description = ["Password for opening the input file(s)."],
    order = 3
  )
  var password: String? = null

  @Option(
    names = ["-s", "--strip"], description = ["Strip empty columns and empty rows."], order = 3
  )
  var strip = false

  @Option(
    names = ["-D", "--datetime-format"],
    description = ["The datetime format.\n[default: '\${DEFAULT-VALUE}']"],
    defaultValue = "yyyy-MM-dd'T'HH:mm:ss.SSS",
    order = 3
  )
  lateinit var dtfmt: String

  @Option(
    names = ["-T", "--time-format"],
    description = ["The time format.\n[default: '\${DEFAULT-VALUE}']"],
    defaultValue = "HH:mm:ss.SSS",
    order = 3
  )
  lateinit var tfmt: String

  @Parameters(description = ["xls(x|m)-file(s) to transform"]) var files: List<File> = listOf()

  override fun call(): Int {
    val out = spec.commandLine().getOut()
    val err = spec.commandLine().getErr()

    val mapper = ObjectMapper()
    val module = SimpleModule()
    module.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(dtfmt))
    module.addSerializer(LocalTime::class.java, LocalTimeSerializer(tfmt))
    mapper.registerModule(module)
    var writer = mapper.writer()
    if (pretty) {
      // val pp =  DefaultPrettyPrinter()
      // pp.indentArraysWith(DefaultIndenter())
      val pp = PrettyPrinter()
      writer = mapper.writer(pp)
    }

    for (file in files) {
      if (showMemory) memory("next wbk", err)
      if (verbose) err.println("Transforming `$file` ...")

      var wbk: Workbook? = null

      try {
        wbk = Workbook(file, password)

        if (showMemory) memory("wbk loaded", err)

        if (list_tables) {
          out.println(writer.writeValueAsString(wbk.sheetnames()))

          if (showMemory) memory("done", err)
          continue
        }

        var otables = tables
        if (otables.isEmpty()) {
          otables = wbk.sheetnames()
        }

        val sheets = xls2json(wbk, otables, strip)
        if (showMemory) memory("sheets", err)

        out.println(writer.writeValueAsString(sheets))

        if (showMemory) memory("done", err)
      } catch (e: Exception) {
        if (verbose) e.printStackTrace(err) else err.println("Error: $e")
        return 1
      } finally {
        if (wbk != null) wbk.close()
      }
    }
    return 0
  }
}

fun main(args: Array<String>) {
  // remove java options - already processed
  // e.g. -Xmx14g for max heap size of 14 GB
  val filteredArgs =
    args.filter { e -> !(e.startsWith("-XX:") || e.startsWith("-Xm") || e.startsWith("-H:")) }
  val cmd = CommandLine(XLS2Json())
  exitProcess(cmd.execute(*filteredArgs.toTypedArray()))
}
