package mx.mobile.solution.nabia04.ui.treasurer

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.App
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityDues
import mx.mobile.solution.nabia04.data.view_models.DuesViewModel
import mx.mobile.solution.nabia04.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04.databinding.FragmentYearlyPaymentBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.treasurer.TreasurerPaymentDetailHost.Companion.SORT
import mx.mobile.solution.nabia04.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DuesBackup
import java.io.File
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentDuesDetailView() : BaseFragment<FragmentYearlyPaymentBinding>(),
    SearchView.OnQueryTextListener {

    private var excelFilePath = ""

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    private val networkViewModel by viewModels<NetworkViewModel>()

    private lateinit var adapter: ListAdapter1

    override fun getLayoutRes(): Int = R.layout.fragment_yearly_payment

    private lateinit var viewModel: DuesViewModel

    private lateinit var selectedList: MutableList<EntityDues>

    private var total = ""

    companion object {
        private var intPosition = 0;
        fun newInstance(pos: Int): FragmentDuesDetailView {
            intPosition = pos
            return FragmentDuesDetailView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[DuesViewModel::class.java]
        adapter = ListAdapter1()
        excelFilePath = arguments?.getString("temp_file") ?: ""
        Log.i("TAG", "Excel to view path: $excelFilePath")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val duesDir = File(App.applicationContext().filesDir, "Dues")
        val isPublish = sharedP.getBoolean(Const.EXCEL_PUBLISHED, false)
        if (excelHelper.getExcelFile().exists() && !isPublish) {
            vb!!.l1.visibility = View.VISIBLE
            vb!!.publish.setOnClickListener { showPublishWarning() }
        }

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        lifecycleScope.launch {
            loadExcel()
            setUpListener()
            showHeader()
        }
    }

    private fun showPublishWarning() {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage(
                "Publishing the current excel document will make it accessible other users. " +
                        "Make sure it has the latest updates. \n\n" +
                        "DO YOU WANT TO CONTINUE?"
            )
            .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                publish()
            }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    private fun publish() {

        val excelFile = excelHelper.getExcelFile()

        val excelUri = excelFile.absolutePath
        val backup = DuesBackup()
        backup.totalAmount = excelHelper.getGrandTotal().toString()
        val pDial = MyAlertDialog(requireContext(), "SENDING EXCEL", "", false).show()
        networkViewModel.publishExcel("dues/Nabiadues.xlsx", excelUri)
            .observe(viewLifecycleOwner) { response: Response<String> ->
                when (response.status) {
                    Status.SUCCESS -> {
                        pDial.dismiss()
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                        sharedP.edit().putBoolean(Const.EXCEL_PUBLISHED, true).apply()
                        findNavController().navigate(R.id.action_move_back)
                    }
                    Status.LOADING -> {
                        pDial.setMessage(response.data.toString())
                    }
                    Status.ERROR -> {
                        pDial.dismiss()
                        showDialog("ERROR", "An error has occurred: ${response.message}")
                    }
                }
            }
    }

    private fun showDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }.show()
    }

    private fun showExelFileNotExistDial() {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("ERROR")
            .setMessage("The excel file does not exist. Make sure it is created first")
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    private suspend fun setUpListener() {
        viewModel.fetchAnn().observe(viewLifecycleOwner) { users: Response<List<EntityDues>> ->
            when (users.status) {
                Status.SUCCESS -> {
                    selectedList = users.data?.toMutableList() ?: ArrayList()
                    lifecycleScope.launch {
                        sortData()
                        vb!!.recyclerView.adapter = adapter
                        adapter.submitList(selectedList)
                        adapter.notifyDataSetChanged()
                    }
                    val total = excelHelper.getGrandTotal().toString()
                    vb!!.total.text = total

                    vb?.totalCont?.text = "Total payment: Ghc $total"
                    vb?.numContributors?.text = "${selectedList.size} Members have paid dues"
                }
                Status.LOADING -> {
                    showProgress(true)
                }
                Status.ERROR -> {
                    Log.i("TAG", "ERROR")
                    showProgress(false)
                    Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun loadExcel() {
        withContext(Dispatchers.IO) {
            if (excelFilePath.isEmpty()) {
                excelHelper.initialize()
            } else {
                excelHelper.initializeTemp(excelFilePath)
            }

        }
    }

    private fun showHeader() {
        val headerItems = excelHelper.getHeader()
        if (headerItems.isNotEmpty()) {
            vb?.y1?.text = headerItems[0]
            vb?.y2?.text = headerItems[1]
            vb?.y3?.text = headerItems[2]
            vb?.y4?.text = headerItems[3]
            vb?.y5?.text = headerItems[4]
        }
    }

    private suspend fun sortData() {
        val l = selectedList[0].payments.size - 1
        Log.i("TAG", "DATA TO SORT: Data size = " + selectedList.size)
        showProgress(true)
        withContext(Dispatchers.Default) {
            if (selectedList.isNotEmpty()) {
                when (SORT) {
                    1 -> {
                        selectedList.sortWith(fun(obj1: EntityDues, obj2: EntityDues): Int {
                            return obj1.name.compareTo(obj2.name)
                        })
                    }
                    2 -> {
                        selectedList.sortWith(fun(obj1: EntityDues, obj2: EntityDues): Int {
                            val amount1 = obj1.payments[l].toDouble()
                            val amount2 = obj2.payments[l].toDouble()
                            return amount2.compareTo(amount1)
                        })
                    }
                    3 -> {
                        selectedList.sortWith(fun(obj1: EntityDues, obj2: EntityDues): Int {
                            val amount1 = obj1.payments[l].toDouble()
                            val amount2 = obj2.payments[l].toDouble()
                            return amount1.compareTo(amount2)
                        })
                    }
                }
                //Log.i("TAG", "selectedList size : ${selectedList.size}")
            }
        }
        showProgress(false)
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

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.dues_detail_menu, menu)

            val searchItem: MenuItem = menu.findItem(R.id.search)
            val searchView = searchItem.actionView as SearchView

            searchView.setOnQueryTextListener(this@FragmentDuesDetailView)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.sort -> {
                    val menuItemView: View =
                        ActivityCompat.requireViewById(requireActivity(), R.id.sort)
                    showSortPopup(menuItemView)
                    true
                }

                R.id.upDateDuesPayment -> {
                    findNavController().navigate(R.id.action_move_dues_update)
                    true
                }

                R.id.publish -> {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("WARNING")
                        .setMessage(
                            "Publishing the current excel document will make it accessible other users. " +
                                    "Make sure it has the latest updates. \n\n" +
                                    "DO YOU WANT TO CONTINUE?"
                        )
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            publish()
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                    true
                }
                else -> true
            }
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
                lifecycleScope.launch {
                    sortData()
                    adapter.submitList(selectedList)
                    adapter.notifyDataSetChanged()
                }
            }
            true
        }
        popupMenu.show()
    }


    private inner class ListAdapter1 :
        ListAdapter<EntityDues, ListAdapter1.MyViewHolder>(DiffCallback()), Filterable {
        private val colors = arrayOf(R.color.light_grey1, R.color.light_grey)
        var colorIndex = 0

        inner class MyViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
            val name: TextView = itemView.findViewById(R.id.name)
            val itemHolder: View = itemView.findViewById(R.id.item_holder)
            val v2018: TextView = itemView.findViewById(R.id.total1)
            val v2019: TextView = itemView.findViewById(R.id.total2)
            val v2020: TextView = itemView.findViewById(R.id.total3)
            val v2021: TextView = itemView.findViewById(R.id.total4)
            val v2022: TextView = itemView.findViewById(R.id.total5)
            val total: TextView = itemView.findViewById(R.id.total6)

            fun bind(dues: EntityDues, i: Int) {
                val payments = dues.payments
                name.text = dues.name
                val size = payments.size
                total.text = payments[size - 1]
                v2022.text = payments[size - 2]
                v2021.text = payments[size - 3]
                v2020.text = payments[size - 4]
                v2019.text = payments[size - 5]
                v2018.text = payments[size - 6]

                val userTotal = excelHelper.getUserTotalByName(dues.name) ?: 0.0
                val numOfMonthsPaid = (userTotal.toInt()) / 5
                val percentagePaged = ((numOfMonthsPaid / excelHelper.totalNumMonths) * 100).toInt()

                if (percentagePaged <= 29) {
                    colorIndex = if (colorIndex == 0) {
                        1
                    } else {
                        0
                    }
                    itemHolder.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            colors[colorIndex]
                        )
                    )
                } else if (percentagePaged in 30..69) {
                    itemHolder.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.average_standing
                        )
                    )
                } else {
                    itemHolder.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.good_standing
                        )
                    )
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.yearly_dues_list_item, parent, false)
            return MyViewHolder(view)
        }

        /* Gets current flower and uses it to bind view. */
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val flower = getItem(position)
            holder.bind(flower, position)

        }

        override fun getFilter(): Filter {
            return customFilter
        }

        private val customFilter = object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val queryValue = query.toString().lowercase(Locale.getDefault())
                val filteredList = mutableListOf<EntityDues>()
                if (queryValue.isEmpty()) {
                    filteredList.addAll(selectedList)
                } else {
                    Log.i("TAG", "Query word: $queryValue")
                    for (item in selectedList) {
                        try {
                            val nameSearch =
                                item.name.lowercase(Locale.getDefault())
                                    .contains(queryValue) ||
                                        item.folio.lowercase(Locale.getDefault())
                                            .contains(queryValue)
                            if (nameSearch) {
                                filteredList.add(item)
                            }
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        }
                    }
                }
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            override fun publishResults(constraint: CharSequence?, filterResults: FilterResults?) {
                submitList(filterResults?.values as MutableList<EntityDues>?)
                //notifyDataSetChanged()
            }

        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<EntityDues>() {
        override fun areItemsTheSame(
            oldItem: EntityDues,
            newItem: EntityDues
        ): Boolean {
            return oldItem.folio == newItem.folio
        }

        override fun areContentsTheSame(
            oldItem: EntityDues,
            newItem: EntityDues
        ): Boolean {
            return oldItem.name == newItem.name && oldItem.folio == newItem.folio
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        Log.i("TAG", "oN Query1: $query")
        adapter.filter.filter(query)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        Log.i("TAG", "oN Query2: $newText")
        adapter.filter.filter(newText)
        return true
    }

}