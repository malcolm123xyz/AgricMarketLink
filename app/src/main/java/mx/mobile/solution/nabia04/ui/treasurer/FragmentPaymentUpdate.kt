package mx.mobile.solution.nabia04.ui.treasurer

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_payment_update.*
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.App
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.view_models.DuesViewModel
import mx.mobile.solution.nabia04.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04.databinding.FragmentPaymentUpdateBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.utilities.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class FragmentPaymentUpdate : BaseFragment<FragmentPaymentUpdateBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_payment_update

    private val networkViewModel by viewModels<NetworkViewModel>()

    private val viewModel by activityViewModels<DuesViewModel>()

    @Inject
    lateinit var excelHelper: ExcelHelper

    @Inject
    lateinit var sharedP: SharedPreferences

    private var folio = ""
    private var name = ""
    private var amount = ""
    private var spinnerPos = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb?.spinner?.adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, excelHelper.names)
        vb?.spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                spinnerPos = i
                if (i > 0) {
                    val member = excelHelper.members[i]
                    folio = member.folio
                    Log.i("TAG", "FOLIO: $folio")
                    name = member.name
                    vb?.holder?.visibility = View.GONE
                } else {
                    vb?.holder?.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        vb?.update?.setOnClickListener {
            amount = vb?.amountEdit?.text.toString()
            if (spinnerPos < 1) {
                folio = vb?.folioEdit?.text.toString()
                name = vb?.nameEdit?.text.toString()
            }

            if (folio.isEmpty()) {
                showDialog("WARNING", "Folio cannot be empty")
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                showDialog("WARNING", "Name cannot be empty")
                return@setOnClickListener
            }


            if (amount.toInt() % 5 != 0) {
                showDialog("WARNING", "Invalide amount. Amount should be a multiple of 5")
                return@setOnClickListener
            }

            if (amount.toInt() > 200) {
                showDialog(
                    "WARNING",
                    "Invalide amount. You cannot update more than Ghc 200 at a go"
                )
                return@setOnClickListener
            }

            if (amount.isEmpty()) {
                showDialog("WARNING", "Amount cannot be empty")
                return@setOnClickListener
            }
            showWarningDialog(amount, name, folio)
        }
    }

    private fun showWarningDialog(amount: String, name: String, folio: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("You are about to make changes to the payment sheet. Are you sure about this?")
            .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                lifecycleScope.launch {
                    excelHelper.saveToTemporalStorage()
                    doUpdate(amount, name, folio)
                }
            }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    private suspend fun doUpdate(amount: String, name: String, folio: String) {
        val p = MyAlertDialog(
            requireContext(), "",
            "Updating the payment sheet... Please wait", false
        ).show()
        val response =
            excelHelper.insertNewPayment(amount.toInt(), name, folio, spinner.selectedItemPosition)
        p.dismiss()
        if (response.status == Status.SUCCESS) {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("IMPORTANT")
                .setMessage(
                    "${response.data}\n\nPayment updated successfully\n\n" +

                            "Review the list and make sure that the expected total is met before saving\n\n" +
                            "Note that, users will not see this changes until you publish the document.\n\n" +

                            "DO YOU WANT TO SAVE THIS CHANGES?"
                ).setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    excelHelper.saveFile()
                    findNavController().navigateUp()
                }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    deleteTemporalFile()
                    excelHelper.reloadExcel()
                    findNavController().navigateUp()
                }.setNeutralButton("View list") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    sharedP.edit().putBoolean(Const.EXCEL_SHOW_SAVE, true).apply()
                    findNavController().navigateUp()
                }.show()
        } else {
            deleteTemporalFile()
            excelHelper.reloadExcel()
            findNavController().navigateUp()
            showDialog("PAYMENT UPDATE", response.message.toString())
        }
    }

    private fun deleteTemporalFile() {

        val backupDir =
            File(Environment.getExternalStorageDirectory().absolutePath, "Nabia04_Dues_backups")

        val fileName = "temp_save.xlsx"

        if (!backupDir.exists()) {
            backupDir.mkdir()
        }
        val file = File(backupDir, fileName)
        if (file.exists()) {
            val b = file.delete()
            Log.i("TAG", "File deleted: $b")
        }
    }

//    private fun showPassWordDialog() {
//        val linf = LayoutInflater.from(requireContext())
//        val v = linf.inflate(R.layout.request_passwrd, null)
//        val passEdit = v.findViewById<EditText>(R.id.password_edit)
//        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
//            .setTitle("Enter Password")
//            .setView(v)
//            .setCancelable(false)
//            .setPositiveButton(
//                "OK"
//            ) { dialog: DialogInterface, _: Int ->
//                val p = passEdit.text.toString()
//                if (p == sharedP.getString(SessionManager.PASSWORD, "")) {
//                    dialog.dismiss()
//                    excelHelper.saveFile(name, folio, amount, "Ghc $amount for $name")
//                    viewModel.fetchDues()
//                    viewModel.notifyFileChange()
//                    val fragment = arguments?.get("fragment") as String
//                    if (fragment == "FragmentDuesDetailView") {
//                        findNavController().navigate(R.id.action_move_dues_detail_view)
//                    } else {
//                        findNavController().navigate(R.id.action_move_treasurer_tools)
//                    }
//                } else {
//                    val fragment = arguments?.get("fragment") as String
//                    if (fragment == "FragmentDuesDetailView") {
//                        findNavController().navigate(R.id.action_move_dues_detail_view)
//                    } else {
//                        findNavController().navigate(R.id.action_move_treasurer_tools)
//                    }
//                    showDialog("ERROR", "Wrong password")
//                }
//            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
//            .show()
//    }

    private fun showExelFileNotExistDial() {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("ERROR")
            .setMessage("The excel file does not exist. Make sure it is created first")
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    private fun publishExcel() {
        val duesDir = File(App.applicationContext().filesDir, "Dues")
        val excelFile = excelHelper.getExcelFile()
        if (excelFile == null) {
            showExelFileNotExistDial()
            return
        }

        val excelUri = excelFile.absolutePath
        val pDial = MyAlertDialog(requireContext(), "", "", false).show()
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
}
