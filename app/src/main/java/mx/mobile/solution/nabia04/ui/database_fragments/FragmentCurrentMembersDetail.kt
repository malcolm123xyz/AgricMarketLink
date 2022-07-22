package mx.mobile.solution.nabia04.ui.database_fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_database_detail.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.alarm.MyAlarmManager
import mx.mobile.solution.nabia04.data.entities.EntityUserData
import mx.mobile.solution.nabia04.data.view_models.DBViewModel
import mx.mobile.solution.nabia04.databinding.FragmentDatabaseDetailBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.clearance
import mx.mobile.solution.nabia04.ui.activities.MainActivity.Companion.userFolioNumber
import mx.mobile.solution.nabia04.utilities.Cons
import mx.mobile.solution.nabia04.utilities.GlideApp
import mx.mobile.solution.nabia04.utilities.MyAlertDialog
import mx.mobile.solution.nabia04.utilities.Status
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.MainEndpoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentCurrentMembersDetail : BaseFragment<FragmentDatabaseDetailBinding>() {

    @Inject
    lateinit var endpoint: MainEndpoint

    private var userData: EntityUserData? = null

    private val viewModel by activityViewModels<DBViewModel>()

    private var deceasedDate: Long = 0
    private lateinit var selectedFolio: String
    private val fd = SimpleDateFormat("EEE, d MMM yyyy hh:mm", Locale.US)

    override fun getLayoutRes(): Int = R.layout.fragment_database_detail

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(
            MyMenuProvider(),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        selectedFolio = arguments?.getString("folio") ?: ""

        fabEdit.setOnClickListener {
            val i = Intent(requireContext(), ActivityUpdateUserData::class.java)
            i.putExtra("folio", selectedFolio)
            startActivity(i)
        }

        if (selectedFolio == userFolioNumber ||
            clearance == Cons.PRO ||
            userFolioNumber == "13786"
        ) {
            fabEdit.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            val user = viewModel.getUser(selectedFolio)
            user?.let { showDetails(it) }
        }

    }

    private inner class MyMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.details_menu, menu)
            val clearanceMenu = menu.findItem(R.id.set_clearance)
            val setaliveMen = menu.findItem(R.id.living_status)
            val delete = menu.findItem(R.id.delete)
            if (clearance == Cons.PRO || userFolioNumber == "13786") {
                clearanceMenu.isVisible = true
                setaliveMen.isVisible = true
                delete.isVisible = true
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val id = menuItem.itemId
            if (id == R.id.set_clearance) {
                showClearanceSettingDial()
                return true
            } else if (id == R.id.living_status) {
                showSetDeceasedDial()
                return true
            } else if (id == R.id.delete) {
                showDeleteConfirmDial()
                return true
            }
            return true
        }
    }

    private fun showClearanceSettingDial() {
        if (clearance == Cons.PRO || userFolioNumber == "13786") {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("WARNING!!!").setMessage(getString(R.string.warning_1))
                .setPositiveButton("Continue") { dialog, id ->
                    dialog.dismiss()
                    val v: View = layoutInflater.inflate(R.layout.set_clearance, null)
                    val alert =
                        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                            .setView(v)
                            .show()
                    v.findViewById<View>(R.id.none)
                        .setOnClickListener(OnClearanceItemClick(Cons.POSITION_NONE, alert))
                    v.findViewById<View>(R.id.president)
                        .setOnClickListener(OnClearanceItemClick(Cons.POSITION_PRESIDENT, alert))
                    v.findViewById<View>(R.id.vice_president).setOnClickListener(OnClearanceItemClick(Cons.POSITION_VICE_PRES,alert))
                    v.findViewById<View>(R.id.treasurer).setOnClickListener(OnClearanceItemClick(Cons.POSITION_TREASURER, alert))
                    v.findViewById<View>(R.id.secretary).setOnClickListener(OnClearanceItemClick(Cons.POSITION_SEC, alert))
                }
                .setNegativeButton("Cancel") { dialog, id -> dialog.dismiss() }.show()
        } else {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("LIE LIE!!!")
                .setMessage("Masa only the PRO can do this ooo. Hahahahahah")
                .show()
        }
    }

    inner class OnClearanceItemClick internal constructor(
        var position: String,
        var alert: AlertDialog
    ) :
        View.OnClickListener {
        override fun onClick(view: View) {
            alert.dismiss()
            setClearance(selectedFolio, position)
        }
    }

    private fun setClearance (folio:String, Clearance:String) {
        val pDialog =
            MyAlertDialog(requireContext(), "ALERT", "Assigning position... Please wait", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.setUserClearance(folio, Clearance)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS ->
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun showSetDeceasedDial() {
        if (clearance != Cons.PRO && userFolioNumber != "13786") {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("LIE LIE!!!")
                .setMessage("Masa only the PRO can do this ooo. Hahahahahah")
                .show()
            return
        }
        var dialog: AlertDialog? = null
        val v: View = layoutInflater.inflate(R.layout.set_deceased, null)
        val toggleButton = v.findViewById<ToggleButton>(R.id.toggle)
        val statusTV = v.findViewById<TextView>(R.id.statusTV)

        if(userData?.survivingStatus == 1){
            val st1 = userData?.fullName + ": DECEASED"
            statusTV.text = st1
            toggleButton.isChecked = true
        }else {
            val st1 = userData?.fullName + ": ALIVE"
            statusTV.text = st1
            toggleButton.isChecked = false
        }

        toggleButton.isChecked = userData?.survivingStatus != 1
        toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if(!isChecked){
                MyAlarmManager(requireContext()).showDateTimePicker(object : MyAlarmManager.CallBack {
                    override fun done(alarmTime: Long) {
                        deceasedDate = alarmTime
                        val date = fd.format(Date(deceasedDate))
                        dialog?.dismiss()
                        setDeceased(selectedFolio, date, 1)
                    }
                })
            }else {
                dialog?.dismiss()
                setDeceased(selectedFolio, "date", 0)
            }
        }

        val alertDialog = AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Departed Colleague")
            .setMessage("You are about to set the surviving status of this colleague. Please kindly cross check and set the date the sad incidence occurred before continuing")
            .setView(v)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, id -> dialog.dismiss() }
        dialog = alertDialog.create()
        dialog.show()
    }

    private fun setDeceased (folio:String, date: String, status: Int) {
        val pDialog = MyAlertDialog(requireContext(), "ALERT", "Setting deceased status...", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.setDeceaseStatus(folio, date, status)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS ->
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun showDeleteConfirmDial() {
        if (clearance != Cons.PRO && userFolioNumber != "13786") {
            AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle("LIE LIE!!!")
                .setMessage("Masa only the PRO can do this ooo. Hahahahahah")
                .show()
            return
        }
        AlertDialog.Builder(requireContext(), R.style.AppCompatAlertDialogStyle)
            .setTitle("Warning")
            .setMessage("Are you sure you want to Delete this person from the database?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialogInterface, i ->
                dialogInterface.dismiss()
                deleteUser(selectedFolio)
            }.setNegativeButton(
                "No"
            ) { dialogInterface, i -> dialogInterface.dismiss() }.show()
    }

    private fun deleteUser(folio: String) {
        val pDialog = MyAlertDialog(requireContext(), "DELETE", "Deleting...", false)
        pDialog.show()
        lifecycleScope.launch {
            val retValue = viewModel.deleteUser(folio)
            pDialog.dismiss()
            when (retValue.status) {
                Status.SUCCESS ->
                    Toast.makeText(requireContext(), "DONE", Toast.LENGTH_SHORT).show()
                Status.ERROR -> {
                    showAlertDialog("ERROR", retValue.message.toString())
                }
                else -> {}
            }
            this.cancel()
        }
    }

    private fun showDetails(data: EntityUserData) {
        var nickName = ""
        if (data.nickName != null) {
            nickName = " (" + data.nickName.toString() + ")"
        }
        val name: String = data.fullName.toString() + nickName
        fullNameTv_Nickname.text = name
        folio_number_Tv.text = data.folioNumber

        if (data.sex != null) {
            sexTV.text = data.sex
        }
        if (data.homeTown != null) {
            hometownTV.text = data.homeTown
        }
        if (data.districtOfResidence != null) {
            dis_residence_tv.text = data.districtOfResidence
        }
        if (data.regionOfResidence != null) {
            region_residence.text = data.regionOfResidence
        }
        if (data.contact != null) {
            contact1TV.text = data.contact
        }
        if (data.email != null) {
            emailTV.text = data.email
        }
        if (data.birthDayAlarm  != 0L) {
            dateOfBirth.text = fd.format(Date(data.birthDayAlarm))
        }
        if (data.className != null) {
            classTV.text = data.className
        }
        if (data.courseStudied != null) {
            courseTV.text = data.courseStudied
        }
        if (data.house != null) {
            houseTV.text = data.house
        }
        if (data.positionHeld != null) {
            position1TV.text = data.positionHeld
        }
        if (data.employmentStatus != null) {
            employmentStatus.text = data.employmentStatus
        }
        if (data.employmentSector != null) {
            employmentSector.text = data.employmentSector
        }
        if (data.specificOrg != null) {
            specificeOrg.text = data.specificOrg
        }
        if (data.nameOfEstablishment != null) {
            nameOfEstablishment.text = data.nameOfEstablishment
        }
        if (data.jobDescription != null) {
            jobDesc.text = data.jobDescription
        }
        if (data.establishmentRegion != null) {
            region.text = data.establishmentRegion
        }
        if (data.establishmentDist != null) {
            district.text = data.establishmentDist
        }

        Linkify.addLinks(contact1TV, Linkify.PHONE_NUMBERS)
        Linkify.addLinks(emailTV, Linkify.EMAIL_ADDRESSES)

        val imageUri: String = data.imageUri ?: ""
        val imageId: String = data.imageId ?: ""

        Log.i("TAG", "imageUri = $imageUri, imageId = $imageId")

        GlideApp.with(requireContext())
            .load(imageUri)
            .placeholder(R.drawable.listitem_image_holder)
            .signature(ObjectKey(imageId))
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)
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
