package mx.mobile.solution.nabia04.ui.treasurer

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentTreasurerToolsBinding
import mx.mobile.solution.nabia04.ui.BaseFragment

class FragmentTreasurerTools : BaseFragment<FragmentTreasurerToolsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_treasurer_tools

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb?.viewDuesPayment?.setOnClickListener { findNavController().navigate(R.id.action_move_to_dues_payment_view) }
        vb?.sendContRequest?.setOnClickListener { findNavController().navigate(R.id.action_move_cont_request) }
        vb?.updateCont?.setOnClickListener { findNavController().navigate(R.id.action_move_cont_update) }
        vb?.upDateDuesPayment?.setOnClickListener { findNavController().navigate(R.id.action_move_dues_update) }
    }
}
