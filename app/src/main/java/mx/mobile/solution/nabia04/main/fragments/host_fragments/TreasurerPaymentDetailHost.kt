package mx.mobile.solution.nabia04.main.fragments.host_fragments

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smarttoolfactory.tutorial7_2bnv_viewpager2_complexarchitecture.adapter.TreasurerYearlyDuesDetailsStateAdapter
import kotlinx.coroutines.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelper
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelperIsInitialized
import mx.mobile.solution.nabia04.main.fragments.BaseDataBindingFragment
import mx.mobile.solution.nabia04.room_database.DuesDetailDao
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter

class TreasurerPaymentDetailHost() :
    BaseDataBindingFragment<mx.mobile.solution.nabia04.databinding.FragmentTreasurerPaymentDetailHostBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_treasurer_payment_detail_host

    private val years = arrayOf("2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025")

    companion object {
        var SORT: Int = 0
        var SELECTED_YEAR = 0
        lateinit var dao: DuesDetailDao
        lateinit var title: TextView
        lateinit var totalAmount: TextView
        lateinit var numContributers: TextView

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        dao = MainDataBase.getDatabase(context).duesDetailsDao()

        checkDataBase()

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkDataBase() {
        Thread {
            if (dao.tableCount() < 1) {
                if (!excelHelperIsInitialized()) {
                    GlobalScope.launch {
                        suspend {
                            delay(5000)
                            withContext(Dispatchers.Main) {
                                createExcelDB()
                            }
                        }.invoke()
                    }
                } else {
                    createExcelDB()
                }
            }
        }.start()
    }

    private fun createExcelDB() {
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
            dao.insert(duesEntityList)
        }
        vb?.viewpager?.adapter?.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb?.viewpager?.adapter = TreasurerYearlyDuesDetailsStateAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        )
        // TabLayout
        val tabLayout = vb!!.tabLayout
        // Bind tabs and viewpager
        TabLayoutMediator(tabLayout, vb!!.viewpager, fun(tab: TabLayout.Tab, position: Int) {
            val sheetName = excelHelper.getWorkbook().getSheetName(position);
            tab.text = sheetName
        }).attach()

        title = vb!!.title
        totalAmount = vb!!.totalCont
        numContributers = vb!!.numContributors

        vb?.viewpager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateHeader(position)
            }
        })
    }

    private fun updateHeader (sheetNum: Int){
        object: BackgroundTasks(){
            var totalValue = 0.00
            var numbPaid: String? = "0"
            override fun onPreExecute() {
            }

            override fun doInBackground() {
                totalValue = excelHelper.getYearTotal(sheetNum)
                numbPaid = excelHelper.getNumPaid(sheetNum)
            }

            override fun onPostExecute() {
                title.text =
                    "Payment details for ${excelHelper.getWorkbook().getSheetName(sheetNum)}"
                totalAmount.text = "Total payment for the year is: Ghc $totalValue"
                numContributers.text = "$numbPaid Members paid"
            }

        }.execute()
    }
}
