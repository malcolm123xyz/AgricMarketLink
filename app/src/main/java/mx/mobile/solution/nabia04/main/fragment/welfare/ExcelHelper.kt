package mx.mobile.solution.nabia04.main.fragment.welfare

import android.content.Context
import android.os.Environment
import android.util.Log
import mx.mobile.solution.nabia04.utilities.Cons
import org.apache.poi.hssf.util.CellReference
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.util.*


class ExcelHelper(val context: Context?, val folio: String) {

    private var formatter = DataFormatter()
    private var OVERRALL_TOTAL: Int = 0
    private var workbook: Workbook? = null
    private lateinit var totals: MutableList<TotalAmount>

    companion object {
        var totalAmount = 0.00
        var userTotalAmount = 0.00
        var numMonths = 0
        var numMonthsOwed = 0

        @Volatile
        private var INSTANCE: ExcelHelper? = null
        fun getInstance(context: Context?, folio: String): ExcelHelper? {
            if (INSTANCE == null) {
                synchronized(ExcelHelper::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = ExcelHelper(context, folio)
                    }
                }
            }
            return INSTANCE
        }
    }

    init {
        workbook = getWorkBook()
        getTotals()
        totalAmount = getOverallTotal()
        userTotalAmount = getUserTotal(folio)
        numMonths = getUserNumMonths(folio)
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val currYearMonths = 12 - month
        val totalMonths = (12 * Cons.DUES_NUM_YEARS) - (7 + currYearMonths)
        numMonthsOwed = totalMonths - numMonths
    }

    private fun getExcelFile(): File? {
        val ourAppFileDirectory = File(Environment.getExternalStorageDirectory().absolutePath)
        ourAppFileDirectory.let {
            //Check if file exists or not
            if (it.exists()) {
                return File(ourAppFileDirectory, "Nabia_dues.xlsx")
            }
        }
        return null
    }

    private fun getWorkBook(): Workbook? {
        //Reading the workbook from the loaded spreadsheet file
        getExcelFile()?.let {
            try {
                Log.i("TAG", "File path: "+it.absolutePath)
                //Reading it as stream
                val workbookStream = FileInputStream(it)
                //Return the loaded workbook
                return WorkbookFactory.create(workbookStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //the workbook may not exist
        return null
    }

    private fun getSheet(sheetNumb: Int): Sheet? {
        //choosing the workbook
        workbook.let { workbook ->
            //Checking the existence of a sheet
            if (workbook?.numberOfSheets!! > 0) {
                //Return the first sheet
                return workbook.getSheetAt(sheetNumb)
            }
        }

        return null
    }

    fun getAllSheets(): MutableList<Sheet> {
        val sheets: MutableList<Sheet> = ArrayList()
        //choosing the workbook
        workbook?.let { workbook ->
            var i = 0
            while (i < workbook.numberOfSheets) {
                sheets.add(workbook.getSheetAt(i))
                i++
            }
        }
        return sheets
    }

    fun getUserNumMonths(folio: String): Int{
        var numMonths = 0
        val sheets = getAllSheets()
        for (sheet in sheets){
            for (row in sheet) {
                val folioCell = row.getCell(2)
                val folioValue = formatter.formatCellValue(folioCell)
                if(folioValue == folio){
                    for (cell in row) {
                        val cellValue = formatter.formatCellValue(cell)
                        if (cell.columnIndex > 2 &&
                            cellValue.isNotEmpty() &&
                            cell.cellTypeEnum != CellType.FORMULA &&
                            cellValue.toInt() > 0
                        ) {
                            numMonths++
                        }
                    }
                    break
                }
            }
        }
        return numMonths
    }

    inner class TotalAmount(mfolio: String, mAmount: Double) {
        val folio = mfolio
        var amount = mAmount
    }

    private fun getTotals() {
        val sheets = getAllSheets()
        totals = ArrayList()
        for (sheet in sheets) {
            for (row in sheet) {
                if (row.rowNum != sheet.lastRowNum) {
                    val folioValue = formatter.formatCellValue(row.getCell(2))
                    if (row.getCell(15)?.cellTypeEnum == CellType.FORMULA) {
                        val amountToAdd = row.getCell(15).numericCellValue
                        var found = false
                        for (user in totals) {
                            if (user.folio == folioValue) {
                                val amount = user.amount
                                user.amount = amount + amountToAdd
                                found = true
                                break
                            }
                        }
                        if (!found) {
                            totals.add(TotalAmount(folioValue, amountToAdd))
                        }
                    }
                }
            }
        }
    }


    fun getRank(amount: Double): Int? {
        if (totals == null) {
            Log.i("TAG", "Totals is null")
        } else {
            Log.i("TAG", "Totals size " + totals.size)
        }
        val localList: List<TotalAmount> = totals.toList()
        Collections.sort(
            localList,
            Comparator<TotalAmount>(fun(first: TotalAmount, second: TotalAmount): Int {
                return if (second.amount <= first.amount) -1 else 1
            })
        )
        for (i in localList.indices) {
            if (localList[i].amount == amount) return i + 1
        }
        return null
    }

    fun getUserTotal(folio: String): Double {
        var totalAmount = 0.00
        val sheets = getAllSheets()
        for (sheet in sheets) {
            for (row in sheet) {
                val cell = row.getCell(2)
                val folioValue = formatter.formatCellValue(cell)
                if (folioValue == folio) {
                    totalAmount += row.getCell(15).numericCellValue
                    break
                }
            }
        }
        return totalAmount
    }

    fun getYearTotal(sheetNum: Int): Double {
        val sheet = getSheet(sheetNum)
        val lastRow = sheet?.lastRowNum
        val totalAmountCell = sheet?.getRow(lastRow!! )?.getCell(15)
        return totalAmountCell?.numericCellValue ?: 0.00
    }

    fun getOverallTotal (): Double {
        var total = 0.00
        for (i in 0 until Cons.DUES_NUM_SHEET) {
            val sheet = getSheet(i)
            val lastRow = sheet?.lastRowNum
            val totalAmountCell = sheet?.getRow(lastRow!! )?.getCell(15)
            total += totalAmountCell?.numericCellValue!!
        }
        return total
    }

    fun getNumPaid(sheetNum: Int): String {
        val sheet = getSheet(sheetNum)
        val lastRow = sheet?.lastRowNum?.minus(1) ?: 0
        var counter = 0
        for (i in lastRow downTo 1) {
            val cell = sheet?.getRow(i)?.getCell(15)
            if(cell?.cellTypeEnum == CellType.FORMULA){
                val value = cell.numericCellValue.toInt()
                if(value > 0){
                    counter++
                }
            }
        }
        Log.i("TAG", "Number payments = $counter")

        return counter.toString()
    }

    fun readSheet(sheetNum: Int){

        val sheet1 = getSheet(sheetNum) ?: return

        for (row in sheet1) {
            for (cell in row) {
                val cellRef = CellReference(row.rowNum, cell.columnIndex)
                System.out.print(cellRef.formatAsString())
                print(" - ")
                // get the text that appears in the cell by getting the cell value and applying any data formats (Date, 0.00, 1.23e9, $1.23, etc)
                val text: String = formatter.formatCellValue(cell)
                println(text)
            }
        }
    }
}