package mx.mobile.solution.nabia04.ui.pro

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04.R
import mx.mobile.solution.nabia04.databinding.FragmentProToolsBinding
import mx.mobile.solution.nabia04.ui.BaseFragment
import mx.mobile.solution.nabia04.ui.activities.ActivitySendAnnouncement
import mx.mobile.solution.nabia04.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04.utilities.ExcelHelper
import javax.inject.Inject

@AndroidEntryPoint
class FragmentProTools : BaseFragment<FragmentProToolsBinding>() {

    override fun getLayoutRes(): Int = R.layout.fragment_pro_tools

    @Inject
    lateinit var excelHelper: ExcelHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb?.sendAnn?.setOnClickListener {
            val intent = Intent(requireActivity(), ActivitySendAnnouncement::class.java)
            startActivity(intent)
        }
        vb?.addNewMember?.setOnClickListener {
            val intent = Intent(requireActivity(), ActivityUpdateUserData::class.java)
            intent.putExtra("folio", "")
            startActivity(intent)
        }
        vb?.manageUsers?.setOnClickListener { findNavController().navigate(R.id.action_move_to_manage_users) }
    }


}
