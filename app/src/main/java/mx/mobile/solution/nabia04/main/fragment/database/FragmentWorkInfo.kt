package mx.mobile.solution.nabia04.main.fragment.database

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import kotlinx.android.synthetic.main.fragment_person_details.*
import kotlinx.android.synthetic.main.fragment_work_info.*
import kotlinx.android.synthetic.main.fragment_work_info.district_spinner
import kotlinx.android.synthetic.main.fragment_work_info.region_spinner
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04.activities.ActivityUpdateUserData.Companion.newImageUri
import mx.mobile.solution.nabia04.activities.DatabaseUpdateViewModel
import mx.mobile.solution.nabia04.databinding.FragmentWorkInfoBinding
import mx.mobile.solution.nabia04.main.MainActivity.Companion.sharedP
import mx.mobile.solution.nabia04.utilities.BackgroundTasks
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.MyAlertDialog
import mx.mobile.solution.nabia04.utilities.SessionManager
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseResponse
import java.io.IOException
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDone.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentWorkInfo : BaseFragment<FragmentWorkInfoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_work_info
    override fun getCallBack(): OnBackPressedCallback = callback

    private var endpoint: MainEndpoint? = null
    private var userData: DatabaseObject? = null
    private var updateMode: DatabaseUpdateViewModel? = null
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

    private val TAG: String = "FragmentWorkInfo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateMode = ViewModelProvider(requireActivity()).get(DatabaseUpdateViewModel::class.java)
        endpoint = getEndpointObject()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButtonSend.setOnClickListener { onNext() }
        backButtonWork.setOnClickListener {findNavController().navigate(R.id.action_work_info_move_back) }

        updateMode?.getValue()?.observe(viewLifecycleOwner) { updateModel: DatabaseObject? ->
            userData = updateModel
            specific_organisation.setText(userData?.specificOrg)
            name_of_establishment.setText(userData?.nameOfEstablishment)
            job_description.setText(userData?.jobDescription)

            employment_status_spinner.setSelection((employment_status_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.employmentStatus ?: ""))
            employment_sector_spinner.setSelection((employment_sector_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.employmentSector ?: ""))

            Log.i(TAG, "Region selected index = "+userData?.regionOfResidence);

            region_spinner.setSelection((region_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.establishmentRegion ?: ""))

            val selRegIndex = region_spinner.selectedItemPosition

            if(selRegIndex  > 0){
                val selectedArray: Array<String> = resources.getStringArray(regionsId[selRegIndex])
                val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, selectedArray)
                district_spinner.adapter = adapter
                val selDistrict = userData?.districtOfResidence ?: "SELECT DISTRICT"
                Log.i("FragPersonalDetails", "District_1 = $selDistrict")
                district_spinner.setSelection((district_spinner.adapter as ArrayAdapter<String>)
                    .getPosition(userData?.establishmentDist ?: ""))
            }

            specific_organisation.setText(userData?.specificOrg)
            name_of_establishment.setText(userData?.nameOfEstablishment)
            job_description.setText(userData?.jobDescription)
            region_spinner.onItemSelectedListener = OnRegionSpinnerAdapterClick()
        }
    }

    val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING").setMessage("Do you want to cancel the update?")
                .setPositiveButton("YES") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss()
                    requireActivity().finish()
                }.setNegativeButton("NO") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss() }.show()

        }
    }

    inner class OnRegionSpinnerAdapterClick : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
            if (i != 0) {
                Log.i(TAG, "ADAPTER SELECTED INDEX = $i")
                val selectedArray: Array<String> = resources.getStringArray(regionsId[i])
                val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, selectedArray)
                district_spinner.adapter = adapter
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    }

    inner class OnDistSpinnerSelect : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {

        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    }

    fun onNext(){

        userData?.employmentStatus = employment_status_spinner.selectedItem.toString()
        userData?.employmentSector = employment_sector_spinner.selectedItem.toString()
        userData?.establishmentRegion = region_spinner.selectedItem.toString()
        if(district_spinner.selectedItem != null){
            userData?.establishmentDist = district_spinner.selectedItem.toString()
        }
        userData?.specificOrg = specific_organisation.text.toString()
        userData?.nameOfEstablishment = name_of_establishment.text.toString()
        userData?.jobDescription = job_description.text.toString()

        val errorStr = validateDataModel(userData)
        if (errorStr.isEmpty()) {
            updateMode?.setValue(userData!!)
              send(userData!!)
        } else {
            warnAndSend(errorStr)
        }
    }

    private fun send(data: DatabaseObject) {

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
                if (p == sharedP.getString(SessionManager.PASSWORK, "")) {
                    dialog.dismiss()
                    sendPicture()
                } else {
                    showAlertDialog("ERROR", "Wrong password")
                }
            }.setNegativeButton("CANCEL") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
    }

    private fun sendPicture() {
        if (newImageUri.isNotEmpty()) {
            val pDialog = MyAlertDialog(requireContext(),"UPDATING USER DATA", "Uploading picture... Please wait")
            val id = System.currentTimeMillis().toString()
            MediaManager.get().upload(newImageUri)
                .option("resource_type", "auto")
                .unsigned("my_preset")
                .option("public_id", "Nabia04/database/$id")
                .option("cloud_name", "callmanager")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        pDialog.show()
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        pDialog.dismiss()
                        val url = resultData["secure_url"] as String?
                        userData?.imageUri = url
                        doUpdate(ActivityUpdateUserData.userFolio)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        pDialog.dismiss()
                        showAlertDialog("FAILED", "Failed to upload picture. Please try again")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        } else {
            doUpdate(ActivityUpdateUserData.userFolio)
        }
    }

    private fun doUpdate(id: String){
        object: BackgroundTasks(){
            val pDialog = MyAlertDialog(requireContext(),"UPDATING USER DATA","Sending Data... Please wait")
            var response: DatabaseResponse? = null
            override fun onPreExecute() {
                pDialog.show()
            }

            override fun doInBackground() {
                val token = sharedP?.getString(SessionManager.LOGIN_TOKEN, "")
                try {
                    if (ActivityUpdateUserData.selectedFolio.isEmpty()) {
                        response = endpoint?.addNewMember(userData)?.execute()
                    } else {
                        response = endpoint?.insertDataModel(userData)?.execute()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onPostExecute() {
                pDialog.dismiss()
                if (response?.returnCode == Cons.OK) {
                    AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                        .setTitle("SUCCESS")
                        .setCancelable(false)
                        .setMessage("Task was successful")
                        .setPositiveButton(
                            "OK"
                        ) { dialog, id -> dialog.dismiss()
                            requireActivity().finish()}.show()
                } else {
                    showAlertDialog("ERROR", "An error occurred. Please try again")
                }
            }

        }.execute()
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
            updateMode?.setValue(userData!!)
            send(userData!!)
        }
        dialog.setNegativeButton(
            "BACK"
        ) { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun validateDataModel(data: DatabaseObject?): List<String> {
        val errorList: MutableList<String> = ArrayList()

        if (data?.employmentStatus == null || data.employmentStatus.lowercase(Locale.getDefault())
                .contains("select")) {
            errorList.add("Invalid Employment status")
        }
        if (data?.employmentSector == null || data.employmentSector.lowercase(Locale.getDefault())
                .contains("select")) {
            errorList.add("Invalid Employment Sector")
        }

        if (data?.establishmentRegion == null || data.establishmentRegion.lowercase(Locale.getDefault())
                .contains("select")) {
            errorList.add("Invalid Establishment region")
        }
        if (data?.establishmentDist == null || data.establishmentDist.lowercase(Locale.getDefault())
                .contains("select")) {
            errorList.add("Invalid establishment district")
        }

        if (data?.nameOfEstablishment == null || data.nameOfEstablishment.isEmpty()) {
            errorList.add("Invalid Establishment name") }
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

    private fun getEndpointObject(): MainEndpoint? {
        if (endpoint == null) {
            val builder = MainEndpoint.Builder(
                AndroidHttp.newCompatibleTransport(),
                AndroidJsonFactory(),
                null
            ).setRootUrl(Cons.ROOT_URL)
            endpoint = builder.build()
        }
        return endpoint
    }
}