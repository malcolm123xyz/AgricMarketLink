package mx.mobile.solution.nabia04.main.fragments.database

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_database_update_note.*
import kotlinx.android.synthetic.main.fragment_database_update_note.buttonCancel
import kotlinx.android.synthetic.main.fragment_person_details.*
import kotlinx.android.synthetic.main.fragment_school_info.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDatabaseUpdateNoteBinding

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDatabaseUpdateNote.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentDatabaseUpdateNote : BaseFragment<FragmentDatabaseUpdateNoteBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_database_update_note

    override fun getCallBack(): OnBackPressedCallback = callback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nextButtonWelcome.setOnClickListener{
            findNavController().navigate(R.id.action_move_forward)
        }
        buttonCancel.setOnClickListener{ requireActivity().finish() }
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
}