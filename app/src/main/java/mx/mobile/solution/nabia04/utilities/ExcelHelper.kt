package mx.mobile.solution.nabia04.utilities

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.data.entities.EntityDues
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellReference
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


@OptIn(DelicateCoroutinesApi::class)
@Singleton
class ExcelHelper @Inject constructor() {

    private val LETTERS_IN_EN_ALFABET = 26.toChar()
    private val BASE = LETTERS_IN_EN_ALFABET
    private val A_LETTER = 65.toChar()

    var numNumYears = 0
    private val ourAppFileDirectory = File(Environment.getExternalStorageDirectory().absolutePath)
    private var formatter = DataFormatter()
    private lateinit var workbook: Workbook
    var totalNumMonths = 0.00
    private var isCreated = false
    var members: MutableList<Member> = ArrayList()
    var names: MutableList<String> = ArrayList()

    @OptIn(ExperimentalTime::class)
    fun createExcel() {
        if (!isCreated) {
            Log.i("TAG", "Creating excel...")
            val time = measureTime {
                createWorkBook()
                getPaidMembers()
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH)
                val currYearMonths = 12 - month
                numNumYears = getNumYears()
                totalNumMonths = ((12 * numNumYears) - (7 + currYearMonths)).toDouble()
                isCreated = true
            }
            Log.i("TAG", "Excel created, (took $time ms)")
        } else {
            Log.i("TAG", "Excel already created...")
        }
    }

    fun reload() {
        createWorkBook()
        getPaidMembers()
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val currYearMonths = 12 - month
        numNumYears = getNumYears()
        totalNumMonths = ((12 * numNumYears) - (7 + currYearMonths)).toDouble()
        isCreated = true
    }

    private fun getNumYears(): Int {
        val row = workbook.getSheetAt(0).getRow(0)
        return row.lastCellNum - 4
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
            val sheet = workbook.getSheetAt(0)
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
            return@withContext duesList
        }
    }

    private fun getCellvalue(cell: Cell): String {
        return formatter.formatCellValue(cell)
    }

    private fun upDatePayment1(amnt: Double, folio: String): Resource<String> {
        val errMsg: String
        val amount = amnt
        val sheet = workbook.getSheetAt(0)
        val row = getUserRowFolio(folio, sheet)
            ?: return Resource.error("Could not find the a member with folio number: $folio", "")
        val currYearNum = getCurrentYearNum(sheet)
        val startCell = row.getCell(currYearNum)
        var spillOver = (getCellvalue(startCell).toDouble() + amount) - 60.0
        Log.i("TAG", "Spill over: $spillOver")
        if (spillOver > 0) {
            startCell.setCellValue(60.0)
            Log.i("TAG", "Moving to next cell, cell: ${startCell.columnIndex + 1}")
            val nextCell = row.getCell(startCell.columnIndex + 1)
            val nextCellIndex = nextCell.columnIndex
            val numCol = ceil(spillOver / 60).toInt()
            val numAvailableCell = numCol - (row.lastCellNum - startCell.columnIndex + 1)


            if (numAvailableCell > 0) {
                Log.i("TAG", "Next cell contains formular. Inserting new columns")

                Log.i("TAG", "Number of columns to add = $numCol")
                addYear(nextCellIndex, numCol)
                Log.i("TAG", "Inserting spilled amounts into created cells")
                for (i in 0 until nextCellIndex + numCol) {
                    Log.i("TAG", "Cell $i initial value = ${row.getCell(i)}")
                    var amountToAdd = 60.0
                    if (spillOver < 60.0) {
                        amountToAdd = spillOver
                    }
                    row.getCell(i).setCellValue(amountToAdd)
                    spillOver -= 60.0
                    Log.i("TAG", "Ammount added = $amountToAdd")
                }
            } else {

            }

        }
        return null
    }


    private fun upDatePayment(amnt: Int, folio: String): Resource<String> {
        val errMsg: String
        var amount = amnt
        val sheet = workbook.getSheetAt(0)
        val row = getUserRowFolio(folio, sheet)
            ?: return Resource.error("Could not find the a member with folio number: $folio", "")
        val currYearNum = getCurrentYearNum(sheet)
        var entryCell = row.getCell(currYearNum)

        try {
            while (amount > 0) {
                Log.i("TAG", "Amount = $amount")
                val entryCellValue = getCellvalue(entryCell).toDouble()
                Log.i("TAG", "Initial cell value: $entryCellValue")
                if (entryCellValue == 60.0) {
                    Log.i("TAG", "Cell amount is greater than 60")

                    val nextCell = row.getCell(row.lastCellNum - 2)
                    val col = CellReference.convertNumToColString(nextCell.columnIndex)
                    Log.i("TAG", "Next cell is: ($col:${row.rowNum})")

                    val col1 = CellReference.convertNumToColString(entryCell.columnIndex)
                    Log.i("TAG", "Entry cell is: ($col1:${row.rowNum})")

                    Log.i("TAG", "Next cell: $nextCell")
                    var addColumn = false

                    if (nextCell.cellTypeEnum == CellType.FORMULA) {
                        addColumn = true
                    } else if (getCellvalue(nextCell).isNotEmpty()) {
                        addColumn = true
                        if (getCellvalue(nextCell).toInt() < 60) {
                            addColumn = false
                        }
                    }


                    Log.i("TAG", "entryCell = ${getCellvalue(entryCell).toInt()}")

                    Log.i("TAG", "addColumn = $addColumn")

                    if (addColumn) {
                        Log.i("TAG", "Next cell is not empty, Additional cell is required")
                        addYear(row.lastCellNum - 1, 1)
                    }

                    entryCell = row.getCell(row.lastCellNum - 2)
                    val colName = CellReference.convertNumToColString(entryCell.columnIndex)
                    Log.i("TAG", "New cell is: ($colName:${row.rowNum})")
                    if (getCellvalue(entryCell).isEmpty()) {
                        entryCell.setCellValue(0.0)
                    }
                }

                val currVal = getCellvalue(entryCell).toDouble() + 5.0
                entryCell.setCellValue(currVal)
                Log.i("TAG", "Cell value after: ${getCellvalue(entryCell)}")
                amount -= 5
            }

            workbook.creationHelper.createFormulaEvaluator().evaluateAll()
            val out = FileOutputStream(File(ourAppFileDirectory, "Nabiadues.xlsx"))
            workbook.write(out)
            out.close()
            Log.i("TAG", "Payment update made successfully")
            return Resource.success("")
        } catch (e: Exception) {
            e.printStackTrace()
            errMsg =
                e.localizedMessage ?: "An Unknown Error occurred while Doing the payment update."
        }
        return Resource.error(errMsg, "")
    }

    private fun getCurrentYearNum(sheet: Sheet): Int {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val row = sheet.getRow(0)
        for (c in row) {
            val celVal = formatter.formatCellValue(c)
            if (celVal.toString() == year.toString()) {
                val celIndex = c.columnIndex
                var emptyCellIndex = 0
                for (i in celIndex until row.lastCellNum) {
                    if (row.getCell(i).cellTypeEnum == CellType.BLANK) {
                        emptyCellIndex = i
                        break
                    }

                }
                return c.columnIndex
            }
        }
        return 7
    }

    fun doSomething() {

        val carryon = 190.0
        val yearPayment = 60.0
        val total = 3.00

        Log.i("TAG", "TOTAL = ${ceil(190.0 / 60.0)}")


//
//         if(total % 1 == 0){
//             Log.i("TAG", "TOTAL IS GREATER THAN 3")
//         }else if (total == 3.0){
//             Log.i("TAG", "TOTAL IS EQUAL TO 3")
//         }else if(total < 3.0){
//             Log.i("TAG", "TOTAL IS LESS THAN 3")
//         }
    }

    private fun addYear(colIndex: Int, numCol: Int) {
        var columnIndex = colIndex
        val evaluator = workbook.creationHelper
            .createFormulaEvaluator()
        evaluator.clearAllCachedResultValues()
        val sheet = workbook.getSheetAt(0)

        Log.i("TAG", "$numCol Culomns to Insert")

        for (i in 0 until numCol) {
            val nrRows: Int = sheet.lastRowNum + 1
            val nrCols: Int = sheet.getRow(0).lastCellNum.toInt()
            Log.i("TAG", "Inserting new column at $columnIndex")
            for (row in 0 until nrRows) {
                val r = sheet.getRow(row) ?: continue
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
                                evaluator.notifySetFormula(newCell)
                                val cellValue = evaluator.evaluate(newCell)
                                evaluator.evaluateFormulaCell(newCell)
                                println(cellValue)
                            }
                        }
                        if (r.rowNum == sheet.lastRowNum) {
                            if (newCell.cellTypeEnum == CellType.FORMULA) {
                                newCell.cellFormula =
                                    updateFormula(newCell.cellFormula, columnIndex)
                                evaluator.notifySetFormula(newCell)
                                evaluator.evaluateFormulaCell(newCell)
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
                    r.createCell(columnIndex)
                }

            }
            columnIndex++
            Log.i("TAG", "Column added $i Created")
        }

    }

    suspend fun insertNewPayment(
        amount: Int,
        name: String,
        folio: String,
        pos: Int
    ): Resource<String> {

        return withContext(Dispatchers.Default) {
            if (pos > 0) {
                return@withContext upDatePayment(amount, folio)
            }

            val response = createNewPaymentRow(name, folio)
            if (response.status == Status.SUCCESS) {
                upDatePayment(amount, folio)
            } else {
                Resource.error(response.message.toString(), "")
            }
        }
    }

    private fun createNewPaymentRow(name: String, folio: String): Resource<String> {
        val errMsg: String
        val sheet = workbook.getSheetAt(0)
        val lastCellNum = sheet.getRow(0).lastCellNum
        val row = sheet.createRow(sheet.lastRowNum - 1)
        row.createCell(0).setCellValue(row.rowNum.toString())
        row.createCell(1).setCellValue(name)
        row.createCell(2).setCellValue(folio.toDouble())

        for (n in 3..lastCellNum) {
            row.createCell(n)
        }

        val lastColName = CellReference.convertNumToColString(lastCellNum - 2)
        val formula = "SUM(D${row.rowNum + 1}:$lastColName${row.rowNum + 1})"
        row.createCell(lastCellNum - 1).cellFormula = formula

        members.add(Member(folio, name, 0.0))

        workbook.creationHelper.createFormulaEvaluator().evaluateAll()
        try {
            val out = FileOutputStream(File(ourAppFileDirectory, "Nabiadues.xlsx"))
            workbook.write(out)
            out.close()
            Log.i("TAG", "New payment made successfully")
            return Resource.success("DONE")
        } catch (e: Exception) {
            e.printStackTrace()
            errMsg =
                e.localizedMessage ?: "An Unknown Error occurred while Creating new Row for member"
        }
        return Resource.error(errMsg, "")
    }

    private fun getPaidMembers() {
        members.add(Member("", "", 0.00))
        names.add("Select member")
        val sheet = workbook.getSheetAt(0)
        for (i in 1 until sheet.lastRowNum) {
            val name: String = formatter.formatCellValue(sheet.getRow(i).getCell(1))
            val folio: String = formatter.formatCellValue(sheet.getRow(i).getCell(2))
            val amount = sheet.getRow(i).getCell(sheet.getRow(i).lastCellNum - 1).numericCellValue
            members.add(Member(folio, name, amount))
            names.add(name)
        }
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

    private fun getExcelFile(): File? {
        ourAppFileDirectory.let {
            //Check if file exists or not
            if (it.exists()) {
                return File(ourAppFileDirectory, "Nabiadues.xlsx")
            }
        }
        return null
    }

    private fun createWorkBook() {
        getExcelFile()?.let {
            try {
                val workbookStream = FileInputStream(it)
                workbook = WorkbookFactory.create(workbookStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getUserTotal(rowIndex: Int): Double {
        val row = workbook.getSheetAt(0).getRow(rowIndex)
        return row.getCell(row.lastCellNum - 1).numericCellValue
    }

    fun getUserTotalByName(n: String): Double? {
        val sheet = workbook.getSheetAt(0)
        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i)
            val name: String = formatter.formatCellValue(row.getCell(1))
            if (name == n) {
                return row.getCell(row.lastCellNum - 1).numericCellValue
            }
        }
        return null
    }

    fun getGrandTotal(): Double {
        val sheet = workbook.getSheetAt(0)
        val row = sheet.getRow(sheet.lastRowNum)
        return row.getCell(row.lastCellNum - 1).numericCellValue
    }


    class Member(mfolio: String, mName: String, amount: Double) {
        val folio = mfolio
        val name = mName
        var totalAmount = amount
    }

    fun updateFormula(cellFormula: String, columnIndex: Int): String {
        val existingColName = CellReference.convertNumToColString(columnIndex)
        val newColName = CellReference.convertNumToColString(columnIndex + 1)
        val newCellFormula = cellFormula.replace(existingColName, newColName)
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
        val sheet = workbook.getSheetAt(0)
        val row = sheet.getRow(0)
        val i = row.lastCellNum - 6
        for (item in i..row.lastCellNum - 2) {
            list.add(formatter.formatCellValue(row.getCell(item)))
        }
        return list
    }

    fun restoreWorkBook() {
        var workbook1: Workbook? = null
        var file: File? = null

        try {
            ourAppFileDirectory.let {
                //Check if file exists or not
                if (it.exists()) {
                    file = File(ourAppFileDirectory, "Nabiadues_backup.xlsx")
                }
            }

            val workbookStream = FileInputStream(file)
            workbook1 = WorkbookFactory.create(workbookStream)
            val out = FileOutputStream(File(ourAppFileDirectory, "Nabiadues.xlsx"))
            workbook1?.write(out)
            out.close()
            Log.i("TAG", "Workbook restored")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}