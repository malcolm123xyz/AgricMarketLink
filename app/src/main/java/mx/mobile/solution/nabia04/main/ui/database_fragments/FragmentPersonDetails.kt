package mx.mobile.solution.nabia04.main.ui.database_fragments

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import kotlinx.android.synthetic.main.fragment_database_detail.*
import kotlinx.android.synthetic.main.fragment_done.*
import kotlinx.android.synthetic.main.fragment_person_details.*
import kotlinx.android.synthetic.main.fragment_person_details.district_spinner
import kotlinx.android.synthetic.main.fragment_person_details.email
import kotlinx.android.synthetic.main.fragment_person_details.region_spinner
import kotlinx.android.synthetic.main.fragment_work_info.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.alarm.MyAlarmManager.CallBack
import mx.mobile.solution.nabia04.databinding.FragmentPersonDetailsBinding
import mx.mobile.solution.nabia04.main.ui.activities.ActivityUpdateUserData.Companion.newImageUri
import mx.mobile.solution.nabia04.main.ui.activities.ActivityUpdateUserData.Companion.selectedFolio
import mx.mobile.solution.nabia04.main.ui.activities.DatabaseUpdateViewModel
import mx.mobile.solution.nabia04.main.ui.activities.MainActivity.Companion.sharedP
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.Utils
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDone.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentPersonDetails : BaseFragment<FragmentPersonDetailsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_person_details
    override fun getCallBack(): OnBackPressedCallback = callback

    private val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: Int = 8574
    private var birthDayValue: Long? = null
    private var userData: DatabaseObject? = null
    private var updateMode: DatabaseUpdateViewModel? = null
    private var strFutureDate: String = ""
    private val TAG: String = "FragmentPersonDetails"
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

    private val fd = SimpleDateFormat("EEE, d MMM", Locale.US)

    private var contentLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateMode = ViewModelProvider(requireActivity()).get(DatabaseUpdateViewModel::class.java)
        Log.i(TAG, "onCreate()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

            folio_edit_view.setText(selectedFolio)

            folio_edit_view.isEnabled = selectedFolio.isEmpty()

            updateMode?.getValue()?.observe(viewLifecycleOwner) { updataObject: DatabaseObject? ->
                userData = updataObject
                val nn = userData?.nickName ?: ""
                var name: String = userData?.fullName ?: ""

                if(nn.isEmpty()){
                    name = "$name ($nn)"
                }

                folio_edit_view.setText(userData?.folioNumber ?: "")
                fullNameTv.text = name
                fullnameEditView.setText(userData?.fullName ?: "")
                val selSex = userData?.sex ?: "SELECT SEX"
                sex_spinner.setSelection((sex_spinner.adapter as ArrayAdapter<String>).getPosition(selSex))
                contact.setText(userData?.contact ?: "")
                email.setText(userData?.email ?: "")
                nickName_edit_view.setText(nn)
                homeTown_edit_view.setText(userData?.homeTown ?: "")

                birthDayValue = userData?.birthDayAlarm ?: 0

                birthDayButton.text = fd.format(Date(birthDayValue!!))

                val selRegion = userData?.regionOfResidence ?: "SELECT REGION"
                region_spinner.setSelection((region_spinner.adapter as ArrayAdapter<String>).getPosition(selRegion))

                region_spinner.onItemSelectedListener = OnRegionSpinnerAdapterClick()

                val selRegIndex = region_spinner.selectedItemPosition

                if(selRegIndex  > 0){
                    val selectedArray: Array<String> = resources.getStringArray(regionsId[selRegIndex])
                    val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, selectedArray)
                    district_spinner.adapter = adapter
                    val selDistrict = userData?.districtOfResidence ?: "SELECT DISTRICT"
                    district_spinner.setSelection(adapter.getPosition(selDistrict))
                    adapter.notifyDataSetChanged()
                }

                GlideApp.with(requireContext())
                    .load(userData?.imageUri ?: "")
                    .placeholder(R.drawable.use_icon)
                    .apply(RequestOptions.circleCropTransform())
                    .signature(ObjectKey(userData?.getImageId() ?: ""))
                    .into(profilePicture)
            }

            birthDayButton.setOnClickListener {
                MyAlarmManager(requireContext()).showDayMonthPicker(object : CallBack {
                    override fun done(alarmTime: Long) {
                        birthDayValue = alarmTime
                        val myFormat = SimpleDateFormat("dd MM yyyy")
                        strFutureDate = myFormat.format(alarmTime)
                        birthDayButton.text = strFutureDate
                        Log.i("MyAlarmManager", "Future  date: $strFutureDate")
                    }
                })}

        changePicture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE)
            } else {
                contentLauncher!!.launch("image/*")
            }
        }

        buttonCancel.setOnClickListener{ requireActivity().finish() }

        nextButtonPersonalD.setOnClickListener{ onNext() }

        registerPicturePickerActivityResults()
    }

    private fun registerPicturePickerActivityResults() {
        contentLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            val dir = File(requireContext().cacheDir.toString() + "/annPicture")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "upload_img.jpg")
            var inputStream: InputStream? = null
            try {
                inputStream = requireContext().contentResolver.openInputStream(uri!!)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            Utils.resizeImageFile(inputStream, file, 900, 750)
            val selImgUri = file.absolutePath ?: ""
            if(selImgUri.isNotEmpty()){
                newImageUri = file.absolutePath ?: ""
                userData?.imageUri = newImageUri
            }
            GlideApp.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(profilePicture)
        }
    }

    fun onNext(){
        userData?.folioNumber = folio_edit_view.text.toString()
        userData?.fullName = fullnameEditView.text.toString()
        userData?.contact = contact.text.toString()
        userData?.email = email.text.toString()
        userData?.nickName = nickName_edit_view.text.toString()
        sharedP.edit()?.putString("nickName", nickName_edit_view.text.toString())?.apply()
        userData?.sex =  sex_spinner.selectedItem.toString()
        userData?.birthDayAlarm = birthDayValue ?: 0
        userData?.homeTown = homeTown_edit_view.text.toString()
        userData?.regionOfResidence = region_spinner.selectedItem.toString()

        userData?.districtOfResidence = ""

        if(district_spinner.selectedItem != null){
            userData?.districtOfResidence = district_spinner.selectedItem.toString()
            Log.i("FragPerson", "District spinner selected item: "+district_spinner.selectedItem.toString())
        }else{
            Log.i("FragPerson", "district_spinner.selectedItem")
        }

        if(userData?.folioNumber.isNullOrBlank() || userData?.fullName.isNullOrEmpty()){
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("ERROR")
                .setMessage("FOLIO NUMBER or NAME not set. The Database cannot be updated without this two parameters")
                .setPositiveButton(
                    "OK"
                ) { dialog, id -> dialog.dismiss() }.show()
            return
        }

        val errorStr = validateDataModel(userData)

        if (errorStr.isEmpty()) {
            updateMode?.setValue(userData!!)
            findNavController().navigate(R.id.action_personal_data_move_forward)
        } else {
            warnAndSend(errorStr)
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
                val selectedArray: Array<String> = resources.getStringArray(regionsId[i])
                val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, selectedArray)
                district_spinner.adapter = adapter
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
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
            findNavController().navigate(R.id.action_personal_data_move_forward)
        }
        dialog.setNegativeButton(
            "BACK"
        ) { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun validateDataModel(data: DatabaseObject?): List<String> {
        val errorList: MutableList<String> = ArrayList()
        if (data?.folioNumber == null || data.folioNumber.isEmpty()) {
            errorList.add("Folio number not set")
        }
        if (data?.fullName == null || data.fullName.isEmpty()) {
            errorList.add("Name not set")
        } else if (data.nickName == null || data.nickName.isEmpty()) {
            errorList.add("Invalid Nickname")
        }
        if (data?.sex == null || data.sex.lowercase(Locale.getDefault()).contains("select")) {
            errorList.add("Invalid Sex")
        }
        if (data?.homeTown == null || data.homeTown.isEmpty()) {
            errorList.add("Invalid Hometown")
        }
        if (data?.contact == null || data.contact.length < 10) {
            errorList.add("Invalid Contact Number")
        }
        if (data?.districtOfResidence == null || data.districtOfResidence.isEmpty()) {
            errorList.add("Invalid District of residence")
        }
        if (data?.regionOfResidence == null || data.regionOfResidence.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Region of residence")
        }
        if (data?.email == null || data.email.isEmpty()) {
            errorList.add("Invalid Email Address")
        }
        if (data?.birthDayAlarm!! <  1) {
            errorList.add("Invalid Date of birth")
        }
        if (data.imageUri.isNullOrEmpty()) {
            errorList.add("No profile picture set")
        }
        return errorList
    }
}