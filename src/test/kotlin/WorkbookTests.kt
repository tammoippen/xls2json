package xls2json

import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WorkbookTest {

  @ParameterizedTest
  @CsvSource("sample.xls", "sample.xlsx")
  fun `sheetnames of sample-files`(fname: String) {
    val classloader = this.javaClass.getClassLoader()
    val file = File(classloader.getResource(fname).getFile())

    val wbk = Workbook(file)

    assertEquals(listOf("Sheet1"), wbk.sheetnames())

    wbk.close()
  }

  fun `sheetnames of sample-file with multiple sheets`() {
    val classloader = this.javaClass.getClassLoader()
    val file = File(classloader.getResource("sampleTwoSheets.xls").getFile())

    val wbk = Workbook(file)

    assertEquals(listOf("Sheet1", "Sheet2"), wbk.sheetnames())

    wbk.close()
  }

  @ParameterizedTest
  @CsvSource("sample.xls", "sample.xlsx")
  fun `number of rows of sample-files`(fname: String) {
    val classloader = this.javaClass.getClassLoader()
    val file = File(classloader.getResource(fname).getFile())

    val wbk = Workbook(file)
    val wst = wbk["Sheet1"]

    // there are some formatted cells without content
    assertEquals(20, wst.lastRowNum())

    wbk.close()
  }

  @ParameterizedTest
  @CsvSource("sample.xls", "sample.xlsx", "sampleTwoSheets.xls")
  fun `read cells of sample-files`(fname: String) {
    val classloader = this.javaClass.getClassLoader()
    val file = File(classloader.getResource(fname).getFile())

    val wbk = Workbook(file)
    val wst = wbk["Sheet1"]

    // there are some formatted cells without content
    var row = 0
    assertEquals("empty", wst[row, 0])
    assertEquals(null, wst[row, 1])
    ++row
    assertEquals("String", wst[row, 0])
    assertEquals("hello", wst[row, 1])
    ++row
    assertEquals("StringNumber", wst[row, 0])
    assertEquals("14.8", wst[row, 1])
    ++row
    assertEquals("Int", wst[row, 0])
    assertEquals(1234, wst[row, 1])
    ++row
    assertEquals("bool", wst[row, 0])
    assertEquals(true, wst[row, 1])
    ++row
    assertEquals("bool", wst[row, 0])
    assertEquals(false, wst[row, 1])
    ++row
    assertEquals("float", wst[row, 0])
    assertEquals(23.12345, wst[row, 1])
    ++row
    assertEquals("datetime", wst[row, 0])
    assertEquals(LocalDateTime.of(2021, Month.of(5), 18, 21, 19, 53, 40000000), wst[row, 1])
    ++row
    assertEquals("time", wst[row, 0])
    assertEquals(LocalTime.of(21, 19, 32, 0), wst[row, 1])
    ++row
    assertEquals("formulars", wst[row, 0])
    assertEquals(null, wst[row, 1])
    ++row
    assertEquals("string", wst[row, 0])
    assertEquals("hello", wst[row, 1])
    ++row
    assertEquals("float", wst[row, 0])
    assertEquals(-0.34678748622465627, wst[row, 1])
    ++row
    assertEquals("int", wst[row, 0])
    assertEquals(5, wst[row, 1])
    ++row
    assertEquals("datetime", wst[row, 0])
    // now()
    // assertEquals(5, wst[row,1])
    ++row
    assertEquals("time", wst[row, 0])
    assertEquals(LocalTime.of(13, 37, 0, 0), wst[row, 1])
    ++row

    wbk.close()
  }
}
