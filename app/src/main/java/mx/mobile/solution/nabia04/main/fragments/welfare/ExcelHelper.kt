package mx.mobile.solution.nabia04.main.fragments.welfare

import android.content.Context
import android.os.Environment
import mx.mobile.solution.nabia04.main.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.utilities.Cons
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.util.*


class ExcelHelper(val context: Context?) {

    private var formatter = DataFormatter()
    private var workbook: Workbook? = null
    private var totalAmount = 0.00
    private var selYeartotal = 0.00
    private var userTotalAmount = 0.00
    private var userNumMonths = 0.00
    private var numMonthsOwed = 0.00
    private var totalMonths = 0.00
    public var sheets: MutableList<Sheet> = ArrayList()

    //private var workbook: Workbook? = null
    private lateinit var totals: MutableList<TotalAmount>

    companion object {
        var namesList: ArrayList<String> = ArrayList()
        var folioList: ArrayList<String> = ArrayList()

        @Volatile
        private var INSTANCE: ExcelHelper? = null
        fun getInstance(context: Context?): ExcelHelper? {
            if (INSTANCE == null) {
                synchronized(ExcelHelper::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = ExcelHelper(context)
                    }
                }
            }
            return INSTANCE
        }
    }

    init {
        createWorkBook()
        getTotals()
        totalAmount = getOverallTotal()
        userTotalAmount = getUserTotal(userFolioNumber)
        userNumMonths = getUserNumMonths(userFolioNumber)
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val currYearMonths = 12 - month
        totalMonths = ((12 * Cons.DUES_NUM_YEARS) - (7 + currYearMonths)).toDouble()
        numMonthsOwed = totalMonths - userNumMonths
    }

    fun getWorkbook(): Workbook {
        return workbook!!
    }

    fun reloadUserData(index: Int): String {

        var folio = ""

        if (index > -1) {
            folio = folioList[index]
        } else {
            folio = userFolioNumber
        }

        userTotalAmount = getUserTotal(folio)
        userNumMonths = getUserNumMonths(folio)

        return folio
    }

    inner class TotalAmount(mfolio: String, mAmount: Double) {
        val folio = mfolio
        var amount = mAmount
    }

    private fun getTotals() {
        totals = ArrayList()
        namesList.add("MY DUES")
        for (sheet in sheets) {
            for (row in sheet) {
                if (row.rowNum != sheet.lastRowNum) {
                    if (row.getCell(15)?.cellTypeEnum == CellType.FORMULA) {
                        val folioValue = formatter.formatCellValue(row.getCell(2))
                        val name = formatter.formatCellValue(row.getCell(1))
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
                            folioList.add(folioValue)
                            namesList.add(name)
                        }
                    }
                }
            }
        }
    }

    fun getRank(amount: Double): Int? {
        val localList: List<TotalAmount> = totals.toList()
        Collections.sort(
            localList,
            Comparator(fun(first: TotalAmount, second: TotalAmount): Int {
                return if (second.amount <= first.amount) -1 else 1
            })
        )
        for (i in localList.indices) {
            if (localList[i].amount == amount) return i + 1
        }
        return null
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

    private fun createWorkBook() {
        getExcelFile()?.let {
            try {
                val workbookStream = FileInputStream(it)
                workbook = WorkbookFactory.create(workbookStream)
                for (i in 0 until workbook!!.numberOfSheets) {
                    sheets.add(workbook!!.getSheetAt(i))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getUserNumMonths(folio: String): Double {
        var numMonths = 0.00
        for (sheet in sheets) {
            for (row in sheet) {
                val folioCell = row.getCell(2)
                val folioValue = formatter.formatCellValue(folioCell)
                if (folioValue == folio) {
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

    private fun getUserTotal(folio: String): Double {
        var totalAmount = 0.00
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
        val sheet = sheets[sheetNum]
        val lastRow = sheet.lastRowNum
        val totalAmountCell = sheet.getRow(lastRow)?.getCell(15)
        return totalAmountCell?.numericCellValue ?: 0.00
    }

    private fun getOverallTotal(): Double {
        var total = 0.00
        for (i in 0 until Cons.DUES_NUM_SHEET) {
            val sheet = sheets[i]
            val lastRow = sheet.lastRowNum
            val totalAmountCell = sheet.getRow(lastRow)?.getCell(15)
            total += totalAmountCell?.numericCellValue!!
        }
        return total
    }

    fun getNumPaid(sheetNum: Int): String {
        val sheet = sheets[sheetNum]
        val lastRow = sheet.lastRowNum.minus(1)
        var counter = 0
        for (i in lastRow downTo 1) {
            val cell = sheet.getRow(i)?.getCell(15)
            if (cell?.cellTypeEnum == CellType.FORMULA) {
                val value = cell.numericCellValue.toInt()
                if (value > 0) {
                    counter++
                }
            }
        }

        return counter.toString()
    }

    fun getPercentagePayment(): Int {
        return ((userNumMonths / totalMonths) * 100).toInt()
    }

    fun getNumMonthsPaid(): Int {
        return ((userNumMonths / totalMonths) * 100).toInt()
    }

    fun getNumMonthsOwed(): Double {
        return numMonthsOwed
    }

    fun getUserTotalAmount(): Double {
        return userTotalAmount
    }

}