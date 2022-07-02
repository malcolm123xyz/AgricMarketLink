package mx.mobile.solution.nabia04.ui.database_fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_person_details.*
import kotlinx.android.synthetic.main.fragment_school_info.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentSchoolInfoBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.DatabaseUpdateViewModel
import solutions.mobile.mx.malcolm1234xyz.com.mainEndpoint.model.DatabaseObject
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDone.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentSchoolInfo : BaseFragment<FragmentSchoolInfoBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_school_info

    private var userData: DatabaseObject? = null
    private var updateMode: DatabaseUpdateViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateMode = ViewModelProvider(requireActivity()).get(DatabaseUpdateViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateMode?.getValue()?.observe(viewLifecycleOwner) { updateModel: DatabaseObject? ->
            userData = updateModel

            val selIndex =

            clsass_spinner.setSelection((clsass_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.className ?: ""))
            course_spinner.setSelection((course_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.courseStudied ?: ""))
            house_held_spinner.setSelection((house_held_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.house ?: ""))
            position_held_spinner.setSelection((position_held_spinner.adapter as ArrayAdapter<String>)
                .getPosition(userData?.positionHeld ?: ""))
        }

        nextButtonSchoolD.setOnClickListener {
            onNext()
        }
        backButtonSchoolD.setOnClickListener {
            findNavController().navigate(R.id.action_school_info_move_back)
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

    fun onNext(){

        userData?.className = clsass_spinner.selectedItem.toString()
        userData?.courseStudied = course_spinner.selectedItem.toString()
        userData?.house = house_held_spinner.selectedItem.toString()
        userData?.positionHeld = position_held_spinner.selectedItem.toString()

        val errorStr = validateDataModel(userData)
        if (errorStr.isEmpty()) {
            updateMode?.setValue(userData!!)
            findNavController().navigate(R.id.action_school_info_move_forward)
        } else {
            warnAndSend(errorStr)
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
            updateMode?.setValue(userData!!)
            findNavController().navigate(R.id.action_school_info_move_forward)
        }
        dialog.setNegativeButton(
            "BACK"
        ) { d, id -> d.dismiss() }
        dialog.show()
    }

    private fun validateDataModel(data: DatabaseObject?): List<String> {
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
}