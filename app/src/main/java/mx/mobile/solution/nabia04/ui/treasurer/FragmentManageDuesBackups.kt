package mx.mobile.solution.nabia04.ui.treasurer

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.App
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.dao.DuesBackupDao
import mx.mobile.solution.nabia04.data.entities.EntityDuesBackup
import mx.mobile.solution.nabia04.data.view_models.DuesBackupViewModel
import mx.mobile.solution.nabia04.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04.databinding.FragmentManageDuesBackupsBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.MyAlertDialog
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.Status
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentManageDuesBackups() : BaseFragment<FragmentManageDuesBackupsBinding>() {

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var dao: DuesBackupDao

    private lateinit var adapter: ListAdapter1

    override fun getLayoutRes(): Int = R.layout.fragment_manage_dues_backups

    private lateinit var viewModel: DuesBackupViewModel

    private val networkViewModel by viewModels<NetworkViewModel>()

    private lateinit var sortList: MutableList<EntityDuesBackup>

    companion object {
        private var intPosition = 0;
        fun newInstance(pos: Int): FragmentManageDuesBackups {
            intPosition = pos
            return FragmentManageDuesBackups()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[DuesBackupViewModel::class.java]
        adapter = ListAdapter1()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb!!.recyclerView.setHasFixedSize(true)
        vb!!.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        lifecycleScope.launch {
            setUpViewListener()
        }
    }


    private fun setUpViewListener() {
        viewModel.fetchList()
            .observe(viewLifecycleOwner) { response: Response<List<EntityDuesBackup>> ->
                Log.i("TAG", "BACKUPS SIZE = ${response.data?.size}")
                when (response.status) {
                    Status.SUCCESS -> {
                        if (response.data != null) {
                            sortList = response.data.toMutableList()
                            sortList.sortWith(fun(
                                obj1: EntityDuesBackup,
                                obj2: EntityDuesBackup
                            ): Int {
                                return obj2.id.compareTo(obj1.id)
                            })
                            vb!!.recyclerView.adapter = adapter
                            adapter.submitList(sortList)
                            adapter.notifyDataSetChanged()
                        }
                        showProgress(false)
                    }
                    Status.LOADING -> {
                        showProgress(true)
                    }
                    Status.ERROR -> {
                        vb!!.recyclerView.adapter = null
                        showProgress(false)
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
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

    private fun showPopupMenu(i: Int, view: View) {

        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.menu.add(0, 1, 0, "View")
        popupMenu.menu.add(0, 2, 0, "Restore")
        popupMenu.menu.add(0, 3, 0, "Delete")
        popupMenu.menu.add(0, 4, 0, "Download")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    viewBackup(i)
                }
                2 -> {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("WARNING!, WARNING!")
                        .setMessage(
                            "You are about to restore this backup as the working document. " +
                                    "this will replace the existing document. The current document will however be " +
                                    "backed up\n\n" +
                                    "DO YOU WANT TO CONTINUE"
                        )
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            restore(i)
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                }
                3 -> {

                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("WARNING")
                        .setMessage(
                            "Note:\nDeleting this backup only deletes it from the cloud server. " +
                                    "If it is the current published document, it will still be visible to users " +
                                    " until a different document is published. " +
                                    "\n\nCONTINUE?"
                        )
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            delete(i)
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()

                }
                4 -> {
                    val duesDir =
                        File(Environment.getExternalStorageDirectory().absolutePath + "/Naba04/Dues_backup")

                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("NOTE")
                        .setMessage(
                            "Downloading the excel document is for external use only. For exemple, to view on a " +
                                    "Computer, share with others etc. Any changes to the downloaded file cannot be uploaded to the " +
                                    "App. Only the App developer can do it." +
                                    "\n\nFile will be donwloaded to: $duesDir" +
                                    "\n\nDO YOU WANT TO DOWNLOAD?"
                        )
                        .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                            saveBackup(duesDir, i)
                        }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                            dialog.dismiss()
                        }.show()
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun delete(i: Int) {
        lifecycleScope.launch {
            deleteBackup(i)
            setUpViewListener()
            Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun deleteBackup(i: Int) {
        val backup = sortList[i]
        dao.delete(backup)
        val fdelete = File(backup.fileFullPath)
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.i("TAG", "file Deleted :" + backup.fileFullPath)
            } else {
                Log.i("TAG", "file not Deleted :" + backup.fileFullPath)
            }
        }
    }

    private fun viewBackup(i: Int) {
        val bundle = bundleOf("temp_file" to sortList[i].fileFullPath)
        findNavController().navigate(R.id.action_move_to_dues_payment_view, bundle)
    }

    private fun saveBackup(duesDir: File, i: Int) {
        val pDial =
            MyAlertDialog(requireContext(), "BACKUP", "Downloading backup file...", false).show()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                downloadBackup(duesDir, i)
                pDial.dismiss()
            }
        }
    }

    private fun restore(i: Int) {
        val pDial =
            MyAlertDialog(requireContext(), "BACKUP", "Restoring...", false).show()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                rest(i)
                pDial.dismiss()
            }
        }
    }

    private fun rest(i: Int) {
        val backUp = sortList[i]
        val duesDir = File(App.applicationContext().filesDir, "Dues")
        excelHelper.backup()
        restoreExcelFile(backUp.filePath, "/${backUp.fileName}", duesDir.absolutePath)
        excelHelper.isCreated = false
        excelHelper.initialize()
    }

    private fun restoreExcelFile(inputPath: String, inputFile: String, outputPath: String) {
        var `in`: InputStream?
        val out: OutputStream?
        try {
            //create output directory if it doesn't exist
            val dir = File(outputPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val outFile = "$outputPath/Nabiadues.xlsx"
            val inFile = inputPath + inputFile

            `in` = FileInputStream(inFile)
            out = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            `in` = null

            // write the output file (You have now copied the file)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun downloadBackup(duesDir: File, i: Int): File? {
        Log.i("TAG", "Downloading file")
        Log.i("TAG", "sortList size = ${sortList.size}")
        val fileUrl = sortList[i].fileFullPath
        var excelFile: File? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(fileUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.doOutput = false
            urlConnection.connect()

            if (!duesDir.exists()) {
                duesDir.mkdirs()
            }
            excelFile = File(duesDir, "Nabiadues.xlsx")
            if (!excelFile.exists()) {
                excelFile.createNewFile()
            }
            val inputStream: InputStream = urlConnection.inputStream
            val totalSize: Int = urlConnection.contentLength
            val outPut = FileOutputStream(excelFile)
            var downloadedSize = 0
            val buffer = ByteArray(2024)
            var bufferLength = 0
            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                outPut.write(buffer, 0, bufferLength)
                downloadedSize += bufferLength
                Log.e(
                    "Progress:",
                    "downloadedSize:" + Math.abs(downloadedSize * 100 / totalSize)
                )
            }
            outPut.close()
            Log.i("TAG", "File downloaded")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("checkException:-", "" + e)
        }
        return excelFile
    }

    private inner class ListAdapter1 :
        ListAdapter<EntityDuesBackup, ListAdapter1.MyViewHolder>(DiffCallback()) {
        private val fd = SimpleDateFormat("EEE, d MMM yyyy mm:ss", Locale.US)

        inner class MyViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
            val itemHolder: MaterialCardView = itemView.findViewById(R.id.item_holder)
            val date: TextView = itemView.findViewById(R.id.date)
            val total: TextView = itemView.findViewById(R.id.totalAmount)
            val menu: ImageView = itemView.findViewById(R.id.actionMenu)

            fun bind(dues: EntityDuesBackup, i: Int) {
                date.text = "Modified on: ${fd.format(Date(dues.id))}"
                total.text = "Total amount = Ghc ${dues.totalAmount}"
                menu.setOnClickListener { showPopupMenu(i, it) }

                if (dues.published) {
                    date.text = "Modified on: ${fd.format(Date(dues.id))} (Published file)"
                    itemHolder.setCardBackgroundColor(
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
                .inflate(R.layout.dues_backup_list_item, parent, false)
            return MyViewHolder(view)
        }

        /* Gets current flower and uses it to bind view. */
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val flower = getItem(position)
            holder.bind(flower, position)
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<EntityDuesBackup>() {
        override fun areItemsTheSame(
            oldItem: EntityDuesBackup,
            newItem: EntityDuesBackup
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: EntityDuesBackup,
            newItem: EntityDuesBackup
        ): Boolean {
            return oldItem.totalAmount == newItem.totalAmount
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
}