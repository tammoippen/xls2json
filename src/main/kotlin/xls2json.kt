package xls2json

fun rstrip(l: List<Any?>, value: Any? = null): List<Any?> {
  var lastValue = l.size - 1
  val itr = l.listIterator(l.size)
  while (itr.hasPrevious()) {
    val v = itr.previous()
    if (v != value) {
      break
    }
    lastValue -= 1
  }
  return l.slice(0..lastValue)
}

fun xls2json(wbk: Workbook, tables: List<String>, strip: Boolean): Map<String, Any?> {
  val result = mutableMapOf<String, Any?>()
  val sheetnames = wbk.sheetnames()
  for (table in tables) {
    if (table !in sheetnames) {
      result[table] = null
    }
    val wst = wbk[table]

    if (strip) {
      result[table] =
        rstrip(
          (0..wst.lastRowNum()).map { row ->
            rstrip((0..wst.lastCellNum(row)).map { col -> wst[row, col] }, null)
          },
          emptyList<List<Any>>()
        )
    } else {
      result[table] =
        (0..wst.lastRowNum()).map { row ->
          (0..wst.lastCellNum(row)).map { col -> wst[row, col] }
        }
    }
  }

  return result
}
