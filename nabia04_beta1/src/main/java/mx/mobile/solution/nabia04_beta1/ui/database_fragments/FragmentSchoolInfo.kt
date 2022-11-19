package mx.mobile.solution.nabia04_beta1.ui.database_fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_school_info.*
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.data.entities.EntityUserData
import mx.mobile.solution.nabia04_beta1.data.view_models.DBUpdateViewModel
import mx.mobile.solution.nabia04_beta1.databinding.FragmentSchoolInfoBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.ActivityUpdateUserData.Companion.selectedFolio
import java.util.*

class FragmentSchoolInfo : Fragment() {

    private val updateModel by activityViewModels<DBUpdateViewModel>()

    private var updateObj: EntityUserData? = null

    private var _binding: FragmentSchoolInfoBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateModel.getDataToObserve(selectedFolio)
            .observe(viewLifecycleOwner) { userData: EntityUserData? ->

                if (userData != null) {
                    updateObj = userData

                    clsass_spinner.setSelection(
                        (clsass_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(updateObj?.className ?: "")
                    )
                    course_spinner.setSelection(
                        (course_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(updateObj?.courseStudied ?: "")
                    )
                    house_held_spinner.setSelection(
                        (house_held_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(updateObj?.house ?: "")
                    )
                    position_held_spinner.setSelection(
                        (position_held_spinner.adapter as ArrayAdapter<String>)
                            .getPosition(updateObj?.positionHeld ?: "")
                    )
                }

            }

        nextButtonSchoolD.setOnClickListener {
            onNext()
        }
        backButtonSchoolD.setOnClickListener {
            findNavController().navigate(R.id.action_school_info_move_back)
        }

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

    private fun onNext() {

        updateObj?.className = clsass_spinner.selectedItem.toString()
        updateObj?.courseStudied = course_spinner.selectedItem.toString()
        updateObj?.house = house_held_spinner.selectedItem.toString()
        updateObj?.positionHeld = position_held_spinner.selectedItem.toString()

        val errorStr = validateDataModel(updateObj)
        if (errorStr.isEmpty()) {
            updateObj?.let { updateModel.postData(it) }
            findNavController().navigate(R.id.action_school_info_move_forward)
        } else {
            warnAndSend(errorStr)
        }
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
            findNavController().navigate(R.id.action_school_info_move_forward)
        }
        dialog.setNegativeButton(
            "BACK"
        ) { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun validateDataModel(data: EntityUserData?): List<String> {
        val errorList: MutableList<String> = ArrayList()

        if (data?.className == null || data.className.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Class")
        }
        if (data?.courseStudied == null || data.courseStudied.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Course of study")
        }
        if (data?.house == null || data.house.lowercase(Locale.getDefault()).contains("select")) {
            errorList.add("Invalid House name")
        }
        if (data?.positionHeld == null || data.positionHeld.lowercase(Locale.getDefault())
                .contains("select")
        ) {
            errorList.add("Invalid Position held")
        }

        return errorList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}