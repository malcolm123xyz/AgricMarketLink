package mx.mobile.solution.nabia04.ui.database_fragments

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_person_details.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.alarm.MyAlarmManager.CallBack
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBUpdateViewModel
import mx.mobile.solution.nabia04.databinding.FragmentPersonDetailsBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData.Companion.newImageUri
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData.Companion.selectedFolio
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.Utils
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentPersonDetails : BaseFragment<FragmentPersonDetailsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_person_details

    private val updateModel by activityViewModels<DBUpdateViewModel>()

    @Inject
    lateinit var sharedP: SharedPreferences

    private val storagPermission: Int = 8574
    private var birthDayValue: Long? = null
    private var strFutureDate: String = ""
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

    private var updateObj: EntityUserData? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        folio_edit_view.setText(selectedFolio)

        folio_edit_view.isEnabled = selectedFolio.isEmpty()

        region_spinner.onItemSelectedListener = OnRegionSpinnerAdapterClick()

        Log.i("TAG", "onViewCreated()")

        updateModel.getDataToObserve(selectedFolio)
            .observe(viewLifecycleOwner) { userData: EntityUserData? ->
                if (userData != null) {
                    updateObj = userData
                    show()
                }
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
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), storagPermission
                )
            } else {
                contentLauncher!!.launch("image/*")
            }
        }

        buttonCancel.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING").setMessage("Do you want to cancel the update?")
                .setPositiveButton("YES") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss()
                    requireActivity().finish()
                }.setNegativeButton("NO") { dialog: DialogInterface, id: Int ->
                    dialog.dismiss()
                }.show()
        }

        nextButtonPersonalD.setOnClickListener { onNext() }

        registerPicturePickerActivityResults()

        listenOnBackPressed()
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

    private fun show() {
        Log.i("TAG", "showing views....()")
        val nn = updateObj?.nickName ?: ""
        var name: String = updateObj?.fullName ?: ""

        if (nn.isEmpty()) {
            name = "$name ($nn)"
        }

        folio_edit_view.setText(updateObj?.folioNumber ?: "")
        fullNameTv.text = name
        fullnameEditView.setText(updateObj?.fullName ?: "")
        val selSex = updateObj?.sex ?: "SELECT SEX"
        sex_spinner.setSelection((sex_spinner.adapter as ArrayAdapter<String>).getPosition(selSex))
        contact.setText(updateObj?.contact ?: "")
        email.setText(updateObj?.email ?: "")
        nickName_edit_view.setText(nn)
        homeTown_edit_view.setText(updateObj?.homeTown ?: "")

        birthDayValue = updateObj?.birthDayAlarm ?: 0

        birthDayButton.text = fd.format(Date(birthDayValue!!))

        val selRegion = updateObj?.regionOfResidence ?: "SELECT REGION"
        region_spinner.setSelection(
            (region_spinner.adapter as ArrayAdapter<String>).getPosition(
                selRegion
            )
        )
        val selRegIndex = region_spinner.selectedItemPosition

        if (selRegIndex > 0) {
            val selectedArray: Array<String> = resources.getStringArray(regionsId[selRegIndex])
            val adapter =
                ArrayAdapter(requireContext(), R.layout.simple_spinner_item, selectedArray)
            district_spinner.adapter = adapter
            adapter.notifyDataSetChanged()
            val selDistrict = updateObj?.districtOfResidence ?: "SELECT DISTRICT"
            val selPos = (district_spinner.adapter as ArrayAdapter<String>).getPosition(selDistrict)
            district_spinner.setSelection(selPos)
        }

        GlideApp.with(requireContext())
            .load(updateObj?.imageUri ?: "")
            .placeholder(R.drawable.use_icon)
            .apply(RequestOptions.circleCropTransform())
            .signature(ObjectKey(updateObj?.imageId ?: ""))
            .into(profilePicture)
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
            }
            GlideApp.with(requireContext())
                .load(file)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(profilePicture)
        }
    }

    fun onNext() {
        if (updateObj == null) {
            updateObj = EntityUserData()
        }
        updateObj?.folioNumber = folio_edit_view.text.toString()
        updateObj?.fullName = fullnameEditView.text.toString()
        updateObj?.contact = contact.text.toString()
        updateObj?.email = email.text.toString()
        updateObj?.nickName = nickName_edit_view.text.toString()
        sharedP.edit()?.putString("nickName", nickName_edit_view.text.toString())?.apply()
        updateObj?.sex = sex_spinner.selectedItem.toString()
        updateObj?.birthDayAlarm = birthDayValue ?: 0
        updateObj?.homeTown = homeTown_edit_view.text.toString()
        updateObj?.regionOfResidence = region_spinner.selectedItem.toString()

        updateObj?.districtOfResidence = ""


        if (district_spinner.selectedItem != null) {
            updateObj?.districtOfResidence = district_spinner.selectedItem.toString()
            Log.i(
                "FragPerson",
                "District spinner selected item: " + district_spinner.selectedItem.toString()
            )
        } else {
            Log.i("FragPerson", "district_spinner.selectedItem")
        }

        val folio = folio_edit_view.text.toString()
        val name = fullnameEditView.text.toString()

        if (folio.isBlank() || name.isBlank()) {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("ERROR")
                .setMessage("FOLIO NUMBER or NAME not set. The Database cannot be updated without this two parameters")
                .setPositiveButton(
                    "OK"
                ) { dialog, id -> dialog.dismiss() }.show()
            return
        }

        val errorStr = validateDataModel(updateObj)

        if (errorStr.isEmpty()) {
            updateObj?.let { updateModel.postData(it) }
            findNavController().navigate(R.id.action_personal_data_move_forward)
        } else {
            warnAndSend(errorStr)
        }
    }

    private fun validateDataModel(data: EntityUserData?): List<String> {
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
        if (data?.birthDayAlarm == null) {
            errorList.add("Invalid Date of birth")
        }
        if (newImageUri.isEmpty() && data?.imageUri.isNullOrEmpty()) {
            errorList.add("No profile picture set")
        }
        return errorList
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
                val selDistrict = updateObj?.districtOfResidence ?: "SELECT DISTRICT"
                val selPos =
                    (district_spinner.adapter as ArrayAdapter<String>).getPosition(selDistrict)
                district_spinner.setSelection(selPos)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}
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
            updateObj?.let { updateModel.postData(it) }
            findNavController().navigate(R.id.action_personal_data_move_forward)
        }
        dialog.setNegativeButton(
            "BACK"
        ) { d, id -> d.dismiss() }
        dialog.show()
    }

}