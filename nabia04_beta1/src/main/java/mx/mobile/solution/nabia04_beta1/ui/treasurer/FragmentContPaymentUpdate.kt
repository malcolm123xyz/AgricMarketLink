package mx.mobile.solution.nabia04_beta1.ui.treasurer

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.alarm.MyAlarmManager
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.view_models.DBViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentContPaymentUpdateBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.endpoint
import mx.mobile.solution.nabia04_beta1.utilities.Response
import mx.mobile.solution.nabia04_beta1.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseContributionData
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FragmentContPaymentUpdate : Fragment() {

    private var longDate: Long = 0L
    private val fd = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)

    private val viewModel by activityViewModels<DBViewModel>()

    private var folio = ""
    private var name = ""
    private var amount = ""
    private var spinnerPos = -1
    private lateinit var userDataList: MutableList<EntityUserData>

    private var _binding: FragmentContPaymentUpdateBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContPaymentUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            binding.progressBar?.visibility = View.VISIBLE
            val members = getMembers()
            members[0] = "Select user"
            binding.spinner?.adapter =
                ArrayAdapter(requireContext(), R.layout.simple_spinner_item, members)
            binding.progressBar?.visibility = View.GONE
        }

        binding.spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(ad: AdapterView<*>?, view: View?, i: Int, l: Long) {
                spinnerPos = i
                if (i > 0) {
                    val member = userDataList[i]
                    folio = member.folioNumber
                    name = member.fullName
                    binding.holder?.visibility = View.GONE
                } else {
                    binding.holder?.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        binding.update?.setOnClickListener {
            amount = binding.amountEdit?.text.toString()
            if (spinnerPos < 1) {
                folio = binding.folioEdit?.text.toString()
                name = binding.nameEdit?.text.toString()
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

        binding.date?.setOnClickListener {
            MyAlarmManager(requireContext()).showDayMonthPicker(object : MyAlarmManager.CallBack {
                override fun done(alarmTime: Long) {
                    val date = fd.format(Date(alarmTime))
                    longDate = alarmTime
                    binding.tvDeadline?.text = date
                }
            })
        }
    }

    private suspend fun getMembers(): Array<String> {
        val list = viewModel.getUserNames()
        val mutableList: MutableList<String> = ArrayList()
        if (list != null) {
            userDataList = list.toMutableList()
            userDataList.add(0, EntityUserData())
            for (item in userDataList) {
                val name = item.fullName
                mutableList.add(name)
            }
        }
        return mutableList.toTypedArray()
    }

    private fun showWarningDialog(amount: String, name: String, folio: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("You are about to update the contribution list. Are you sure about this?")
            .setPositiveButton("YES") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                val p = ProgressDialog(requireContext())
                p.setTitle("PAYMENT UPDATE")
                p.setMessage("Updating contribution... Please wait")
                p.setCancelable(false)
                p.show()
                lifecycleScope.launch {
                    val response = withContext(Dispatchers.IO) {
                        doUpdate(amount, name, folio)
                    }
                    p.dismiss()
                    if (response.status == Status.SUCCESS) {
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_move_back)
                    } else {
                        showDialog("FAILED", response.message.toString())
                    }
                }
            }.setNegativeButton("NO") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }.show()
    }

    private fun doUpdate(amount: String, name: String, folio: String): Response<String> {

        val cData: ResponseContributionData? = endpoint.contributions.execute()

        if (cData != null) {
            if (cData.data.contribution == null) {
                val newPayments: List<Map<String, String>> = ArrayList()
                cData.data.contribution = newPayments
            }
            for (map in cData.data.contribution) {
                val thisFolio = map["folio"]
                if (thisFolio == folio) {
                    return Response.error("This person has already paid for this contribution", "")
                }
            }
            val map: MutableMap<String, String> = HashMap()
            map["name"] = name
            map["folio"] = folio
            map["payment"] = amount
            map["date"] = fd.format(Date(longDate))
            cData.data.contribution.add(map)
            val response = endpoint.upDateContPayment(cData.data).execute()
            return if (response.status == Status.SUCCESS.toString()) {
                Response.success(null)
            } else {
                Response.error(response.message, "")
            }
        } else {
            return Response.error(
                "Contribution item not set. Please set a contribution topic/item first",
                ""
            )
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
