package mx.mobile.solution.nabia04_beta1.ui.treasurer

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.App
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.dao.DuesBackupDao
import mx.mobile.solution.nabia04_beta1.data.entities.EntityDuesBackup
import mx.mobile.solution.nabia04_beta1.data.view_models.DuesBackupViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentManageDuesBackupsBinding
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import mx.mobile.solution.nabia04_beta1.utilities.MyAlertDialog
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class FragmentManageDuesBackups : Fragment() {

    private val fd = SimpleDateFormat("EEE, d MMM yyyy mm:ss", Locale.US)

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var dao: DuesBackupDao

    private lateinit var adapter: ListAdapter1

    private lateinit var viewModel: DuesBackupViewModel

    private lateinit var sortList: MutableList<EntityDuesBackup>

    companion object {
        private var intPosition = 0
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

    private var _binding: FragmentManageDuesBackupsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageDuesBackupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        lifecycleScope.launch {
            setUpViewListener()
        }
    }


    private fun setUpViewListener() {
        viewModel.fetchBackups()
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
                            binding.recyclerView.adapter = adapter
                            adapter.submitList(sortList)
                            adapter.notifyDataSetChanged()
                        }
                        showProgress(false)
                    }
                    Status.LOADING -> {
                        showProgress(true)
                    }
                    Status.ERROR -> {
                        binding.recyclerView.adapter = null
                        showProgress(false)
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
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
                            doRestore(sortList[i])
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

    private fun doRestore(backUp: EntityDuesBackup) {
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
            val inFile = "${backUp.filePath}/${backUp.fileName}"

            Log.i("TAG", "OUT FILE: $outFile")

            Log.i("TAG", "IN FILE: $inFile")

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
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finishBackup(backUp)
        viewModel.fetchBackups()
        excelHelper.reloadExcel()
    }

    private fun finishBackup(backup: EntityDuesBackup) {
        lifecycleScope.launch {
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
    }


    private fun delete(i: Int) {
        lifecycleScope.launch {
            deleteBackup(i)
            viewModel.fetchBackups()
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
                    "downloadedSize:" + abs(downloadedSize * 100 / totalSize)
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

        private val generator: ColorGenerator = ColorGenerator.MATERIAL

        inner class MyViewHolder(parent: View) : RecyclerView.ViewHolder(parent) {
            val date: TextView = itemView.findViewById(R.id.date)
            val total: ImageView = itemView.findViewById(R.id.totalAmount)
            val menu: ImageView = itemView.findViewById(R.id.actionMenu)

            fun bind(dues: EntityDuesBackup, i: Int) {
                date.text = "Backup on: ${fd.format(Date(dues.id))}"
                total.background = getDrawable("GHC ${dues.totalAmount}0")
                menu.setOnClickListener { showPopupMenu(i, it) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_dues_backup, parent, false)
            return MyViewHolder(view)
        }

        /* Gets current flower and uses it to bind view. */
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val flower = getItem(position)
            holder.bind(flower, position)
        }

        private fun getDrawable(s: String): Drawable {
            return TextDrawable.Builder()
                .setColor(generator.randomColor)
                .setShape(TextDrawable.SHAPE_ROUND)
                .setText(s)
                .setBold()
                .setFontSize(30)
                .build()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}