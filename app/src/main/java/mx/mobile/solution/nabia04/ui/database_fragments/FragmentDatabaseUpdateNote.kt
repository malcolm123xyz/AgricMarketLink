package mx.mobile.solution.nabia04.ui.database_fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_database_update_note.*
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentDatabaseUpdateNoteBinding
import mx.mobile.solution.nabia04.ui.BaseFragment

class FragmentDatabaseUpdateNote : BaseFragment<FragmentDatabaseUpdateNoteBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_database_update_note

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nextButtonWelcome.setOnClickListener {
            findNavController().navigate(R.id.action_move_forward)
        }
        buttonCancel.setOnClickListener { requireActivity().finish() }

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
                    dialog.dismiss() }.show()

        }
    }
}