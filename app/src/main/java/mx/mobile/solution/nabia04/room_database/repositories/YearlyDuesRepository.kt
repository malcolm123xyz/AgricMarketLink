package mx.mobile.solution.nabia04.room_database.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelper
import mx.mobile.solution.nabia04.main.fragments.host_fragments.TreasurerPaymentDetailHost.Companion.SELECTED_YEAR
import mx.mobile.solution.nabia04.room_database.DuesDetailDao
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter


class YearlyDuesRepository(context: Context) {
    private val years = arrayOf("2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025")

    companion object {
        private const val TAG = "AnnDataRepository"
        private var dues: MutableList<EntityYearlyDues>? = null
        private var dao: DuesDetailDao? = null
    }

    init {
        dao = MainDataBase.getDatabase(context).duesDetailsDao()
    }

    suspend fun refreshVideos() {
        withContext(Dispatchers.IO) {
            setUpExcelDB()
        }
    }

    suspend fun getDataFrmDB(year: String) {
        withContext(Dispatchers.IO) {
            dues = dao?.getThisYearDues(year)
        }
    }


    private fun setUpExcelDB() {
        val formatter = DataFormatter()
        for ((sheetNum, sheet) in excelHelper.sheets.withIndex()) {
            val duesEntityList: MutableList<EntityYearlyDues> = ArrayList()
            for (row in sheet) {
                val duesItem = EntityYearlyDues()
                for ((index, cell) in row.withIndex()) {
                    duesItem.year = years[sheetNum]
                    when (index) {
                        0 -> duesItem.index = formatter.formatCellValue(cell)
                        1 -> duesItem.name = formatter.formatCellValue(cell)
                        2 -> duesItem.folio = formatter.formatCellValue(cell)
                        else -> {
                            val i = index - 3
                            if (i > 12) {
                                break
                            }
                            if (cell.cellTypeEnum == CellType.FORMULA) {
                                duesItem.payments[i] = cell.numericCellValue.toString()
                            } else {
                                duesItem.payments[i] = formatter.formatCellValue(cell)
                            }
                        }
                    }
                }
                duesEntityList.add(duesItem)
            }
            dao?.insert(duesEntityList)
        }
    }


    fun reloadFromLocalDB(year: String) {
        Log.i("TAG", "reloadFromLocalDB: " + year)
        object : BackgroundTasks() {
            override fun onPreExecute() {
                //duesLoadingModel.setValue(State(true))
            }

            override fun doInBackground() {
                dues = dao?.getThisYearDues(year)
            }

            override fun onPostExecute() {
                Log.i("TAG", "reloadFromLocalDB, onPostExecute: " + year)
                //duesModel.setData(dues)
                //duesLoadingModel.setValue(State(false))
            }
        }.execute()
    }

    fun reloadFromBackend() {
        object : BackgroundTasks() {
            val formatter = DataFormatter()
            override fun onPreExecute() {
            }

            override fun doInBackground() {
                for ((sheetNum, sheet) in excelHelper.sheets.withIndex()) {
                    val duesEntityList: MutableList<EntityYearlyDues> = ArrayList()
                    for (row in sheet) {
                        val duesItem = EntityYearlyDues()
                        for ((index, cell) in row.withIndex()) {
                            duesItem.year = years[sheetNum]
                            when (index) {
                                0 -> duesItem.index = formatter.formatCellValue(cell)
                                1 -> duesItem.name = formatter.formatCellValue(cell)
                                2 -> duesItem.folio = formatter.formatCellValue(cell)
                                else -> {
                                    val i = index - 3
                                    if(i > 12){
                                        break
                                    }
                                    if(cell.cellTypeEnum == CellType.FORMULA){
                                        duesItem.payments[i] = cell.numericCellValue.toString()
                                    }else {
                                        duesItem.payments[i] = formatter.formatCellValue(cell)
                                    }
                                }
                            }
                        }
                        duesEntityList.add(duesItem)
                    }
                    dao?.insert(duesEntityList)
                }
            }

            override fun onPostExecute() {
                val y = excelHelper.getWorkbook().getSheetName(SELECTED_YEAR)
                reloadFromLocalDB(y)
            }
        }.execute()
    }

}