package mx.mobile.solution.nabia04.room_database.repositories

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper
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
import org.apache.poi.ss.usermodel.Sheet
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint


class YearlyDuesRepository(private val excelHelper: ExcelHelper) {
    private val years = arrayOf("2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025")
    private val context: Context? = excelHelper.context

    companion object {
        private const val TAG = "AnnDataRepository"
        private var dues: MutableList<EntityYearlyDues>? = null
        private var dao: DuesDetailDao? = null
        private var sharedP: SharedPreferences? = null
        private var endpoint: MainEndpoint? = null

        @Volatile
        private var INSTANCE: YearlyDuesRepository? = null
        fun getInstance(excelHelper: ExcelHelper): YearlyDuesRepository? {
            if (INSTANCE == null) {
                synchronized(YearlyDuesRepository::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = YearlyDuesRepository(excelHelper)
                    }
                }
            }
            return INSTANCE
        }

    }

    init {
        dao = MainDataBase.getDatabase(context).duesDetailsDao()
        sharedP = PreferenceManager.getDefaultSharedPreferences(context)
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
                duesLoadingModel.setValue(LoadingStatus(false))
                duesModel.setData(dues)
            }
        }.execute()
    }

    fun reloadFromBackend() {
        object : BackgroundTasks() {
            private var allSheets: MutableList<Sheet>? = null
            val formatter = DataFormatter()
            override fun onPreExecute() {
                duesLoadingModel.setValue(LoadingStatus(true))
            }

            override fun doInBackground() {
                allSheets = excelHelper.getAllSheets()
                for ((sheetNum, sheet) in allSheets!!.withIndex()){
                    val duesEntityList: MutableList<EntityYearlyDues> = ArrayList()
                    for (row in sheet) {
                        val duesItem = EntityYearlyDues()
                        for((index, cell) in row.withIndex()){
                            duesItem.year = years[sheetNum]
                            when (index){
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
        object: BackgroundTasks(){
            var b: Boolean = false
            override fun onPreExecute() {}

            override fun doInBackground() {
                b = dao?.tableCount()!! < 1

            }
            override fun onPostExecute() {
                if (b){
                    reloadFromBackend()
                }else {
                    reloadFromLocalDB(year)
                }
            }
        }.execute()
    }
}