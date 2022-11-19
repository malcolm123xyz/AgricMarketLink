package mx.mobile.solution.nabia04_beta1.ui.pro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mx.mobile.solution.nabia04_beta1.R
import mx.mobile.solution.nabia04_beta1.databinding.FragmentProToolsBinding
import mx.mobile.solution.nabia04_beta1.ui.activities.ActivitySendAnnouncement
import mx.mobile.solution.nabia04_beta1.ui.activities.ActivityUpdateUserData
import mx.mobile.solution.nabia04_beta1.utilities.ExcelHelper
import javax.inject.Inject

@AndroidEntryPoint
class FragmentProTools : Fragment() {

    @Inject
    lateinit var excelHelper: ExcelHelper

    private var _binding: FragmentProToolsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendAnn.setOnClickListener {
            val intent = Intent(requireActivity(), ActivitySendAnnouncement::class.java)
            startActivity(intent)
        }
        binding.addNewMember.setOnClickListener {
            val intent = Intent(requireActivity(), ActivityUpdateUserData::class.java)
            intent.putExtra("folio", "")
            startActivity(intent)
        }
        binding.manageUsers.setOnClickListener { findNavController().navigate(R.id.action_move_to_manage_users) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
