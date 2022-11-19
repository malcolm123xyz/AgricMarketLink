package mx.mobile.solution.nabia04_beta1.ui.treasurer

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import mx.mobile.solution.nabia04_beta1.App
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.dao.DuesBackupDao
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDues
import mx.mobile.solution.nabia04_beta1.data.view_models.DuesViewModel
import mx.mobile.solution.nabia04_beta1.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentDuesDetailViewBinding
import mx.mobile.solution.nabia04_beta1.utilities.*
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DuesBackup
import java.io.*
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class FragmentDuesDetailView : Fragment(),
    SearchView.OnQueryTextListener {

    private lateinit var undoMenuItem: MenuItem
    private lateinit var saveMenuItem: MenuItem
    private var sort = 2
    private var excelFilePath = ""

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var duesBackupDao: DuesBackupDao

    private val networkViewModel by viewModels<NetworkViewModel>()

    private val viewModel by activityViewModels<DuesViewModel>()

    private lateinit var adapter: ListAdapter1

    private lateinit var selectedList: MutableList<EntityDues>

    companion object {
        private var intPosition = 0
        fun newInstance(pos: Int): FragmentDuesDetailView {
            intPosition = pos
            return FragmentDuesDetailView()
        }
    }


    private var _binding: FragmentDuesDetailViewBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDuesDetailViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ListAdapter1()
        excelFilePath = arguments?.getString("temp_file") ?: ""
        Log.i("TAG", "Excel to view path: $excelFilePath")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        lifecycleScope.launch {
            loadExcel()
            setUpListener()
            showHeader()
        }
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
                        binding.l1.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        pDial.setMessage(response.data.toString())
                    }
                    Status.ERROR -> {
                        pDial.dismiss()
                        showDialog("ERROR", "An error has occurred: ${response.message}")
                    }
                    else -> {}
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

    private suspend fun setUpListener() {
        viewModel.fetchDues().observe(viewLifecycleOwner) { users: Response<List<EntityDues>> ->
            when (users.status) {
                Status.SUCCESS -> {
                    selectedList = users.data?.toMutableList() ?: ArrayList()
                    lifecycleScope.launch {
                        sortData()
                        binding.recyclerView.adapter = adapter
                        adapter.submitList(selectedList)
                        adapter.notifyDataSetChanged()
                    }
                    val total = excelHelper.getGrandTotal().toString()
                    binding.total.text = total

                    binding.totalCont.text = "Total payment: Ghc $total"
                    binding.numContributors.text = "${selectedList.size} Members have paid dues"
                }
                Status.LOADING -> {
                    showProgress(true)
                }
                Status.ERROR -> {
                    Log.i("TAG", "ERROR")
                    showProgress(false)
                    Toast.makeText(requireContext(), users.message, Toast.LENGTH_LONG).show()
                }
                Status.EVENT -> {
                    saveMenuItem.isVisible = true
                    undoMenuItem.isVisible = true
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
            binding.y1.text = headerItems[0]
            binding.y2.text = headerItems[1]
            binding.y3.text = headerItems[2]
            binding.y4.text = headerItems[3]
            binding.y5.text = headerItems[4]
        }
    }

    private suspend fun sortData() {
        val l = selectedList[0].payments.size - 1
        Log.i("TAG", "DATA TO SORT: Data size = " + selectedList.size)
        showProgress(true)
        withContext(Dispatchers.Default) {
            if (selectedList.isNotEmpty()) {
                when (sort) {
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
            binding.progressBar.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        Log.i("TAG", "onResume()")
        val backupDir =
            File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")
        val fileName = "temp_save.xlsx"
        val file = File(backupDir, fileName)
        if (file.exists()) {
            Log.i("TAG", "Temporal file exist")
            if (this::undoMenuItem.isInitialized) {
                undoMenuItem.isVisible = true
                Log.i("TAG", "Menu is innitialized")
            } else {
                Log.i("TAG", "Menu is not innitialized")
            }
        } else {
            Log.i("TAG", "Temporal file does not exist")
        }

        val isPublish = sharedP.getBoolean(Const.EXCEL_SHOW_SAVE, false)
        Log.i("TAG", "isPublish: $isPublish")
        if (isPublish) {
            if (this::saveMenuItem.isInitialized) {
                saveMenuItem.isVisible = true
            }
        }
    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.dues_detail_menu, menu)

            undoMenuItem = menu.findItem(R.id.undo)
            saveMenuItem = menu.findItem(R.id.save)
            undoMenuItem.isVisible = false
            saveMenuItem.isVisible = false

            val searchItem: MenuItem = menu.findItem(R.id.search)
            val searchView = searchItem.actionView as SearchView

            searchView.setOnQueryTextListener(this@FragmentDuesDetailView)

            val backupDir =
                File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")
            val fileName = "temp_save.xlsx"
            val file = File(backupDir, fileName)
            if (file.exists()) {
                Log.i("TAG", "Temporal file exist")
                undoMenuItem.isVisible = true
            } else {
                Log.i("TAG", "Temporal file does not exist")
            }

            val isPublish = sharedP.getBoolean(Const.EXCEL_SHOW_SAVE, false)
            Log.i("TAG", "isPublish: $isPublish")
            if (isPublish) {
                saveMenuItem.isVisible = true
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {

                android.R.id.home -> {
                    findNavController().navigateUp()
                    true
                }
                R.id.sort -> {
                    val menuItemView: View =
                        ActivityCompat.requireViewById(requireActivity(), R.id.sort)
                    showSortPopup(menuItemView)
                    true
                }

                R.id.upDateDuesPayment -> {
                    val bundle = bundleOf("fragment" to "FragmentDuesDetailView")
                    findNavController().navigate(R.id.action_move_dues_update, bundle)
                    true
                }

                R.id.save -> {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("WARNING")
                        .setMessage("Are you sure you want to save this current document?")
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            excelHelper.saveFile()
                            sharedP.edit().putBoolean(Const.EXCEL_SHOW_SAVE, false).apply()
                            saveMenuItem.isVisible = false
                            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                    true
                }

                R.id.undo -> {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("WARNING")
                        .setMessage(
                            "Are you sure you want to do the current changes done to the excel sheet?" +
                                    " This will override all current changes with previous state. " +
                                    "\n" +
                                    "\nTHIS CANNOT BE UNDONE\n\n" +
                                    "Do you want to continue?"
                        )
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            undo()
                            undoMenuItem.isVisible = false
                            sharedP.edit().putBoolean(Const.EXCEL_SHOW_SAVE, false).apply()
                            Toast.makeText(requireContext(), "Changes reverted", Toast.LENGTH_SHORT)
                                .show()
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                    true
                }

                R.id.share -> {
                    val imagePath = File(requireContext().filesDir, "Dues")
                    val newFile = File(imagePath, "Nabiadues.xlsx")
                    val contentUri: Uri = getUriForFile(
                        requireContext(),
                        "mx.mobile.solution.nabia04_beta1.fileprovider",
                        newFile
                    )
                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    val file = excelHelper.getExcelFile()
                    if (file.exists()) {
                        val resInfoList: List<ResolveInfo> =
                            requireActivity().packageManager.queryIntentActivities(
                                sharingIntent,
                                PackageManager.MATCH_DEFAULT_ONLY
                            )
                        for (resolveInfo in resInfoList) {
                            val packageName = resolveInfo.activityInfo.packageName
                            requireContext().grantUriPermission(
                                packageName,
                                contentUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        sharingIntent.type = "application/pdf"
                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...")
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, "Sharing File...")
                        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        startActivity(Intent.createChooser(sharingIntent, "Share File"))
                    }
                    true
                }

                R.id.copy -> {
                    copyToStorage()
                    true
                }

                R.id.backup -> {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("WARNING")
                        .setMessage(
                            "Are you sure you want to backup the current dues sheet?"
                        )
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            excelHelper.backupDues()
                            Toast.makeText(requireContext(), "Backup done", Toast.LENGTH_SHORT)
                                .show()
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
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

    private fun undo() {
        val backupDir =
            File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")

        val duesDir = File(App.applicationContext().filesDir, "Dues")
        val `in`: InputStream?
        val out: OutputStream?
        try {
            //create output directory if it doesn't exist
            val dir = File(duesDir.absolutePath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val outFile = "${duesDir.absolutePath}/Nabiadues.xlsx"
            val inFile = File(backupDir, "temp_save.xlsx")

            `in` = FileInputStream(inFile)
            out = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            // write the output file (You have now copied the file)
            out.flush()
            out.close()

            if (inFile.exists()) {
                val b = inFile.delete()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        excelHelper.reloadExcel()
        viewModel.fetchDues()

    }

    private fun copyToStorage() {
        val inFile = excelHelper.getExcelFile()

        val backupDir =
            File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")
        val `in`: InputStream?
        val out: OutputStream?
        try {
            //create output directory if it doesn't exist
            val dir = File(backupDir.absolutePath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val outFile = "${backupDir.absolutePath}/Nabiadues_copy.xlsx"

            `in` = FileInputStream(inFile)
            out = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            // write the output file (You have now copied the file)
            out.flush()
            out.close()
            Toast.makeText(requireContext(), "File Copied", Toast.LENGTH_SHORT).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
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
                sort = item.itemId
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
                .inflate(R.layout.list_item_dues_detail_view, parent, false)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}