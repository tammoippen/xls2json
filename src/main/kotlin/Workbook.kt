package xls2json

import java.io.File
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory

class Worksheet(val wst: Sheet, val evaluator: FormulaEvaluator) {
    private fun numCell(cell: Cell): Any {
        val numVal = cell.numericCellValue
        if (DateUtil.isCellDateFormatted(cell)) {
            if (numVal <= 1) {
                // time
                return cell.localDateTimeCellValue.toLocalTime()
            } else {
                // datetime
                return cell.localDateTimeCellValue
            }
        } else {
            if (Math.floor(numVal) == numVal) {
                return numVal.toInt()
            }
            return numVal
        }
    }

    operator fun get(row_idx: Int, col_idx: Int): Any? {
        val row = wst.getRow(row_idx)
        if (row == null) {
            return null
        }
        val cell = row.getCell(col_idx)
        if (cell == null) {
            return null
        }

        var cellType = cell.cellType
        if (cellType == CellType.FORMULA) {
            try {
                cellType = evaluator.evaluateFormulaCell(cell)
            } catch (e: Exception) {
                cellType = cell.cachedFormulaResultType
                System.err.println(
                        "Formular error: ${wst.sheetName}[${row_idx}, ${col_idx}] = '${cell.toString()}'" +
                                "\n  cached type: ${cellType}" +
                                "\n  $e")
            }
        }

        when (cellType) {
            CellType.BLANK -> return null
            CellType.BOOLEAN -> return cell.booleanCellValue
            CellType.STRING -> return cell.stringCellValue
            CellType.NUMERIC -> return numCell(cell)
            CellType.ERROR -> {
                System.err.println(
                        "Error cell: ${wst.sheetName}[${row_idx}, ${col_idx}] = '${cell.errorCellValue}'")
                return "Error#${cell.errorCellValue}"
            }
            else -> return null
        }
    }

    fun lastRowNum(): Int = wst.lastRowNum

    fun lastCellNum(row_idx: Int): Short {
        val row = wst.getRow(row_idx)
        if (row == null) {
            return -1
        }
        return row.lastCellNum
    }
}

class Workbook(val path: File, val password: String? = null) {
    val wbk = WorkbookFactory.create(path, password, true)
    init {
        if (wbk == null) {
            throw IllegalArgumentException("Cannot open file `$path` as Excel Spreadsheet.")
        }
    }
    val evaluator = wbk.creationHelper.createFormulaEvaluator()

    fun sheetnames(): List<String> {
        return (0..wbk.numberOfSheets - 1).map { wbk.getSheetName(it) }
    }

    fun close() {
        wbk.close()
    }

    operator fun get(name: String): Worksheet {
        return Worksheet(wbk.getSheet(name), evaluator)
    }
}
