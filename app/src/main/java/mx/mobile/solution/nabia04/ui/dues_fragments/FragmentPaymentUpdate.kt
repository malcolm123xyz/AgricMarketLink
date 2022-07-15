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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, excelHelper.names)
        vb?.spinner?.adapter = adapter
        vb?.spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                val member = excelHelper.members[i]
                vb?.folioEdit?.setText(member.folio)
                vb?.nameEdit?.setText(member.name)
                vb?.folioEdit?.isEnabled = spinner.selectedItemPosition == 0
                vb?.nameEdit?.isEnabled = spinner.selectedItemPosition == 0
            }


            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        vb?.update?.setOnClickListener {
            Log.i("TAG", "ON_cLICK")
            val folio = vb?.folioEdit?.text.toString()
            val name = vb?.nameEdit?.text.toString()
            val amount = vb?.amountEdit?.text.toString()

            if (folio.isEmpty()) {
                showDialog("WARNING", "Folio cannot be empty")
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                showDialog("WARNING", "Folio cannot be empty")
                return@setOnClickListener
            }
            if (amount.isEmpty()) {
                showDialog("WARNING", "Folio cannot be empty")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                doUpdate(amount, name, folio)
            }
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
