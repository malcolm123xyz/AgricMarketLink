package mx.mobile.solution.nabia04.ui.dues_fragments

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_payment_update.*
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentPaymentUpdateBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import mx.mobile.solution.nabia04.utilities.Status
import javax.inject.Inject

@AndroidEntryPoint
class FragmentPaymentUpdate : BaseFragment<FragmentPaymentUpdateBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_payment_update

    @Inject
    lateinit var excelHelper: ExcelHelper

    private var folio = ""
    private var name = ""
    private var amount = ""
    private var spinnerPos = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        excelHelper.names[0] = "New Payment"
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
            if (amount.isEmpty()) {
                showDialog("WARNING", "Amount cannot be empty")
                return@setOnClickListener
            }
            showWarningDialog(amount, name, folio)
        }
    }

    private suspend fun doUpdate(amount: String, name: String, folio: String) {
        val p = ProgressDialog(requireContext())
        p.setTitle("PAYMENT UPDATE")
        p.setMessage("Updating the payment sheet... Please wait")
        p.setCancelable(false)
        p.show()
        val response =
            excelHelper.insertNewPayment(amount.toInt(), name, folio, spinner.selectedItemPosition)
        p.dismiss()
        if (response.status == Status.SUCCESS) {
            showSuccessDialog("PAYMENT UPDATE", "Success")
        } else {
            showDialog("PAYMENT UPDATE", response.message.toString())
        }

    }

    private fun showWarningDialog(amount: String, name: String, folio: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("You are about to make changes to the payment sheet. Are you sure about this?")
            .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                lifecycleScope.launch {
                    doUpdate(amount, name, folio)
                }
            }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    private fun showSuccessDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                findNavController().navigate(R.id.action_move_back)
            }.show()
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
