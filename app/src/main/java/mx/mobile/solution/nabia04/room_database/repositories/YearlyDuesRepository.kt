package mx.mobile.solution.nabia04.room_database.repositories

import android.content.Context
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper.Companion.sheets
import mx.mobile.solution.nabia04.main.fragment.welfare.FragmentDuesPaymentDetail.Companion.duesLoadingModel
import mx.mobile.solution.nabia04.main.fragment.welfare.FragmentDuesPaymentDetail.Companion.duesModel
import mx.mobile.solution.nabia04.main.fragment.welfare.FragmentDuesPaymentDetail.Companion.year
import mx.mobile.solution.nabia04.room_database.DuesDetailDao
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues
import mx.mobile.solution.nabia04.room_database.view_models.LoadingStatus
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint


class YearlyDuesRepository(context: Context) {
    private val years = arrayOf("2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025")

    companion object {
        private const val TAG = "AnnDataRepository"
        private var dues: MutableList<EntityYearlyDues>? = null
        private var dao: DuesDetailDao? = null
        private var endpoint: MainEndpoint? = null

        @Volatile
        private var INSTANCE: YearlyDuesRepository? = null
        fun getInstance(context: Context): YearlyDuesRepository? {
            if (INSTANCE == null) {
                synchronized(YearlyDuesRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = YearlyDuesRepository(context)
                    }
                }
            }
            return INSTANCE
        }
    }

    init {
        dao = MainDataBase.getDatabase(context).duesDetailsDao()
    }

    fun reloadFromLocalDB(year: String) {
        object : BackgroundTasks() {
            override fun onPreExecute() {
                duesLoadingModel.setValue(LoadingStatus(true))
            }

            override fun doInBackground() {
                dues = dao?.getThisYearDues(year)
            }

            override fun onPostExecute() {
                duesModel.setData(dues)
                duesLoadingModel.setValue(LoadingStatus(false))
            }
        }.execute()
    }

    fun reloadFromBackend() {
        object : BackgroundTasks() {
            val formatter = DataFormatter()
            override fun onPreExecute() {
                duesLoadingModel.setValue(LoadingStatus(true))
            }

            override fun doInBackground() {
                for ((sheetNum, sheet) in sheets.withIndex()) {
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
                duesLoadingModel.setValue(LoadingStatus(false))
                loadData(year)
            }
        }.execute()
    }


    fun loadData(year: String){
        object : BackgroundTasks() {
            var b: Boolean = false
            override fun onPreExecute() {}

            override fun doInBackground() {
                b = dao?.tableCount()!! < 1
            }

            override fun onPostExecute() {
                if (b) {
                    reloadFromBackend()
                }else {
                    reloadFromLocalDB(year)
                }
            }
        }.execute()
    }
}