package mx.mobile.solution.nabia04_beta1.utilities

import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.App.Companion.applicationContext
import mx.mobile.solution.nabia04_beta1.data.dao.DuesBackupDao
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDues
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDuesBackup
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.ceil


@Singleton
class ExcelHelper @Inject constructor(
    val sharedP: SharedPreferences,
    val duesBackupDao: DuesBackupDao
) {

    private var excelFile: File? = null
    private var numNumYears = 0
    private var formatter = DataFormatter()
    private var workbook: Workbook? = null
    private var isCreated = false
    var members: MutableList<Member> = ArrayList()
    var names: MutableList<String> = ArrayList()


    fun initialize() {
        if (!isCreated) {
            excelFile = getExcelFile()
            if (excelFile!!.length() == 0L) {
                excelFile = downloadFile()
            }
            if (excelFile == null) {
                return
            }
            isCreated = loadWorkBook(excelFile!!)
            if (isCreated) {
                getPaidMembers()
            }
        }
    }

    fun reloadExcel() {
        isCreated = loadWorkBook(excelFile!!)
        if (isCreated) {
            getPaidMembers()
        }
    }

    fun initializeTemp(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            loadWorkBook(file)
            isCreated = false
        }
    }

    private fun downloadFile(): File? {
        val duesDir = File(applicationContext().filesDir, "Dues")
        Log.i("TAG", "Downloading file")
        var excelFile: File? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(Const.EXCEL_URL)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.doOutput = false
            urlConnection.connect()

            if (!duesDir.exists()) {
                duesDir.mkdirs()
            }
            excelFile = File(duesDir, "Nabiadues.xlsx")
            if (!excelFile.exists()) {
                excelFile.createNewFile()
            }
            val inputStream: InputStream = urlConnection.inputStream
            val totalSize: Int = urlConnection.contentLength
            val outPut = FileOutputStream(excelFile)
            var downloadedSize = 0
            val buffer = ByteArray(2024)
            var bufferLength = 0
            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                outPut.write(buffer, 0, bufferLength)
                downloadedSize += bufferLength
                Log.e(
                    "Progress:",
                    "downloadedSize:" + abs(downloadedSize * 100 / totalSize)
                )
            }
            outPut.close()
            Log.i("TAG", "File downloaded")
        } catch (e: IOException) {
            excelFile = null
            e.printStackTrace()
            Log.e("checkException:-", "" + e)
        }
        return excelFile
    }

    private fun getPaidMembers() {
        members.clear()
        names.clear()
        members.add(Member("", "", 0.00))
        names.add("Select member")
        val sheet = workbook?.getSheetAt(0)
        if (sheet != null) {
            for (i in 1 until sheet.lastRowNum) {
                val name: String = formatter.formatCellValue(sheet.getRow(i).getCell(1))
                val folio: String = formatter.formatCellValue(sheet.getRow(i).getCell(2))
                val amount =
                    sheet.getRow(i).getCell(sheet.getRow(i).lastCellNum - 1).numericCellValue
                members.add(Member(folio, name, amount))
                names.add(name)
            }
        }
        Log.i("TAG", "Members = ${members.size}")
    }

    val totalNumMonths: Double
        get() {
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH)
            val currYearMonths = 12 - month
            return ((12 * numNumYears) - (7 + currYearMonths)).toDouble()
        }

    private fun loadWorkBook(excelFile: File): Boolean {
        try {
            val workbookStream = FileInputStream(excelFile)
            workbook = WorkbookFactory.create(workbookStream)
            if (workbook != null) {
                numNumYears = workbook!!.getSheetAt(0).getRow(0).lastCellNum - 4
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun getExcelFile(): File {
        val duesDir = File(applicationContext().filesDir, "Dues")
        return File(duesDir, "Nabiadues.xlsx")
    }

    private fun getUserRowFolio(folio: String, sheet: Sheet): Row? {

        for (row in sheet) {
            val folioCell = row.getCell(2)
            val folioValue = formatter.formatCellValue(folioCell)
            if (folioValue == folio) {
                return row
            }
        }
        return null
    }

    fun getName(n: String): String {
        for (member in members) {
            if (member.folio == n) {
                return member.name
            }
        }
        return ""
    }

    suspend fun getDues(): List<EntityDues> {
        return withContext(Dispatchers.Default) {
            val duesList: MutableList<EntityDues> = ArrayList()
            val sheet = workbook?.getSheetAt(0)
            if (sheet != null) {
                for (i in 1 until sheet.lastRowNum) {
                    val row = sheet.getRow(i)
                    val dues = EntityDues()

                    dues.index = formatter.formatCellValue(row.getCell(0))
                    dues.name = formatter.formatCellValue(row.getCell(1))
                    dues.folio = formatter.formatCellValue(row.getCell(2))

                    val duesArray: MutableList<String> = ArrayList()
                    for (t in 0 until numNumYears) {
                        val v = formatter.formatCellValue(row.getCell(t + 3))
                        duesArray.add(v)
                    }

                    duesArray.add(row.getCell(row.lastCellNum - 1).numericCellValue.toString())

                    dues.payments = duesArray.toTypedArray()

                    duesList.add(dues)
                }
            }
            return@withContext duesList
        }
    }

    private fun getCellvalue(cell: Cell): String {
        return formatter.formatCellValue(cell)
    }

    private fun upDatePayment(amnt: Double, folio: String): Response<String> {
        val errMsg: String
        var amount = amnt
        val sheet = workbook?.getSheetAt(0)
        val row = sheet?.let { getUserRowFolio(folio, it) }
            ?: return Response.error("Could not find the a member with folio number: $folio", "")
        val emptyCellIndex = getEmptyCellIndex(sheet, folio)
        val startCell = row.getCell(emptyCellIndex - 1)
        val spillOver = (getCellvalue(startCell).toDouble() + amount) - 60.0
        Log.i("TAG", "Spill over: $spillOver")
        val initialGrandTotal = getGrandTotal().toString()
        val userTotal = getUserTotal(row.rowNum)
        try {

            if (spillOver > 0) {
                if ((row.lastCellNum - 1) - emptyCellIndex == 0) {
                    Log.i("TAG", "Moving to next cell, cell: ${startCell.columnIndex + 1}")
                    val numCol = ceil(spillOver / 60).toInt()
                    val numNewColumns = numCol - (row.lastCellNum - (emptyCellIndex + 1))

                    if (numNewColumns > 0) {
                        Log.i("TAG", "Number of columns to add = $numNewColumns")
                        addColumns(row.lastCellNum - 1, numNewColumns)
                    }
                }
            }

            Log.i("TAG", "Inserting amounts into cells")
            for (i in emptyCellIndex - 1 until row.lastCellNum - 1) {
                val cell = row.getCell(i)
                val cValue = getCellvalue(cell).toDouble()
                var amountToAdd = 60.0 - cValue
                Log.i("TAG", "Cell $i initial value = $cell")
                if (amount < amountToAdd) {
                    amountToAdd = amount
                }
                row.getCell(i).setCellValue(amountToAdd + cValue)
                amount -= amountToAdd
                Log.i("TAG", "Ammount added = $amountToAdd")
            }
            Log.i("TAG", "Payment update made successfully")

            workbook?.creationHelper?.createFormulaEvaluator()?.evaluateAll()

            var isCorrectGrandT = false
            var isCorrectUserT = false

            var errorMsg = ""

            val grandTotal = getGrandTotal()
            val expT = amnt + initialGrandTotal.toDouble()
            val expUT = amnt + userTotal
            val userT = getUserTotal(row.rowNum)

            if (expT != grandTotal) {
                errorMsg = "Final Grand Total did not equal Expected Grand Total\n\n" +
                        "Expected Grand Total = ${amnt + initialGrandTotal.toDouble()}\n" +
                        "Final Grand total = $grandTotal\n\n" +
                        "CLOSE THE APP AND TRY AGAIN"
            }

            if (expUT != userT) {
                errorMsg = "User final total did not equal expected User final total\n\n" +
                        "Expected User Total = ${amnt + userTotal}\n" +
                        "Final User Total = ${getUserTotal(row.rowNum)}\n\n" +
                        "CLOSE THE APP AND TRY AGAIN"
            }

            if (errorMsg.isNotEmpty()) {
                return Response.error(errorMsg, "")
            }
            return Response.success("")
        } catch (e: Exception) {
            e.printStackTrace()
            errMsg =
                e.localizedMessage ?: "An Unknown Error occurred while Doing the payment update."
        }
        return Response.error(errMsg, "")
    }

    fun saveFile() {
        val duesDir = File(applicationContext().filesDir, "Dues")
        try {
            val out1 = FileOutputStream(File(duesDir, "Nabiadues.xlsx"))
            workbook?.write(out1)
            out1.close()
            Log.i("TAG", "File saved")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        reloadExcel()
    }

    fun saveToTemporalStorage() {
        try {
            val backupDir =
                File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")
            val fileName = "temp_save.xlsx"

            if (!backupDir.exists()) {
                backupDir.mkdir()
            }
            val file = File(backupDir, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            val out2 = FileOutputStream(file)
            workbook?.write(out2)
            out2.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun backupDues() {
        val time = System.currentTimeMillis()
        try {
            val backupDir =
                File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")
            val fileName = "$time.xlsx"

            if (!backupDir.exists()) {
                backupDir.mkdir()
            }
            val file = File(backupDir, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            val out2 = FileOutputStream(file)
            workbook?.write(out2)
            out2.close()

            val backup = EntityDuesBackup(time)
            backup.fileFullPath = file.absolutePath
            backup.filePath = backupDir.absolutePath
            backup.fileName = fileName
            backup.totalAmount = getGrandTotal().toString()

            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                duesBackupDao.insert(backup)
                Log.i("TAG", "Backup saved")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getEmptyCellIndex(sheet: Sheet, folio: String): Int {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val row = getUserRowFolio(folio, sheet)
        var lastCellNum = 0
        if (row != null) {
            val headerRow = sheet.getRow(0)
            for (index in 0..row.lastCellNum) {
                val celVal = formatter.formatCellValue(headerRow.getCell(index))
                if (celVal.toString() == year.toString()) {
                    for (i in index until row.lastCellNum) {
                        val cell = row.getCell(i)
                        Log.i("TAG", "Cell $i value: ${getCellvalue(row.getCell(i))}")
                        if (cell.cellTypeEnum == CellType.FORMULA || getCellvalue(cell).toDouble() == 0.0) {
                            Log.i("TAG", "Empty cell index: $i")
                            return i
                        }
                    }
                    break
                }
            }
            lastCellNum = row.lastCellNum - 1
        }
        Log.i("TAG", "Empty cell index: $lastCellNum")
        return lastCellNum
    }

    private fun addColumns(colIndex: Int, numCol: Int) {
        var columnIndex = colIndex
        val evaluator = workbook?.creationHelper
            ?.createFormulaEvaluator()
        evaluator?.clearAllCachedResultValues()
        val sheet = workbook?.getSheetAt(0)

        Log.i("TAG", "$numCol Culomns to Insert")

        for (i in 0 until numCol) {
            val nrRows: Int = (sheet?.lastRowNum ?: 0) + 1
            val nrCols: Int = sheet?.getRow(0)?.lastCellNum?.toInt() ?: 0
            Log.i("TAG", "Inserting new column at $columnIndex")
            for (row in 0 until nrRows) {
                val r = sheet?.getRow(row) ?: continue
                // shift to right
                for (col in nrCols downTo columnIndex + 1) {
                    val rightCell = r.getCell(col)
                    if (rightCell != null) {
                        r.removeCell(rightCell)
                    }
                    val leftCell = r.getCell(col - 1)
                    if (leftCell != null) {
                        val newCell = r.createCell(col)
                        cloneCell(newCell, leftCell)
                        if (r.rowNum != 0) {
                            if (newCell.cellTypeEnum == CellType.FORMULA) {
                                newCell.cellFormula =
                                    updateFormula(newCell.cellFormula, columnIndex - 1)
                                evaluator?.notifySetFormula(newCell)
                                val cellValue = evaluator?.evaluate(newCell)
                                evaluator?.evaluateFormulaCell(newCell)
                                println(cellValue)
                            }
                        }
                        if (r.rowNum == sheet.lastRowNum) {
                            if (newCell.cellTypeEnum == CellType.FORMULA) {
                                newCell.cellFormula =
                                    updateFormula(newCell.cellFormula, columnIndex)
                                evaluator?.notifySetFormula(newCell)
                                evaluator?.evaluateFormulaCell(newCell)
                            }
                        }
                    }
                }
                // delete old column
                val currentEmptyWeekCell = r.getCell(columnIndex)
                if (r.rowNum == 0) {
                    val cYear = formatter.formatCellValue(r.getCell(columnIndex - 1)).toInt() + 1
                    currentEmptyWeekCell.setCellValue(cYear.toDouble())
                } else if (r.rowNum < sheet.lastRowNum) {
                    r.removeCell(currentEmptyWeekCell)
                    r.createCell(columnIndex).setCellValue(0.0)
                }

            }
            columnIndex++
            Log.i("TAG", "Column inserted")
        }

    }

    suspend fun insertNewPayment(
        amount: Int,
        name: String,
        folio: String,
        pos: Int
    ): Response<String> {

        return withContext(Dispatchers.Default) {
            if (pos > 0) {
                return@withContext upDatePayment(amount.toDouble(), folio)
            }
            val response = createNewPaymentRow(name, folio)
            if (response.status == Status.SUCCESS) {
                upDatePayment(amount.toDouble(), folio)
            } else {
                Response.error(response.message.toString(), "")
            }
        }
    }

    private fun createNewPaymentRow(name: String, folio: String): Response<String> {
        val sheet = workbook?.getSheetAt(0)
        val lastCellNum = sheet?.getRow(0)?.lastCellNum
        val row = sheet?.createRow(sheet.lastRowNum - 1)
        row?.createCell(0)?.setCellValue(row.rowNum.toString())
        row?.createCell(1)?.setCellValue(name)
        row?.createCell(2)?.setCellValue(folio.toDouble())

        for (n in 3 until lastCellNum!!) {
            row?.createCell(n)?.setCellValue(0.0)
        }

        val lastColName = CellReference.convertNumToColString(lastCellNum - 2)
        val formula = "SUM(D${(row?.rowNum ?: 0) + 1}:$lastColName${(row?.rowNum ?: 0) + 1})"
        if (row != null) {
            row.createCell(lastCellNum - 1).cellFormula = formula
        }

        members.add(Member(folio, name, 0.0))
        return Response.success("DONE")

    }

    fun getRank(index: Int): Int? {
        val amount = members[index].totalAmount

        val localList: List<Member> = members.toList()
        Collections.sort(
            localList,
            Comparator(fun(first: Member, second: Member): Int {
                return if (second.totalAmount <= first.totalAmount) -1 else 1
            })
        )
        for (i in localList.indices) {
            if (localList[i].totalAmount == amount) return i + 1
        }
        return null
    }

    fun getUserTotal(rowIndex: Int): Double {
        Log.i("TAG", "Row index: $rowIndex")
        val row = workbook?.getSheetAt(0)?.getRow(rowIndex)
        if (row != null) {
            return row.getCell(row.lastCellNum - 1).numericCellValue
        }
        return 0.0
    }

    fun getUserTotalByName(n: String): Double? {
        val sheet = workbook?.getSheetAt(0)
        if (sheet != null) {
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i)
                val name: String = formatter.formatCellValue(row.getCell(1))
                if (name == n) {
                    return row.getCell(row.lastCellNum - 1).numericCellValue
                }
            }
        }
        return null
    }

    fun getGrandTotal(): Double {
        val sheet = workbook?.getSheetAt(0)
        val row = sheet?.getRow(sheet.lastRowNum)
        if (row != null) {
            return row.getCell(row.lastCellNum - 1).numericCellValue
        }
        return 0.0
    }


    class Member(mfolio: String, mName: String, amount: Double) {
        val folio = mfolio
        val name = mName
        var totalAmount = amount
    }

    private fun updateFormula(cellFormula: String, columnIndex: Int): String {
        val existingColName = CellReference.convertNumToColString(columnIndex)
        val newColName = CellReference.convertNumToColString(columnIndex + 1)
        var newCellFormula = cellFormula.replace(existingColName, newColName)

        if (existingColName.equals("S")) {
            newCellFormula = newCellFormula.replaceBefore("(", "SUM")
        }
        if (existingColName.equals("U")) {
            newCellFormula = newCellFormula.replaceBefore("(", "SUM")
        }
        if (existingColName.equals("M")) {
            newCellFormula = newCellFormula.replaceBefore("(", "SUM")
        }

        println(
            "Replacing : " + existingColName + " with : " + newColName + " in "
                    + cellFormula + ", result: " + newCellFormula
        )
        return newCellFormula
    }

    private fun cloneCell(cNew: Cell, cOld: Cell) {
        cNew.cellComment = cOld.cellComment
        cNew.cellStyle = cOld.cellStyle
        when (cOld.cellTypeEnum) {
            CellType.BOOLEAN -> {
                cNew.setCellValue(cOld.booleanCellValue)
            }
            CellType.NUMERIC -> {
                cNew.setCellValue(cOld.numericCellValue)
            }
            CellType.STRING -> {
                cNew.setCellValue(cOld.stringCellValue)
            }
            CellType.ERROR -> {
                cNew.setCellErrorValue(cOld.errorCellValue)
            }
            CellType.FORMULA -> {
                cNew.cellFormula = cOld.cellFormula
            }
            CellType.BLANK -> {
                cNew.setCellValue(cOld.stringCellValue)
            }
            else -> {
                cNew.setCellValue("")
            }
        }

    }

    fun getHeader(): List<String> {
        val list: MutableList<String> = ArrayList()
        val sheet = workbook?.getSheetAt(0)
        val row = sheet?.getRow(0)
        val i = (row?.lastCellNum ?: 0) - 6
        if (row != null) {
            for (item in i..row.lastCellNum - 2) {
                list.add(formatter.formatCellValue(row.getCell(item)))
            }
        }
        return list
    }

}

