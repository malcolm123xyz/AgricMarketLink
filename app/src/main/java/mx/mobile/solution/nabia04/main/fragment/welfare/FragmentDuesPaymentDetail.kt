package mx.mobile.solution.nabia04.main.fragment.welfare

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.util.ColorGenerator
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDuesPaymentDetailBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelper
import mx.mobile.solution.nabia04.main.MainActivity.Companion.excelHelperViewModel
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.room_database.DuesDetailDao
import mx.mobile.solution.nabia04.room_database.MainDataBase
import mx.mobile.solution.nabia04.room_database.entities.EntityYearlyDues
import mx.mobile.solution.nabia04.room_database.repositories.YearlyDuesRepository
import mx.mobile.solution.nabia04.room_database.view_models.DuesLoadingStatusViewModel
import mx.mobile.solution.nabia04.room_database.view_models.LoadingStatus
import mx.mobile.solution.nabia04.room_database.view_models.YearlyDuesViewModel
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import java.text.SimpleDateFormat
import java.util.*

class FragmentDuesPaymentDetail() : BaseDataBindingFragment<FragmentDuesPaymentDetailBinding>() {

    private var SORT: Int = 0
    private var dao: DuesDetailDao? = null
    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    private lateinit var adapter: ListAdapter

    private var repository: YearlyDuesRepository? = null

    override fun getLayoutRes(): Int = R.layout.fragment_dues_payment_detail

    companion object {
        var year = "2018"
        var yr = 0
        lateinit var duesLoadingModel: DuesLoadingStatusViewModel
        lateinit var duesModel: YearlyDuesViewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        duesModel = ViewModelProvider(requireActivity()).get(YearlyDuesViewModel::class.java)
        duesLoadingModel =
            ViewModelProvider(requireActivity()).get(DuesLoadingStatusViewModel::class.java)
        repository = YearlyDuesRepository.getInstance(requireContext())
        dao = MainDataBase.getDatabase(context).duesDetailsDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter()
        observeLiveData()
        duesLoadingModel.value.observe(viewLifecycleOwner) { loadingStatus: LoadingStatus ->
            showProgress(loadingStatus.isLoading)
        }
        excelHelperViewModel.value.observe(viewLifecycleOwner) { mExcelHelper: ExcelHelper ->
            repository?.loadData(year)
        }
    }

    private fun observeLiveData() {
        duesModel.data.observe(viewLifecycleOwner) { data: List<EntityYearlyDues> ->
            Log.i("TAG", "duesModel.data.observe")
            setData(data)
        }
    }

    private fun setData(list: List<EntityYearlyDues>) {
        val data: MutableList<EntityYearlyDues> = list.toMutableList()
        object : BackgroundTasks() {
            override fun onPreExecute() {}
            override fun doInBackground() {
                when (SORT) {
                    1 -> {
                        Log.i("TAG", "Sort: $SORT")
                        data.sortWith(fun(obj1: EntityYearlyDues, obj2: EntityYearlyDues): Int {
                            return obj1.name.compareTo(obj2.name)
                        })
                    }
                    2 -> {
                        val headerItem = data[0]
                        val totalItem = data[data.size - 1]
                        data.removeAt(0)
                        data.removeAt(data.size - 1)
                        data.sortWith(fun(obj1: EntityYearlyDues, obj2: EntityYearlyDues): Int {
                            val amount1 = obj1.payments[12].toDouble()
                            val amount2 = obj2.payments[12].toDouble()
                            return amount2.compareTo(amount1)
                        })
                        data.add(0, headerItem)
                        data.add(data.size, totalItem)
                    }
                    3 -> {
                        val headerItem = data[0]
                        val totalItem = data[data.size - 1]
                        data.removeAt(0)
                        data.removeAt(data.size - 1)
                        data.sortWith(fun(obj1: EntityYearlyDues, obj2: EntityYearlyDues): Int {
                            val amount1 = obj1.payments[12].toDouble()
                            val amount2 = obj2.payments[12].toDouble()
                            return amount1.compareTo(amount2)
                        })
                        data.add(0, headerItem)
                        data.add(data.size, totalItem)
                    }
                }
            }

            override fun onPostExecute() {
                adapter.upDateList(data)
                vb!!.recyclerView.adapter = adapter
                updateHeader(yr);
            }
        }.execute()
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            vb!!.progressBar.visibility = View.VISIBLE
            vb!!.recyclerView.visibility = View.INVISIBLE
        } else {
            vb!!.progressBar.visibility = View.GONE
            vb!!.recyclerView.visibility = View.VISIBLE
        }
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
                vb!!.title.text = "Payment details for $year"
                vb!!.totalCont.text = "Total payment for the year is: $totalValue"
                vb!!.numContributors.text = "$numbPaid Members paid"
            }

        }.execute()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.dues_detail_menu, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort -> {
                val menuItemView: View =
                    ActivityCompat.requireViewById(requireActivity(), R.id.sort)
                showSortPopup(menuItemView)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2018 -> {
                year = "2018"
                yr = 0
                requireActivity().title = year
                repository?.loadData(year)
                updateHeader(0)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2019 -> {
                year = "2019"
                yr = 1
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2020 -> {
                year = "2020"
                yr = 2
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2021 -> {
                year = "2021"
                yr = 3
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2022 -> {
                year = "2022 "
                yr = 4
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2023 -> {
                year = "2023"
                yr = 5
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2024 -> {
                year = "2024"
                yr = 6
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            R.id.year_2025 -> {
                year = "2025"
                yr = 7
                requireActivity().title = year
                repository?.loadData(year)
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortPopup(view: View) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "Sort A - z")
        val amountSubMenu = popupMenu.menu.addSubMenu(1, 5, 1, "Sort by Amount")
        amountSubMenu.add(1, 2, 0, "Highest to Lowest")
        amountSubMenu.add(1, 3, 1, "Lowest to Highest")
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId < 5) {
                SORT = item.itemId
                repository?.loadData(year)
            }
            true
        }
        popupMenu.show()
    }

    private inner class ListAdapter() : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        private var payments: MutableList<EntityYearlyDues> = ArrayList()
        private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

        fun upDateList(actors: List<EntityYearlyDues>) {
            val diffCallback = ActorDiffCallback(this.payments, actors)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            this.payments.clear()
            this.payments.addAll(actors)
            diffResult.dispatchUpdatesTo(this)
        }

        inner class MyViewHolder(val parent: View) :
            RecyclerView.ViewHolder(parent) {
            val index: TextView = itemView.findViewById(R.id.index)
            val name: TextView = itemView.findViewById(R.id.name)
            val folio: TextView = itemView.findViewById(R.id.folio)
            val itemHolder: View = itemView.findViewById(R.id.item_holder)
            val total: TextView = itemView.findViewById(R.id.total)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.yearly_dues_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {

            val payment = payments[i]

            val amounts = payment.payments

            holder.index.text = i.toString()
            holder.name.text = payment.name
            holder.folio.text = payment.folio
            holder.total.text = amounts[12]

            holder.itemHolder.setBackgroundColor(generator.randomColor)

        }

        private fun getDate(l: Long): String {
            return fd.format(l)
        }

        override fun getItemCount(): Int {
            return payments.size
        }
    }

    class ActorDiffCallback(
        private val oldList: List<EntityYearlyDues>, private val newList: List<EntityYearlyDues>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].name == newList[newItemPosition].name
        }

    }

    private fun showErrorDialog(title: String, errMsg: String) {
        AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(title)
            .setMessage("Could not load information: $errMsg")
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }.show()
    }
}
