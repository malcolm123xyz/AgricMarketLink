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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_work_info.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBUpdateViewModel
import mx.mobile.solution.nabia04.databinding.FragmentWorkInfoBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData.Companion.newImageUri
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData.Companion.selectedFolio
import mx.mobile.solution.nabia04.utilities.MyAlertDialog
import mx.mobile.solution.nabia04.utilities.SessionManager
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.ResponseString
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

@AndroidEntryPoint
class FragmentWorkInfo : BaseFragment<FragmentWorkInfoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_work_info

    private val updateModel by activityViewModels<DBUpdateViewModel>()

    @Inject
    lateinit var sharedP: SharedPreferences

    @Inject
    lateinit var endpoint: MainEndpoint

    private var userData: EntityUserData? = null
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
                    specific_organisation.setText(userData?.specificOrg)
                    name_of_establishment.setText(userData?.nameOfEstablishment)
                    job_description.setText(userData?.jobDescription)

                    employment_status_spinner.setSelection(
                        (employment_status_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(userData?.employmentStatus ?: "")
                    )
                    employment_sector_spinner.setSelection(
                        (employment_sector_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(userData?.employmentSector ?: "")
                    )
                    region_spinner.setSelection(
                        (region_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(userData?.establishmentRegion ?: "")
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
                        val selDistrict = userData?.districtOfResidence ?: "SELECT DISTRICT"
                        Log.i("FragPersonalDetails", "District_1 = $selDistrict")
                        district_spinner.setSelection(
                            (district_spinner.adapter as ArrayAdapter<String>)
                                .getPosition(userData?.establishmentDist ?: "")
                        )
                    }

                    specific_organisation.setText(userData?.specificOrg)
                    name_of_establishment.setText(userData?.nameOfEstablishment)
                    job_description.setText(userData?.jobDescription)
                    region_spinner.onItemSelectedListener = OnRegionSpinnerAdapterClick()
                }
            }

        vb?.employmentStatusSpinner?.onItemSelectedListener = OnEmploymentStatusClickListener()


        listenOnBackPressed()
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

        userData?.employmentStatus = employment_status_spinner.selectedItem.toString()
        userData?.employmentSector = employment_sector_spinner.selectedItem.toString()
        userData?.establishmentRegion = region_spinner.selectedItem.toString()
        if (district_spinner.selectedItem != null) {
            userData?.establishmentDist = district_spinner.selectedItem.toString()
        }
        userData?.specificOrg = specific_organisation.text.toString()
        userData?.nameOfEstablishment = name_of_establishment.text.toString()
        userData?.jobDescription = job_description.text.toString()

        val errorStr = validateDataModel(userData)
        if (errorStr.isEmpty()) {
            updateModel.postData(userData!!)
            send()
        } else {
            warnAndSend(errorStr)
        }
    }

    private fun send() {
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("WARNING")
            .setMessage("You are about to update the Database. Do you want to continue?")
            .setPositiveButton("YES") { dialog, id ->
                dialog.dismiss()
                showPassWordDialog()
            }.setNegativeButton("NO") { dialog, id -> dialog.dismiss() }.show()
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
                    sending()
                } else {
                    showAlertDialog("ERROR", "Wrong password")
                }
            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun sending() {
        val pDial = MyAlertDialog(requireContext(), "DATABASE", "Updating...", false).show()
        lifecycleScope.launch {
            sendPicture(pDial)
        }
    }


    private suspend fun sendPicture(pDial: MyAlertDialog) {
        if (newImageUri.isEmpty()) {
            withContext(IO) {
                doDataUpdate(pDial, "")
            }
            return
        }

        val id = System.currentTimeMillis().toString()
        MediaManager.get().upload(newImageUri)
            .option("resource_type", "auto")
            .unsigned("my_preset")
            .option("public_id", "Nabia04/database/$id")
            .option("cloud_name", "callmanager")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.i("TAG", "Sending image started...")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    Log.i("TAG", "onSuccess called")
                    val imageUri = resultData["secure_url"].toString()
                    lifecycleScope.launch {
                        withContext(IO) {
                            doDataUpdate(pDial, imageUri)
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    pDial.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Failed to upload picture. Please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

//    private suspend fun upDateDate(pDialog: MyAlertDialog): Resource<EntityUserData> {
//        return withContext(Dispatchers.IO){
//            doDataUpdate(pDialog)
//        }
//
//    }

    private fun doDataUpdate(pDial: MyAlertDialog, imageUri: String) {
        var erMsg = ""
        val response: ResponseString
        userData?.imageUri = imageUri
        try {
            response = if (selectedFolio.isEmpty()) {
                endpoint.addNewMember(getBackendModel(userData)).execute()
            } else {
                endpoint.insertDataModel(getBackendModel(userData)).execute()
            }
            pDial.dismiss()
            if (response != null) {
                lifecycleScope.launch {
                    when (response.status) {
                        Status.SUCCESS.toString() -> {
                            lifecycleScope.launch {
                                Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                            }
                            requireActivity().finish()
                        }
                        else -> {
                            showAlertDialog("FAILED", response.message)
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            pDial.dismiss()
            lifecycleScope.launch {
                erMsg = if (ex is SocketTimeoutException || ex is SSLHandshakeException ||
                    ex is UnknownHostException
                ) {
                    "Cause: NO INTERNET CONNECTION"
                } else {
                    ex.localizedMessage ?: ""
                }
                showAlertDialog("ERROR", erMsg)
                ex.printStackTrace()
            }
        }
    }

    private fun warnAndSend(errors: List<String>) {
        val v = layoutInflater.inflate(R.layout.dialog_view1, null)
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("INVALID ENTRIES")
        dialog.setView(v)
        val lv = v.findViewById<ListView>(R.id.list)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, errors)
        lv.adapter = adapter
        dialog.setPositiveButton("PROCEED") { d, id ->
            d.dismiss()
            updateModel.postData(userData!!)
            send()
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

    private fun getBackendModel(obj: EntityUserData?): DatabaseObject {
        val u = DatabaseObject()
        if (obj != null) {
            u.birthDayAlarm = obj.birthDayAlarm
            u.className = obj.className
            u.contact = obj.contact
            u.courseStudied = obj.courseStudied
            u.districtOfResidence = obj.districtOfResidence
            u.email = obj.email
            u.folioNumber = obj.folioNumber
            u.homeTown = obj.homeTown
            u.house = obj.house
            u.imageId = obj.imageId
            u.imageUri = obj.imageUri
            u.nickName = obj.nickName
            u.jobDescription = obj.jobDescription
            u.specificOrg = obj.specificOrg
            u.employmentStatus = obj.employmentStatus
            u.employmentSector = obj.employmentSector
            u.nameOfEstablishment = obj.nameOfEstablishment
            u.establishmentRegion = obj.establishmentRegion
            u.establishmentDist = obj.establishmentDist
            u.positionHeld = obj.positionHeld
            u.regionOfResidence = obj.regionOfResidence
            u.sex = obj.sex
            u.fullName = obj.fullName
            u.survivingStatus = obj.survivingStatus
            u.dateDeparted = obj.dateDeparted
            u.biography = obj.biography
            u.tributes = obj.tributes
        }
        return u
    }
}