package mx.mobile.solution.nabia04.ui.database_fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_work_info.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBUpdateViewModel
import mx.mobile.solution.nabia04.data.view_models.NetworkViewModel
import mx.mobile.solution.nabia04.databinding.FragmentWorkInfoBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData.Companion.newImageUri
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData.Companion.selectedFolio
import mx.mobile.solution.nabia04.utilities.MyAlertDialog
import mx.mobile.solution.nabia04.utilities.Response
import mx.mobile.solution.nabia04.utilities.SessionManager
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentWorkInfo : BaseFragment<FragmentWorkInfoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_work_info

    private var other = ""
    private val updateModel by activityViewModels<DBUpdateViewModel>()

    private val networkViewModel by viewModels<NetworkViewModel>()

    @Inject
    lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var endpoint: MainEndpoint

    private lateinit var userData: EntityUserData
    private val regionsId = intArrayOf(
        0,
        R.array.ahafo,
        R.array.ashanti,
        R.array.bono_east,
        R.array.bono,
        R.array.easthern,
        R.array.accra,
        R.array.northern,
        R.array.oti,
        R.array.savanna,
        R.array.upper_east,
        R.array.upper_west,
        R.array.volta,
        R.array.western,
        R.array.western_north,
        R.array.central,
        R.array.north_east
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButtonSend.setOnClickListener { onNext() }
        backButtonWork.setOnClickListener { findNavController().navigate(R.id.action_work_info_move_back) }

        updateModel.getDataToObserve(selectedFolio)
            .observe(viewLifecycleOwner) { user: EntityUserData? ->

                if (user != null) {
                    userData = user
                    specific_organisation.setText(userData.specificOrg)
                    name_of_establishment.setText(userData.nameOfEstablishment)
                    job_description.setText(userData.jobDescription)

                    employment_status_spinner.setSelection(
                        (employment_status_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(userData.employmentStatus)
                    )
                    employment_sector_spinner.setSelection(
                        (employment_sector_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(userData.employmentSector)
                    )
                    region_spinner.setSelection(
                        (region_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(userData.establishmentRegion)
                    )

                    val selRegIndex = region_spinner.selectedItemPosition

                    if (selRegIndex > 0) {
                        val selectedArray: Array<String> =
                            resources.getStringArray(regionsId[selRegIndex])
                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.simple_spinner_item,
                            selectedArray
                        )
                        district_spinner.adapter = adapter
                        val selDistrict = userData.districtOfResidence
                        Log.i("FragPersonalDetails", "District_1 = $selDistrict")
                        district_spinner.setSelection(
                            (district_spinner.adapter as ArrayAdapter<String>)
                                .getPosition(userData.establishmentDist)
                        )
                    }

                    specific_organisation.setText(userData.specificOrg)
                    name_of_establishment.setText(userData.nameOfEstablishment)
                    job_description.setText(userData.jobDescription)
                    region_spinner.onItemSelectedListener = OnRegionSpinnerAdapterClick()
                }
            }

        vb?.employmentStatusSpinner?.onItemSelectedListener = OnEmploymentStatusClickListener()

        vb?.employmentSectorSpinner?.onItemSelectedListener = OnEmploymentSectorClickListener()

        listenOnBackPressed()
    }

    private inner class OnEmploymentSectorClickListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapter: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {

            if (employment_sector_spinner.selectedItem.toString() == "Other") {
                otherDial()
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
    }

    private fun otherDial() {
        val v: View = layoutInflater.inflate(R.layout.other_layout, null)
        val alert = AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setView(v)
            .show()
        val questionEdit = v.findViewById<TextView>(R.id.message)
        val sendButt = v.findViewById<Button>(R.id.send)
        sendButt.setOnClickListener {
            val txt = questionEdit.text.toString()
            if (txt.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Input cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                other = txt
                alert.dismiss()
            }
        }
    }

    private inner class OnEmploymentStatusClickListener : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            if (p2 == 1) {
                vb?.holder?.visibility = View.VISIBLE
            } else {
                vb?.holder?.visibility = View.GONE
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }

    }

    private fun listenOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onResume() {
        super.onResume()
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onResume()")
        callback.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        callback.isEnabled = false
        println("🏠 ${this.javaClass.simpleName} #${this.hashCode()}  onPause()")
    }

    val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING").setMessage("Do you want to cancel the update?")
                .setPositiveButton("YES") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss()
                    requireActivity().finish()
                }.setNegativeButton("NO") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss()
                }.show()

        }
    }

    inner class OnRegionSpinnerAdapterClick : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
            if (i != 0) {
                val selectedArray: Array<String> = resources.getStringArray(regionsId[i])
                val adapter =
                    ArrayAdapter(requireContext(), R.layout.simple_spinner_item, selectedArray)
                district_spinner.adapter = adapter
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    }


    fun onNext() {
        userData.employmentStatus = employment_status_spinner.selectedItem.toString()
        userData.employmentSector = employment_sector_spinner.selectedItem.toString()
        if (employment_sector_spinner.selectedItem.toString() == "Other") {
            userData.employmentSector = other
        }
        userData.establishmentRegion = region_spinner.selectedItem.toString()
        if (district_spinner.selectedItem != null) {
            userData.establishmentDist = district_spinner.selectedItem.toString()
        }
        userData.specificOrg = specific_organisation.text.toString()
        userData.nameOfEstablishment = name_of_establishment.text.toString()
        userData.jobDescription = job_description.text.toString()

        val errorStr = validateDataModel(userData)
        if (errorStr.isEmpty()) {
            updateModel.postData(userData)
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING")
                .setMessage("You are about to update the Database. Do you want to continue?")
                .setPositiveButton("YES") { dialog, id ->
                    dialog.dismiss()
                    showPassWordDialog()
                }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
        } else {
            warnAndSend(errorStr)
        }
    }

    private fun send(newImageUri: String) {
        val pDial = MyAlertDialog(requireContext(), "DATABASE", "", false).show()
        networkViewModel.upDateUserData(userData, newImageUri)
            .observe(viewLifecycleOwner) { response: Response<String> ->
                when (response.status) {
                    Status.SUCCESS -> {
                        pDial.dismiss()
                        Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                        requireActivity().finish()
                    }
                    Status.LOADING -> {
                        Log.i("TAG", "Loading: ${response.data}")
                        response.data?.let { pDial.setMessage(it) }
                    }
                    Status.ERROR -> {
                        pDial.dismiss()
                        showAlertDialog("ERROR", "An error has occurred: ${response.data}")
                    }
                    else -> {}
                }
            }
    }


    private fun showPassWordDialog() {
        val linf = LayoutInflater.from(requireContext())
        val v = linf.inflate(R.layout.request_passwrd, null)
        val passEdit = v.findViewById<EditText>(R.id.password_edit)
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Enter Password")
            .setView(v)
            .setCancelable(false)
            .setPositiveButton(
                "OK"
            ) { dialog: DialogInterface, _: Int ->
                val p = passEdit.text.toString()
                if (p == sharedP.getString(SessionManager.PASSWORD, "")) {
                    dialog.dismiss()
                    send(newImageUri)
                } else {
                    showAlertDialog("ERROR", "Wrong password")
                }
            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun warnAndSend(errors: List<String>) {
        val v = layoutInflater.inflate(R.layout.send_warning_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("INVALID ENTRIES")
        dialog.setView(v)
        val lv = v.findViewById<ListView>(R.id.list)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, errors)
        lv.adapter = adapter
        dialog.setPositiveButton("PROCEED") { d, id ->
            d.dismiss()
            Log.i("TAG", "Model imageUri: ${userData.imageUri}")
            updateModel.postData(userData)
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING")
                .setMessage("You are about to update the Database. Do you want to continue?")
                .setPositiveButton("YES") { dialog, id ->
                    dialog.dismiss()
                    showPassWordDialog()
                }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
        }
        dialog.setNegativeButton(
            "BACK"
        ) { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun validateDataModel(data: EntityUserData?): List<String> {
        val errorList: MutableList<String> = ArrayList()

        if (data?.employmentStatus == null || data.employmentStatus.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Employment status")
        }
        if (data?.employmentSector == null || data.employmentSector.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Employment Sector")
        }

        if (data?.establishmentRegion == null || data.establishmentRegion.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Establishment region")
        }
        if (data?.establishmentDist == null || data.establishmentDist.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid establishment district")
        }

        if (data?.nameOfEstablishment == null || data.nameOfEstablishment.isEmpty()) {
            errorList.add("Invalid Establishment name")
        }
        if (data?.jobDescription == null || data.jobDescription.isEmpty()) {
            errorList.add("Invalid Jog Description")
        }
        if (data?.specificOrg == null || data.specificOrg.isEmpty()) {
            errorList.add("Invalid specific organisation")
        }

        return errorList
    }

    private fun showAlertDialog(t: String, s: String) {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle(t)
            .setMessage(s)
            .setPositiveButton(
                "OK"
            ) { dialog, id -> dialog.dismiss() }.show()
    }

}