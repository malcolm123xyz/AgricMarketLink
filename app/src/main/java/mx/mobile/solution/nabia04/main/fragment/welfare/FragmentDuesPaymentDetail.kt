package mx.mobile.solution.nabia04.main.fragment.welfare

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.util.ColorGenerator
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDuesPaymentDetailBinding
import mx.mobile.solution.nabia04.main.fragment.BaseDataBindingFragment
import mx.mobile.solution.nabia04.main.fragment.host_fragments.WelfareHostFragment.Companion.excelHelper
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

    private var dao: DuesDetailDao? = null
    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    companion object {
        var year = "2018"
        var yr = 0
        lateinit var duesLoadingModel: DuesLoadingStatusViewModel
        lateinit var duesModel: YearlyDuesViewModel
    }

    private lateinit var adapter: ListAdapter

    private var repository: YearlyDuesRepository? = null

    override fun getLayoutRes(): Int = R.layout.fragment_dues_payment_detail

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        duesModel = ViewModelProvider(requireActivity()).get(YearlyDuesViewModel::class.java)
        duesLoadingModel =
            ViewModelProvider(requireActivity()).get(DuesLoadingStatusViewModel::class.java)
        repository = YearlyDuesRepository.getInstance(excelHelper)
        dao = MainDataBase.getDatabase(context).duesDetailsDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ListAdapter()
        vb!!.recyclerView.adapter = adapter
        observeLiveData()
        listenForLoadingStatus()
        repository?.loadData(year)
    }

    private fun observeLiveData() {
        duesModel.data.observe(viewLifecycleOwner) { data: List<EntityYearlyDues> ->
            Log.i("TAG", "on Observe data, Data size = "+data.size)
            adapter.upDateList(data)
            updateHeader(yr);
        }
    }

    private fun listenForLoadingStatus() {
        duesLoadingModel.value.observe(viewLifecycleOwner) { loadingStatus: LoadingStatus ->
            showProgress(loadingStatus.isLoading)
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            vb!!.shimmer.startShimmer()
            vb!!.shimmer.visibility = View.VISIBLE
            vb!!.recyclerView.visibility = View.INVISIBLE
        } else {
            vb!!.shimmer.stopShimmer()
            vb!!.shimmer.visibility = View.GONE
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
                totalValue = excelHelper!!.getYearTotal(sheetNum)
                numbPaid = excelHelper?.getNumPaid(sheetNum)
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


    private fun setData(year: String) {
        var dues: List<EntityYearlyDues> = ArrayList()
        object : BackgroundTasks() {
            override fun onPreExecute() {
                duesLoadingModel.setValue(LoadingStatus(true))}
            override fun doInBackground() {
                dues = dao?.getThisYearDues(year)!!
                Log.i("TAG", "setData(), dues size = "+dues.size)
//                dues.sortWith { obj1: EntityYearlyDues, obj2: EntityYearlyDues ->
//                    obj2.index.compareTo(obj1.index)
//                }
            }
            override fun onPostExecute() {
                duesLoadingModel.setValue(LoadingStatus(false))
                if(dues.isEmpty()){
                    showErrorDialog("ERROR", "No data found for the selected year.")
                }else{
                    adapter.upDateList(dues)
                    try{
                        requireActivity().title = year
                    }catch (er: java.lang.IllegalStateException){
                        er.printStackTrace()
                    }
                }

            }
        }.execute()
    }

    private inner class ListAdapter() : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
        private var payments: List<EntityYearlyDues> = ArrayList()
        private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

        @SuppressLint("NotifyDataSetChanged")
        fun upDateList(receivedData: List<EntityYearlyDues>) {
            payments = receivedData
            Log.i("TAG", "payments size: "+payments.size)
            notifyDataSetChanged()
        }

        inner class MyViewHolder(val parent: View) :
            RecyclerView.ViewHolder(parent) {
            val index: TextView = itemView.findViewById(R.id.index)
            val name: TextView = itemView.findViewById(R.id.name)
            val folio: TextView = itemView.findViewById(R.id.folio)
            val itemHolder: View = itemView.findViewById(R.id.item_holder)
            val monthsViews: MutableList<TextView> = ArrayList()
            init {
                monthsViews.add(itemView.findViewById(R.id.jan))
                monthsViews.add(itemView.findViewById(R.id.feb))
                monthsViews.add(itemView.findViewById(R.id.mar))
                monthsViews.add(itemView.findViewById(R.id.apr))
                monthsViews.add(itemView.findViewById(R.id.may))
                monthsViews.add(itemView.findViewById(R.id.jun))
                monthsViews.add(itemView.findViewById(R.id.jul))
                monthsViews.add(itemView.findViewById(R.id.aug))
                monthsViews.add(itemView.findViewById(R.id.sep))
                monthsViews.add(itemView.findViewById(R.id.oct))
                monthsViews.add(itemView.findViewById(R.id.nov))
                monthsViews.add(itemView.findViewById(R.id.dec))
                monthsViews.add(itemView.findViewById(R.id.total))
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.yearly_dues_list_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, i: Int) {
            val payment = payments[i]

            val amounts = payment.payments

            holder.index.text = payment.index.toString()
            holder.name.text = payment.name
            holder.folio.text = payment.folio

            for((index, amount) in amounts.withIndex()){
                holder.monthsViews[index].text = amount.toString()
            }

            holder.itemHolder.setBackgroundColor(generator.randomColor)

        }

        private fun getDate(l: Long): String {
            return fd.format(l)
        }

        override fun getItemCount(): Int {
            return if (payments == null) {
                0
            } else payments!!.size
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
